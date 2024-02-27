package com.garganttua.api.example.tenants;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.garganttua.api.repository.dto.AbstractGGAPIDTOObject;
import com.garganttua.api.repository.dto.IGGAPIDTOFactory;
import com.garganttua.api.repository.dto.IGGAPIDTOObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Document(collection = "tenants")
@NoArgsConstructor
public class TenantDto extends AbstractGGAPIDTOObject<TenantEntity>{

	@Field
	private String name;
	
	@Field
	private String surname;

	@Field
	private String password;
	
	@Field 
	private String email;
	
	@Field
	private List<String> authorities;
	
	public TenantDto(String tenantId, TenantEntity entity) {
		super(tenantId, entity);
	}
	
	@Override
	public void create(TenantEntity entity) {
		this.name = entity.getName();
		this.surname = entity.getSurname();
		this.password = entity.getPassword();
		this.authorities = entity.getUserAuthorities();
		this.email = entity.getId();
	}

	@Override
	public TenantEntity convert() {
		TenantEntity tenantEntity = new TenantEntity(this.uuid, this.id, this.name, this.surname, this.password);
		tenantEntity.setUserAuthorities(this.authorities);
		tenantEntity.setTenantId(this.tenantId);
		return tenantEntity;
	}

	@Override
	public void update(IGGAPIDTOObject<TenantEntity> object) {
		this.name = ((TenantDto) object).name;
		this.surname = ((TenantDto) object).surname;
		
		//Uncomment below to authorize password update
//		this.password = ((TenantDto) object).password;
		
		this.authorities = ((TenantDto) object).authorities;
	}

}
