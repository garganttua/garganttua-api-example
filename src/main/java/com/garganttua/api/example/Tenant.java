package com.garganttua.api.example;

import java.time.Instant;

import com.garganttua.core.reflection.annotations.Reflected;

@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class Tenant {

String id;
String uuid;
String name;
Instant createdAt;

    // Server-authoritative super-tenant marker. Mandated by the framework on
    // any .tenant(true) domain (DomainBuilder.validateSuperFields): it feeds
    // the Api super-tenant registry scanned at startup. The framework stamps
    // the auto-created master tenant to true and rejects runtime promotion.
    Boolean superTenant = false;

    public Tenant() {
        // No-op: framework instantiates via no-arg constructor and populates fields via setters.
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Boolean getSuperTenant() { return superTenant; }
    public void setSuperTenant(Boolean superTenant) { this.superTenant = superTenant; }

    @Override
    public String toString() {
        return "Tenant{" +
                "id='" + id + '\'' +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", superTenant=" + superTenant +
                '}';
    }
}
