package com.garganttua.api.example.tenants;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.garganttua.api.core.dto.GenericGGAPIDto;
import com.garganttua.api.core.dto.annotations.GGAPIDto;
import com.garganttua.api.core.mapper.annotations.GGAPIFieldMappingRule;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Document(collection = "tenants")
@NoArgsConstructor
@GGAPIDto(entityClass = TenantEntity.class)
public class TenantDto extends GenericGGAPIDto {

	@Field
	@GGAPIFieldMappingRule(sourceFieldAddress = "name")
	private String name;
	
	@Field
	@GGAPIFieldMappingRule(sourceFieldAddress = "surname")
	private String surname;

	@Field
	@GGAPIFieldMappingRule(sourceFieldAddress = "password")
	private String password;
	
	@Field 
	@GGAPIFieldMappingRule(sourceFieldAddress = "email")
	private String email;
	
	@Field
	@GGAPIFieldMappingRule(sourceFieldAddress = "userAuthorities")
	private List<String> authorities;

}
