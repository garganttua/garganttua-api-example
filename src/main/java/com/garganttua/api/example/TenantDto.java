package com.garganttua.api.example;

import com.garganttua.core.mapper.annotations.FieldMappingRule;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected(
        allDeclaredFields = true,
        queryAllDeclaredMethods = true,
        queryAllDeclaredConstructors = true)
public class TenantDto {

    @FieldMappingRule(sourceFieldAddress = "id")
String id;
    @FieldMappingRule(sourceFieldAddress = "uuid")
String uuid;
    @FieldMappingRule(sourceFieldAddress = "name")
String name;

    public TenantDto() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
