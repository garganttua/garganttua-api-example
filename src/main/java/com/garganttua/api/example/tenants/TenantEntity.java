package com.garganttua.api.example.tenants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.garganttua.api.core.GGAPIServiceAccess;
import com.garganttua.api.core.IGGAPICaller;
import com.garganttua.api.core.entity.GenericGGAPIEntity;
import com.garganttua.api.core.entity.annotations.GGAPIBusinessAnnotations.GGAPIEntityBeforeCreate;
import com.garganttua.api.core.entity.annotations.GGAPIEntity;
import com.garganttua.api.core.entity.annotations.GGAPIEntityAuthorizeUpdate;
import com.garganttua.api.core.entity.annotations.GGAPIEntityGeolocalized;
import com.garganttua.api.core.entity.annotations.GGAPIEntityHiddenable;
import com.garganttua.api.core.entity.annotations.GGAPIEntityId;
import com.garganttua.api.core.entity.annotations.GGAPIEntityMandatory;
import com.garganttua.api.core.entity.annotations.GGAPIEntityOwner;
import com.garganttua.api.core.entity.annotations.GGAPIEntityOwnerId;
import com.garganttua.api.core.entity.annotations.GGAPIEntityPublic;
import com.garganttua.api.core.entity.annotations.GGAPIEntityShared;
import com.garganttua.api.core.entity.annotations.GGAPIEntitySuperOwner;
import com.garganttua.api.core.entity.annotations.GGAPIEntitySuperTenant;
import com.garganttua.api.core.entity.annotations.GGAPIEntityTenant;
import com.garganttua.api.core.entity.annotations.GGAPIEntityTenantId;
import com.garganttua.api.core.entity.annotations.GGAPIEntityUnicity;
import com.garganttua.api.core.entity.annotations.GGAPIEntityUuid;
import com.garganttua.api.core.entity.exceptions.GGAPIEntityException;
import com.garganttua.api.engine.registries.IGGAPIAccessRulesRegistry;
import com.garganttua.api.security.authentication.GGAPIAuthenticator;
import com.garganttua.api.security.authentication.GGAPIAuthenticatorAccountNonExpired;
import com.garganttua.api.security.authentication.GGAPIAuthenticatorAccountNonLocked;
import com.garganttua.api.security.authentication.GGAPIAuthenticatorAuthorities;
import com.garganttua.api.security.authentication.GGAPIAuthenticatorCredentialsNonExpired;
import com.garganttua.api.security.authentication.GGAPIAuthenticatorEnabled;
import com.garganttua.api.security.authentication.modes.loginpassword.GGAPIAuthenticatorLogin;
import com.garganttua.api.security.authentication.modes.loginpassword.GGAPIAuthenticatorPassword;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@GGAPIEntity (
	domain = "tenants", 
	creation_access = GGAPIServiceAccess.anonymous,
	count_access = GGAPIServiceAccess.tenant,
	delete_one_access = GGAPIServiceAccess.tenant,
	read_one_access = GGAPIServiceAccess.tenant,
	update_one_access = GGAPIServiceAccess.tenant,
	read_all_access = GGAPIServiceAccess.tenant,
	delete_all_access = GGAPIServiceAccess.tenant,
	allow_count = true,
	allow_creation = true,
	allow_delete_all = true,
	allow_delete_one = true,
	allow_read_all = true,
	allow_read_one = true,
	allow_update_one = true,
	count_authority = true,
	creation_authority = false,
	delete_one_authority = true,
	read_all_authority = true,
	read_one_authority = true,
	update_one_authority = true,
	delete_all_authority = true
)
@NoArgsConstructor
@Getter
@GGAPIAuthenticator
@GGAPIEntityTenant
@GGAPIEntityOwner
public class TenantEntity extends GenericGGAPIEntity {
	
	@GGAPIEntityUuid
	@GGAPIEntityOwnerId
	@GGAPIEntityTenantId
	protected String uuid;
	
	@GGAPIEntityId
	@GGAPIEntityUnicity
	@GGAPIEntityMandatory
	protected String id;
	
	@GGAPIAuthenticatorLogin
	@JsonProperty
	protected String email;
	
	@JsonInclude
	@GGAPIEntityAuthorizeUpdate
	private String name;
	
	@JsonInclude
	private String surname;
	
	@JsonInclude
	@GGAPIAuthenticatorPassword
	@GGAPIEntityMandatory
	private String password;
	
	@JsonInclude
	@Setter
	@GGAPIAuthenticatorAuthorities
	private List<String> userAuthorities;
	
	@JsonIgnore
	@GGAPIAuthenticatorAccountNonExpired
	@GGAPIAuthenticatorAccountNonLocked
	@GGAPIAuthenticatorCredentialsNonExpired
	@GGAPIAuthenticatorEnabled
	private boolean enabled = true;
	
	@GGAPIEntitySuperTenant
	private boolean superTenant;
	
	@GGAPIEntitySuperOwner
	private boolean superOwner;

	@Inject
	@JsonIgnore
	private IGGAPIAccessRulesRegistry accessRulesRegistry;
	
	public TenantEntity(String uuid, String id, String name, String surname, String password) {
		this.uuid = uuid;
		this.id = id;
		this.email = id;
		this.name = name;
		this.surname = surname;
		this.password = password;
		this.userAuthorities = new ArrayList<String>();
	}

	@GGAPIEntityBeforeCreate
	public void beforeCreate(IGGAPICaller caller, Map<String, String> parameters) throws GGAPIEntityException {
		//Authorize everything for a new user
		List<String> auths = new ArrayList<String>();
		
		this.accessRulesRegistry.getAccessRules().forEach(r -> {
			if( r.getAuthority() != null ) {
				auths.add(r.getAuthority());
			}
		});
		
		List<String> listWithoutDuplicates = new ArrayList<String>(new HashSet<>(auths));
		
		this.setUserAuthorities(listWithoutDuplicates);
	}
}
