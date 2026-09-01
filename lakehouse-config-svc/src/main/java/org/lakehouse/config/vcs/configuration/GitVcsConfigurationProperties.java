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

package org.lakehouse.config.vcs.configuration;

import org.lakehouse.common.ConfigurationPropertiesAbstract;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration of the Git configuration repository used by the VCS subsystem.
 * <p>
 * Prefix: {@code lakehouse.config.vcs}
 */
@Component
@ConfigurationProperties(prefix = "lakehouse.config.vcs")
public class GitVcsConfigurationProperties extends ConfigurationPropertiesAbstract {

    private Git git = new Git();

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public static class Git {

        private String repositoryUrl;
        private String branch;
        private String localClonePath;
        private String privateKeyPath;
        private Sync sync = new Sync();

        public String getRepositoryUrl() {
            return repositoryUrl;
        }

        public void setRepositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getLocalClonePath() {
            return localClonePath;
        }

        public void setLocalClonePath(String localClonePath) {
            this.localClonePath = localClonePath;
        }

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        public Sync getSync() {
            return sync;
        }

        public void setSync(Sync sync) {
            this.sync = sync;
        }
    }

    public static class Sync {

        private boolean enabled;
        private long intervalMs = 30000;
        private long initialDelayMs = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }
    }
}