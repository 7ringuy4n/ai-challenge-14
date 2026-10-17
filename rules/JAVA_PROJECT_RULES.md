# Feature-Oriented Java Project Rules

Apply this rule when creating a Java project/module or when a Java refactor is
asked to use the feature-oriented Spring structure defined here. Preserve the
target repository's Java/framework/database versions and public behavior. The
directory blueprint is consistent across projects; framework adapters and
resource paths follow the target stack.

## 1. Before changing a project

1. Read `AGENTS.md`, `rules/ENGINEERING_RULES.md`, `rules/REQUIREMENT.md`, the
   active workflow, architecture, history, and the target module's README.
2. Inspect the POM/build, base package, current modules, dependency management,
   framework, API response, security, persistence, migration, test, resource,
   deployment, and CI conventions.
3. Map each existing class to its owner and responsibility. Record current
   routes, request/response shapes, exceptions, persistence queries, and
   externally visible behavior before moving code.
4. Write an ordered, non-overlapping to-do list. Move one ownership boundary at
   a time, keep each intermediate state compilable, and preserve behavior unless
   the request explicitly changes it.
5. Reuse the project's dependencies, versions, patterns, and tools. Add no
   framework, database, migration tool, module, or runtime dependency solely to
   imitate a directory tree.
6. Never copy credentials or private configuration. Use environment-variable
   names and safe placeholders in examples.

## 2. Project root and package layout

Keep the existing build topology and create only files needed by the product.
For a typical Maven service, recognized root areas are:

| Path | Responsibility |
|---|---|
| `pom.xml` | Build, dependency management, plugins, profiles, packaging. |
| `src/main/java/` | Bootstrap class and production packages. |
| `src/main/resources/` | Application configuration, mapper XML, migrations, templates and static assets. |
| `src/test/java/` | Tests in packages that mirror production ownership. |
| `src/test/resources/` | Focused fixtures and test configuration. |
| `docker/`, `scripts/` | Required local runtime assets and repeatable developer operations. |
| `README.md` | Build, configuration, run, API, persistence and verification instructions. |

The application bootstrap class sits at the base package so framework component
scanning includes its features. Put business behavior under a feature package;
keep framework and cross-cutting responsibilities in small, clearly named
shared packages.

```text
<base-package>/
├── <feature>/
│   ├── api/
│   │   └── dto/
│   ├── service/
│   └── domain/
│       ├── mapper/          # When using MyBatis
│       └── persistence/     # Feature-owned database adapters when needed
├── common/                  # Truly shared errors, web advice, small utilities
├── config/                  # Framework and integration configuration
├── security/                # Identity, authentication and authorization adapters
├── audit/                   # Shared audit mechanisms, if the system has them
└── observability/           # Shared tracing/metrics adapters, if needed
```

`<feature>` is a product/domain name such as `claim`, `order`, or `catalog`.
Use the actual approved base package from the target POM/module. Do not invent a
new root package inside an existing application.

### 2.1 Feature ownership

| Package | Owns | Does not own |
|---|---|---|
| `<feature>.api` | HTTP routes, binding, validation entry point, status and response mapping. | SQL or business policy. |
| `<feature>.api.dto` | Request, query and response shapes; input validation metadata. | Persistence rows or service state. |
| `<feature>.service` | Feature use cases, authorization decisions, orchestration and transaction boundary. | HTTP framework details or mapper SQL. |
| `<feature>.domain` | Feature values, invariants and repository/engine ports. | HTTP objects or vendor-specific transaction APIs. |
| `<feature>.domain.mapper` | Typed mapper interfaces and persistence projections when using MyBatis. | Controller or transport DTOs. |
| `<feature>.domain.persistence` | Feature-owned database adapters, type handlers and serialization helpers when the adapter merits a package. | Shared framework configuration. |
| `common` | Small responsibilities used by multiple features. | A dumping ground for feature-specific classes. |
| `config` | Framework bean registration and integration setup. | Business decisions. |
| `workflow` | Workflow definition, loader, evaluator and workflow-specific contracts. | HTTP binding or persistence. |
| `security` | Principal extraction and framework-specific security adapters. | Request-supplied actor identity. |

Keep small modules flat until nested packages improve ownership. Never retain a
global `web`, `application`, `contract`, or `persistence` bucket for all business
features when the classes can be owned by a feature. Cross-cutting classes that
truly serve multiple features remain shared.

## 2.2 Application configuration and Liquibase

Keep framework-owned properties under their Spring Boot namespaces. Put feature
settings in nested, feature-owned keys (for example `claims.workflow`,
`claims.security`, and `claims.outbox`) and use matching property names in the
code that consumes them. Preserve environment-variable overrides and never add
production secrets as defaults.

Use the standard packaged resource layout for Liquibase:

```text
src/main/resources/
├── application.yml
├── db/changelog/
│   ├── db.changelog-master.yml
│   └── changes/
│       ├── <version>-<purpose>.yml
│       └── <version>-<purpose>.sql
└── sql/service/<feature>/*.xml  # MyBatis mapper XML
```

Set `spring.liquibase.change-log` to
`classpath:db/changelog/db.changelog-master.yml`; include changesets relative to
the master changelog. Let Maven package `src/main/resources` as-is. Avoid copying
a second root migration tree into `db/`, which makes the source/classpath layout
diverge. Align Testcontainers and standalone Liquibase test support with the same
master changelog path. Keep mapper XML beneath `src/main/resources/sql/service/<feature>/`
and ensure the configured mapper glob includes every feature-owned mapper.

## 3. Naming and dependency direction

1. Use `<Feature>Controller`, `<Feature>ControllerDoc` (when API docs use an
   interface), `<Feature>Service`, `<Feature>ServiceImpl`, `<Feature>Mapper`,
   and typed `*VO`/row names according to neighboring project convention.
2. Controllers depend on a service interface. Services depend on domain ports.
   Persistence adapters implement those ports. Configuration wires framework
   dependencies. Keep imports flowing toward domain contracts, not outward from
   domain code to web, database, or security frameworks.
3. Use constructor injection. Keep an interface when it defines a stable use
   case boundary, supports multiple implementations, or isolates an adapter;
   avoid interfaces that only rename a single helper.
4. Keep route fragments in named constants when routes are part of the public
   contract. Preserve existing routes during structural changes.
5. Reuse the project's response envelope, error format, API documentation and
   status conventions. Do not create a parallel contract for one feature.

## 4. API, identity, and validation

1. Bind requests to transport DTOs, never directly to persistence rows.
2. Validate required, blank, length, format, numeric and cross-field inputs at
   the appropriate boundary. Keep business validation in the service/domain.
3. Authenticate and authorize each endpoint using the active project mechanism.
   Authentication alone does not grant access.
4. Derive actor identity from the authenticated principal. Never trust user or
   role fields supplied in request bodies for authorization or audit.
5. Keep credential DTOs separate from normal user responses. Exclude secrets
   and sensitive personal data from serialization, generated `toString`, logs,
   errors, and audit details.
6. Keep provider-specific code inside `security` and its configuration; changing
   providers must not activate conflicting authentication paths.

## 5. Services and transactions

1. Keep use-case orchestration, authorization rules, state changes and business
   invariants in the feature service/domain. Controllers translate HTTP; mappers
   execute persistence operations.
2. Put transaction ownership at the use-case boundary using the target project's
   existing transaction mechanism. Verify proxy/interceptor/pointcut coverage
   rather than assuming annotations are active.
3. Commit coupled state, audit, idempotency receipt, payment and outbox writes
   atomically when required by the domain.
4. Protect duplicate and concurrent writes with the project's established
   uniqueness, locking, compare-and-set and idempotency patterns.
5. Use precise numeric types for money and an injected `Clock` for time-sensitive
   behavior when determinism matters.
6. Return typed results. Bound collection reads and use stable ordering when the
   API or data volume requires paging.

## 6. Persistence and resources

1. Keep mapper interfaces and typed persistence projections feature-owned under
   `<feature>.domain.mapper` when using MyBatis. Mapper XML belongs beneath
   `src/main/resources/sql/service/<feature>/` unless the existing configured
   project convention uses another location.
2. Mapper XML namespaces match mapper fully qualified names; statement IDs match
   method names; mapper discovery matches actual XML locations.
3. Select explicit columns and bind request data. Avoid `SELECT *`, SQL string
   concatenation, and untrusted `${...}` interpolation.
4. Keep count and page filters identical, order pages deterministically, and
   verify the actual paging mechanism limits database rows.
5. Check affected-row counts for uniqueness, optimistic locking and state
   transitions. Keep null, empty, soft-delete and status filters consistent.
6. Keep migrations under the target project's configured versioned migration
   root. Do not introduce a competing migration tool. Verify SQL against the
   actual runtime database; an in-memory database alone does not prove it.
7. Place database initialization used only by local Compose under its established
   development path. Keep operational manual SQL separate from auto migrations.

## 7. Configuration and delivery

1. Preserve the existing Java level, parent/BOM, framework/database versions,
   profiles, mapper settings, logging, security, CI and container conventions.
2. Store safe configuration in the current application config format and
   environment profiles. Read secrets from environment or the approved secret
   store; never put reusable secret values in source, examples or logs.
3. Update the owning deployment/setup source when configuration behavior changes.
   Local initialization is not production identity or migration policy.
4. Keep README instructions aligned with actual build, profile, environment
   variable names, database setup, run command, API docs, health endpoint and
   verification command.
5. Add a separate module only when it has a real ownership, runtime/classloader,
   consumer, or release boundary.

## 8. Tests and refactor proof

1. For a refactor, record the behavior-preservation boundary and run the same
   baseline checks before and after moving code. Preserve routes, DTO JSON,
   authorization, exceptions, ordering, transactions and persistence behavior.
2. Mirror production feature packages under `src/test/java`. Keep shared test
   fixtures in focused test-support packages and file fixtures under
   `src/test/resources`.
3. Use unit tests for isolated domain/service decisions. Use the real database,
   migrations, HTTP and security integration when correctness depends on them.
   Mocks cannot prove SQL dialect, grants, proxy behavior or rollback.
4. Keep existing build and coverage gates. Do not weaken, skip, or exclude
   business code to get a passing result. Report actual test counts and coverage.
5. Print `running test case N/TOTAL` when the existing project test harness uses
   that convention. Record unavailable checks as `NOT_RUN` with the command and
   concrete environment reason.

## 9. Completion checklist

- [ ] Each business class is owned by a feature package with API, DTO, service,
      and domain boundaries appropriate to the target stack.
- [ ] Cross-feature packages contain only genuinely shared configuration,
      common, security, audit, and observability responsibilities.
- [ ] Controllers delegate through service boundaries; domain code does not
      depend on web or database frameworks.
- [ ] DTOs, route constants, authorization, principal-derived identity, and
      response/error shapes match the public contract.
- [ ] Mapper interfaces, XML namespace/IDs, typed rows, SQL filters, paging,
      schema changes and runtime database behavior agree.
- [ ] Transaction and idempotency behavior is verified using the active
      framework mechanism.
- [ ] Tests and resources follow the feature package; documentation/config is
      updated where affected.
- [ ] Baseline and post-refactor checks are reported with actual outcomes; no
      behavioral change is hidden in a structural refactor.

## 10. Precedence

The user's current request and security constraints take priority, followed by
the target repository's established conventions, then this reusable blueprint.
When conventions conflict, preserve public compatibility and document the
choice. A folder name is not a reason to change the framework, API, data model,
or runtime behavior.
