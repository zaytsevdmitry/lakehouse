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

package org.jarina.config.vcs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VCS synchronization settings of {@code jarina-config-svc}. The synchronization loop is
 * independent of the VCS backend, so it lives in its own {@code vcs.sync} block rather than
 * under the Git implementation. The domain parameters themselves are processed by
 * {@link org.jarina.domain.settings.DomainSettings} ({@code jarina.domains.*}).
 *
 * <pre>
 * jarina:
 *   config:
 *     vcs:
 *       sync:
 *         enabled: false
 *         interval-ms: 30000
 *         initial-delay-ms: 10000
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "jarina.config.vcs")
public class JarinaVCSProperties {

    private SyncProperties sync = new SyncProperties();

    public SyncProperties getSync() {
        return sync;
    }

    public void setSync(SyncProperties sync) {
        this.sync = sync;
    }

    public static class SyncProperties {

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
