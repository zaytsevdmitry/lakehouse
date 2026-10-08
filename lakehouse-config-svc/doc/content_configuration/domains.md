# Domain

A configuration domain is the unit of isolation of `lakehouse-config-svc`. A domain is at
the same time a logical scope for metadata and a **Git repository**: every domain owns its
own repository, is fetched and applied on its own, and is stamped onto every construct
loaded from it as `domainKeyName` (see [GitOps: declarative configuration from Git
repositories](../readme.md#gitops-declarative-configuration-from-git-repositories-vcs)).

The domain is **not** part of the YAML content. A construct file never carries a domain
field: the domain is derived from the repository the file is loaded from and always wins
over anything stored in the parsed DTO.

## Tree and apply order

Domains form a tree - a domain may declare nested domains via
`lakehouse.config.vcs.domains.<name>.domains`, for example `platform` ⊃ `processing` ⊃
`analytics`. The tree is traversed depth-first (`LakehouseVCSProperties.orderedDomains()`),
so a parent is always applied before its children. Within one level the domains are
sorted by `priority` ascending; domains without a `priority` are treated as
`Integer.MAX_VALUE` and therefore go last, and equal priorities are broken by domain name.

Nesting is a **configuration** relation, not a storage one. The same three repositories
may be declared as siblings or as a chain - it is the administrator's choice. The only
behavioural consequence of nesting is failure propagation: when the synchronization of a
domain fails, its whole sub-tree is skipped until the parent succeeds again, because a
child may reference constructs of its parent.

If a domain has no `git.repository-url`, it is skipped entirely (its content, if any, is
not synchronized).

## Domain ownership

- `keyName` stays **global**: it is the sole primary key (`KeyEntityAbstract.keyName`,
  `@Id`), unique across the whole configuration and *not* part of a composite key with
  `domainKeyName`. The same `keyName` therefore cannot exist in two domains - to describe
  the same physical table in two domains you need two distinct `keyName`s.
- The domain is therefore an **ownership** attribute, not part of the identity: it decides
  which repository a construct comes from, which filters and isolation rules apply to it
  (a `Schedule` may only reference data sets of its own domain), and how it is reported -
  never whether it collides with another construct.
- The domain is stamped at synchronization time by `GitOpsSynchronizer.stampDomainOn(...)`
  for every managed kind except `Script`. Scripts (`SQLTemplate`) are content-only and
  shared between domains by key; the domain stored on the `SQLTemplate` row is derived
  from the `Driver` or `Task` that references it (`SQLTemplateEntitySpecifier.domainOf()`).
- A construct created through the REST API has no domain (`domainKeyName = null`).

## Domain isolation rules

- A `Schedule` may reference **only** data sets of its own domain. A reference to a data
  set of another domain - or to a manually created data set that belongs to no domain -
  fails the whole commit with `DataSetDomainConflictException`
  (`GitOpsSynchronizer.checkScheduleDatasetDomains()`), because applicability of a commit
  must not depend on a repository the commit does not control.
- The REST API rejects an update that would overwrite a construct of one domain with a
  construct of another: `DomainConflictException` → `409 Conflict`. The check is applied by
  `rejectDomainConflict(...)` in the services of `DataSet`, `DataSource`, `Driver`,
  `Task`, `ScenarioActTemplate`, `Schedule`, `QualityMetricsConf` and
  `TaskExecutionServiceGroup`. A construct whose stored or incoming domain is `null` is not
  checked.
- Deleting a file from a domain repository does not delete the construct: the service only
  clears `isVcsManaged` and the user removes the construct through the REST API. See
  [VCS management flag](../readme.md#vcs-management-flag).

## Configuration

Everything is bound under the `lakehouse.config.vcs` prefix by `LakehouseVCSProperties`:

```yaml
lakehouse:
  config:
    vcs:
      domains:
        platform:
          priority: 0
          git:
            repository-url: git://git-server:9418/platform.git
            branch: main
            local-clone-path: /tmp/platform
          domains:                    # nested sub-domains
            processing:
              priority: 0
              git:
                repository-url: git://git-server:9418/processing.git
                branch: main
                local-clone-path: /tmp/processing
```

| Property | Default | Meaning |
|---|---|---|
| `lakehouse.config.vcs.domains.<name>.git.repository-url` | *(empty)* | Repository of the domain; `git://`, `ssh://`, `http(s)://` or a local path. Empty means the domain is skipped |
| `lakehouse.config.vcs.domains.<name>.git.branch` | `main` | Branch to track |
| `lakehouse.config.vcs.domains.<name>.git.local-clone-path` | *(empty)* | Local clone owned by the service for this domain. Must be unique per domain |
| `lakehouse.config.vcs.domains.<name>.git.private-key-path` | *(empty)* | SSH private key, used only for `ssh://` URLs |
| `lakehouse.config.vcs.domains.<name>.priority` | `Integer.MAX_VALUE` | Position within one level of the tree (ascending) |
| `lakehouse.config.vcs.domains.<name>.domains` | - | Nested sub-domains |

The scheduler itself is global, not per domain: `lakehouse.config.vcs.git.sync.enabled`,
`...sync.interval-ms` and `...sync.initial-delay-ms` (see
[appconf/service_configuration.md](../appconf/service_configuration.md)). Per-domain
environment variables follow the pattern `LAKEHOUSE_CONFIG_GIT_<NAME>_URL`,
`..._<NAME>_BRANCH`, `..._<NAME>_CLONE_PATH`, `..._<NAME>_PRIVATE_KEY_PATH`.

### Legacy single-repository configuration

The pre-domains properties `lakehouse.config.vcs.git.repository-url` / `.branch` /
`.local-clone-path` / `.private-key-path` are still honoured. When `domains` is **empty**
and `git.repository-url` is not blank, that single repository is exposed as one domain
named `default` (`LakehouseVCSProperties.rootDomains()`). As soon as at least one entry is
present in `domains`, the legacy block is ignored - it does not become an additional
`default` domain.

## Observability

Both VCS log tables carry the domain, so a synchronization can be attributed to the
repository it came from:

| Table / endpoint | Column / parameter |
|---|---|
| `vcs_sync_log` | `domain_key_name` |
| `vcs_object_log` | `domain_key_name` |
| `GET /v1_0/configs/vcs/logs` | `domainKeyName` (optional filter, with required `from`/`to` and optional `status`, `commitId`) |
| `GET /v1_0/configs/vcs/objectlogs` | `domainKeyName` (optional filter; `commitId` or both `from` and `to` are required) |

## Object fields

Most configuration DTOs expose `domainKeyName` in their REST representation - `DataSet`,
`DataSource`, `Driver`, `Task`, `ScenarioActTemplate`, `Schedule`, `QualityMetricsConf`,
`TaskExecutionServiceGroup` - and for these kinds the domain is an ownership attribute,
not a part of the object identity. The same field reaches the other services: `lakehouse-ui-svc` uses it as a
read-only filter of the VCS logs, and `lakehouse-scheduler-svc` compares it against the
domains allowed by a `TaskExecutionServiceGroup`.
