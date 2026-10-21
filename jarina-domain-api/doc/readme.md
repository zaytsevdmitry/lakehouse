# Configuration domains (`jarina-domain-api`)

This document is the **canonical description of the shared domain model** of the Jarina
platform. It is owned by the module `jarina-domain-api`, which every service that deals
with configuration domains consumes (`jarina-config-svc`, `jarina-ui-svc`).

The module holds two things:

- the **domain settings model** — package `org.jarina.domain.settings`: `DomainSettings`
  (`@ConfigurationProperties(prefix = "jarina")`, i.e. the namespace
  `jarina.domains.<name>.*`), `GitProperties` and `GitCredentialsSettings`. Parsing,
  canonicalization, ordering and validation of domain parameters live here;
- the **VCS layer** — package `org.jarina.domain.vcs`: the transport-agnostic `VcsClient`
  interface, its JGit implementation `GitVcsClient`, the credentials
  (`GitCredentials`) and `DomainVcsSpec`/`DomainVcsFactory`.

The services only **transport the raw `jarina.domains.*` properties** and call the shared
`DomainSettings` functions; they do not re-implement any resolution rule. The service-side
concerns (the GitOps sync cycle of `jarina-config-svc`, the branch panel / workspace /
review pipeline of `jarina-ui-svc`) are described in
[config-svc: Domains](../../jarina-config-svc/doc/content_configuration/domains.md) and in
[ui-svc](../../jarina-ui-svc/doc/readme.md#domains).

## 1. What a configuration domain is

A **domain** is the unit of isolation of the configuration. A domain is at the same time a
logical scope for metadata and a **Git repository**: every domain owns exactly one
repository, is fetched and applied on its own, and is stamped onto every construct loaded
from it as `domainKeyName`.

```
jarina.domains  ──▶  DomainSettings.orderedDomains()  ──▶  for each domain
                                                                fetch + diff
                                                                apply in one txn
                                                                stamp domainKeyName
                                                                    │
                                                                    ▼
                                         DataSet / DataSource / Driver / Task /
                                         ScenarioActTemplate / Schedule /
                                         QualityMetricsConf / TaskExecutionServiceGroup
                                         (ownership = domainKeyName, identity = keyName)
```

Rules that follow from this definition:

- The domain is **not** part of the YAML content. A construct file never carries a domain
  field: the domain is derived from the repository the file is loaded from and always wins
  over anything stored in the parsed DTO.
- The domain is an **ownership** attribute, not part of the identity. `keyName` stays
  **global** — it is the sole primary key (`KeyEntityAbstract.keyName`, `@Id`), unique
  across the whole configuration and *not* part of a composite key with `domainKeyName`.
  The same `keyName` therefore cannot exist in two domains: to describe the same physical
  table in two domains you need two distinct `keyName`s.
- The domain decides which filters and isolation rules apply to a construct (see
  [§7](#7-isolation-rules)) and how it is reported — never whether it collides with
  another construct.
- **Scripts are shared.** Scripts (`kind: Script` / `SQLTemplate`) are content-only and
  shared between domains by key; they are never stamped with a domain. The domain stored
  on the `SQLTemplate` row is derived from the `Driver` or `Task` that references it.
- A construct created through the REST API has no domain (`domainKeyName = null`).

Domain names are canonicalized by `DomainSettings.canonicalDomain(...)`: trimmed and
lower-cased. Two configured domains that collide case-insensitively are a start-up error.

## 2. The domain tree, ordering and failure propagation

Domains form a tree — a domain may declare nested domains via
`jarina.domains.<name>.domains`, for example `platform` ⊃ `processing` ⊃ `analytics`.
The resolution lives in `DomainSettings`:

| Function | Meaning |
|---|---|
| `rootDomains()` | Top-level domains sorted by `priority` ascending |
| `nestedDomains(parent)` | The nested sub-domains of one domain, sorted by `priority` ascending |
| `orderedDomains()` | Flat, dependency-ordered list: **parent before its children**, depth-first, each level sorted by `priority` |
| `domainSettings(name)` | Per-domain settings; the lookup descends the **whole** tree, so nested domains resolve by their leaf name too; `null` for an unknown domain |
| `hasRepository(domain)` / `branch(domain)` / `buildSpec(domain, …)` | Derived per-domain facts used by the services |

Ordering: within one level the domains are sorted by `priority` **ascending**; a domain
without a `priority` is treated as `Integer.MAX_VALUE` and therefore goes **last**; equal
priorities are broken by domain name.

Nesting is a **configuration** relation, not a storage one. The same three repositories
may be declared as siblings or as a chain — it is the administrator's choice. The only
behavioural consequence of nesting is **failure propagation**: when the synchronization of
a domain fails, its whole sub-tree is skipped until the parent succeeds again, because a
child may reference constructs of its parent.

## 3. A domain is active iff `git.repository-url` is set

A domain without `git.repository-url` (blank/absent) is **inactive** and never produces a
VCS client:

- `jarina-config-svc` skips it on every sync cycle (its content, if any, is not
  synchronized);
- `jarina-ui-svc` fails **on use**: any attempt to build the client of such a domain
  surfaces `Domain '<name>': no repository URL configured (property:
  jarina.domains.<name>.git.repository-url)`.

There is **no legacy single-repository fallback**: the pre-domains properties and the
domain `default` that used to be synthesized from a single repository are gone in both
services. Declare every repository explicitly under `jarina.domains.<name>.git.repository-url`.

## 4. Configuration namespace `jarina.domains.<name>.*`

`DomainSettings` binds the whole tree; the services deliver the raw properties unchanged.

```yaml
jarina:
  domains:
    platform:
      priority: 0
      git:
        repository-url: git://git-server:9418/platform.git
        branch: main
        local-clone-path: /tmp/platform
        credentials:                    # per-domain, never merged with another domain
          auth-type: ssh                # ssh | token | basic (may be inferred)
          ssh-private-key-path: /keys/platform_ed25519
      domains:                          # nested sub-domains
        processing:
          priority: 1
          git:
            repository-url: git://git-server:9418/processing.git
            branch: main
            local-clone-path: /tmp/processing
            credentials:
              auth-type: token
              token: ${PROCESSING_GIT_TOKEN:}
    analytics:
      git:
        repository-url: git://git-server:9418/analytics.git
```

| Property | Default | Meaning |
|---|---|---|
| `jarina.domains.<name>.priority` | `Integer.MAX_VALUE` | Position within one level of the tree (ascending); absent = last |
| `jarina.domains.<name>.git.repository-url` | *(empty)* | Repository of the domain: `git://`, `ssh://`, `http(s)://` or a local path. Empty means the domain is inactive |
| `jarina.domains.<name>.git.branch` | `main` | Branch to track; `DomainSettings.branch(...)` returns `main` for an unknown domain |
| `jarina.domains.<name>.git.local-clone-path` | *(empty)* | Local clone owned by the service for this domain; must be unique per domain. `jarina-config-svc` **requires** it (`buildSpec(domain, true)`), `jarina-ui-svc` does not |
| `jarina.domains.<name>.git.credentials.auth-type` | *(inferred)* | `ssh` \| `token` \| `basic`; see [§5](#5-per-domain-credentials) |
| `jarina.domains.<name>.git.credentials.ssh-private-key-path` | *(empty)* | OpenSSH private key, used for `auth-type: ssh` |
| `jarina.domains.<name>.git.credentials.username` | *(empty)* | Username for `basic` (optional for `token`) |
| `jarina.domains.<name>.git.credentials.token` | *(empty)* | Access token for `token` |
| `jarina.domains.<name>.git.credentials.password` | *(empty)* | Password for `basic` |
| `jarina.domains.<name>.domains` | - | Nested sub-domains |

Environment variables follow Spring relaxed binding of the same namespace, for example
`JARINA_DOMAINS_PLATFORM_GIT_REPOSITORY_URL`, `JARINA_DOMAINS_PLATFORM_GIT_BRANCH`,
`JARINA_DOMAINS_PLATFORM_GIT_LOCAL_CLONE_PATH`,
`JARINA_DOMAINS_PLATFORM_GIT_CREDENTIALS_SSH_PRIVATE_KEY_PATH` (the pattern used by the
shipped `application.yml` of both services).

## 5. Per-domain credentials

Credentials are declared **per domain** under `jarina.domains.<name>.git.credentials.*`:

- there is **no global/system account** any more: neither `jarina.config.vcs.system-account`
  nor `jarina.modeller.vcs-system-account` exists;
- credentials are **never merged** — not between domains, not with any default block. A
  domain either carries its own credentials or speaks to its repository **anonymously**;
- an entirely empty credential block is always valid and means anonymous access.

`auth-type` is resolved by `GitCredentialsSettings.effectiveAuthType()`:

1. an explicitly configured `auth-type` wins;
2. otherwise `ssh` when `ssh-private-key-path` is set;
3. otherwise `token` when any of `token` / `username` / `password` is set;
4. otherwise no auth-type at all → anonymous access.

Validation (`GitCredentialsSettings.validate(...)`) is checked when a client is built and
reports the problem against the property prefix:

| Declared `auth-type` | Requirement |
|---|---|
| `ssh` | `ssh-private-key-path` must be set |
| `token` | `token` must be set |
| `basic` | `username` **and** `password` must be set |

Transport mapping (`GitCredentials.forDomain(...)`): `ssh` installs an Apache SSH session
factory for the domain key (restored on close, so one domain never leaks its key into
another), `basic` sends HTTP Basic credentials, `token` sends the token as the HTTP
password (user `oauth2` unless a `username` is given).

## 6. The scheduler cadence is a separate concern

The domain model says nothing about **when** a domain is synchronized. The cadence lives
in `jarina-config-svc` only, under its own prefix (see
[appconf/service_configuration.md](../../jarina-config-svc/doc/appconf/service_configuration.md)):

| Property | Default | Meaning |
|---|---|---|
| `jarina.config.vcs.sync.enabled` | `false` | Enables the VCS scheduler bean |
| `jarina.config.vcs.sync.interval-ms` | `30000` | Cycle period |
| `jarina.config.vcs.sync.initial-delay-ms` | `10000` | Delay of the first cycle after startup |

The loop is **global**, not per domain: every cycle walks the domains in the order of
`DomainSettings.orderedDomains()` and synchronizes each repository independently.

## 7. Isolation rules

Enforced by `jarina-config-svc` on top of the shared model:

- A `Schedule` may reference **only** data sets of its own domain. A reference to a data
  set of another domain — or to a manually created data set that belongs to no domain —
  fails the whole commit with `DataSetDomainConflictException`, because applicability of a
  commit must not depend on a repository the commit does not control.
- The REST API rejects an update that would overwrite a construct of one domain with a
  construct of another: `DomainConflictException` → `409 Conflict`. A construct whose
  stored or incoming domain is `null` is not checked.
- Deleting a file from a domain repository does **not** delete the construct: the service
  only clears `isVcsManaged`; the user removes the construct through the REST API
  afterwards.

## 8. Observability

Both VCS log tables carry the domain, so a synchronization can be attributed to the
repository it came from:

| Table / endpoint | Column / parameter |
|---|---|
| `vcs_sync_log` | `domain_key_name` |
| `vcs_object_log` | `domain_key_name` |
| `GET /v1_0/configs/vcs/logs` | `domainKeyName` (optional filter; `from`/`to` required) |
| `GET /v1_0/configs/vcs/objectlogs` | `domainKeyName` (optional filter; `commitId` or both `from` and `to` required) |

`domainKeyName` is also part of the REST representation of the domain-scoped DTOs and is
consumed as a read-only filter by `jarina-ui-svc`. Most domain-scoped DTOs expose
`domainKeyName` — `DataSet`, `DataSource`, `Driver`, `Task`, `ScenarioActTemplate`,
`Schedule`, `QualityMetricsConf`, `TaskExecutionServiceGroup` — and for these kinds the
domain is an ownership attribute, never a part of the object identity.

## 9. Where each piece lives

| Concern | Owner |
|---|---|
| Domain tree, ordering, canonicalization, `git.*` and `git.credentials.*` parsing/validation | `jarina-domain-api` — `org.jarina.domain.settings` |
| `VcsClient` abstraction, JGit implementation, per-domain credentials | `jarina-domain-api` — `org.jarina.domain.vcs` |
| Sync cycle, change-set building, YAML parsing, `domainKeyName` stamping, isolation rules, `vcs_sync_log`/`vcs_object_log` | `jarina-config-svc` — [VCS subsystem for developers](../../jarina-config-svc/doc/vcs/vcs_for_developers.md) |
| Branch panel, workspaces `(domain, branch)`, per-domain review, VCS log filter | `jarina-ui-svc` — [readme](../../jarina-ui-svc/doc/readme.md#domains) |
| Domain check at publication time (`TaskExecutionServiceGroup.allowedDomains`) | `jarina-scheduler-svc` |

Further reading:
[config-svc: Domains](../../jarina-config-svc/doc/content_configuration/domains.md),
[Git extension user guide](../../jarina-config-svc/doc/vcs/git_extension_user_guide.md),
[ui-svc: Domains](../../jarina-ui-svc/doc/readme.md#domains),
[русская версия](../../jarina-domain-api/doc-ru/readme.md).
