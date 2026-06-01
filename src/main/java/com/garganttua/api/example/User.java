package com.garganttua.api.example;

import com.garganttua.core.reflection.annotations.Reflected;

@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class User {

String id;
String uuid;
String tenantId;

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

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
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
