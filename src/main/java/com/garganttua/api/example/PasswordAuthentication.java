package com.garganttua.api.example;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.garganttua.api.commons.definition.IAuthenticatorDefinition;
import com.garganttua.api.commons.security.authentication.Authentication;
import com.garganttua.api.commons.security.authentication.IAuthentication;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected(allDeclaredFields = true, queryAllDeclaredMethods = true, queryAllDeclaredConstructors = true)
public class PasswordAuthentication {

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public IAuthentication authenticate(Object principal, byte[] credentials, IAuthenticatorDefinition definition) {
        if (!(principal instanceof User user)) {
            return failed(credentials);
        }
        String submitted = new String(credentials, StandardCharsets.UTF_8);
        boolean ok = user.getPassword() != null
                && passwordEncoder.matches(submitted, user.getPassword());
        if (!ok) {
            return failed(credentials);
        }
        List<String> authorities = user.getAuthorities() != null
                ? user.getAuthorities()
                : List.of("ROLE_USER");
        return new Authentication(
                true, user, credentials,
                user.getUuid(),
                authorities,
                true, true, true, true);
    }

    /**
     * {@code applySecurityOnEntity} hook (wired via
     * {@code .applySecurityOnEntity("hashPassword")}).
     * The framework calls it on the User being created/updated, BEFORE persist,
     * with the
     * entity auto-wired as the first argument ({@code SecuredEntitySupplier},
     * supplied type
     * {@code Object}). Hashes the plaintext password in place with the SAME
     * {@link BCryptPasswordEncoder} that {@link #authenticate} verifies with, so
     * the store
     * never holds it in clear. Returning the entity is optional (in-place mutation
     * is honored).
     */
    public Object hashPassword(Object entity) {
        if (entity instanceof User user && user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return entity;
    }

    IAuthentication failed(byte[] credentials) {
        return new Authentication(false, null, credentials, null, null, true, true, true, true);
    }
}
