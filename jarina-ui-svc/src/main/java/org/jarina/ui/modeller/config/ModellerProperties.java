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

package org.jarina.ui.modeller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Declarative configuration matrix of the Modeller ({@code jarina.modeller.*}).
 * Workspace/storage/auth/session strategies are switched here; the configuration domains
 * themselves are declared under {@code jarina.domains.*} and processed by
 * {@link org.jarina.domain.settings.DomainSettings}.
 */
@ConfigurationProperties(prefix = "jarina.modeller")
public class ModellerProperties {

    private Storage storage = new Storage();

    /**
     * Authorization mode: jwt-rbac | token-exchange
     */
    private String authStrategy = "jwt-rbac";

    private Session session = new Session();

    private Logging logging = new Logging();

    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }
    public String getAuthStrategy() { return authStrategy; }
    public void setAuthStrategy(String authStrategy) { this.authStrategy = authStrategy; }
    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }
    public Logging getLogging() { return logging; }
    public void setLogging(Logging logging) { this.logging = logging; }

    public static class Storage {
        private String type = "local";
        private String rootDirectory = "/tmp/jarina-workspaces";
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
