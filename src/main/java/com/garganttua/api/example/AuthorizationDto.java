package com.garganttua.api.example;

import java.time.Instant;
import java.util.List;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Persistence shape of {@link Authorization} (bound to the DAO via {@code .db(...)}).
 * The framework maps entity → DTO on store and DTO → entity on read, by
 * {@link FieldMappingRule}. It therefore must mirror EVERY persistable field of
 * the entity — a partial DTO silently drops the missing fields at store time, so
 * a read-back authorization comes out mostly null (the previous bug: only
 * id/uuid/tenantId survived). It carries {@code signature} / {@code signedBy} too
 * so a stored token round-trips losslessly (token reuse can re-encode it); those
 * are hidden from HTTP reads by {@code @JsonIgnore} on the entity, not here — the
 * DTO is never serialized over the wire, it is the internal storage record.
 */
@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class AuthorizationDto {

    @FieldMappingRule(sourceFieldAddress = "id")
    String id;
    @FieldMappingRule(sourceFieldAddress = "uuid")
    String uuid;
    @FieldMappingRule(sourceFieldAddress = "tenantId")
    String tenantId;
    @FieldMappingRule(sourceFieldAddress = "ownerId")
    String ownerId;

    @FieldMappingRule(sourceFieldAddress = "type")
    String type;
    @FieldMappingRule(sourceFieldAddress = "authorities")
    List<String> authorities;

    @FieldMappingRule(sourceFieldAddress = "createdAt")
    Instant createdAt;
    @FieldMappingRule(sourceFieldAddress = "expiresAt")
    Instant expiresAt;
    @FieldMappingRule(sourceFieldAddress = "revoked")
    Boolean revoked;

    @FieldMappingRule(sourceFieldAddress = "signature")
    byte[] signature;
    @FieldMappingRule(sourceFieldAddress = "signedBy")
    String signedBy;

    public AuthorizationDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<String> getAuthorities() { return authorities; }
    public void setAuthorities(List<String> authorities) { this.authorities = authorities; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Boolean getRevoked() { return revoked; }
    public void setRevoked(Boolean revoked) { this.revoked = revoked; }

    public byte[] getSignature() { return signature; }
    public void setSignature(byte[] signature) { this.signature = signature; }
    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; }
}
