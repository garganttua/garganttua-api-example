package com.garganttua.api.example;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected(allDeclaredFields = true, queryAllDeclaredMethods = true, queryAllDeclaredConstructors = true)
public class Authorization {

    String id;
    String uuid;
    String tenantId;
    String ownerId;

    String type;
    List<String> authorities;

    Instant createdAt;
    Instant expiresAt;
    Boolean revoked;

    byte[] signature;

    // Qualified reference (${keyDomain}:${uuid}) of the @Key that signed this
    // token. STAMPED BY THE FRAMEWORK *after* signing (SecurityExpressions
    // .signIfSignable → stampSignedBy), and read back at verify time
    // (verifyTokenSignature → DomainKeySupplier.resolveSignerKey) to resolve the
    // exact verification key. Declared via .authorization().signedBy("signedBy").
    // It therefore must NOT be part of getDataToSign() (it is null when the bytes
    // are signed) but MUST survive the wire round-trip — see toWire/fromWire.
    String signedBy;

    public Authorization() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    // signature / signedBy / dataToSign are wire-internal signing material: they
    // belong in the JWT and the persistence DTO, never in an HTTP read body.
    // @JsonIgnore hides them from serialization only; the framework's reflective
    // field mapping (entity <-> AuthorizationDto) still round-trips them.
    @JsonIgnore
    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    @JsonIgnore
    public String getSignedBy() {
        return signedBy;
    }

    public void setSignedBy(String signedBy) {
        this.signedBy = signedBy;
    }

    @JsonIgnore
    public byte[] getDataToSign() {
        // The JWT signing input over the SIGNED claim set (signedBy excluded — it
        // is stamped only after signing). Deterministic, so the bytes signed at
        // mint match the bytes recomputed at verify on the decoded token.
        return signingInput().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Transport (wire) encode — a real compact JWT:
     * {@code base64url(header).base64url(payload).base64url(signature)} with a
     * JSON header {@code {"alg":"ES256","typ":"JWT"}} and JSON claims. Declared
     * via {@code .authorization().encode("toWire")}; the framework returns this
     * as the authenticate/refresh response.
     * <p>
     * The wire payload carries one claim MORE than {@link #getDataToSign()}: the
     * {@code kid} = {@link #signedBy} reference, which the framework stamps only
     * after signing and which verify needs to resolve the signing key. The
     * signature therefore covers the signed claim set (without {@code kid}); the
     * framework's verify recomputes {@code getDataToSign()} on the decoded entity
     * — which also omits {@code kid} — so it round-trips. (A third-party JWT lib
     * that re-hashes the literal wire {@code header.payload} would see the extra
     * {@code kid}; this matches the framework's existing non-RFC ES256 DER
     * signature encoding — interop is internal to garganttua.)
     */
    public String toWire() {
        String sig = signature == null ? ""
                : java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        return header() + "." + payload(true) + "." + sig;
    }

    /** Decode side — rebuilds this entity from a JWT produced by {@link #toWire()}. */
    public void fromWire(byte[] raw) {
        String[] parts = new String(raw, StandardCharsets.UTF_8).split("\\.", -1);
        try {
            java.util.Map<?, ?> c = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(java.util.Base64.getUrlDecoder().decode(parts[1]), java.util.Map.class);
            this.uuid = str(c.get("jti"));
            this.ownerId = str(c.get("sub"));
            this.tenantId = str(c.get("tid"));
            this.type = str(c.get("ttype"));
            Object auth = c.get("authorities");
            this.authorities = (auth instanceof java.util.List<?> l)
                    ? l.stream().map(String::valueOf).toList() : null;
            this.createdAt = c.get("iat") == null ? null
                    : Instant.ofEpochSecond(((Number) c.get("iat")).longValue());
            this.expiresAt = c.get("exp") == null ? null
                    : Instant.ofEpochSecond(((Number) c.get("exp")).longValue());
            this.revoked = Boolean.valueOf(String.valueOf(c.get("revoked")));
            this.signedBy = str(c.get("kid"));
        } catch (Exception e) {
            throw new IllegalStateException("fromWire: invalid JWT payload", e);
        }
        this.signature = parts[2].isEmpty() ? null
                : java.util.Base64.getUrlDecoder().decode(parts[2]);
    }

    /** Fixed, deterministic JWT header segment {@code base64url({"alg":"ES256","typ":"JWT"})}. */
    private String header() {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"ES256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    }

    /** The JWT signing input {@code header.payload} over the SIGNED claim set (no {@code kid}). */
    private String signingInput() {
        return header() + "." + payload(false);
    }

    /**
     * Builds the base64url JSON claims segment. {@code includeSignedBy} adds the
     * {@code kid} claim (the {@link #signedBy} key reference) — true for the wire
     * form, false for the signed bytes. Ordered (LinkedHashMap) so the segment is
     * deterministic across mint and verify.
     */
    private String payload(boolean includeSignedBy) {
        var enc = java.util.Base64.getUrlEncoder().withoutPadding();
        java.util.Map<String, Object> claims = new java.util.LinkedHashMap<>();
        claims.put("jti", uuid);
        claims.put("sub", ownerId);
        claims.put("tid", tenantId);
        claims.put("ttype", type);
        claims.put("authorities", authorities);
        claims.put("iat", createdAt == null ? null : createdAt.getEpochSecond());
        claims.put("exp", expiresAt == null ? null : expiresAt.getEpochSecond());
        claims.put("revoked", revoked == null ? Boolean.FALSE : revoked);
        if (includeSignedBy) {
            claims.put("kid", signedBy);
        }
        try {
            return enc.encodeToString(
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(claims));
        } catch (Exception e) {
            throw new IllegalStateException("toWire: failed to build JWT payload", e);
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    @Override
    public String toString() {
        return "Authorization{" +
                "id='" + id + '\'' +
                ", uuid='" + uuid + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", type='" + type + '\'' +
                ", authorities=" + authorities +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", revoked=" + revoked +
                ", signature=" + (signature == null ? "null" : "[" + signature.length + " bytes]") +
                ", signedBy='" + signedBy + '\'' +
                '}';
    }
}
