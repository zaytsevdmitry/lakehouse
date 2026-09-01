/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
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

package org.lakehouse.config.vcs.client;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.lakehouse.config.vcs.VcsChangeType;
import org.lakehouse.config.vcs.VcsClient;
import org.lakehouse.config.vcs.VcsClientException;
import org.lakehouse.config.vcs.VcsDiffEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Git based implementation of {@link VcsClient} built on top of JGit.
 * <p>
 * The client keeps a local clone owned by the service. {@code init()} clones the 
 * configured remote when no local clone exists, otherwise it just opens the local
 * repository. SSH transport is configured only when a private key path is provided.
 */
public class GitVcsClient implements VcsClient {

    private static final Logger logger = LoggerFactory.getLogger(GitVcsClient.class);

    private final String repositoryUrl;
    private final String branch;
    private final Path localClonePath;
    private final String privateKeyPath;

    private Git git;
    private Repository repository;

    public GitVcsClient(String repositoryUrl, String branch, String localClonePath, String privateKeyPath) {
        if (!StringUtils.hasText(repositoryUrl))
            throw new IllegalArgumentException("Git repository URL must be configured");
        if (!StringUtils.hasText(localClonePath))
            throw new IllegalArgumentException("Git local clone path must be configured");
        this.repositoryUrl = repositoryUrl;
        this.branch = StringUtils.hasText(branch) ? branch : "main";
        this.localClonePath = Paths.get(localClonePath).toAbsolutePath();
        this.privateKeyPath = privateKeyPath;
    }

    @Override
    public synchronized void init() {
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
            close();
            throw new VcsClientException("Cannot init VCS client for repository " + repositoryUrl, e);
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
            throw new VcsClientException("Cannot pull repository " + repositoryUrl, e);
        }
    }

    @Override
    public synchronized String getCurrentCommitId() {
        requireReady();
        try {
            ObjectId head = repository.resolve(remoteBranchRef());
            if (head == null)
                throw new VcsClientException("No reachable commit on branch " + branch);
            return head.name();
        } catch (IOException e) {
            throw new VcsClientException("Cannot resolve current commit on branch " + branch, e);
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
            throw new VcsClientException("Cannot compute diff against " + baseCommitId, e);
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
            throw new VcsClientException("Cannot read file " + path + " at commit " + commitId, e);
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
            throw new VcsClientException("VCS client is not initialized; call init() first");
    }

    private ObjectId getCurrentCommitObjectId() throws IOException {
        ObjectId head = repository.resolve(remoteBranchRef());
        if (head == null)
            throw new VcsClientException("No reachable commit on branch " + branch);
        return head;
    }

    private String remoteBranchRef() throws IOException {
        String remoteRef = Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + "/" + branch;
        if (repository.getRefDatabase().findRef(remoteRef) != null)
            return remoteRef;
        return Constants.R_HEADS + branch;
    }

    private void applySshSettings() {
        if (!StringUtils.hasText(privateKeyPath))
            return;
        Path key = Paths.get(privateKeyPath).toAbsolutePath();
        if (!Files.isReadable(key))
            throw new VcsClientException("SSH private key is not readable: " + key);
        try {
            Path home = Paths.get(System.getProperty("user.home", ".")).toAbsolutePath();
            SshdSessionFactory sshdFactory = new SshdSessionFactoryBuilder()
                    .setHomeDirectory(home.toFile())
                    .setSshDirectory(home.resolve(".ssh").toFile())
                    .setPreferredAuthentications("publickey")
                    .setDefaultIdentities(ignored -> List.of(key.toAbsolutePath()))
                    .build(new JGitKeyCache());
            SshSessionFactory.setInstance(sshdFactory);
        } catch (Exception e) {
            throw new VcsClientException("Cannot create SSH session factory for key " + key, e);
        }
    }

    private void close() {
        if (git != null) {
            git.close();
            git = null;
            repository = null;
        }
    }
}