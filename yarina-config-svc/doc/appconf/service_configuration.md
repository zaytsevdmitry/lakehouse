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
        default_schema: yarina_config #
        
yarina:
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

    vcs: # GitOps (VCS) subsystem: the synchronization loop itself (global, not per domain)
      sync: # independent of the VCS backend, hence its own vcs.sync block
        enabled: ${YARINA_CONFIG_GIT_SYNC_ENABLED:false} # enables the scheduler bean
        interval-ms: ${YARINA_CONFIG_GIT_SYNC_INTERVAL_MS:30000} # cycle period
        initial-delay-ms: ${YARINA_CONFIG_GIT_SYNC_INITIAL_DELAY_MS:10000} # delay of the first cycle

  domains: # one repository per domain; the domain list and names are user-defined
    # bound by the shared DomainSettings of yarina-domain-api (namespace yarina.domains.*)
    <domain>: # any name; a domain can contain nested domains
      priority: 0 # apply order: ascending, parents before nested domains
      git:
        repository-url: ${YARINA_DOMAINS_<DOMAIN>_GIT_REPOSITORY_URL:} # e.g. git://git-server:9418/<domain>.git
        branch: ${YARINA_DOMAINS_<DOMAIN>_GIT_BRANCH:main} # branch to synchronize
        local-clone-path: ${YARINA_DOMAINS_<DOMAIN>_GIT_LOCAL_CLONE_PATH:} # where the service keeps the clone
        credentials: # per-domain, never merged; an empty block means anonymous access
          auth-type: ssh # optional: ssh | token | basic; inferred from the declared credentials
          ssh-private-key-path: /keys/<domain>_ed25519 # for auth-type == ssh
      domains:
        <nested-domain>: # a nested sub-domain of the parent domain
          priority: 1
          git:
            repository-url: ${YARINA_DOMAINS_<NESTED_DOMAIN>_GIT_REPOSITORY_URL:}
            branch: ${YARINA_DOMAINS_<NESTED_DOMAIN>_GIT_BRANCH:main}
            local-clone-path: ${YARINA_DOMAINS_<NESTED_DOMAIN>_GIT_LOCAL_CLONE_PATH:}
            credentials: # credentials may differ within one tree
              auth-type: token
              token: ${<NESTED_DOMAIN>_GIT_TOKEN:} # for auth-type == token

  health: # Service health check endpoints
    liveness-path: /healthz # Liveness probe
    readiness-path: /readyz # Readiness probe
```

## Domains

`yarina.domains` is bound by the shared `DomainSettings` of `yarina-domain-api`
(namespace `yarina.domains.<name>.*`; canonical reference:
[yarina-domain-api/doc/readme.md](../../../yarina-domain-api/doc/readme.md)); the `domains`
map above is the only place where repositories are declared. Each domain is one Git
repository, is fetched and applied on its own, and every construct loaded from it is
stamped with `domainKeyName`. `local-clone-path` must be unique per domain, and a domain
without `git.repository-url` is not active: this service skips it entirely, while
`yarina-ui-svc` fails when such a domain is used.

The synchronisation cycle itself is global, not per domain: `yarina.config.vcs.sync.*`
(enabled / interval-ms / initial-delay-ms). Full description of the tree, the apply order
(`priority`, parents before children), the isolation rules and the observation of which
domain a change came from: [Domains](../content_configuration/domains.md).

## Git access

Every domain repository is accessed through a single Git integration: a local clone per
domain maintained via JGit, synchronized on every cycle. The `repository-url` accepts any
Git form - `git://`, `ssh://`, `http(s)://` or a local path.

Authentication is declared **per domain** in `yarina.domains.<name>.git.credentials`:
`token` sends the token as the HTTP password, `basic` sends HTTP Basic credentials, `ssh`
uses the domain's `ssh-private-key-path`. There is no global/system account and no merge:
a domain without credentials is reached anonymously, and different domains may
authenticate with different accounts. Full setup:
[Git extension user guide](../vcs/git_extension_user_guide.md).