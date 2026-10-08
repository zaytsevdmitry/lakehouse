```yaml
spring:
  datasource: # datasource where the service data will be stored. All user metadata configurations
    url: jdbc:postgresql://localhost:5432/postgresDB?ApplicationName=ConfigSVC
    username: postgresUser
    password: postgresPW
    driver-class-name: org.postgresql.Driver
  jpa:
    generate-ddl: true
    ddl-auto: create-drop
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
    hibernate:
      transaction:
        jta:
          platform: org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform
    properties:
      jakarta:
        persistence:
          create-database-schemas: true
      hibernate:
        default_schema: lakehouse_config #
        
lakehouse:
  config:
    produce: # Universal configuration change (outbox) notification sending settings
      delay-ms: 10000 # Delay between sends
      initial-delay-ms: 20000 # Delay of the first send on service startup
      limit: 100 # Limit of changes per one interval
      topic: configuration_changes # Topic name for sending configuration changes
      kafka:
        producer:
          properties: # https://kafka.apache.org/41/configuration/producer-configs/
            bootstrap.servers: localhost:9092

    vcs: # GitOps (VCS) subsystem: declarative configuration from per-domain Git repositories
      git:
        sync:
          enabled: ${LAKEHOUSE_CONFIG_GIT_SYNC_ENABLED:false} # enables the scheduler bean
          interval-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INTERVAL_MS:30000} # cycle period
          initial-delay-ms: ${LAKEHOUSE_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000} # delay of the first cycle
      domains: # one repository per domain; the domain list and names are user-defined
        <domain>: # any name; a domain can contain nested domains
          priority: 0 # apply order: ascending, parents before nested domains
          git:
            repository-url: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_URL:} # e.g. git://git-server:9418/<domain>.git
            branch: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_BRANCH:main} # branch to synchronize
            local-clone-path: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_CLONE_PATH:} # where the service keeps the clone
            private-key-path: ${LAKEHOUSE_CONFIG_GIT_<DOMAIN>_PRIVATE_KEY_PATH:} # SSH key, only for ssh:// URLs
          domains:
            <nested-domain>: # a nested sub-domain of the parent domain
              priority: 1
              git:
                repository-url: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_URL:}
                branch: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_BRANCH:main}
                local-clone-path: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_CLONE_PATH:}
                private-key-path: ${LAKEHOUSE_CONFIG_GIT_<NESTED_DOMAIN>_PRIVATE_KEY_PATH:}

  health: # Service health check endpoints
    liveness-path: /healthz # Liveness probe
    readiness-path: /readyz # Readiness probe
```

## Domains

`lakehouse.config.vcs` is bound by `LakehouseVCSProperties`; the `domains` map above is the only
place where repositories are declared. Each domain is one Git repository, is fetched and applied
on its own, and every construct loaded from it is stamped with `domainKeyName`. `local-clone-path`
must be unique per domain, and a domain without `git.repository-url` is skipped entirely.

When `domains` is empty and the legacy `lakehouse.config.vcs.git.repository-url` is set, that
single repository is exposed as one domain named `default`; as soon as `domains` has at least one
entry, the legacy block is ignored and does **not** become an additional `default` domain.

The synchronisation cycle itself is global, not per domain: `lakehouse.config.vcs.git.sync.*`
(enabled / interval-ms / initial-delay-ms). Full description of the tree, the apply order
(`priority`, parents before children), the isolation rules and the legacy block:
[Domains](../content_configuration/domains.md).