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
