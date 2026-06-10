package com.garganttua.api.example;

import java.nio.charset.StandardCharsets;

import com.garganttua.api.commons.ApiException;
import com.garganttua.api.commons.context.IApi;
import com.garganttua.api.commons.security.authorization.IAuthorizationProtocol;
import com.garganttua.core.reflection.IClass;

/**
 * Maps the HTTP {@code Authorization: Bearer <jwt>} scheme to the example's
 * {@link Authorization} token.
 *
 * <p>Needed for a NON-authorization domain (e.g. {@code keys}, {@code users}) to
 * be reached with a token over HTTP. VERIFY_AUTHORIZATION decodes such a request
 * in two ways: Mode A on the token's OWN authorization domain self-decodes the
 * raw header via the configured {@code decode} method; but any OTHER protected
 * domain must resolve an {@code IAuthorizationProtocol} for the header's scheme
 * to turn {@code Bearer <jwt>} into an {@link Authorization} entity — which then
 * runs the usual self-verify (signature, expiration, revocation). Registered via
 * {@code builder.authorizationProtocol(...)}.
 */
public class BearerAuthorizationProtocol implements IAuthorizationProtocol {

    @Override
    public String scheme() {
        return "Bearer";
    }

    @Override
    public IClass<?> targetDomain() {
        return IClass.getClass(Authorization.class);
    }

    @Override
    public Object decode(String rawAuthorizationValue, IApi api) throws ApiException {
        Authorization token = new Authorization();
        token.fromWire(rawAuthorizationValue.getBytes(StandardCharsets.UTF_8));
        return token;
    }
}
