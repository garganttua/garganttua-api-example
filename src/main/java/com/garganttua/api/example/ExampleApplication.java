package com.garganttua.api.example;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.garganttua.api.commons.ApiException;
import com.garganttua.api.commons.caller.ICaller;
import com.garganttua.api.commons.context.IApi;
import com.garganttua.api.commons.context.IDomain;
import com.garganttua.api.commons.context.dsl.IApiBuilder;
import com.garganttua.api.commons.context.dsl.IDomainBuilder;
import com.garganttua.api.commons.operation.Access;
import com.garganttua.api.commons.operation.OperationDefinition;
import com.garganttua.api.commons.security.annotations.AuthenticatorKeyUsage;
import com.garganttua.api.commons.security.authenticator.AuthenticatorScope;
import com.garganttua.api.commons.service.IOperationResponse;
import com.garganttua.api.commons.service.OperationResponseCode;
import com.garganttua.api.binding.javalin.JavalinInterface;
import com.garganttua.api.binding.javalin.JavalinProtocol;
import com.garganttua.api.core.api.ApiBuilder;
import com.garganttua.api.core.caller.Caller;
import com.garganttua.api.core.security.authentication.AuthenticateCredentialsSupplierBuilder;
import com.garganttua.api.core.security.authentication.AuthenticationRequest;
import com.garganttua.api.core.security.authentication.AuthenticatorDefinitionSupplierBuilder;
import com.garganttua.api.core.security.authentication.DecodedAuthorizationSupplierBuilder;
import com.garganttua.api.core.security.authentication.PrincipalSupplierBuilder;
import com.garganttua.api.core.service.RequestBuilder;
import com.garganttua.core.bootstrap.dsl.Bootstrap;
import com.garganttua.core.bootstrap.dsl.IBootstrap;
import com.garganttua.core.crypto.KeyAlgorithm;
import com.garganttua.core.crypto.SignatureAlgorithm;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.dsl.FixedSupplierBuilder;
import com.garganttua.core.workflow.WorkflowTimingConfig;

/**
 * Standalone showcase of garganttua-api v3 wiring:
 *
 * - multi-tenancy enabled
 * - Tenant entity (tenant role)
 * - User entity (authenticator, login-password)
 * - Authorization entity (signable bearer token)
 * - Key entity (persisted-mode signing key — auto-create / lookup is
 * driven by the framework at sign time, scoped per
 * {@link AuthenticatorKeyUsage})
 *
 * Demo data is declared via {@code .upsert(entity)} on each domain builder —
 * the framework upserts those at domain startup using its own pipeline.
 */
public final class ExampleApplication {

    private static final String SUPER_TENANT = "SUPER_TENANT";
    private static final String[] DOMAIN_NAMES = { "tenants", "authorizations", "keys", "users" };

    /** HTTP port used in server mode (the Javalin interface). */
    private static final int HTTP_PORT = 7000;

    private final InMemoryDao tenantDao = new InMemoryDao();
    private final InMemoryDao userDao = new InMemoryDao();
    private final InMemoryDao authorizationDao = new InMemoryDao();
    private final InMemoryDao keyDao = new InMemoryDao();

    private final CoreStatsObserver coreStats = new CoreStatsObserver();

    public static void main(String[] args) throws Exception {
        // logs / --logs (alias debug / --debug): raise the slf4j-simple level to
        // DEBUG so the CoreLoggingObserver streams every captured observability
        // event (core-start/end/error/log) to the console live. MUST run before
        // the first logger is created, hence the very first statement here — a
        // system property overrides simplelogger.properties in slf4j-simple.
        if (hasArg(args, "logs", "--logs", "debug", "--debug")) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }

        // Run modes:
        //   headless (default) - no interface; runs the in-process demo calls.
        //   server             - attaches the Javalin HTTP interface to every
        //                        domain and serves on http://localhost:HTTP_PORT.
        // Extra flags (any position, combinable): observe, logs.
        //   Usage: java ... ExampleApplication [server] [observe] [logs]
        boolean server = hasArg(args, "server", "--server");
        // observe / --observe: trace to the console what observability captured
        // (the CoreStatsObserver aggregate, sliced by source layer). In headless
        // mode it prints after the demos; in server mode it prints on shutdown.
        boolean observe = hasArg(args, "observe", "--observe");

        ExampleApplication app = new ExampleApplication();
        IApi api = app.buildApi(server);

        if (server) {
            // Building the API ran the lifecycle, which started the Javalin
            // server (Domain.doStart -> IInterface.onStart binds the port).
            System.out.println();
            System.out.println("========== SERVER MODE ==========");
            System.out.println("Garganttua API example serving on http://localhost:" + HTTP_PORT);
            System.out.println("Domains exposed: " + String.join(", ", DOMAIN_NAMES));
            System.out.println("Try: curl http://localhost:" + HTTP_PORT + "/tenants");
            System.out.println("Press Ctrl+C to stop.");
            if (observe) {
                System.out.println("Observability: a capture summary prints on shutdown (Ctrl+C).");
                Runtime.getRuntime().addShutdownHook(
                        new Thread(() -> printObservability(app), "observability-dump"));
            }
            // Keep the JVM alive; the server runs on its own (Jetty) threads.
            Thread.currentThread().join();
            return;
        }

        ICaller caller = Caller.createSuperCaller(api.getSuperTenantId());

        System.out.println(api.getAuthorities());

        section("TENANTS");
        demoTenantCrud(api, caller);

        section("AUTHENTICATION");
        Authorization aliceAuthorization = demoAuthentication(api);

        // Replaying the issued token through the API (Mode B) exercises the
        // verifyAuthorization → token self-verify pipeline. Currently disabled:
        // the self-verify path returns 401 "All authentication methods failed"
        // (the token authenticator's authenticate method is never reached — the
        // pipeline fails earlier, at token-principal resolution). Tracked as a
        // framework follow-up; see TokenAuthentication for the related signable
        // key-realm limitation.
         if (aliceAuthorization != null) {
            section("AUTHENTICATED CALLS (using Alice's authorization)");
            demoAuthenticatedCalls(api, aliceAuthorization);

            section("USERS");
            demoUserCrud(api, aliceAuthorization);
        }

        if (observe) {
            printObservability(app);
        }
    }

    /**
     * Returns true if {@code args} contains any of {@code names} (case-insensitive).
     */
    private static boolean hasArg(String[] args, String... names) {
        for (String a : args) {
            for (String n : names) {
                if (a.equalsIgnoreCase(n)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Traces to the console what observability captured during the run: the
     * {@link CoreStatsObserver} aggregate (count / ok / ko / timing per source),
     * sliced by layer. The api emits operation-level events on
     * {@code api:operation:<domain>:<op>} plus nested engine events
     * (stage:*, script:*, mapper:*, runtime:*, …); CoreStatsObserver is
     * {@code @Observer}-scanned and routes every layer through a shared static
     * aggregate, so this read handle sees them all. Enabled by {@code observe}.
     */
    private static void printObservability(ExampleApplication app) {
        section("OBSERVABILITY — operation stats");
        printCoreStats(app.coreStats.snapshot(), s -> s.source().startsWith("api:operation:"), "operation");

        section("OBSERVABILITY — workflow timing map (stages)");
        printCoreStats(app.coreStats.snapshot(), s -> s.source().startsWith("stage:"), "stage");

        section("OBSERVABILITY — workflow timing map (scripts)");
        printCoreStats(app.coreStats.snapshot(), s -> s.source().startsWith("script:"), "script");

        section("OBSERVABILITY — core engines (mapper / runtime / scriptcontext / …)");
        printCoreStats(app.coreStats.snapshot(),
                s -> !s.source().startsWith("stage:")
                        && !s.source().startsWith("script:")
                        && !s.source().startsWith("api:operation:"),
                "source");
    }

    /**
     * Renders a filtered slice of the {@link CoreStatsObserver} snapshot as
     * a fixed-width table, sorted by total time descending. The {@code label}
     * controls the first column's header — kept short so each section reads
     * like "what dominates this layer of the pipeline".
     */
    private static void printCoreStats(
            Map<String, CoreStatsObserver.SourceStats> snapshot,
            java.util.function.Predicate<CoreStatsObserver.SourceStats> filter,
            String label) {
        var rows = snapshot.values().stream()
                .filter(filter)
                .sorted((a, b) -> Long.compare(toMicros(b.totalDuration()), toMicros(a.totalDuration())))
                .toList();
        if (rows.isEmpty()) {
            System.out.println("  (no " + label + " events recorded)");
            return;
        }
        String header = String.format("  %-50s %6s %6s %6s %10s %10s %10s %10s",
                label, "count", "ok", "ko", "total(µs)", "min(µs)", "max(µs)", "avg(µs)");
        System.out.println(header);
        System.out.println("  " + "-".repeat(header.length() - 2));
        for (var s : rows) {
            System.out.printf("  %-50s %6d %6d %6d %10d %10d %10d %10d%n",
                    truncate(s.source(), 50),
                    s.count(),
                    s.successCount(),
                    s.failureCount(),
                    toMicros(s.totalDuration()),
                    toMicros(s.minDuration()),
                    toMicros(s.maxDuration()),
                    toMicros(s.averageDuration()));
        }
    }

    private static long toMicros(Duration d) {
        return d == null ? 0L : d.toNanos() / 1_000L;
    }

    private static String truncate(String s, int max) {
        if (s == null)
            return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /**
     * Runs the four authenticate scenarios (valid / wrong-password / unknown
     * login / missing tenantId) and returns the {@link Authorization} the
     * pipeline minted on the valid path — so subsequent demos can pass it
     * back into the API as a real authenticated caller (Mode B —
     * {@code IOperationRequest.AUTHORIZATION}).
     *
     * @return the Authorization issued for Alice, or {@code null} if the
     *         valid-path authentication failed (in which case the failure
     *         is already printed and the dependent demos will be skipped).
     */
    private static Authorization demoAuthentication(IApi api) {
        IDomain<?> domain = api.getDomain("users").orElseThrow();
        OperationDefinition authOp = OperationDefinition.authenticate("users", IClass.getClass(User.class));
        String tenantId = "tenant-acme-uuid";

        Authorization issued = runForResponse("authenticate (valid)",
                () -> RequestBuilder.builder(domain)
                        .operation(authOp)
                        .body(new AuthenticationRequest(
                                "alice@acme",
                                "hunter2".getBytes(StandardCharsets.UTF_8),
                                tenantId))
                        .build().execute(),
                Authorization.class);

        run("authenticate (wrong password)", () -> RequestBuilder.builder(domain)
                .operation(authOp)
                .body(new AuthenticationRequest(
                        "alice@acme",
                        "wrong-password".getBytes(StandardCharsets.UTF_8),
                        tenantId))
                .build().execute());

        run("authenticate (unknown login)", () -> RequestBuilder.builder(domain)
                .operation(authOp)
                .body(new AuthenticationRequest(
                        "ghost@acme",
                        "anything".getBytes(StandardCharsets.UTF_8),
                        tenantId))
                .build().execute());

        run("authenticate (missing tenantId, scope=tenant)", () -> RequestBuilder.builder(domain)
                .operation(authOp)
                .body(new AuthenticationRequest(
                        "alice@acme",
                        "hunter2".getBytes(StandardCharsets.UTF_8),
                        null))
                .build().execute());

        return issued;
    }

    /**
     * Replays the issued Authorization on the authorizations domain — the
     * framework's VERIFY_AUTHORIZATION step picks it up via the
     * {@code "authorization"} arg (Mode B) and grants Alice the rights
     * stamped onto the token.
     */
    private static void demoAuthenticatedCalls(IApi api, Authorization authorization) {
        IDomain<?> authzDomain = api.getDomain("authorizations").orElseThrow();

        System.out.println("  [issued] uuid=" + authorization.getUuid()
                + " ownerId=" + authorization.getOwnerId()
                + " tenantId=" + authorization.getTenantId()
                + " expiresAt=" + authorization.getExpiresAt()
                + " signature=" + (authorization.getSignature() == null
                        ? "null"
                        : "[" + authorization.getSignature().length + " bytes]"));

        run("readAll authorizations (as Alice)", () -> aliceRequest(authzDomain, authorization)
                .readAll().build().execute());

        run("readOne own authorization (as Alice)", () -> aliceRequest(authzDomain, authorization)
                .readOne(authorization.getUuid()).build().execute());
    }

    private static void demoTenantCrud(IApi api, ICaller caller) {
        IDomain<?> domain = api.getDomain("tenants").orElseThrow();
        String uuid = "demo-tenant-uuid";

        Tenant entity = new Tenant();
        entity.setId("demo-tenant");
        entity.setUuid(uuid);
        entity.setName("Demo Corp");
        entity.setCreatedAt(Instant.now());

        run("createOne", () -> RequestBuilder.builder(domain).caller(caller).createOne(entity).build().execute());
        run("readAll", () -> RequestBuilder.builder(domain).caller(caller).readAll().build().execute());
        run("readOne", () -> RequestBuilder.builder(domain).caller(caller).readOne(uuid).build().execute());

        entity.setName("Demo Corp Renamed");
        run("updateOne", () -> RequestBuilder.builder(domain).caller(caller).updateOne(uuid, entity).build().execute());
        run("readOne (post-update)",
                () -> RequestBuilder.builder(domain).caller(caller).readOne(uuid).build().execute());

        run("deleteOne", () -> RequestBuilder.builder(domain).caller(caller).deleteOne(uuid).build().execute());
        run("readAll (post-delete)", () -> RequestBuilder.builder(domain).caller(caller).readAll().build().execute());
        run("deleteAll", () -> RequestBuilder.builder(domain).caller(caller).deleteAll().build().execute());
    }

    /**
     * Exercises the users domain authenticated as Alice. The User domain is
     * {@code .owner("uuid")}, so the framework's owner-filter restricts what
     * Alice can see / mutate to her own row — the calls below show both the
     * golden path (read/update self) and the rejected path (touch another
     * user's row).
     */
    private static void demoUserCrud(IApi api, Authorization authorization) {
        IDomain<?> domain = api.getDomain("users").orElseThrow();
        String ownUuid = authorization.getOwnerId();
        String otherUuid = "demo-user-uuid";

        run("readOne self (as Alice)", () -> aliceRequest(domain, authorization)
                .readOne(ownUuid).build().execute());

        run("readAll users (as Alice — owner-scoped)", () -> aliceRequest(domain, authorization)
                .readAll().build().execute());

        run("readOne someone else (as Alice — must fail)", () -> aliceRequest(domain, authorization)
                .readOne(otherUuid).build().execute());

        // Self-update: change Alice's login string. The framework verifies the
        // owner filter and lets it through because ownUuid matches the
        // caller's ownerId.
        User selfPatch = new User();
        selfPatch.setId("alice@acme");
        selfPatch.setUuid(ownUuid);
        selfPatch.setTenantId(authorization.getTenantId());
        selfPatch.setLogin("alice.renamed@acme");
        selfPatch.setPasswordHash(PasswordAuthentication.hash("hunter2"));
        selfPatch.setAuthorities(authorization.getAuthorities());

        run("updateOne self (as Alice)", () -> aliceRequest(domain, authorization)
                .updateOne(ownUuid, selfPatch).build().execute());
        run("readOne self (post-update)", () -> aliceRequest(domain, authorization)
                .readOne(ownUuid).build().execute());

        // Attempt to create another user — Alice is ROLE_USER, no admin
        // authority, so the pipeline rejects this.
        User bob = new User();
        bob.setId("bob@demo");
        bob.setUuid(otherUuid);
        bob.setTenantId(authorization.getTenantId());
        bob.setLogin("bob@demo");
        bob.setPasswordHash(PasswordAuthentication.hash("s3cret"));
        bob.setAuthorities(List.of("ROLE_USER"));

        run("createOne another user (as Alice — must fail)", () -> aliceRequest(domain, authorization)
                .createOne(bob).build().execute());
    }

    /**
     * Builds a {@code RequestBuilder} primed with Alice's caller context
     * (tenantId, ownerId, authorities) and the issued Authorization in the
     * Mode B {@code "authorization"} arg — every authenticated demo call
     * shares this prelude.
     */
    private static com.garganttua.api.commons.service.IRequestBuilder aliceRequest(IDomain<?> domain,
            Authorization authorization) {
        return RequestBuilder.builder(domain)
                .tenantId(authorization.getTenantId())
                .requestedTenantId(authorization.getTenantId())
                .callerId(authorization.getOwnerId())
                .ownerId(authorization.getOwnerId())
                .authorities(authorization.getAuthorities())
                .param("authorization", authorization);
    }

    private static void demoKeyCrud(IApi api, ICaller caller) {
        IDomain<?> domain = api.getDomain("keys").orElseThrow();
        String uuid = "demo-key-uuid";

        Key entity = new Key();
        entity.setId("demo-key");
        entity.setUuid(uuid);
        entity.setTenantId("tenant-acme-uuid");
        entity.setOwnerId("user-alice-uuid");
        entity.setRealmName("cryptokeys:owner:user-alice-uuid");
        entity.setAlgorithm("EC-256");
        entity.setSignatureAlgorithm("SHA256");
        entity.setExpiration(Instant.now().plusSeconds(365L * 24 * 3600));
        entity.setRevoked(false);

        run("createOne", () -> RequestBuilder.builder(domain).caller(caller).createOne(entity).build().execute());
        run("readAll", () -> RequestBuilder.builder(domain).caller(caller).readAll().build().execute());
        run("readOne", () -> RequestBuilder.builder(domain).caller(caller).readOne(uuid).build().execute());

        entity.setSignatureAlgorithm("SHA512");
        run("updateOne", () -> RequestBuilder.builder(domain).caller(caller).updateOne(uuid, entity).build().execute());
        run("readOne (post-update)",
                () -> RequestBuilder.builder(domain).caller(caller).readOne(uuid).build().execute());

        run("deleteOne", () -> RequestBuilder.builder(domain).caller(caller).deleteOne(uuid).build().execute());
        run("readAll (post-delete)", () -> RequestBuilder.builder(domain).caller(caller).readAll().build().execute());
        run("deleteAll", () -> RequestBuilder.builder(domain).caller(caller).deleteAll().build().execute());
    }

    private static void demoAuthorizationCrud(IApi api, ICaller caller) {
        IDomain<?> domain = api.getDomain("authorizations").orElseThrow();
        String uuid = "demo-authz-uuid";

        Authorization entity = new Authorization();
        entity.setId("demo-authz");
        entity.setUuid(uuid);
        entity.setTenantId("tenant-acme-uuid");
        entity.setOwnerId("user-alice-uuid");
        entity.setType("bearer");
        entity.setAuthorities(List.of("ROLE_USER"));
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plusSeconds(3600));
        entity.setRevoked(Boolean.FALSE);

        run("createOne", () -> RequestBuilder.builder(domain).caller(caller).createOne(entity).build().execute());
        run("readAll", () -> RequestBuilder.builder(domain).caller(caller).readAll().build().execute());
        run("readOne", () -> RequestBuilder.builder(domain).caller(caller).readOne(uuid).build().execute());

        entity.setRevoked(Boolean.TRUE);
        run("updateOne", () -> RequestBuilder.builder(domain).caller(caller).updateOne(uuid, entity).build().execute());
        run("readOne (post-update)",
                () -> RequestBuilder.builder(domain).caller(caller).readOne(uuid).build().execute());

        run("deleteOne", () -> RequestBuilder.builder(domain).caller(caller).deleteOne(uuid).build().execute());
        run("readAll (post-delete)", () -> RequestBuilder.builder(domain).caller(caller).readAll().build().execute());
        run("deleteAll", () -> RequestBuilder.builder(domain).caller(caller).deleteAll().build().execute());
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }

    private static void run(String label, java.util.concurrent.Callable<IOperationResponse> action) {
        try {
            IOperationResponse resp = action.call();
            System.out.println("  [" + label + "] OK    -> " + resp);
        } catch (Exception e) {
            System.out.println("  [" + label + "] FAIL  -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Same as {@link #run(String, java.util.concurrent.Callable)}, but extracts
     * the typed payload from the response so the caller can chain it into
     * subsequent calls. Returns {@code null} on failure or on a payload type
     * mismatch — the diagnostic is printed inline.
     */
    private static <T> T runForResponse(String label,
            java.util.concurrent.Callable<IOperationResponse> action, Class<T> expected) {
        try {
            IOperationResponse resp = action.call();
            OperationResponseCode code = resp.getResponseCode();
            Object body = resp.getResponse();
            if (code == OperationResponseCode.UNAUTHORIZED
                    || code == OperationResponseCode.FORBIDDEN
                    || code == OperationResponseCode.CLIENT_ERROR
                    || code == OperationResponseCode.SERVER_ERROR) {
                System.out.println("  [" + label + "] FAIL  -> code=" + code + " body=" + body);
                return null;
            }
            System.out.println("  [" + label + "] OK    -> code=" + code + " body=" + body);
            if (expected.isInstance(body)) {
                return expected.cast(body);
            }
            System.out.println("  [" + label + "] WARN  -> response payload is "
                    + (body == null ? "null" : body.getClass().getSimpleName())
                    + ", expected " + expected.getSimpleName());
            return null;
        } catch (Exception e) {
            System.out.println("  [" + label + "] FAIL  -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings({ "unchecked" })
    IApi buildApi(boolean withHttp) throws ApiException {
        IBootstrap bootstrap = new Bootstrap();
        bootstrap.autoDetect(true).withPackage("com.garganttua");

        // Register OUR ApiBuilder BEFORE load() — Bootstrap.loadBootstrapBuildersFromSpi
        // dedupes by class name on manualBuilders (Bootstrap.java:519), so registering
        // first makes the SPI factory (ApiBuilderFactory) skip its empty duplicate.
        IApiBuilder builder = ApiBuilder.builder();
        bootstrap.withBuilder(builder);

        // Server mode: ONE shared Javalin interface (one HTTP server) attached to
        // every domain below via .interfasse(...). The JavalinProtocol and the
        // Jackson serializer are auto-detected from the classpath (the
        // garganttua-api-javalin dependency). In headless mode this stays null and
        // no interface is attached, so no server is started.
        FixedSupplierBuilder<JavalinInterface> httpInterface = withHttp
                ? new FixedSupplierBuilder<>(new JavalinInterface(HTTP_PORT), IClass.getClass(JavalinInterface.class))
                : null;

        // The transport protocol must be registered explicitly (it is not picked
        // up by asset auto-detection here): it teaches the pipeline how to read a
        // JavalinServletContext request and write the HTTP response back onto it.
        if (withHttp) {
            builder.protocol(new JavalinProtocol());
            // The api ships no production serializer yet (binding-jackson is an
            // empty WIP), so register the example's own JSON serializer; the
            // RESPONSE stage uses it to render entities as JSON (otherwise the
            // protocol falls back to text/plain toString).
            builder.serializer(new ExampleJsonSerializer());
        }

        // Enable workflow execution timing — injects the
        // observe("start"|"end","stage:<name>") / "script:<stage>.<name>"
        // markers ScriptGenerator emits when timing is on, so the
        // stage:*/script:* observability sections populate.
        //
        // MUST be set BEFORE bootstrap.load(). Under full-AOT the
        // dependent-builder CONFIGURE stage (which forwards this config via
        // DomainWorkflowAssembler.populateStages -> WorkflowBuilder.timing(...)
        // and then precompiles each domain workflow script) runs during
        // load(), earlier than in runtime-reflection mode where it runs during
        // build(). Set after load() and the AOT scripts are already
        // precompiled with the default disabled() timing -> empty
        // stage:*/script:* sections. Verified: before load() => both modes
        // populate; after load() => runtime OK but AOT empty. See
        // docs/AOT_workflow_timing_requires_before_load.md.
        builder.workflowTiming(WorkflowTimingConfig.of().stages(true).scripts(true));

        bootstrap.load();

        builder.multiTenant(true)
                .superTenantId(SUPER_TENANT)
                .superTenantAutoCreate(true);

        builder.exposeAuthorities().access(Access.anonymous);

        PasswordAuthentication authImpl = new PasswordAuthentication();
        var authBuilder = builder.security()
                .authentication(new FixedSupplierBuilder<>(authImpl, IClass.getClass(PasswordAuthentication.class)));
        authBuilder.authenticate("authenticate")
                .withParam(0, new PrincipalSupplierBuilder())
                .withParam(1, new AuthenticateCredentialsSupplierBuilder())
                .withParam(2, new AuthenticatorDefinitionSupplierBuilder());
        authBuilder.up();

        // Token authentication strategy for the authorization domain (the token
        // verifies itself). Param 3 injects the runtime IDomain so the method can
        // call SecurityExpressions.verifyIfSignable for real signature checking.
        TokenAuthentication tokenAuthImpl = new TokenAuthentication();
        var tokenAuthBuilder = builder.security()
                .authentication(new FixedSupplierBuilder<>(tokenAuthImpl, IClass.getClass(TokenAuthentication.class)));
        tokenAuthBuilder.authenticate("authenticate")
                .withParam(0, new PrincipalSupplierBuilder())
                // The decoded token travels as the authenticate request's
                // credentials (an Object). AuthenticateCredentialsSupplier only
                // yields byte[] (login+password) and supplies null for a token —
                // the token self-verify needs DecodedAuthorizationSupplier.
                .withParam(1, new DecodedAuthorizationSupplierBuilder())
                .withParam(2, new AuthenticatorDefinitionSupplierBuilder());
        tokenAuthBuilder.up();

        // ---- Demo entities seeded at startup via .upsert(...) ----

        Tenant acme = new Tenant();
        acme.setUuid("tenant-acme-uuid");
        acme.setId("acme");
        acme.setName("Acme Corp");
        acme.setCreatedAt(Instant.now());

        User alice = new User();
        alice.setUuid("user-alice-uuid");
        alice.setId("alice@acme");
        alice.setTenantId(acme.getUuid());
        alice.setLogin("alice@acme");
        alice.setPasswordHash(PasswordAuthentication.hash("hunter2"));
        alice.setAuthorities(List.of("ROLE_USER"));

        // 1) Tenant domain (the entity that *is* the tenant).
        IDomainBuilder<Tenant> tenantBuilder = builder.domain(IClass.getClass(Tenant.class))
                .tenant(true)
                .superTenant("superTenant")
                .entity()
                .id("id").uuid("uuid")
                .up()
                .dto(IClass.getClass(TenantDto.class))
                .id("id").uuid("uuid")
                .db(tenantDao)
                .up()
                .creation(true).readAll(true).readOne(true)
                .security()
                .readAllAccess(Access.anonymous)
                .readOneAccess(Access.anonymous)
                .creationAccess(Access.anonymous)
                .deleteAllAuthority(true)
                .up()
                .upsert(acme);
        if (withHttp) tenantBuilder.interfasse(httpInterface);
        tenantBuilder.up();

        // 2) Authorization domain (signable JWT-like token). Owned by a user.
        // Since the verifyAuthorization / authenticate unification, an
        // authorization domain MUST also be an authenticator: an incoming token
        // verifies ITSELF through this domain's authenticate pipeline. We wire
        // TokenAuthentication (real signature check) below. creation/readAll are
        // enabled because the framework persists tokens and looks them up by
        // routing through this domain's own pipeline (createOne / readAll).
        IDomainBuilder<Authorization> authorizationBuilder = builder.domain(IClass.getClass(Authorization.class))
                .tenant(false)
                .owned("ownerId")
                .entity()
                .id("id").uuid("uuid").tenantId("tenantId")
                .up()
                .dto(IClass.getClass(AuthorizationDto.class))
                .id("id").uuid("uuid").tenantId("tenantId")
                .db(authorizationDao)
                .up()
                .creation(true).readAll(true).readOne(true);

        authorizationBuilder.security()
                .authorization()
                    .type("type")
                    .authorities("authorities")
                    .expirable("expiresAt")
                    .revokable("revoked")
                    .storable(true)
                    .signable()
                        .signature("signature")
                        .getDataToSign("getDataToSign")
                    .up()
                .up()
                .authenticator()
                    .login("uuid")
                    .scope(AuthenticatorScope.tenant)
                    .alwaysEnabled(true)
                    .authentication(tokenAuthBuilder);

        if (withHttp) authorizationBuilder.interfasse(httpInterface);
        authorizationBuilder.up();

        // 3) Key domain — backing store for the persisted signing keys. The
        // .key() sub-builder binds the entity fields that hold each piece
        // of key material; the framework will auto-create / look up rows
        // on this domain at sign time, scoped by the .usage(...) below.
        IDomainBuilder<Key> keyBuilder = builder.domain(IClass.getClass(Key.class))
                .tenant(false)
                .owned("ownerId")
                .entity()
                .id("id").uuid("uuid").tenantId("tenantId")
                .up()
                .dto(IClass.getClass(Key.class))
                .id("id").uuid("uuid").tenantId("tenantId")
                .db(keyDao)
                .up()
                .creation(true).readAll(true).readOne(true);
        // @Key config now lives under .security().key() (api DSL refactor:
        // domain().key() -> domain().security().key()). Mandatory: the users
        // authenticator references this domain as its signing @Key domain.
        keyBuilder.security().key()
                .name("realmName")
                .keyAlgorithm("algorithm")
                .signatureAlgorithm("signatureAlgorithm")
                .keyForSignatureVerification("publicMaterial")
                .keyForSigning("privateMaterial")
                .expiration("expiration")
                .revoked("revoked")
                .up();
        if (withHttp) keyBuilder.interfasse(httpInterface);
        keyBuilder.up();

        // 4) User domain — the authenticator. Owns the authorizations it issues.
        IDomainBuilder<User> userBuilder = builder.domain(IClass.getClass(User.class))
                .tenant(false)
                .owner("uuid")
                .superOwner("superOwner")
                .entity()
                .id("id").uuid("uuid").tenantId("tenantId").update("login", "user-update-login")
                .up()
                .dto(IClass.getClass(UserDto.class))
                .id("id").uuid("uuid").tenantId("tenantId")
                .db(userDao)
                .up()
                .upsert(alice);

        // .authenticator().authentication(ab) returns the per-authentication
        // sub-builder; on it we declare the token domain (entity + lifeTime +
        // persisted key). The custom mint issuer would be declared on the SAME
        // sub-builder via .authorization(issuer, "issue").withParam(...) — left
        // commented for now (default framework minting signs the token).
        var userAuth = userBuilder.security()
                .creationAuthority(true)
                .authenticator()
                    .login("login")
                    .authorities("authorities")
                    .enabled("enabled")
                    .accountNonLocked("accountNonLocked")
                    .accountNonExpired("accountNonExpired")
                    .credentialsNonExpired("credentialsNonExpired")
                    .scope(AuthenticatorScope.system)
                    .authentication(authBuilder);

        // userAuth.authorization(new FixedSupplierBuilder<>(new TokenIssuer(), IClass.getClass(TokenIssuer.class)), "issue")
        //         .withParam(0, new com.garganttua.api.core.security.authorization.AuthenticationSupplierBuilder())
        //         .withParam(1, new DomainSupplierBuilder())
        //         .withParam(2, new com.garganttua.api.core.security.authorization.RequestSupplierBuilder());

        userAuth.authorization((IDomainBuilder) authorizationBuilder)
                .lifeTime(24, TimeUnit.HOURS)
                .key((IDomainBuilder) keyBuilder)
                    .usage(AuthenticatorKeyUsage.oneForEach)
                    .algorithm(KeyAlgorithm.EC_256)
                    .signatureAlgorithm(SignatureAlgorithm.SHA256)
                    .lifeTime(365, TimeUnit.DAYS);

        if (withHttp) userBuilder.interfasse(httpInterface);
        userBuilder.up();


        return bootstrap.build().toList().stream()
                .filter(IApi.class::isInstance)
                .map(IApi.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private void showDomains(IApi api) {
        System.out.println("\n[domains] registered:");
        for (String name : DOMAIN_NAMES) {
            api.getDomain(name).ifPresent(d -> System.out.println("  - " + d.getDomainName()
                    + "  entity=" + d.getEntityClass().getSimpleName()));
        }
        System.out.println(
                "\n[next] invoke an authenticate OperationRequest on the users domain to issue a signed Authorization.");
        System.out.println(
                "       See garganttua-api-core SignAuthorizationIntegrationTest for the exact request shape.");
    }
}
