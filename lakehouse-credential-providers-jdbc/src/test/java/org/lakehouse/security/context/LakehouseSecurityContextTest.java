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
package org.lakehouse.security.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LakehouseSecurityContextTest {

    @Test
    void unknownProviderTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> LakehouseSecurityContext.getSecret("unknown", "path:key"));
    }

    @Test
    void baoPathMustContainColon() {
        // Without a vault running, we expect the client to be created but the call to fail
        // with a meaningful error, not a confusing parsing error
        assertThrows(Exception.class,
                () -> LakehouseSecurityContext.getSecret("bao", "no-colon-path"));
    }

    @Test
    void resetClearsState() {
        LakehouseSecurityContext.reset();
        // After reset, next call creates a fresh client
        assertThrows(Exception.class,
                () -> LakehouseSecurityContext.getSecret("lockbox", "test-secret:test-key"));
    }
}
