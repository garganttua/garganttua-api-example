package com.garganttua.api.example;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.garganttua.api.binding.javalin.JavalinInterface;
import com.garganttua.api.binding.javalin.JavalinProtocol;
import com.garganttua.api.commons.ApiException;
import com.garganttua.api.commons.caller.ICaller;
import com.garganttua.api.commons.context.IApi;
import com.garganttua.api.commons.context.IDomain;
import com.garganttua.api.commons.context.dsl.IApiBuilder;
import com.garganttua.api.commons.context.dsl.IDomainBuilder;
import com.garganttua.api.commons.context.dsl.security.IAuthenticationBuilder;
import com.garganttua.api.commons.context.dsl.security.IAuthenticationMethodBinderBuilder;
import com.garganttua.api.commons.operation.Access;
import com.garganttua.api.commons.operation.OperationDefinition;
import com.garganttua.api.commons.security.annotations.AuthenticatorKeyUsage;
import com.garganttua.api.commons.security.authenticator.AuthenticatorScope;
import com.garganttua.api.commons.service.IOperationResponse;
import com.garganttua.api.commons.service.OperationResponseCode;
import com.garganttua.api.core.api.ApiBuilder;
import com.garganttua.api.core.caller.Caller;
import com.garganttua.api.core.security.authentication.AuthenticateCredentialsSupplierBuilder;
import com.garganttua.api.core.security.authentication.AuthenticationRequest;
import com.garganttua.api.core.security.authentication.AuthenticatorDefinitionSupplierBuilder;
import com.garganttua.api.core.security.authentication.DecodedAuthorizationSupplierBuilder;
import com.garganttua.api.core.security.authentication.PrincipalSupplierBuilder;
import com.garganttua.api.core.security.authentication.SecuredEntitySupplierBuilder;
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
    /** Domains exposed in non-multi-tenant mode (no Tenant domain). */
    private static final String[] SINGLE_TENANT_DOMAIN_NAMES = { "authorizations", "keys", "users" };
    private static final String DEMO_TENANT_ID = "tenant-acme-uuid";

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
        // notenant / single / --no-tenant: build the API WITHOUT multi-tenancy —
        // no Tenant domain, authenticator scope=system (no X-Tenant-Id required),
        // owner-scoping still applies. Default is multi-tenant. Combinable with
        // every other flag, e.g. `java ... ExampleApplication notenant server`.
        boolean multiTenant = !hasArg(args, "notenant", "--notenant", "--no-tenant", "single", "--single", "singletenant");

        ExampleApplication app = new ExampleApplication();
        IApi api = app.buildApi(server, multiTenant);

        String[] domains = multiTenant ? DOMAIN_NAMES : SINGLE_TENANT_DOMAIN_NAMES;

        if (server) {
            // Building the API ran the lifecycle, which started the Javalin
            // server (Domain.doStart -> IInterface.onStart binds the port).
            System.out.println();
            System.out.println("========== SERVER MODE" + (multiTenant ? "" : " (single-tenant)") + " ==========");
            System.out.println("Garganttua API example serving on http://localhost:" + HTTP_PORT);
            System.out.println("Domains exposed: " + String.join(", ", domains));
            System.out.println("Try: curl http://localhost:" + HTTP_PORT + "/" + domains[0]);
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

        System.out.println(api.getAuthorities());

        // The Tenant domain only exists in multi-tenant mode.
        if (multiTenant) {
            ICaller caller = Caller.createSuperCaller(api.getSuperTenantId());
            section("TENANTS");
            demoTenantCrud(api, caller);
        }

        section("AUTHENTICATION" + (multiTenant ? "" : " (single-tenant, scope=system)"));
        Authorization aliceAuthorization = demoAuthentication(api, multiTenant);

        // Replay the issued token through the API (Mode B): the
        // verifyAuthorization → token self-verify pipeline now performs real
        // signature verification (verifyTokenSignature) before the business
        // authenticate, then grants the rights stamped on the token.
        if (aliceAuthorization != null) {
            section("AUTHENTICATED CALLS (using Alice's authorization)");
            demoAuthenticatedCalls(api, aliceAuthorization, multiTenant);

            section("USERS");
            demoUserCrud(api, aliceAuthorization, multiTenant);
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
     * Renders a filtered slice of the {@link CoreStatsObserver} snapshot as a
     * box-drawn table, sorted by total time descending, with a TOTAL footer.
     * The {@code label} is the first column's header — kept short so each section
     * reads like "what dominates this layer of the pipeline". The first column
     * sizes to the widest source (capped); numeric columns are right-aligned with
     * thousands separators.
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

        String[] headers = { label, "count", "ok", "ko", "total µs", "min µs", "max µs", "avg µs" };
        boolean[] rightAlign = { false, true, true, true, true, true, true, true };
        int cols = headers.length;
        int firstColCap = 56;

        java.util.List<String[]> table = new java.util.ArrayList<>();
        long tCount = 0;
        long tOk = 0;
        long tKo = 0;
        long tTotal = 0;
        long tMin = Long.MAX_VALUE;
        long tMax = 0;
        for (var s : rows) {
            long total = toMicros(s.totalDuration());
            long min = toMicros(s.minDuration());
            long max = toMicros(s.maxDuration());
            table.add(new String[] {
                    truncate(s.source(), firstColCap),
                    num(s.count()), num(s.successCount()), num(s.failureCount()),
                    num(total), num(min), num(max), num(toMicros(s.averageDuration())) });
            tCount += s.count();
            tOk += s.successCount();
            tKo += s.failureCount();
            tTotal += total;
            tMin = Math.min(tMin, min);
            tMax = Math.max(tMax, max);
        }
        String[] footer = {
                "TOTAL (" + rows.size() + ")",
                num(tCount), num(tOk), num(tKo),
                num(tTotal), num(tMin == Long.MAX_VALUE ? 0 : tMin), num(tMax),
                num(tCount == 0 ? 0 : tTotal / tCount) };

        int[] w = new int[cols];
        for (int i = 0; i < cols; i++) {
            w[i] = headers[i].length();
        }
        for (String[] r : table) {
            for (int i = 0; i < cols; i++) {
                w[i] = Math.max(w[i], r[i].length());
            }
        }
        for (int i = 0; i < cols; i++) {
            w[i] = Math.max(w[i], footer[i].length());
        }

        System.out.println(tableBorder(w, '┌', '┬', '┐'));
        System.out.println(tableRow(headers, w, rightAlign));
        System.out.println(tableBorder(w, '├', '┼', '┤'));
        for (String[] r : table) {
            System.out.println(tableRow(r, w, rightAlign));
        }
        System.out.println(tableBorder(w, '├', '┼', '┤'));
        System.out.println(tableRow(footer, w, rightAlign));
        System.out.println(tableBorder(w, '└', '┴', '┘'));
    }

    private static long toMicros(Duration d) {
        return d == null ? 0L : d.toNanos() / 1_000L;
    }

    /** Groups an integer with thousands separators (ROOT locale → stable comma). */
    private static String num(long v) {
        return String.format(java.util.Locale.ROOT, "%,d", v);
    }

    /** A horizontal box-drawing rule with the given left / column-junction / right glyphs. */
    private static String tableBorder(int[] widths, char left, char junction, char right) {
        StringBuilder sb = new StringBuilder("  ").append(left);
        for (int i = 0; i < widths.length; i++) {
            sb.append("─".repeat(widths[i] + 2));
            sb.append(i == widths.length - 1 ? right : junction);
        }
        return sb.toString();
    }

    /** A table row; each cell padded to its column width and left/right aligned. */
    private static String tableRow(String[] cells, int[] widths, boolean[] rightAlign) {
        StringBuilder sb = new StringBuilder("  │");
        for (int i = 0; i < cells.length; i++) {
            String fmt = "%" + (rightAlign[i] ? "" : "-") + widths[i] + "s";
            sb.append(' ').append(String.format(fmt, cells[i])).append(" │");
        }
        return sb.toString();
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
    private static Authorization demoAuthentication(IApi api, boolean multiTenant) {
        IDomain<?> domain = api.getDomain("users").orElseThrow();
        OperationDefinition authOp = OperationDefinition.authenticate("users", IClass.getClass(User.class));
        // Tenant-scoped auth takes the tenant from the CALLER (request tenantId /
        // X-Tenant-Id header). In single-tenant mode (scope=system) there is no
        // tenant: authReq() simply omits it.
        String tenantId = multiTenant ? DEMO_TENANT_ID : null;

        // The authorization domain declares .encode("toWire") (JWT), so authenticate
        // returns the JWT compact string, not the entity. Capture it as a String
        // and rebuild the Authorization (via fromWire) so the Mode-B demos below can
        // replay it.
        String issuedJwt = runForResponse("authenticate (valid)",
                () -> authReq(domain, authOp, tenantId, "alice@acme", "hunter2").build().execute(),
                String.class);
        Authorization issued = null;
        if (issuedJwt != null) {
            issued = new Authorization();
            issued.fromWire(issuedJwt.getBytes(StandardCharsets.UTF_8));
        }

        run("authenticate (wrong password)",
                () -> authReq(domain, authOp, tenantId, "alice@acme", "wrong-password").build().execute());

        run("authenticate (unknown login)",
                () -> authReq(domain, authOp, tenantId, "ghost@acme", "anything").build().execute());

        // The "missing caller tenant" rejection only exists for tenant scope.
        if (multiTenant) {
            run("authenticate (missing caller tenant, scope=tenant)",
                    () -> authReq(domain, authOp, null, "alice@acme", "hunter2").build().execute());
        }

        return issued;
    }

    /**
     * Builds an authenticate request, attaching the caller tenant only when
     * {@code tenantId} is non-null (single-tenant / scope=system passes null).
     */
    private static com.garganttua.api.commons.service.IRequestBuilder authReq(IDomain<?> domain,
            OperationDefinition authOp, String tenantId, String login, String password) {
        var rb = RequestBuilder.builder(domain)
                .operation(authOp)
                .body(new AuthenticationRequest(login, password.getBytes(StandardCharsets.UTF_8)));
        if (tenantId != null) {
            rb.tenantId(tenantId);
        }
        return rb;
    }

    /**
     * Replays the issued Authorization on the authorizations domain — the
     * framework's VERIFY_AUTHORIZATION step picks it up via the
     * {@code "authorization"} arg (Mode B) and grants Alice the rights
     * stamped onto the token.
     */
    private static void demoAuthenticatedCalls(IApi api, Authorization authorization, boolean multiTenant) {
        IDomain<?> authzDomain = api.getDomain("authorizations").orElseThrow();

        System.out.println("  [issued] uuid=" + authorization.getUuid()
                + " ownerId=" + authorization.getOwnerId()
                + " tenantId=" + authorization.getTenantId()
                + " expiresAt=" + authorization.getExpiresAt()
                + " signature=" + (authorization.getSignature() == null
                        ? "null"
                        : "[" + authorization.getSignature().length + " bytes]"));

        run("readAll authorizations (as Alice)", () -> aliceRequest(authzDomain, authorization, multiTenant)
                .readAll().build().execute());

        run("readOne own authorization (as Alice)", () -> aliceRequest(authzDomain, authorization, multiTenant)
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
    private static void demoUserCrud(IApi api, Authorization authorization, boolean multiTenant) {
        IDomain<?> domain = api.getDomain("users").orElseThrow();
        String ownUuid = authorization.getOwnerId();
        String otherUuid = "demo-user-uuid";

        run("readOne self (as Alice)", () -> aliceRequest(domain, authorization, multiTenant)
                .readOne(ownUuid).build().execute());

        run("readAll users (as Alice — owner-scoped)", () -> aliceRequest(domain, authorization, multiTenant)
                .readAll().build().execute());

        run("readOne someone else (as Alice — must fail)", () -> aliceRequest(domain, authorization, multiTenant)
                .readOne(otherUuid).build().execute());

        // Self-update: change Alice's login string. The framework verifies the
        // owner filter and lets it through because ownUuid matches the
        // caller's ownerId.
        User selfPatch = new User();
        selfPatch.setId("alice@acme");
        selfPatch.setUuid(ownUuid);
        selfPatch.setTenantId(authorization.getTenantId());
        selfPatch.setLogin("alice.renamed@acme");
        selfPatch.setPassword("hunter2");
        selfPatch.setAuthorities(authorization.getAuthorities());

        run("updateOne self (as Alice)", () -> aliceRequest(domain, authorization, multiTenant)
                .updateOne(ownUuid, selfPatch).build().execute());
        run("readOne self (post-update)", () -> aliceRequest(domain, authorization, multiTenant)
                .readOne(ownUuid).build().execute());

        // Attempt to create another user — Alice is ROLE_USER, no admin
        // authority, so the pipeline rejects this.
        User bob = new User();
        bob.setId("bob@demo");
        bob.setUuid(otherUuid);
        bob.setTenantId(authorization.getTenantId());
        bob.setLogin("bob@demo");
        bob.setPassword("s3cret");
        bob.setAuthorities(List.of("ROLE_USER"));

        run("createOne another user (as Alice — must fail)", () -> aliceRequest(domain, authorization, multiTenant)
                .createOne(bob).build().execute());
    }

    /**
     * Builds a {@code RequestBuilder} primed with Alice's caller context
     * (tenantId, ownerId, authorities) and the issued Authorization in the
     * Mode B {@code "authorization"} arg — every authenticated demo call
     * shares this prelude.
     */
    private static com.garganttua.api.commons.service.IRequestBuilder aliceRequest(IDomain<?> domain,
            Authorization authorization, boolean multiTenant) {
        var rb = RequestBuilder.builder(domain)
                .callerId(authorization.getOwnerId())
                .ownerId(authorization.getOwnerId())
                .authorities(authorization.getAuthorities())
                .param("authorization", authorization);
        // Tenant context only in multi-tenant mode; under scope=system there is none.
        if (multiTenant && authorization.getTenantId() != null) {
            rb.tenantId(authorization.getTenantId())
              .requestedTenantId(authorization.getTenantId());
        }
        return rb;
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
    IApi buildApi(boolean withHttp, boolean multiTenant) throws ApiException {
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
            // Serializers are NOT picked up by asset auto-detection in this wiring,
            // so register each media type explicitly (the list is additive). Without
            // a matching serializer the negotiator answers 406.
            //   application/json + application/xml -> the binding's production
            //     serializers (now Instant-safe: they register JavaTimeModule).
            //   text/xml -> the binding ships no text/xml serializer, so the example
            //     fills that alias with ExampleXmlSerializer.
            builder.serializer(new com.garganttua.api.binding.jackson.JacksonJsonSerializer());
            builder.serializer(new com.garganttua.api.binding.jackson.JacksonXmlSerializer());
            builder.serializer(new ExampleXmlSerializer(com.garganttua.api.commons.MimeType.TEXT_XML));
            // Bearer scheme -> Authorization token. Lets a NON-authorization domain
            // (keys, users) be reached over HTTP with `Authorization: Bearer <jwt>`;
            // the token's own domain self-decodes the raw header without it.
            builder.authorizationProtocol(new BearerAuthorizationProtocol());
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

        // Multi-tenancy toggle. In single-tenant mode there is no Tenant domain
        // and no super-tenant; authenticators run with scope=system (below).
        builder.multiTenant(multiTenant);
        if (multiTenant) {
            builder.superTenantId(SUPER_TENANT).superTenantAutoCreate(true);
        }

        // Authenticator scope: tenant-scoped (per-tenant principal lookup, caller
        // must carry a tenantId) vs system (global, no tenant). Used by both the
        // users authenticator and the authorization self-verify authenticator.
        AuthenticatorScope scope = multiTenant ? AuthenticatorScope.tenant : AuthenticatorScope.system;

        builder.exposeAuthorities().access(Access.anonymous);

        PasswordAuthentication authImpl = new PasswordAuthentication();
        IAuthenticationBuilder authBuilder = (IAuthenticationBuilder) builder.security()
                .authentication(new FixedSupplierBuilder<>(authImpl, IClass.getClass(PasswordAuthentication.class)))
                .authenticate("authenticate")
                .withParam(0, new PrincipalSupplierBuilder())
                .withParam(1, new AuthenticateCredentialsSupplierBuilder())
                .withParam(2, new AuthenticatorDefinitionSupplierBuilder()).up()
                .applySecurityOnEntity("hashPassword")
                .withParam(0,  new SecuredEntitySupplierBuilder()).up();


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
                .withParam(2, new AuthenticatorDefinitionSupplierBuilder()).up();
        tokenAuthBuilder.up();

        // ---- Demo entities seeded at startup via .upsert(...) ----

        Tenant acme = new Tenant();
        // Single source of truth: acme's uuid IS the tenant the auth demos
        // authenticate against (DEMO_TENANT_ID / X-Tenant-Id). They MUST match —
        // authentication is tenant-scoped, so a mismatch means alice is looked up
        // in the wrong tenant → "All authentication methods failed".
        acme.setUuid(DEMO_TENANT_ID);
        acme.setId("acme");
        acme.setName("Acme Corp");
        acme.setCreatedAt(Instant.now());

        User alice = new User();
        alice.setUuid("user-alice-uuid");
        alice.setId("alice@acme");
        // No tenant binding in single-tenant mode.
        alice.setTenantId(multiTenant ? acme.getUuid() : null);
        alice.setLogin("alice@acme");
        alice.setPassword("hunter2");
        alice.setAuthorities(List.of("ROLE_USER"));

        // 1) Tenant domain (the entity that *is* the tenant) — multi-tenant only.
        if (multiTenant) {
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
        }

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
                    // JWT-shaped transport form: the framework returns toWire()
                    // (type.base64(payload).base64(signature)) as the authenticate
                    // response and decodes incoming Bearer tokens via fromWire().
                    .encode("toWire")
                    .decode("fromWire")
                    // Records WHICH persisted @Key signed the token (${keyDomain}:${uuid}).
                    // Framework stamps it after signing; the self-verify path
                    // (verifyTokenSignature → DomainKeySupplier) reads it to resolve the
                    // exact verification key. Without it: 401 "cannot verify the token
                    // signature — it carries no qualified signedBy".
                    .signedBy("signedBy")
                    .signable()
                        .signature("signature")
                        .getDataToSign("getDataToSign")
                    .up()
                .up()
                .authenticator()
                    .login("uuid")
                    .scope(scope)
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
                // The authenticator domain must expose readAll: the principal
                // lookup now routes through the domain's readAll pipeline
                // (SecurityExpressions.invokeReadAll), so an authenticator-only
                // domain still needs the read operations enabled.
                .creation(true).readAll(true).readOne(true)
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
                    .scope(scope)
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
