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

package org.lakehouse.config.cvs;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Test fixture for a real Git repository standing in for the declarative configuration
 * repository. The fixture owns a bare remote and a producer work tree; commits made through
 * the producer are force pushed to the {@code main} branch of the bare remote, which is what
 * the CVS client under test clones and pulls.
 */
public final class TestGitRepository implements AutoCloseable {

    private static final PersonIdent IDENTITY = new PersonIdent("tester", "tester@example.com");

    private final Path remoteBare;
    private final Path clonePath;
    private final Git producer;

    private TestGitRepository(Path baseDir) throws IOException, GitAPIException, URISyntaxException {
        this.remoteBare = baseDir.resolve("remote");
        this.clonePath = baseDir.resolve("clone");
        Path producerDir = baseDir.resolve("producer");
        Files.createDirectories(producerDir);

        try (Git bare = Git.init().setBare(true).setDirectory(remoteBare.toFile()).call()) {
            // bare remote initialized
        }
        this.producer = Git.init().setDirectory(producerDir.toFile()).call();
        pointHeadAtMain();
        writeFile("README.md", "# Configuration repository\n");
        addAndCommit("README.md", "initial commit");
        producer.remoteAdd()
                .setName(Constants.DEFAULT_REMOTE_NAME)
                .setUri(new URIish(remoteBare.toUri().toString()))
                .call();
        pushForce();
    }

    public static TestGitRepository create(Path baseDir) {
        try {
            return new TestGitRepository(baseDir);
        } catch (IOException | GitAPIException | URISyntaxException e) {
            throw new IllegalStateException("Cannot create test git repository", e);
        }
    }

    public String bareUri() {
        return remoteBare.toUri().toString();
    }

    public Path clonePath() {
        return clonePath;
    }

    public String head() {
        try {
            return producer.getRepository().resolve(Constants.HEAD).name();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String commitFile(String path, String content, String message) {
        try {
            writeFile(path, content);
            addAndCommit(path, message);
            pushForce();
            return head();
        } catch (IOException | GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    public String commitAll(Map<String, String> files, String message) {
        try {
            for (Map.Entry<String, String> file : files.entrySet()) {
                writeFile(file.getKey(), file.getValue());
                producer.add().addFilepattern(file.getKey()).call();
            }
            commit(message);
            pushForce();
            return head();
        } catch (IOException | GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    public String deleteFile(String path, String message) {
        return deleteFiles(java.util.List.of(path), message);
    }

    public String deleteFiles(Iterable<String> paths, String message) {
        try {
            for (String path : paths)
                producer.rm().addFilepattern(path).call();
            commit(message);
            pushForce();
            return head();
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    public String moveFile(String from, String to, String message) {
        try {
            Path source = workTree().resolve(from);
            Path target = workTree().resolve(to);
            Files.createDirectories(target.getParent());
            Files.move(source, target);
            producer.rm().addFilepattern(from).call();
            producer.add().addFilepattern(to).call();
            commit(message);
            pushForce();
            return head();
        } catch (IOException | GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    private void addAndCommit(String path, String message) throws GitAPIException {
        producer.add().addFilepattern(path).call();
        commit(message);
    }

    private void commit(String message) throws GitAPIException {
        producer.commit()
                .setMessage(message)
                .setAuthor(IDENTITY)
                .setCommitter(IDENTITY)
                .call();
    }

    private void pushForce() throws GitAPIException {
        producer.push()
                .setRemote(Constants.DEFAULT_REMOTE_NAME)
                .setForce(true)
                .add(Constants.HEAD + ":" + Constants.R_HEADS + "main")
                .call();
    }

    private void writeFile(String path, String content) throws IOException {
        Path file = workTree().resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private Path workTree() {
        return producer.getRepository().getWorkTree().toPath();
    }

    private void pointHeadAtMain() throws IOException {
        Repository repository = producer.getRepository();
        RefUpdate refUpdate = repository.updateRef(Constants.HEAD);
        refUpdate.setForceUpdate(true);
        refUpdate.link(Constants.R_HEADS + "main");
    }

    @Override
    public void close() {
        producer.close();
    }
}