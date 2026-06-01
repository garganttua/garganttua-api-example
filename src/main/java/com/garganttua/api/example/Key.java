package com.garganttua.api.example;

import java.time.Instant;

import com.garganttua.core.crypto.IKey;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Key entity — merged entity + DTO (the framework now expects identical
 * field shapes for both, with {@code IKey}-typed material fields rather
 * than raw {@code byte[]}, so the redundant DTO has been collapsed away).
 *
 * <p>Material fields hold {@link com.garganttua.core.crypto.IKey} runtime
 * crypto objects directly. The framework's mapper layer is responsible
 * for the {@code IKey ↔ bytes} bridge when a real DAO is plugged in;
 * the in-memory DAO used here just keeps the references as-is.
 */
@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class Key {

    String id;
    String uuid;
    String tenantId;
    String ownerId;

    String realmName;
    String algorithm;
    String signatureAlgorithm;

    IKey publicMaterial;
    IKey privateMaterial;

    Instant expiration;
    boolean revoked;

    public Key() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getRealmName() { return realmName; }
    public void setRealmName(String realmName) { this.realmName = realmName; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public void setSignatureAlgorithm(String s) { this.signatureAlgorithm = s; }

    public IKey getPublicMaterial() { return publicMaterial; }
    public void setPublicMaterial(IKey publicMaterial) { this.publicMaterial = publicMaterial; }

    public IKey getPrivateMaterial() { return privateMaterial; }
    public void setPrivateMaterial(IKey privateMaterial) { this.privateMaterial = privateMaterial; }

    public Instant getExpiration() { return expiration; }
    public void setExpiration(Instant expiration) { this.expiration = expiration; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    @Override
    public String toString() {
        return "Key{" +
                "id='" + id + '\'' +
                ", uuid='" + uuid + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", realmName='" + realmName + '\'' +
                ", algorithm='" + algorithm + '\'' +
                ", signatureAlgorithm='" + signatureAlgorithm + '\'' +
                ", publicMaterial=" + (publicMaterial == null ? "null" : "<IKey>") +
                ", privateMaterial=" + (privateMaterial == null ? "null" : "***") +
                ", expiration=" + expiration +
                ", revoked=" + revoked +
                '}';
    }
}
