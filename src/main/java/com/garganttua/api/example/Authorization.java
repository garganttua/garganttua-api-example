package com.garganttua.api.example;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

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

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getDataToSign() {
        String payload = String.valueOf(uuid)
                + "|" + String.valueOf(ownerId)
                + "|" + String.valueOf(tenantId)
                + "|" + String.valueOf(type);
        return payload.getBytes(StandardCharsets.UTF_8);
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
                '}';
    }
}
