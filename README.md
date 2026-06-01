# garganttua-api-example (v3)

Standalone Java example showing how to wire **garganttua-api v3** with:

- multi-tenancy enabled, with super-tenant auto-created
- a **Tenant** entity carrying the tenant role
- a **User** entity acting as the authenticator (login / password)
- an **Authorization** entity issued on successful authentication, signable
- a **Key** entity backing the persisted-mode signing key (auto-create + lookup at sign time, scoped per `AuthenticatorKeyUsage`)

Bootstrap is intentionally explicit (no Spring, no DB, no HTTP) so the order of operations is visible at a glance.

## Run

```bash
mvn -q exec:java
```

You should see the four domains register, demo CRUD run, an authentication
issue a signed `Authorization`, and the observability summary print per-stage /
per-script timings.

## Reflection modes

The example builds in four reflection modes, each driven by a single starter
artefact from `garganttua-core`, selected via the `reflection` Maven property
(default: **`hybrid`**).

```bash
mvn package                       # hybrid (default) — AOT + runtime fallback
mvn package -Dreflection=aot      # full AOT only
mvn package -Dreflection=runtime  # full runtime reflection (no AOT artifacts)
mvn package -Dreflection=native   # AOT + GraalVM Feature for native-image
```

| Mode      | Starter pulled in            | Providers                                    | Scanners                              |
|-----------|------------------------------|----------------------------------------------|---------------------------------------|
| `hybrid`  | `garganttua-starter-hybrid`  | `AOTReflectionProvider@20` + `RuntimeReflectionProvider@10` | `AOTAnnotationScanner@20` + `ReflectionsAnnotationScanner@10` |
| `aot`     | `garganttua-starter-aot`     | `AOTReflectionProvider@20`                   | `AOTAnnotationScanner@20`             |
| `runtime` | `garganttua-starter-runtime` | `RuntimeReflectionProvider@10`               | `ReflectionsAnnotationScanner@10`     |
| `native`  | `garganttua-starter-native`  | `AOTReflectionProvider@20` + GraalVM Feature | `AOTAnnotationScanner@20`             |

- **hybrid** — both stacks; AOT wins for `@Reflected` types (`@20` > `@10`), runtime handles the rest. Safest; use for day-to-day dev.
- **aot** — pure AOT, no runtime fallback. Pressure-tests AOT coverage: anything not pre-registered in `AOTRegistry` fails loudly.
- **runtime** — JDK reflection + classpath scan, no AOT artifacts. For regression/baseline comparison.
- **native** — same as aot + the GraalVM Feature; produces a native binary (see below).

The mode drives Maven profile activation; each profile contributes one starter
dependency. The AOT annotation processor always runs (it emits the
`META-INF/garganttua/index/*` files the AOT scanner reads); the direct-binder
emission (`AOTClass_*.java`) is gated by
`-Agarganttua.direct.binders=${garganttua.direct.binders}` (default `true`,
overridden to `false` in `reflection-runtime`).

Verify the SPI selection on the first banner line at startup, e.g.:

```
SPI bootstrap: providers=[AOTReflectionProvider@20], scanners=[AOTAnnotationScanner@20]   # aot/native
```

## Building a native binary (GraalVM native-image)

Produces a standalone binary (no JVM at runtime). Verified end-to-end: ~330 ms
startup, full CRUD, signed-authentication, and `stage:*`/`script:*`
observability.

### 1. Prerequisite — a GraalVM JDK 21

```bash
sdk install java 21.0.2-graalce          # or 21.0.5-graal
~/.sdkman/candidates/java/21.0.2-graalce/bin/native-image --version
```

`native-image` ships with the GraalVM distribution (no `gu install` on recent CE builds).

### 2. Build — Maven must run on the GraalVM JDK

```bash
export JAVA_HOME=~/.sdkman/candidates/java/21.0.2-graalce
export PATH="$JAVA_HOME/bin:$PATH"

mvn -Dreflection=native clean package -DskipTests
```

~1–2 min. Binary: `target/garganttua-api-example`. Add `-o` for an offline build.

### 3. Run

```bash
./target/garganttua-api-example
```

### What the `reflection-native` profile wires

1. **`garganttua-starter-native`** — AOT provider + the GraalVM Feature
   `GarganttuaAotFeature` (auto-activated via `META-INF/native-image/.../native-image.properties`),
   which registers AOT descriptors for reflection at image-analysis time.
2. **`garganttua-aot-maven-plugin`** (goals `aggregate-index` + `aggregate-registry`,
   `process-classes`) — merges every dependency's annotation indices and emits
   `reflect-config.json` + `resource-config.json`.
3. **`native-maven-plugin`** (`compile-no-fork`, `package`) — invokes
   `native-image` with `--enable-preview --no-fallback`.

### Temporary overrides

`src/main/resources/META-INF/native-image/com.garganttua.api/garganttua-api-example-overrides/`
holds two files that complete the native config until some framework modules
ship their AOT descriptors:

- `reflect-config.json` — declares core/api classes resolved reflectively at
  runtime but without an AOT descriptor (`ExpressionContext`, `Expressions`,
  `ObservabilityExpressions`, `Authentication`, …).
- `resource-config.json` — bundles `META-INF/garganttua/index/.*` (the annotation
  indices) as resources, needed by the `@ChildContext` / `@Expression` scans at
  native runtime.

These overrides disappear once the framework modules are native-ready (see
*Known gaps → Native readiness*). Do not remove them before then.

### Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `native-image: command not found` / toolchain error | Maven not on a GraalVM JDK | `export JAVA_HOME=…/21.0.2-graalce` before `mvn` |
| `Classes … initialized at run time got initialized during image building` | AOT descriptor not declared build-time-init | normally handled by the Feature; ensure `garganttua-starter-native` is on the classpath |
| `NoSuchMethodException: <name>` at startup | framework class resolved reflectively, no descriptor nor reflect-config entry | add the class to the override `reflect-config.json` |
| `No child context factory registered for …` | annotation index not bundled as a resource | check the override `resource-config.json` (`META-INF/garganttua/index/.*`) |
| `Cannot resolve class: <fqn>` (`AOTClass.getTypeResolved`) | a step's output type isn't registered for native reflection | declare `<fqn>` in the override `reflect-config.json` |

## Layout

| File | Purpose |
|---|---|
| `Tenant`, `TenantDto` | The tenant entity itself — one row per organisation. |
| `User`, `UserDto` | Authenticator entity. Owns the authorizations it issues. |
| `Authorization`, `AuthorizationDto` | Token / session record. `signable()` is enabled — `getDataToSign()` returns the bytes the framework signs. |
| `Key`, `KeyDto` | Persisted signing-key entity. Wired via `keyBuilder.key().realmName(...).algorithm(...).signatureAlgorithm(...).publicMaterial(...).privateMaterial(...).expiration(...).revoked(...).up()`; auto-created on first sign and looked up on subsequent calls. |
| `InMemoryDao` | Minimal `IDao` storing DTOs in an `ArrayList`. Supports `$field/$eq` filters. |
| `PasswordAuthentication` | The `IAuthentication`-compatible strategy. Compares submitted credentials against a SHA-256 hash on the user. |
| `CoreStatsObserver`, `CoreLoggingObserver` | `@Observer`-annotated, bootstrap-scanned. Aggregate / log the `ObservableEvent` stream (operation, stage, script, engine timings). |
| `ExampleApplication` | Bootstraps reflection / injection / expression contexts, then builds the API and seeds demo data. |

## Key wiring (persisted mode)

The signing key is driven entirely by the framework. The Key entity exposes the seven canonical fields the auto-create / lookup pipeline expects — `realmName`, `algorithm`, `signatureAlgorithm`, `publicMaterial`, `privateMaterial`, `expiration`, `revoked` — and the bindings are declared via `keyBuilder.key()...up()` on the key domain.

The authenticator points at the key domain through `.key(keyBuilder)` and configures the policy:

```java
authzLink.key(keyBuilder)
        .usage(AuthenticatorKeyUsage.oneForEach)   // one key per caller (alt: oneForAll, oneForTenant)
        .algorithm(KeyAlgorithm.EC_256)
        .signatureAlgorithm(SignatureAlgorithm.SHA256)
        .lifeTime(365, TimeUnit.DAYS);
```

`.key(supplier)` (direct mode — the renamed `.keyRealm(...)`) is still available for HSM / Vault back-ends. The two modes are mutually exclusive on the same authorization.

> The DTO (`KeyDto`) must mirror **every** key field. The auto-create round-trips through the DTO, so dropping a field silently loses the matching piece of key material.

## Observability

Workflow timing is enabled with `builder.workflowTiming(WorkflowTimingConfig.of().stages(true).scripts(true))`
in `buildApi()`. The `@Observer`-scanned `CoreStatsObserver` then aggregates the
event stream, sliced by source prefix into operation / `stage:*` / `script:*` /
engine sections at the end of the run.

Two notes:
- **Call order under AOT**: `workflowTiming(...)` must be set **before**
  `bootstrap.load()`. Under full-AOT the configure stage (which forwards the
  config and precompiles each workflow script) runs during `load()`, earlier
  than in runtime mode. Set it after `load()` and the AOT scripts are already
  precompiled with timing disabled → empty `stage:*`/`script:*` sections.
- `api:operation:*` and `stage:*`/`script:*` events share one `executionId`
  (correlatable in a tracing backend).

## Known gaps (v3.0.0-ALPHA01)

### Gap #1 — no `autoCreate(boolean)` on the key DSL

`IAuthenticatorAuthorizationKeyBuilder` only exposes `usage`, `algorithm`,
`signatureAlgorithm`, `lifeTime` — there is **no** `.autoCreate(boolean)` for a
"one key per user, generated on user creation" policy; keys are materialized
lazily on the first sign. Acceptable here, but a hook for eager creation would be
useful.

### Gap #3 — `superTenantAutoCreate(true)` fails when an `owned` domain is registered

With `superTenantAutoCreate(true)`, `Api.doStart()` → `autoCreateMasterTenant()`
walks into the `authorizations` domain (`.owned("ownerId")`), reaches
`SecurityExpressions.requireOwnerId` and throws `Owner ID is required for this
operation`. The example sidesteps this with `.superTenantAutoCreate(false)` and a
manual master-tenant `.upsert(...)`. Boot-time master-tenant creation should
bypass owned-domain security checks (super-tenant + super-owner caller context).

### Gap #4 — `.upsert(entity)` / `.create(entity)` lifecycle ordering

`Domain.doStart()` calls `upsertStartupEntities()` **before** the lifecycle flag
`started` is flipped, and the upsert internally calls `invoke()` which begins
with `ensureStarted()` → `LifecycleException: Lifecycle not started`. The example
declares `.upsert(...)` on the domain builders; this is expected to be fixed
upstream.

### Native readiness (framework)

The native binary works today via the override config above, but the proper fix
is per-module in `garganttua-core` / `garganttua-api`: several modules ship **no**
`AOTClass_*` descriptors, so classes resolved reflectively at native runtime
fail. Two changes per affected module make it native-ready:

1. add a dependency on `com.garganttua.core:garganttua-aot-reflection` (the
   generated `AOTClass_*` extend it);
2. build with `garganttua.direct.binders=true` (the AOT processor is already
   wired in the parent pom; the flag defaults `false` in core).

Plus one mojo fix: `AggregateAOTRegistryMojo.writeResourceConfig` should also
include `META-INF/garganttua/index/.*` so annotation indices are readable at
native runtime. Once both land, the consumer-side override files become
unnecessary. Confirmed not-yet-shipping descriptors: `garganttua-expression`,
`garganttua-observability` (and `Authentication` in `garganttua-api-commons`).

## Notes

- The old v1.0.1 example was moved to `.archive-v1/` (not deleted).
- `garganttua-api-spring` and `garganttua-api-security/*` modules are commented out in the root POM of v3, so we use neither: a standalone `main()` and a hand-rolled `PasswordAuthentication` shaped like the API's `IAuthentication` contract.
- Framework evolution requests filed during v3 bring-up (workflow `timing()`
  surface, `@Observer`→workflow attach, executionId correlation) have all
  **landed** in `garganttua-api` / `garganttua-core`.
