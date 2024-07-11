package com.garganttua.api.example.tenants;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.garganttua.api.spec.dto.annotations.GGAPIDto;
import com.garganttua.api.spec.dto.annotations.GGAPIDtoTenantId;
import com.garganttua.objects.mapper.annotations.GGFieldMappingRule;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Document(collection = "tenants")
@NoArgsConstructor
@GGAPIDto(entityClass = TenantEntity.class, db = "gg:SpringMongoDao")
public class TenantDto {

	@Field
	@GGFieldMappingRule(sourceFieldAddress = "name")
	private String name;
	
	@Field
	@GGFieldMappingRule(sourceFieldAddress = "surname")
	private String surname;

	@Field
	@GGFieldMappingRule(sourceFieldAddress = "password")
	private String password;
	
	@Field
	@GGFieldMappingRule(sourceFieldAddress = "userAuthorities")
	private List<String> authorities;
	
	@Id
	@GGFieldMappingRule(sourceFieldAddress = "uuid")
	@GGAPIDtoTenantId
	protected String uuid;
	
	@GGFieldMappingRule(sourceFieldAddress = "id")
	protected String id;

}
