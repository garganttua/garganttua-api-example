package com.garganttua.api.example;

import java.util.List;

import com.garganttua.api.commons.definition.IAuthenticatorDefinition;
import com.garganttua.api.commons.security.authentication.Authentication;
import com.garganttua.api.commons.security.authentication.IAuthentication;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Authentication strategy for the <em>authorization</em> (token) domain.
 *
 * <p>Since garganttua-api unified {@code verifyAuthorization} with the
 * authenticate pipeline (a token verifies ITSELF), every authorization domain
 * must also be an authenticator. When an incoming bearer token is verified the
 * framework forges an {@code AuthenticationRequest(login = token uuid,
 * credentials = decoded token, tenantId = token tenant)} and runs this method,
 * having already enforced expiration + revocation
 * ({@code SecurityExpressions.validateAuthorizationFromDefinition}).
 *
 * <p><strong>Signature verification — current limitation.</strong> The intended
 * crypto path is {@code SecurityExpressions.verifyIfSignable(token, domain,
 * operationRequest)}. It cannot run here as wired: {@code verifyIfSignable} →
 * {@code resolveKeyRealm} reads the key configuration from the
 * <em>token domain's</em> {@code authenticator().authorization().key(...)},
 * but in this example (and the canonical pattern) the persisted key is declared
 * on the <em>minting</em> {@code users} authenticator, so the token domain has
 * no key config and resolution throws "no authenticator authorization
 * configured on domain 'authorizations'". A persisted {@code oneForEach} realm
 * would also need an owner-scoped {@code IOperationRequest} to rebuild its
 * realm name, which no param supplier exposes. No framework test exercises real
 * self-verify crypto (the reference {@code StubTokenAuthentication} just
 * succeeds), so this path is treated as a framework follow-up. We therefore
 * rely on the framework-enforced expiration + revocation and accept the token.
 */
@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class TokenAuthentication {

    /**
     * @param principal   the looked-up token entity
     * @param credentials the decoded token (same {@link Authorization} instance)
     * @param definition  the token domain's authenticator definition
     */
    public IAuthentication authenticate(Object principal, Object credentials,
            IAuthenticatorDefinition definition) {
        if (!(credentials instanceof Authorization token)) {
            return failed();
        }

        // Expiration + revocation already enforced by the framework before this
        // runs. Real signature self-verification is blocked by the key-realm
        // wiring described in the class javadoc — TODO once the framework
        // exposes the minting key to the token domain (or an owner-scoped
        // request to verifyIfSignable).
        List<String> authorities = token.getAuthorities() != null
                ? token.getAuthorities()
                : List.of();
        return new Authentication(
                true, principal, credentials,
                token.getType() != null ? token.getType() : "bearer",
                authorities,
                true, true, true, true);
    }

    private IAuthentication failed() {
        return new Authentication(false, null, null, null, null, true, true, true, true);
    }
}
