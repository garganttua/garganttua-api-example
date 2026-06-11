package com.garganttua.api.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class User {

String id;
String uuid;
String tenantId;

// Server-authoritative super-owner marker. Mandated by the framework on any
// .owner(field) domain (DomainBuilder.validateSuperFields): it feeds the Api
// super-owner registry scanned at startup and is recomputed into the caller's
// super flags during VERIFY_AUTHORIZATION (the registry is the source of truth).
Boolean superOwner = false;

String login;
String passwordHash;

Boolean enabled = true;
Boolean accountNonLocked = true;
Boolean accountNonExpired = true;
Boolean credentialsNonExpired = true;

java.util.List<String> authorities;

    public User() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Boolean getSuperOwner() { return superOwner; }
    public void setSuperOwner(Boolean superOwner) { this.superOwner = superOwner; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    // Never serialize the password hash over the wire (a read would leak it).
    // @JsonIgnore only affects (de)serialization; the authenticate pipeline reads
    // the field reflectively for credential checks, so it is unaffected.
    @JsonIgnore
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getAccountNonLocked() { return accountNonLocked; }
    public void setAccountNonLocked(Boolean v) { this.accountNonLocked = v; }
    public Boolean getAccountNonExpired() { return accountNonExpired; }
    public void setAccountNonExpired(Boolean v) { this.accountNonExpired = v; }
    public Boolean getCredentialsNonExpired() { return credentialsNonExpired; }
    public void setCredentialsNonExpired(Boolean v) { this.credentialsNonExpired = v; }

    public java.util.List<String> getAuthorities() { return authorities; }
    public void setAuthorities(java.util.List<String> authorities) { this.authorities = authorities; }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", uuid='" + uuid + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", login='" + login + '\'' +
                ", passwordHash=" + (passwordHash == null ? "null" : "***") +
                ", enabled=" + enabled +
                ", accountNonLocked=" + accountNonLocked +
                ", accountNonExpired=" + accountNonExpired +
                ", credentialsNonExpired=" + credentialsNonExpired +
                ", authorities=" + authorities +
                '}';
    }
}
