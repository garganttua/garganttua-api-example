package com.garganttua.api.example;

import java.util.List;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class UserDto {

    @FieldMappingRule(sourceFieldAddress = "id")
String id;
    @FieldMappingRule(sourceFieldAddress = "uuid")
String uuid;
    @FieldMappingRule(sourceFieldAddress = "tenantId")
String tenantId;
    @FieldMappingRule(sourceFieldAddress = "login")
String login;
    @FieldMappingRule(sourceFieldAddress = "password")
String password;
    @FieldMappingRule(sourceFieldAddress = "authorities")
List<String> authorities;
    @FieldMappingRule(sourceFieldAddress = "enabled")
Boolean enabled;
    @FieldMappingRule(sourceFieldAddress = "accountNonLocked")
Boolean accountNonLocked;
    @FieldMappingRule(sourceFieldAddress = "accountNonExpired")
Boolean accountNonExpired;
    @FieldMappingRule(sourceFieldAddress = "credentialsNonExpired")
Boolean credentialsNonExpired;

    public UserDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<String> getAuthorities() { return authorities; }
    public void setAuthorities(List<String> authorities) { this.authorities = authorities; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getAccountNonLocked() { return accountNonLocked; }
    public void setAccountNonLocked(Boolean v) { this.accountNonLocked = v; }
    public Boolean getAccountNonExpired() { return accountNonExpired; }
    public void setAccountNonExpired(Boolean v) { this.accountNonExpired = v; }
    public Boolean getCredentialsNonExpired() { return credentialsNonExpired; }
    public void setCredentialsNonExpired(Boolean v) { this.credentialsNonExpired = v; }
}
