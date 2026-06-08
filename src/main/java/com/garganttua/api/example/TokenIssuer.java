package com.garganttua.api.example;

import java.time.Instant;
import java.util.UUID;

import com.garganttua.api.commons.context.IDomain;
import com.garganttua.api.commons.security.authentication.IAuthentication;
import com.garganttua.api.commons.service.IOperationRequest;
import com.garganttua.api.core.expression.SecurityExpressions;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Custom token forge — the mint-side dual of {@link TokenAuthentication}.
 *
 * <p>Declared as a <em>method</em> (not an interface instance) via
 * {@code .authorization().issuer(supplier, "issue").withParam(...)}, exactly
 * like the verify-side {@code .authentication(supplier).authenticate("method")}.
 * The parameters below are a free signature resolved from the runtime context
 * by their suppliers: the {@code IAuthentication} result, the authenticator
 * {@code IDomain}, and the in-flight {@code IOperationRequest}.
 *
 * <p>When declared, the framework delegates token production to this method
 * instead of its built-in minting (so this method owns the token's shape; the
 * built-in signing step does not run on this path — a real issuer would sign or
 * delegate to an external authorization server here).
 */
@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class TokenIssuer {

    public Object issue(IAuthentication authentication, IDomain<?> domain, IOperationRequest request) {
        Authorization token = new Authorization();
        token.setUuid(UUID.randomUUID().toString());
        // Marker proving the framework delegated production to this method
        // rather than running its default minting.
        token.setType("ISSUED-BY-CUSTOM-METHOD");
        token.setAuthorities(authentication.authorities());
        if (authentication.principal() instanceof User user) {
            token.setOwnerId("users:" + user.getUuid());
            token.setTenantId(user.getTenantId());
        }
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setRevoked(Boolean.FALSE);

        // A custom issuer owns production INCLUDING the signature (the framework
        // does not auto-sign on this path). Delegate to the framework's own
        // signing, which resolves the persisted key realm from the authenticator
        // domain (.authorization(tokenDomain).key(...)) scoped by the request,
        // signs getDataToSign() into the signature field, and stamps signedBy.
        SecurityExpressions.signIfSignable(token, domain, request);
        return token;
    }
}
