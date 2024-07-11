package com.garganttua.api.example.tenants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.garganttua.api.core.entity.exceptions.GGAPIEntityException;
import com.garganttua.api.spec.GGAPIException;
import com.garganttua.api.spec.IGGAPICaller;
import com.garganttua.api.spec.engine.IGGAPIAccessRulesRegistry;
import com.garganttua.api.spec.entity.IGGAPIEntityDeleteMethod;
import com.garganttua.api.spec.entity.IGGAPIEntitySaveMethod;
import com.garganttua.api.spec.entity.annotations.GGAPIBusinessAnnotations.GGAPIEntityBeforeCreate;
import com.garganttua.api.spec.entity.annotations.GGAPIEntity;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityAuthorizeUpdate;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityDeleteMethod;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityDeleteMethodProvider;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityGotFromRepository;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityId;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityMandatory;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityOwner;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityRepository;
import com.garganttua.api.spec.entity.annotations.GGAPIEntitySaveMethod;
import com.garganttua.api.spec.entity.annotations.GGAPIEntitySaveMethodProvider;
import com.garganttua.api.spec.entity.annotations.GGAPIEntitySuperOwner;
import com.garganttua.api.spec.entity.annotations.GGAPIEntitySuperTenant;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityTenant;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityUnicity;
import com.garganttua.api.spec.entity.annotations.GGAPIEntityUuid;
import com.garganttua.api.spec.repository.IGGAPIRepository;
import com.garganttua.api.spec.security.IGGAPISecurity;
import com.garganttua.api.spec.security.annotations.GGAPIEntitySecurity;
import com.garganttua.api.spec.service.GGAPIServiceAccess;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@GGAPIEntity (
	domain = "tenants", 
	allow_count = true,
	allow_creation = true,
	allow_delete_all = true,
	allow_delete_one = true,
	allow_read_all = true,
	allow_read_one = true,
	allow_update_one = true, 
	interfaces = { "gg:SpringRestInterface" }
)
@GGAPIEntitySecurity(
		creation_access = GGAPIServiceAccess.anonymous
)
@NoArgsConstructor
@Getter
//@GGAPIAuthenticator
@GGAPIEntityTenant(tenantId="uuid")
@GGAPIEntityOwner(ownerId="uuid")
@JsonIgnoreProperties(value = { "gotFromRepository","saveMethod","deleteMethod", "repository", "save", "delete" })
public class TenantEntity {
	
	@GGAPIEntityUuid
	@Setter
	@GGAPIEntityMandatory
	protected String uuid;
	
	@GGAPIEntityId
	@Setter
	@GGAPIEntityUnicity
	@GGAPIEntityMandatory
	protected String id;
	
	@GGAPIEntityGotFromRepository
	private boolean gotFromRepository;

	@GGAPIEntitySaveMethodProvider
	private IGGAPIEntitySaveMethod<TenantEntity> saveMethod;

	@GGAPIEntityDeleteMethodProvider
	private IGGAPIEntityDeleteMethod<TenantEntity> deleteMethod;
	
	@GGAPIEntityRepository
	private IGGAPIRepository<Object> repository;

	@GGAPIEntitySaveMethod
	public void save(IGGAPICaller caller, Map<String, String> parameters, Optional<IGGAPISecurity> security) throws GGAPIException {
		this.saveMethod.save(caller, parameters, this);
	}

	@GGAPIEntityDeleteMethod
	public void delete(IGGAPICaller caller, Map<String, String> parameters) throws GGAPIException {
		this.deleteMethod.delete(caller, parameters, this);
	}
	
	@JsonInclude
	@GGAPIEntityAuthorizeUpdate
	private String name;
	
	@JsonInclude
	@GGAPIEntityAuthorizeUpdate
	private String surname;
	
	@JsonInclude
//	@GGAPIAuthenticatorPassword
	@GGAPIEntityMandatory
	private String password;
	
	@JsonInclude
	@Setter
//	@GGAPIAuthenticatorAuthorities
	private List<String> userAuthorities;

	
	@JsonIgnore
//	@GGAPIAuthenticatorAccountNonExpired
//	@GGAPIAuthenticatorAccountNonLocked
//	@GGAPIAuthenticatorCredentialsNonExpired
//	@GGAPIAuthenticatorEnabled
	private boolean enabled = true;
	
	@GGAPIEntitySuperTenant
	private boolean superTenant;
	
	@GGAPIEntitySuperOwner
	private boolean superOwner;

	@Inject
	@JsonIgnore
	private IGGAPIAccessRulesRegistry accessRulesRegistry;

	@GGAPIEntityBeforeCreate
	@JsonIgnore
	public void beforeCreate(IGGAPICaller caller, Map<String, String> parameters) throws GGAPIEntityException {
		//Authorize everything for a new user
		List<String> auths = new ArrayList<String>();
		
		if( this.accessRulesRegistry != null ) {
			this.accessRulesRegistry.getAccessRules().forEach(r -> {
				if( r.getAuthority() != null ) {
					auths.add(r.getAuthority());
				}
			});
		}
		
		List<String> listWithoutDuplicates = new ArrayList<String>(new HashSet<>(auths));
		
		this.setUserAuthorities(listWithoutDuplicates);
	}
}
