package org.lakehouse.modeller.config;

import org.lakehouse.modeller.auth.UserContextService;
import org.lakehouse.modeller.service.AdminWorkspaceService;
import org.lakehouse.modeller.service.AuthService;
import org.lakehouse.modeller.service.EditorService;
import org.lakehouse.modeller.service.ReviewService;
import org.lakehouse.modeller.service.SchemaService;
import org.lakehouse.modeller.service.SyncLogService;
import org.lakehouse.modeller.service.VcsService;
import org.lakehouse.modeller.service.YamlEditorService;
import org.lakehouse.modeller.storage.LocalFsWorkspaceStorage;
import org.lakehouse.modeller.storage.WorkspaceStorage;
import org.lakehouse.modeller.storage.s3.S3ObjectStorageClient;
import org.lakehouse.modeller.storage.s3.S3WorkspaceStorage;
import org.lakehouse.modeller.vcs.GitCredentials;
import org.lakehouse.modeller.vcs.VcsProvider;
import org.lakehouse.modeller.vcs.VcsProviderFactory;
import org.lakehouse.modeller.workspace.WorkspaceCleanupTask;
import org.lakehouse.modeller.workspace.WorkspaceManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Locale;

/**
 * Explicit wiring of the application-level strategies, all driven by the
 * {@code lakehouse.configurator.*} switchboard.
 */
@Configuration
public class ModellerConfiguration {

    // ------------------------------------------------------------------
    // storage
    // ------------------------------------------------------------------

    @Bean
    public WorkspaceStorage workspaceStorage(ConfiguratorProperties properties) {
        String type = properties.getStorage().getType();
        return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case "s3", "minio" -> {
                ConfiguratorProperties.S3 s3 = properties.getStorage().getS3();
                S3ObjectStorageClient client = new S3ObjectStorageClient(
                        s3.getEndpoint(), s3.getBucket(), s3.getAccessKey(), s3.getSecretKey(), s3.getRegion());
                yield new S3WorkspaceStorage(client);
            }
            case "local", "filesystem", "" -> new LocalFsWorkspaceStorage(properties.getStorage().getRootDirectory());
            default -> throw new IllegalArgumentException(
                    "Unsupported lakehouse.configurator.storage.type: " + type);
        };
    }

    // ------------------------------------------------------------------
    // VCS provider
    // ------------------------------------------------------------------

    @Bean
    public VcsProvider vcsProvider(ConfiguratorProperties properties) {
        var credentials = GitCredentials.forSystemAccount(properties.getVcsSystemAccount());
        return VcsProviderFactory.create(properties.getVcsProvider(), properties, credentials);
    }

    // ------------------------------------------------------------------
    // workspaces
    // ------------------------------------------------------------------

    @Bean
    public WorkspaceManager workspaceManager(WorkspaceStorage storage, VcsProvider vcs,
                                             ConfiguratorProperties properties) {
        int ttlHours = (int) Math.min(Integer.MAX_VALUE,
                Math.max(1, properties.getStorage().getCleanupTtlHours()));
        WorkspaceManager manager = new WorkspaceManager(storage, vcs::readBranchFiles, ttlHours);
        return manager;
    }

    @Bean
    public WorkspaceCleanupTask workspaceCleanupTask(WorkspaceManager manager) {
        return new WorkspaceCleanupTask(manager);
    }

    // ------------------------------------------------------------------
    // services
    // ------------------------------------------------------------------

    @Bean
    public UserContextService userContextService() {
        return new UserContextService();
    }

    @Bean
    public SyncLogService syncLogService(ConfiguratorProperties properties) {
        return new SyncLogService(properties.getLogging().getSyncLogCapacity());
    }

    @Bean
    public YamlEditorService yamlEditorService() {
        return new YamlEditorService();
    }

    @Bean
    public SchemaService schemaService() {
        return new SchemaService();
    }

    @Bean
    public EditorService editorService(WorkspaceManager manager, WorkspaceStorage storage,
                                       UserContextService users, YamlEditorService yaml, SyncLogService logs) {
        return new EditorService(manager, storage, users, yaml, logs);
    }

    @Bean
    public VcsService vcsService(WorkspaceManager manager, VcsProvider vcs, WorkspaceStorage storage,
                                 UserContextService users, SyncLogService logs) {
        return new VcsService(manager, vcs, storage, users, logs);
    }

    @Bean
    public ReviewService reviewService(WorkspaceManager manager, WorkspaceStorage storage, VcsProvider vcs,
                                       ConfiguratorProperties properties, UserContextService users, SyncLogService logs) {
        return new ReviewService(manager, storage, vcs, properties, users, logs);
    }

    @Bean
    public AuthService authService(ConfiguratorProperties properties, UserContextService users) {
        return new AuthService(properties, users);
    }

    @Bean
    public AdminWorkspaceService adminWorkspaceService(WorkspaceManager manager,
                                                       UserContextService users, SyncLogService logs) {
        return new AdminWorkspaceService(manager, users, logs);
    }

    // ------------------------------------------------------------------
    // JWT decoding (configurator-driven, with a fail-safe fallback decoder)
    // ------------------------------------------------------------------

    @Bean
    public JwtDecoder jwtDecoder(ConfiguratorProperties properties) {
        ConfiguratorProperties.Security.OAuth2.ResourceServer.Jwt jwt =
                properties.getSecurity().getOauth2().getResourceServer().getJwt();
        String jwkSetUri = jwt == null ? null : jwt.getJwkSetUri();
        String issuer = jwt == null ? null : jwt.getIssuerUri();
        if (isBlank(jwkSetUri) && !isBlank(issuer))
            jwkSetUri = issuer.replaceAll("/+$", "") + "/protocol/openid-connect/certs";
        if (!isBlank(jwkSetUri))
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        return token -> {
            throw new JwtException(
                    "JWT validation is not configured (set lakehouse.configurator.security.oauth2.resourceserver.jwt)");
        };
    }

    // ------------------------------------------------------------------
    // CORS for the SPA
    // ------------------------------------------------------------------

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}