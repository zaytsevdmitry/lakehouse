/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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

package org.jarina.domain.vcs.client;

import org.eclipse.jgit.internal.transport.sshd.OpenSshServerKeyDatabase;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.List;

/**
 * A {@link ServerKeyDatabase} that accepts every host key without any interaction, so
 * headless services never fail with "Server key did not validate" and never block on the
 * "Accept and store this key?" prompt. The accepted key is still recorded into
 * {@code known_hosts} (trust on first use) through the OpenSSH-backed database, so later
 * sessions reuse the cached entry.
 * <p>
 * This intentionally trades away host-key verification; it is meant for trusted,
 * isolated deployments (CI/containers) where the repository hosts are known.
 */
public class AcceptAllServerKeyDatabase implements ServerKeyDatabase {

    private static final Logger logger = LoggerFactory.getLogger(AcceptAllServerKeyDatabase.class);

    private final ServerKeyDatabase delegate;

    public AcceptAllServerKeyDatabase(Path knownHostsFile) {
        this.delegate = knownHostsFile == null
                ? null
                : new OpenSshServerKeyDatabase(false, List.of(knownHostsFile));
    }

    /**
     * @return an empty list so JGit always asks {@link #accept(...)} for unknown hosts
     *         instead of failing on the lookup
     */
    @Override
    public List<PublicKey> lookup(String connectAddress, InetSocketAddress remoteAddress,
                                  Configuration config) {
        if (delegate != null) {
            List<PublicKey> known = delegate.lookup(connectAddress, remoteAddress, config);
            if (known != null && !known.isEmpty())
                return known;
        }
        return List.of();
    }

    /**
     * @return always {@code true}: the host key is accepted without any prompt; the key is
     *         persisted into {@code known_hosts} when possible
     */
    @Override
    public boolean accept(String connectAddress, InetSocketAddress remoteAddress, PublicKey serverKey,
                          Configuration config, CredentialsProvider credentialsProvider) {
        if (delegate != null) {
            try {
                delegate.accept(connectAddress, remoteAddress, serverKey, config, credentialsProvider);
            } catch (Exception e) {
                logger.warn("Could not persist host key of {} into known_hosts: {}",
                        connectAddress, e.getMessage(), e);
            }
        }
        logger.info("Accepting SSH host key of {} ({}): {}",
                connectAddress, remoteAddress, serverKey.getAlgorithm());
        return true;
    }
}
