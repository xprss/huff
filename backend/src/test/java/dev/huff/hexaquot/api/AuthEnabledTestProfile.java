package dev.huff.hexaquot.api;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class AuthEnabledTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "app.auth.enabled", "true",
            "quarkus.oidc.tenant-enabled", "false"
        );
    }
}
