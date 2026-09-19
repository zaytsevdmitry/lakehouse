package org.lakehouse.ui.modeller.vcs;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.lakehouse.ui.modeller.config.ModellerProperties;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

/**
 * Builds the JGit transport credentials for the system technical account
 * ({@code lakehouse.modeller.vcs-system-account}), installing the Apache SSH session
 * factory so the {@code ssh} auth-type works through OpenSSH private keys.
 */
public final class GitCredentials {

    private GitCredentials() {
    }

    public static CredentialsProvider forSystemAccount(ModellerProperties.SystemAccount account) {
        if (account.isSsh() && account.getSshPrivateKeyPath() != null && !account.getSshPrivateKeyPath().isBlank()) {
            installSshFactory(account.getSshPrivateKeyPath());
            // SSH transport carries no HTTP Basic header; provide an empty provider.
            return new UsernamePasswordCredentialsProvider("", "");
        }
        if (account.isToken() && account.getToken() != null)
            return new UsernamePasswordCredentialsProvider(account.getUsername() == null
                    ? "oauth2" : account.getUsername(), account.getToken());
        if (account.isBasic())
            return new UsernamePasswordCredentialsProvider(account.getUsername(), account.getPassword());
        throw new VcsProviderException("vcs-system-account is not fully configured");
    }

    private static void installSshFactory(String privateKeyPath) {
        File home = new java.io.File(System.getProperty("user.home", "."));
        SshdSessionFactory factory = new SshdSessionFactoryBuilder()
                .setHomeDirectory(home)
                .setPreferredAuthentications("publickey")
                .setDefaultIdentities(sshDir -> Collections.singletonList(Path.of(privateKeyPath)))
                .build(null);
        SshSessionFactory.setInstance(factory);
    }
}