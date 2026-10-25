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

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.yarina.domain.settings.GitCredentialsSettings;
import org.yarina.domain.vcs.VcsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * JGit transport credentials of a domain. For the {@code ssh} auth-type the Apache SSH
 * session factory is installed so OpenSSH private keys work; the previously installed
 * factory is remembered and restored on {@link #close()}, so a client of one domain never
 * leaves its key behind for another (JGit keeps the SSH factory in a static).
 */
public final class GitCredentials implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(GitCredentials.class);

    private final CredentialsProvider provider;
    private final SshSessionFactory previousSshSessionFactory;
    private final SshdSessionFactory ownSshSessionFactory;

    private GitCredentials(CredentialsProvider provider,
                           SshSessionFactory previousSshSessionFactory,
                           SshdSessionFactory ownSshSessionFactory) {
        this.provider = provider;
        this.previousSshSessionFactory = previousSshSessionFactory;
        this.ownSshSessionFactory = ownSshSessionFactory;
    }

    /**
     * Builds the JGit credentials of a domain from its declared settings. No credentials
     * means anonymous HTTP(S) access ({@code null} provider).
     */
    public static GitCredentials forDomain(GitCredentialsSettings settings) {
        if (settings == null || !settings.hasCredentials())
            return new GitCredentials(null, null, null);
        if (settings.isSsh()) {
            SshSessionFactory previous = SshSessionFactory.getInstance();
            SshdSessionFactory own = installSshFactory(settings.getSshPrivateKeyPath());
            logger.info("Installed SSH credentials for repository access (private key {})",
                    settings.getSshPrivateKeyPath());
            // SSH transport carries no HTTP Basic header; provide an empty provider.
            return new GitCredentials(new UsernamePasswordCredentialsProvider("", ""), previous, own);
        }
        if (settings.isBasic()) {
            logger.info("Using basic authentication credentials for repository access (user {})",
                    settings.getUsername());
            return new GitCredentials(new UsernamePasswordCredentialsProvider(
                    settings.getUsername(), settings.getPassword()), null, null);
        }
        // token
        logger.info("Using token authentication credentials for repository access");
        return new GitCredentials(new UsernamePasswordCredentialsProvider(
                settings.getUsername() == null ? "oauth2" : settings.getUsername(),
                settings.getToken()), null, null);
    }

    /** The JGit credentials to pass into transport operations; may be {@code null}. */
    public CredentialsProvider provider() {
        return provider;
    }

    /** Restores the SSH session factory that was installed before this account. */
    @Override
    public void close() {
        if (ownSshSessionFactory == null)
            return;
        SshSessionFactory.setInstance(previousSshSessionFactory);
        try {
            ownSshSessionFactory.close();
        } catch (Exception e) {
            // the factory is discarded anyway; nothing sensible to do here
            logger.debug("Cannot close the SSH session factory: {}", e.getMessage(), e);
        }
    }

    static SshdSessionFactory installSshFactory(String privateKeyPath) {
        File home = new File(System.getProperty("user.home", "."));
        File sshDir = resolveSshDir(home.toPath());
        Path knownHosts = sshDir.toPath().resolve("known_hosts");
        try {
            SshdSessionFactory factory = new SshdSessionFactoryBuilder()
                    .setHomeDirectory(home)
                    .setSshDirectory(sshDir)
                    .setPreferredAuthentications("publickey")
                    .setDefaultIdentities(dir -> Collections.singletonList(Path.of(privateKeyPath)))
                    .setServerKeyDatabase((h, dir) -> new AcceptAllServerKeyDatabase(knownHosts))
                    .build(new JGitKeyCache());
            SshSessionFactory.setInstance(factory);
            return factory;
        } catch (Exception e) {
            logger.error("Cannot create SSH session factory for key {}: {}", privateKeyPath, e.getMessage(), e);
            throw new VcsException("Cannot create SSH session factory for key " + privateKeyPath, e);
        }
    }

    /**
     * The {@code .ssh} directory JGit should use; created when missing. JGit's
     * {@link SshdSessionFactoryBuilder} derives the ssh directory from
     * {@code setSshDirectory}/{@code setHomeDirectory} and rejects {@code null}, so a real
     * directory is always returned (falling back to the temporary directory when
     * {@code ~/.ssh} cannot be created).
     */
    private static File resolveSshDir(Path home) {
        Path sshDir = home.resolve(".ssh");
        try {
            Files.createDirectories(sshDir);
            return sshDir.toFile();
        } catch (IOException e) {
            logger.warn("Cannot create the .ssh directory {}: {}, falling back to a temporary directory",
                    sshDir, e.getMessage(), e);
            try {
                return Files.createTempDirectory("yarina-ssh").toFile();
            } catch (IOException ex) {
                logger.error("Cannot create a temporary SSH directory: {}", ex.getMessage(), ex);
                throw new VcsException("Cannot prepare an SSH directory for the Git client", ex);
            }
        }
    }
}
