package org.lakehouse.ui.modeller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Declarative configuration matrix of the Modeller ({@code lakehouse.modeller.*}).
 * All workspace/storage/VCS/auth strategies are switched here.
 */
@ConfigurationProperties(prefix = "lakehouse.modeller")
public class ModellerProperties {

    private Storage storage = new Storage();

    /**
     * Git provider integration strategy: local-git | gitlab-api | github-app | gerrit-ssh
     */
    private String vcsProvider = "local-git";

    private Git git = new Git();

    private Github github = new Github();

    /**
     * Authorization mode: jwt-rbac | token-exchange
     */
    private String authStrategy = "jwt-rbac";

    private Session session = new Session();

    private SystemAccount vcsSystemAccount = new SystemAccount();

    private Logging logging = new Logging();

    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }
    public String getVcsProvider() { return vcsProvider; }
    public void setVcsProvider(String vcsProvider) { this.vcsProvider = vcsProvider; }
    public Git getGit() { return git; }
    public void setGit(Git git) { this.git = git; }
    public Github getGithub() { return github; }
    public void setGithub(Github github) { this.github = github; }
    public String getAuthStrategy() { return authStrategy; }
    public void setAuthStrategy(String authStrategy) { this.authStrategy = authStrategy; }
    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }
    public SystemAccount getVcsSystemAccount() { return vcsSystemAccount; }
    public void setVcsSystemAccount(SystemAccount vcsSystemAccount) { this.vcsSystemAccount = vcsSystemAccount; }
    public Logging getLogging() { return logging; }
    public void setLogging(Logging logging) { this.logging = logging; }

    public static class Storage {
        private String type = "local";
        private String rootDirectory = "/tmp/lakehouse-workspaces";
        private S3 s3 = new S3();
        private long cleanupTtlHours = 4;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getRootDirectory() { return rootDirectory; }
        public void setRootDirectory(String rootDirectory) { this.rootDirectory = rootDirectory; }
        public S3 getS3() { return s3; }
        public void setS3(S3 s3) { this.s3 = s3; }
        public long getCleanupTtlHours() { return cleanupTtlHours; }
        public void setCleanupTtlHours(long cleanupTtlHours) { this.cleanupTtlHours = cleanupTtlHours; }
    }

    public static class S3 {
        private String endpoint;
        private String bucket;
        private String accessKey;
        private String secretKey;
        private String region = "us-east-1";

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
    }

    public static class Git {
        private String remoteUrl;
        private String branchMain = "main";

        public String getRemoteUrl() { return remoteUrl; }
        public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }
        public String getBranchMain() { return branchMain; }
        public void setBranchMain(String branchMain) { this.branchMain = branchMain; }
    }

    public static class Github {
        private String appId;
        private String appPrivateKeyPath;
        private String installationId;

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAppPrivateKeyPath() { return appPrivateKeyPath; }
        public void setAppPrivateKeyPath(String appPrivateKeyPath) { this.appPrivateKeyPath = appPrivateKeyPath; }
        public String getInstallationId() { return installationId; }
        public void setInstallationId(String installationId) { this.installationId = installationId; }
    }

    /**
     * System technical account speaking to the central Git under one identity.
     * {@code auth-type}: ssh | token | basic.
     */
    public static class SystemAccount {
        private String authType = "token";
        private String sshPrivateKeyPath;
        private String username;
        private String token;
        private String password;

        public String getAuthType() { return authType; }
        public void setAuthType(String authType) { this.authType = authType; }
        public String getSshPrivateKeyPath() { return sshPrivateKeyPath; }
        public void setSshPrivateKeyPath(String sshPrivateKeyPath) { this.sshPrivateKeyPath = sshPrivateKeyPath; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public boolean isSsh() { return "ssh".equalsIgnoreCase(authType); }
        public boolean isToken() { return "token".equalsIgnoreCase(authType); }
        public boolean isBasic() { return "basic".equalsIgnoreCase(authType); }

        /** Effectively configured request credentials for HTTP(S) transports. */
        public String bearerOrBasic() {
            if (isToken())
                return token;
            if (isBasic())
                return username;
            return null;
        }

        public boolean hasCredentials() {
            return isSsh()
                    ? sshPrivateKeyPath != null && !sshPrivateKeyPath.isBlank()
                    : (token != null && !token.isBlank()) || (username != null && !username.isBlank());
        }

        public List<String> validate() {
            List<String> problems = new ArrayList<>();
            if (isSsh() && (sshPrivateKeyPath == null || sshPrivateKeyPath.isBlank()))
                problems.add("vcs-system-account.auth-type=ssh requires vcs-system-account.ssh-private-key-path");
            if (isToken() && (token == null || token.isBlank()))
                problems.add("vcs-system-account.auth-type=token requires vcs-system-account.token");
            if (isBasic()) {
                if (username == null || username.isBlank())
                    problems.add("vcs-system-account.auth-type=basic requires vcs-system-account.username");
                if (password == null || password.isBlank())
                    problems.add("vcs-system-account.auth-type=basic requires vcs-system-account.password");
            }
            return problems;
        }
    }

    /**
     * Browser session longevity: the SPA keeps the token alive (silent refresh) while
     * the user is active and ends the session only after this many minutes of no
     * user interaction.
     */
    public static class Session {
        private int inactivityMinutes = 30;

        public int getInactivityMinutes() {
            return inactivityMinutes;
        }

        public void setInactivityMinutes(int inactivityMinutes) {
            this.inactivityMinutes = inactivityMinutes;
        }
    }

    public static class Logging {
        private int syncLogCapacity = 500;

        public int getSyncLogCapacity() { return syncLogCapacity; }
        public void setSyncLogCapacity(int syncLogCapacity) { this.syncLogCapacity = syncLogCapacity; }
    }
}