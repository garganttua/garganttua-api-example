package com.garganttua.api.example;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import com.garganttua.api.commons.definition.IAuthenticatorDefinition;
import com.garganttua.api.commons.security.authentication.Authentication;
import com.garganttua.api.commons.security.authentication.IAuthentication;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class PasswordAuthentication {

    public static String hash(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public IAuthentication authenticate(Object principal, byte[] credentials, IAuthenticatorDefinition definition) {
        if (!(principal instanceof User user)) {
            return failed(credentials);
        }
        String submitted = new String(credentials, StandardCharsets.UTF_8);
        boolean ok = user.getPasswordHash() != null
                && user.getPasswordHash().equals(hash(submitted));
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

IAuthentication failed(byte[] credentials) {
        return new Authentication(false, null, credentials, null, null, true, true, true, true);
    }
}
