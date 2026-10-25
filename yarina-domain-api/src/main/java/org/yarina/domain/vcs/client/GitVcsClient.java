/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yarina.domain.vcs.client;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.yarina.domain.layout.DomainLayout;
import org.yarina.domain.vcs.VcsChangeType;
import org.yarina.domain.vcs.VcsClient;
import org.yarina.domain.vcs.VcsDiffEntry;
import org.yarina.domain.vcs.VcsException;
import org.yarina.domain.vcs.VcsReviewResult;
import org.yarina.domain.vcs.VcsReviewSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Git based implementation of {@link VcsClient} built on top of JGit.
 * <p>
 * The client keeps a local clone owned by the service: {@code init()} clones the
 * configured remote when no local clone exists, otherwise it just opens the local
 * repository. SSH transport is configured only when a private key path is provided.
 * <p>
 * The client also serves review-style repository operations (branch reads, branch
 * creation, commit + push) against transient clones, so no Git archive is ever held
 * in the service-side workspace storage; the transport credentials come from the
 * constructor. Pushes go to {@code refs/heads/<branch>}.
 */
public class GitVcsClient implements VcsClient {

    private static final Logger logger = LoggerFactory.getLogger(GitVcsClient.class);

    public static final String COMMITTER_NAME = "yarina-modeller-svc";
    public static final String COMMITTER_EMAIL = "yarina-modeller-svc@yarina.local";

    private final String repositoryUrl;
    private final String branch;
    private final Path localClonePath;
    private final String privateKeyPath;
    private final CredentialsProvider credentials;
    private final GitCredentials gitCredentials;

    private Git git;
    private Repository repository;
    private SshSessionFactory previousSshSessionFactory;
    private SshdSessionFactory ownSshSessionFactory;
    private boolean sshSessionFactoryReplaced;

    /**
     * Configuration-side client: persistent local clone, credentials chosen by the
     * JGit transport settings ({@code privateKeyPath} for SSH).
     */
    public GitVcsClient(String repositoryUrl, String branch, String localClonePath, String privateKeyPath) {
        if (!StringUtils.hasText(repositoryUrl))
            throw new IllegalArgumentException("Git repository URL must be configured");
        if (!StringUtils.hasText(localClonePath))
            throw new IllegalArgumentException("Git local clone path must be configured");
        this.repositoryUrl = repositoryUrl;
        this.branch = StringUtils.hasText(branch) ? branch : "main";
        this.localClonePath = Paths.get(localClonePath).toAbsolutePath();
        this.privateKeyPath = privateKeyPath;
        this.credentials = null;
        this.gitCredentials = null;
    }

    /**
     * Review-side client over a single repository whose SSH credentials install/restore
     * the JGit SSH session factory for the lifetime of this client (see
     * {@link GitCredentials#close()}); serves branch reads, branch creation and pushes
     * through transient clones.
     */
    public GitVcsClient(String repositoryUrl, String branch, GitCredentials credentials) {
        if (!StringUtils.hasText(repositoryUrl))
            throw new IllegalArgumentException("Git repository URL must be configured");
        this.repositoryUrl = repositoryUrl;
        this.branch = StringUtils.hasText(branch) ? branch : "main";
        this.localClonePath = null;
        this.privateKeyPath = null;
        this.credentials = credentials == null ? null : credentials.provider();
        this.gitCredentials = credentials;
    }

    // ------------------------------------------------------------------
    // VcsClient: persistent clone pipeline (configuration-as-code reads)
    // ------------------------------------------------------------------

    @Override
    public synchronized void init() {
        if (localClonePath == null)
            throw new VcsException("VCS client has no local clone path; "
                    + "a review-side (provider mode) client cannot serve configuration reads");
        try {
            applySshSettings();
            if (Files.isDirectory(localClonePath.resolve(Constants.DOT_GIT))) {
                git = Git.open(localClonePath.toFile());
            } else {
                logger.info("Cloning configuration repository {} into {}", repositoryUrl, localClonePath);
                Files.createDirectories(localClonePath.getParent());
                git = Git.cloneRepository()
                        .setURI(repositoryUrl)
                        .setDirectory(localClonePath.toFile())
                        .setBranch(branch)
                        .call();
            }
            repository = git.getRepository();
            logger.info("Configuration repository opened at {}", localClonePath);
        } catch (Exception e) {
            logger.error("Cannot init VCS client for repository {}: {}", repositoryUrl, e.getMessage(), e);
            close();
            throw new VcsException("Cannot init VCS client for repository " + repositoryUrl, e);
        }
    }

    @Override
    public synchronized void pull() {
        requireReady();
        try {
            FetchCommand fetchCommand = git.fetch()
                    .setRemote(Constants.DEFAULT_REMOTE_NAME)
                    .setRefSpecs(new RefSpec("+refs/heads/*:refs/remotes/origin/*"))
                    .setRemoveDeletedRefs(true);
            fetchCommand.call();
            String ref = remoteBranchRef();
            ObjectId head = repository.resolve(ref);
            if (head == null) {
                logger.warn("Remote branch ref {} not found after fetch; branch may not exist", ref);
                return;
            }
            git.reset().setMode(ResetCommand.ResetType.HARD).setRef(head.name()).call();
            logger.info("Pulled {} to {}", ref, head.name());
        } catch (Exception e) {
            logger.error("Cannot pull repository {}: {}", repositoryUrl, e.getMessage(), e);
            throw new VcsException("Cannot pull repository " + repositoryUrl, e);
        }
    }

    @Override
    public synchronized String getCurrentCommitId() {
        requireReady();
        try {
            ObjectId head = repository.resolve(remoteBranchRef());
            if (head == null)
                throw new VcsException("No reachable commit on branch " + branch);
            return head.name();
        } catch (IOException e) {
            logger.error("Cannot resolve current commit on branch {} of {}: {}", branch, repositoryUrl, e.getMessage(), e);
            throw new VcsException("Cannot resolve current commit on branch " + branch, e);
        }
    }

    @Override
    public synchronized List<VcsDiffEntry> getDiff(String baseCommitId) {
        requireReady();
        try {
            if (!StringUtils.hasText(baseCommitId))
                return listAllFiles(getCurrentCommitObjectId());
            List<VcsDiffEntry> result = new ArrayList<>();
            try (DiffFormatter formatter = new DiffFormatter(OutputStream.nullOutputStream())) {
                formatter.setRepository(repository);
                formatter.setDetectRenames(true);
                for (DiffEntry entry : formatter.scan(ObjectId.fromString(baseCommitId), getCurrentCommitObjectId())) {
                    switch (entry.getChangeType()) {
                        case ADD -> addPath(result, entry.getNewPath(), VcsChangeType.CREATED);
                        case MODIFY, COPY -> addPath(result, entry.getNewPath(), VcsChangeType.UPDATED);
                        case DELETE -> addPath(result, entry.getOldPath(), VcsChangeType.DELETED);
                        case RENAME -> {
                            //Configuration constructs are identified by their content, so a rename is
                            //a removal of the old construct and the creation of a new one.
                            addPath(result, entry.getOldPath(), VcsChangeType.DELETED);
                            addPath(result, entry.getNewPath(), VcsChangeType.CREATED);
                        }
                        default -> logger.debug("Ignoring diff entry {} of type {}", entry, entry.getChangeType());
                    }
                }
            }
            return result;
        } catch (IOException e) {
            logger.error("Cannot compute diff against {} in {}: {}", baseCommitId, repositoryUrl, e.getMessage(), e);
            throw new VcsException("Cannot compute diff against " + baseCommitId, e);
        }
    }

    private List<VcsDiffEntry> listAllFiles(ObjectId commitId) throws IOException {
        List<VcsDiffEntry> result = new ArrayList<>();
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(commitId);
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);
                while (treeWalk.next())
                    result.add(new VcsDiffEntry(treeWalk.getPathString(), VcsChangeType.CREATED));
            }
        }
        return result;
    }

    @Override
    public synchronized Optional<String> readFileContent(String commitId, String path) {
        requireReady();
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(ObjectId.fromString(commitId));
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree())) {
                if (treeWalk == null)
                    return Optional.empty();
                ObjectId blobId = treeWalk.getObjectId(0);
                byte[] bytes = repository.open(blobId, Constants.OBJ_BLOB).getBytes();
                return Optional.of(new String(bytes, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.error("Cannot read file {} at commit {} in {}: {}",
                    path, commitId, repositoryUrl, e.getMessage(), e);
            throw new VcsException("Cannot read file " + path + " at commit " + commitId, e);
        }
    }

    private void addPath(List<VcsDiffEntry> result, String path, VcsChangeType type) {
        String normalized = normalizePath(path);
        if (normalized != null)
            result.add(new VcsDiffEntry(normalized, type));
    }

    private String normalizePath(String path) {
        if (path == null || DiffEntry.DEV_NULL.equals(path) || path.isBlank())
            return null;
        return path;
    }

    private void requireReady() {
        if (git == null || repository == null)
            throw new VcsException("VCS client is not initialized; call init() first");
    }

    private ObjectId getCurrentCommitObjectId() throws IOException {
        ObjectId head = repository.resolve(remoteBranchRef());
        if (head == null)
            throw new VcsException("No reachable commit on branch " + branch);
        return head;
    }

    private String remoteBranchRef() throws IOException {
        String remoteRef = Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/" + branch;
        if (repository.getRefDatabase().findRef(remoteRef) != null)
            return remoteRef;
        return Constants.R_HEADS + branch;
    }

    private void applySshSettings() {
        if (!StringUtils.hasText(privateKeyPath)) {
            return;
        }
        Path key = Paths.get(privateKeyPath).toAbsolutePath();
        if (!Files.isReadable(key))
            throw new VcsException("SSH private key is not readable: " + key);
        try {
            Path home = Paths.get(System.getProperty("user.home", ".")).toAbsolutePath();
            Path sshDir = home.resolve(".ssh");
            Files.createDirectories(sshDir);
            Path knownHosts = sshDir.resolve("known_hosts");
            SshdSessionFactory sshdFactory = new SshdSessionFactoryBuilder()
                    .setHomeDirectory(home.toFile())
                    .setSshDirectory(sshDir.toFile())
                    .setPreferredAuthentications("publickey")
                    .setDefaultIdentities(ignored -> List.of(key.toAbsolutePath()))
                    .setServerKeyDatabase((h, dir) -> new AcceptAllServerKeyDatabase(knownHosts))
                    .build(new JGitKeyCache());
            // JGit keeps the SSH factory in a static: the previously installed instance is
            // restored on close so a client of one domain never inherits the key of another.
            previousSshSessionFactory = SshSessionFactory.getInstance();
            SshSessionFactory.setInstance(sshdFactory);
            ownSshSessionFactory = sshdFactory;
            sshSessionFactoryReplaced = true;
        } catch (Exception e) {
            logger.error("Cannot create SSH session factory for key {}: {}", key, e.getMessage(), e);
            throw new VcsException("Cannot create SSH session factory for key " + key, e);
        }
    }

    @Override
    public synchronized void close() {
        if (git != null) {
            git.close();
            git = null;
            repository = null;
        }
        if (sshSessionFactoryReplaced) {
            SshSessionFactory.setInstance(previousSshSessionFactory);
            closeSshSessionFactory(ownSshSessionFactory);
            ownSshSessionFactory = null;
            sshSessionFactoryReplaced = false;
            previousSshSessionFactory = null;
        }
        if (gitCredentials != null)
            gitCredentials.close();
    }

    private void closeSshSessionFactory(SshdSessionFactory factory) {
        if (factory == null)
            return;
        try {
            factory.close();
        } catch (Exception e) {
            logger.warn("Cannot close SSH session factory of repository {}: {}", repositoryUrl, e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // review pipeline (transient clones)
    // ------------------------------------------------------------------

    @Override
    public Map<String, String> readBranchFiles(String branch) {
        return engineReadBranch(repositoryUrl, branch, credentials);
    }

    @Override
    public List<String> listBranches() {
        return engineListBranches(repositoryUrl, credentials);
    }

    @Override
    public void createBranch(String branch, String baseBranch) {
        String base = StringUtils.hasText(baseBranch) ? baseBranch : this.branch;
        engineCreateBranch(repositoryUrl, branch, base, credentials);
    }

    @Override
    public VcsReviewResult submitReview(VcsReviewSubmission submission, String authorName, String authorEmail) {
        PersonIdent author = new PersonIdent(effectiveAuthorName(authorName), effectiveAuthorEmail(authorEmail));
        if (!commitAndPush(submission, author))
            return VcsReviewResult.noChanges();
        return VcsReviewResult.plainPushed(repositoryUrl);
    }

    /**
     * Rewrites the working tree of the review branch with the submission files, commits
     * once (author = the given user, committer = the technical account) and pushes.
     *
     * @return false when the workspace equals the branch content (nothing to push)
     */
    boolean commitAndPush(VcsReviewSubmission submission, PersonIdent author) {
        return engineCommitAndPush(repositoryUrl, submission.domain(), submission.branch(),
                submission.effectiveCommitMessage(), author, submission.files(), credentials);
    }

    static String effectiveAuthorName(String name) {
        return name == null || name.isBlank() ? "anonymous" : name;
    }

    static String effectiveAuthorEmail(String email) {
        return email == null || email.isBlank() ? "anonymous@yarina.local" : email;
    }

    // ------------------------------------------------------------------
    // JGit engine (transient clones)
    // ------------------------------------------------------------------

    /**
     * Reads the YAML metadata files of a branch as {@code path -> content}.
     */
    private static Map<String, String> engineReadBranch(String remoteUrl, String branch,
                                                        CredentialsProvider credentials) {
        try (TransientRepo holder = openReadRepository(remoteUrl, credentials)) {
            if (!isRemoteUrl(remoteUrl))
                fetchIfRepositoriesLinked(holder.repo(), credentials);
            ObjectId tree = resolveTree(holder.repo(), branch);
            return readYamlTree(holder.repo(), tree);
        } catch (IOException | GitAPIException e) {
            logger.error("Cannot read branch {} of {}: {}", branch, remoteUrl, e.getMessage(), e);
            throw new VcsException("Cannot read branch " + branch + " of " + remoteUrl + ": " + e.getMessage(), e);
        }
    }

    /**
     * Lists the remote branches of the repository (short names, de-duplicated, sorted).
     */
    private static List<String> engineListBranches(String remoteUrl, CredentialsProvider credentials) {
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
            logger.error("Cannot list branches of {}: {}", remoteUrl, e.getMessage(), e);
            throw new VcsException("Cannot list branches of " + remoteUrl + ": " + e.getMessage(), e);
        }
        return branches.stream().distinct().sorted().toList();
    }

    /**
     * Forks {@code baseBranch} into {@code branch} at the same commit and pushes
     * {@code refs/heads/<branch>} to the remote.
     */
    private static void engineCreateBranch(String remoteUrl, String branch, String baseBranch,
                                           CredentialsProvider credentials) {
        Path dir = tempClone(remoteUrl, credentials);
        try {
            try (Git git = openClone(dir)) {
                Repository repo = git.getRepository();
                fetchIfRepositoriesLinked(repo, credentials);
                String start = resolveStartPoint(repo, baseBranch);
                git.checkout().setCreateBranch(true).setName(branch).setStartPoint(start).call();
                push(git, branch, credentials);
            }
        } catch (GitAPIException | IOException e) {
            logger.error("Cannot create branch {} of {}: {}", branch, remoteUrl, e.getMessage(), e);
            throw new VcsException("Cannot create branch " + branch + ": " + e.getMessage(), e);
        } finally {
            deleteRecursively(dir.toFile());
        }
    }

    /**
     * Rewrites the working tree of the branch with the given files, commits once (author =
     * the logged-in user, committer = the technical account) and pushes to the branch.
     */
    private static boolean engineCommitAndPush(String remoteUrl, String domain, String branch, String commitMessage,
                                               PersonIdent author, Map<String, String> files,
                                               CredentialsProvider credentials) {
        Path dir = tempClone(remoteUrl, credentials);
        try {
            try (Git git = openClone(dir)) {
                Repository repo = git.getRepository();
                fetchIfRepositoriesLinked(repo, credentials);
                checkoutReviewBranch(git, branch);
                boolean domainLayout = usesDomainLayout(dir);
                Map<String, String> repositoryFiles = DomainLayout.expandDomain(domain, files, domainLayout);
                removeManagedYamlFiles(dir, domain, domainLayout);
                writeFiles(dir, repositoryFiles);
                stageAll(git);
                if (git.status().call().isClean())
                    return false;
                git.commit()
                        .setMessage(commitMessage)
                        .setAuthor(author)
                        .setCommitter(new PersonIdent(COMMITTER_NAME, COMMITTER_EMAIL))
                        .call();
                push(git, branch, credentials);
                return true;
            }
        } catch (GitAPIException | IOException e) {
            logger.error("Cannot push workspace content to {}: {}", remoteUrl, e.getMessage(), e);
            throw new VcsException("Cannot push workspace content to " + remoteUrl + ": " + e.getMessage(), e);
        } finally {
            deleteRecursively(dir.toFile());
        }
    }

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
            Path dir = Files.createTempDirectory("yarina-modeller-repo-");
            final Git git;
            try {
                git = Git.cloneRepository()
                        .setURI(remoteUrl)
                        .setDirectory(dir.toFile())
                        .setCloneAllBranches(true)
                        .setCredentialsProvider(credentials)
                        .call();
            } catch (GitAPIException e) {
                logger.error("Cannot clone {} into {}: {}", remoteUrl, dir, e.getMessage(), e);
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
            logger.debug("No bare repository at {}: {}, retrying as a work tree",
                    target, bareFailure.getMessage(), bareFailure);
            return new FileRepositoryBuilder().setGitDir(new File(target, ".git")).setMustExist(true).build();
        }
    }

    private static File convertToFile(String remoteUrl) {
        String path = remoteUrl;
        if (path.startsWith("file://"))
            path = path.substring("file://".length());
        return new File(path);
    }

    private static void fetchIfRepositoriesLinked(Repository repo, CredentialsProvider credentials)
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

    private static ObjectId resolveTree(Repository repo, String branch) {
        List<String> candidates = List.of("refs/heads/" + branch, "refs/remotes/origin/" + branch);
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
                logger.debug("Cannot resolve candidate ref {} of branch {}: {}",
                        candidate, branch, ignored.getMessage(), ignored);
            }
        }
        throw new VcsException("Branch " + branch + " not found in the repository");
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

    private static Path tempClone(String remoteUrl, CredentialsProvider credentials) {
        try {
            Path dir = Files.createTempDirectory("yarina-modeller-repo-");
            Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(dir.toFile())
                    .setCloneAllBranches(true)
                    .setCredentialsProvider(credentials)
                    .call()
                    .close();
            return dir;
        } catch (GitAPIException | IOException e) {
            logger.error("Cannot clone {}: {}", remoteUrl, e.getMessage(), e);
            throw new VcsException("Cannot clone " + remoteUrl + ": " + e.getMessage(), e);
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

    private static void checkoutReviewBranch(Git git, String branch)
            throws GitAPIException, IOException {
        Repository repo = git.getRepository();
        if (repo.findRef("refs/heads/" + branch) != null) {
            git.checkout().setName(branch).call();
            return;
        }
        if (repo.findRef("refs/remotes/origin/" + branch) != null) {
            git.checkout().setCreateBranch(true).setName(branch)
                    .setStartPoint("refs/remotes/origin/" + branch).call();
            return;
        }
        throw new VcsException("Branch " + branch + " not found in the repository");
    }

    private static boolean usesDomainLayout(Path dir) throws IOException {
        Path domains = dir.resolve("domains");
        if (!Files.isDirectory(domains))
            return false;
        try (var children = Files.list(domains)) {
            return children.anyMatch(Files::isDirectory);
        }
    }

    private static void removeManagedYamlFiles(Path dir, String domain, boolean domainLayout) throws IOException {
        Path managedRoot = domainLayout ? dir.resolve("domains").resolve(domain) : dir;
        if (!Files.isDirectory(managedRoot))
            return;
        Path gitDir = dir.resolve(".git");
        try (var paths = Files.walk(managedRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !path.startsWith(gitDir))
                    .filter(GitVcsClient::isYaml)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            logger.error("Cannot remove managed file {}: {}", path, e.getMessage(), e);
                            throw new VcsException("Cannot remove managed file " + path, e);
                        }
                    });
        }
    }

    private static boolean isYaml(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

    /**
     * Stages the rewritten managed subtree: new and modified files first, then the
     * removals (JGit {@code setUpdate(true)} only refreshes already tracked paths,
     * so a single {@code add "."} would leave deleted files behind in the commit).
     */
    private static void stageAll(Git git) throws GitAPIException {
        git.add().addFilepattern(".").call();
        git.add().addFilepattern(".").setUpdate(true).call();
    }

    private static void writeFiles(Path dir, Map<String, String> files) throws IOException {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path target = dir.resolve(entry.getKey()).normalize();
            if (!target.startsWith(dir))
                throw new VcsException("Illegal file path in workspace: " + entry.getKey());
            Files.createDirectories(target.getParent() == null ? dir : target.getParent());
            Files.writeString(target, entry.getValue() == null ? "" : entry.getValue(), StandardCharsets.UTF_8);
        }
    }

    private static void push(Git git, String branch, CredentialsProvider credentials)
            throws GitAPIException {
        String destination = "refs/heads/" + branch;
        Iterable<org.eclipse.jgit.transport.PushResult> results = git.push()
                .setRemote("origin")
                .setRefSpecs(new RefSpec("HEAD:" + destination))
                .setCredentialsProvider(credentials)
                .call();
        for (org.eclipse.jgit.transport.PushResult result : results) {
            for (org.eclipse.jgit.transport.RemoteRefUpdate update : result.getRemoteUpdates()) {
                if (update.getStatus() != org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK
                        && update.getStatus() != org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE) {
                    throw new VcsException("Push to " + destination + " failed: " + update.getStatus()
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
