package org.lakehouse.ui.modeller.vcs;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared JGit engine for all providers: reads a branch's tracked YAML files as a flat
 * map, creates new branches, and commits/pushes a workspace file set with a user author
 * and the fixed technical committer. All operations run against transient clones, so no
 * Git archive is ever held in the server-side workspace storage.
 */
public final class GitRepositoryOps {

    public static final String COMMITTER_NAME = "lakehouse-modeller-svc";
    public static final String COMMITTER_EMAIL = "lakehouse-modeller-svc@lakehouse.local";

    private GitRepositoryOps() {
    }

    /**
     * Reads the YAML metadata files of a branch as {@code path -> content}.
     *
     * @param remoteUrl  repository to read from (remote URL or local/git path, may be bare)
     */
    public static Map<String, String> readBranch(String remoteUrl, String branch, String defaultBranch,
                                                 CredentialsProvider credentials) {
        try (TransientRepo holder = openReadRepository(remoteUrl, credentials)) {
            if (!isRemoteUrl(remoteUrl))
                fetchIfRepositoriesLinked(holder.repo(), defaultBranch, credentials);
            ObjectId tree = resolveTree(holder.repo(), branch, defaultBranch);
            return readYamlTree(holder.repo(), tree);
        } catch (IOException | GitAPIException e) {
            throw new VcsProviderException("Cannot read branch " + branch + " of " + remoteUrl + ": " + e.getMessage(), e);
        }
    }

    /**
     * Lists the remote branches of the repository (short names, de-duplicated, sorted).
     */
    public static List<String> listBranches(String remoteUrl, CredentialsProvider credentials) {
        List<String> branches = new ArrayList<>();
        try (TransientRepo holder = openReadRepository(remoteUrl, credentials)) {
            Repository repo = holder.repo();
            if (!isRemoteUrl(remoteUrl)) {
                if (repo.findRef(Constants.HEAD) == null) {
                    try (Git git = new Git(repo)) {
                        git.fetch().setRemote("origin")
                                .setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*"))
                                .setCredentialsProvider(credentials)
                                .call();
                    }
                }
            }
            for (Ref ref : repo.getRefDatabase().getRefsByPrefix("refs/remotes/origin/")) {
                String name = ref.getName().substring("refs/remotes/origin/".length());
                if (!name.equals("HEAD"))
                    branches.add(name);
            }
            if (branches.isEmpty()) {
                for (Ref ref : repo.getRefDatabase().getRefsByPrefix("refs/heads/"))
                    branches.add(ref.getName().substring("refs/heads/".length()));
            }
        } catch (IOException | GitAPIException e) {
            throw new VcsProviderException("Cannot list branches of " + remoteUrl + ": " + e.getMessage(), e);
        }
        return branches.stream().distinct().sorted().toList();
    }

    /**
     * Forks {@code baseBranch} into {@code branch} at the same commit and pushes
     * {@code refs/heads/<branch>} to the remote.
     */
    public static void createBranch(String remoteUrl, String branch, String baseBranch,
                                    CredentialsProvider credentials) {
        Path dir = tempClone(remoteUrl, credentials);
        try {
            try (Git git = openClone(dir)) {
                Repository repo = git.getRepository();
                fetchIfRepositoriesLinked(repo, baseBranch, credentials);
                String start = resolveStartPoint(repo, baseBranch);
                git.checkout().setCreateBranch(true).setName(branch).setStartPoint(start).call();
                push(git, branch, false, credentials);
            }
        } catch (GitAPIException | IOException e) {
            throw new VcsProviderException("Cannot create branch " + branch + ": " + e.getMessage(), e);
        } finally {
            deleteRecursively(dir.toFile());
        }
    }

    /**
     * Rewrites the working tree of the branch with the given files, commits once (author =
     * the logged-in user, committer = the technical account) and pushes to the branch
     * (or the Gerrit {@code refs/for/<branch>} magic ref when {@code gerrit=true}).
     */
    public static void commitAndPush(String remoteUrl, String branch, String baseBranch, String commitMessage,
                                     PersonIdent author, Map<String, String> files,
                                     boolean gerrit, CredentialsProvider credentials) {
        Path dir = tempClone(remoteUrl, credentials);
        try {
            try (Git git = openClone(dir)) {
                Repository repo = git.getRepository();
                fetchIfRepositoriesLinked(repo, baseBranch, credentials);
                checkoutReviewBranch(git, repo, branch, baseBranch);
                clearWorkTree(dir);
                writeFiles(dir, files);
                git.add().addFilepattern(".").call();
                git.commit()
                        .setMessage(commitMessage)
                        .setAuthor(author)
                        .setCommitter(new PersonIdent(COMMITTER_NAME, COMMITTER_EMAIL))
                        .call();
                push(git, branch, gerrit, credentials);
            }
        } catch (GitAPIException | IOException e) {
            throw new VcsProviderException("Cannot push workspace content to " + remoteUrl + ": " + e.getMessage(), e);
        } finally {
            deleteRecursively(dir.toFile());
        }
    }

    // ------------------------------------------------------------------
    // reading
    // ------------------------------------------------------------------

    /**
     * Repository plus optional transient clone directory that is cleaned up on close.
     */
    private static final class TransientRepo implements AutoCloseable {
        private final Repository repo;
        private final Path dir;

        private TransientRepo(Repository repo, Path dir) {
            this.repo = repo;
            this.dir = dir;
        }

        private Repository repo() {
            return repo;
        }

        @Override
        public void close() {
            repo.close();
            if (dir != null)
                deleteRecursively(dir.toFile());
        }
    }

    private static TransientRepo openReadRepository(String remoteUrl, CredentialsProvider credentials)
            throws IOException, GitAPIException {
        if (isRemoteUrl(remoteUrl)) {
            Path dir = Files.createTempDirectory("lakehouse-modeller-repo-");
            final Git git;
            try {
                git = Git.cloneRepository()
                        .setURI(remoteUrl)
                        .setDirectory(dir.toFile())
                        .setCloneAllBranches(true)
                        .setCredentialsProvider(credentials)
                        .call();
            } catch (GitAPIException e) {
                deleteRecursively(dir.toFile());
                throw e;
            }
            return new TransientRepo(git.getRepository(), dir);
        }
        return new TransientRepo(openRepository(remoteUrl), null);
    }

    private static boolean isRemoteUrl(String url) {
        // scheme://host/path (git://, http(s)://, ssh://, file:// only when scheme is file)
        if (url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))
            return !url.startsWith("file://");
        // scp-like syntax: user@host:path
        return url.matches("^[^/@]+@[^:]+:.*");
    }

    private static Repository openRepository(String remoteUrl) throws IOException {
        File target = convertToFile(remoteUrl);
        try {
            return new FileRepositoryBuilder().setGitDir(target).setMustExist(true).build();
        } catch (IOException bareFailure) {
            return new FileRepositoryBuilder().setGitDir(new File(target, ".git")).setMustExist(true).build();
        }
    }

    private static File convertToFile(String remoteUrl) {
        String path = remoteUrl;
        if (path.startsWith("file://"))
            path = path.substring("file://".length());
        return new File(path);
    }

    private static void fetchIfRepositoriesLinked(Repository repo, String defaultBranch, CredentialsProvider credentials)
            throws GitAPIException, IOException {
        Ref head = repo.findRef(Constants.HEAD);
        if (head == null) {
            try (Git git = new Git(repo)) {
                git.fetch().setRemote("origin")
                        .setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*"))
                        .setCredentialsProvider(credentials)
                        .call();
            }
        }
    }

    private static ObjectId resolveTree(Repository repo, String branch, String defaultBranch) {
        List<String> candidates = new ArrayList<>();
        candidates.add("refs/heads/" + branch);
        candidates.add("refs/remotes/origin/" + branch);
        if (!branch.equals(defaultBranch)) {
            candidates.add("refs/heads/" + defaultBranch);
            candidates.add("refs/remotes/origin/" + defaultBranch);
        }
        candidates.add(Constants.HEAD);
        for (String candidate : candidates) {
            try {
                Ref ref = repo.findRef(candidate);
                if (ref == null)
                    continue;
                ObjectId id = ref.getPeeledObjectId() != null ? ref.getPeeledObjectId() : ref.getObjectId();
                if (id != null) {
                    ObjectId tree = repo.resolve(id.getName() + "^{tree}");
                    if (tree != null)
                        return tree;
                }
            } catch (IOException ignored) {
                // try the next candidate
            }
        }
        throw new VcsProviderException("Branch " + branch + " not found in the repository");
    }

    private static Map<String, String> readYamlTree(Repository repo, ObjectId treeId) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        try (TreeWalk walk = new TreeWalk(repo)) {
            walk.addTree(treeId);
            walk.setRecursive(true);
            while (walk.next()) {
                String path = walk.getPathString();
                if (!path.endsWith(".yaml") && !path.endsWith(".yml"))
                    continue;
                ObjectId blob = walk.getObjectId(0);
                byte[] bytes = repo.open(blob).getBytes();
                files.put(path, new String(bytes, StandardCharsets.UTF_8));
            }
        }
        return files;
    }

    // ------------------------------------------------------------------
    // writing
    // ------------------------------------------------------------------

    private static Path tempClone(String remoteUrl, CredentialsProvider credentials) {
        try {
            Path dir = Files.createTempDirectory("lakehouse-modeller-repo-");
            Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(dir.toFile())
                    .setCloneAllBranches(true)
                    .setCredentialsProvider(credentials)
                    .call()
                    .close();
            return dir;
        } catch (GitAPIException | IOException e) {
            throw new VcsProviderException("Cannot clone " + remoteUrl + ": " + e.getMessage(), e);
        }
    }

    private static Git openClone(Path dir) throws IOException {
        return Git.open(dir.toFile());
    }

    private static String resolveStartPoint(Repository repo, String baseBranch) throws IOException {
        if (baseBranch != null) {
            if (repo.findRef("refs/remotes/origin/" + baseBranch) != null)
                return "refs/remotes/origin/" + baseBranch;
            if (repo.findRef("refs/heads/" + baseBranch) != null)
                return "refs/heads/" + baseBranch;
        }
        return Constants.HEAD;
    }

    private static void checkoutReviewBranch(Git git, Repository repo, String branch, String baseBranch)
            throws GitAPIException, IOException {
        // The transient clone checks out a local default branch; pushing to that same
        // branch (e.g. "main") must reuse it instead of re-creating it.
        if (repo.findRef("refs/heads/" + branch) != null) {
            git.checkout().setName(branch).call();
            return;
        }
        if (repo.findRef("refs/remotes/origin/" + branch) != null) {
            git.checkout().setCreateBranch(true).setName(branch)
                    .setStartPoint("refs/remotes/origin/" + branch).call();
        } else {
            git.checkout().setCreateBranch(true).setName(branch)
                    .setStartPoint(resolveStartPoint(repo, baseBranch)).call();
        }
    }

    private static void clearWorkTree(Path dir) throws IOException {
        File[] children = dir.toFile().listFiles();
        if (children == null)
            return;
        for (File child : children) {
            if (!".git".equals(child.getName()))
                deleteRecursively(child);
        }
    }

    private static void writeFiles(Path dir, Map<String, String> files) throws IOException {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path target = dir.resolve(entry.getKey()).normalize();
            if (!target.startsWith(dir))
                throw new VcsProviderException("Illegal file path in workspace: " + entry.getKey());
            Files.createDirectories(target.getParent() == null ? dir : target.getParent());
            Files.writeString(target, entry.getValue() == null ? "" : entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    private static void push(Git git, String branch, boolean gerrit, CredentialsProvider credentials)
            throws GitAPIException {
        String destination = gerrit ? "refs/for/" + branch : "refs/heads/" + branch;
        Iterable<org.eclipse.jgit.transport.PushResult> results = git.push()
                .setRemote("origin")
                .setRefSpecs(new RefSpec("HEAD:" + destination))
                .setCredentialsProvider(credentials)
                .call();
        for (org.eclipse.jgit.transport.PushResult result : results) {
            for (org.eclipse.jgit.transport.RemoteRefUpdate update : result.getRemoteUpdates()) {
                if (update.getStatus() != org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK
                        && update.getStatus() != org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE) {
                    throw new VcsProviderException("Push to " + destination + " failed: " + update.getStatus()
                            + (update.getMessage() == null ? "" : " (" + update.getMessage() + ")"));
                }
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists())
            return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children)
                deleteRecursively(child);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}