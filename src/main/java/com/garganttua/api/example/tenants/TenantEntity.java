package com.garganttua.api.example.tenants;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.garganttua.api.core.AbstractGGAPIEntity;
import com.garganttua.api.core.GGAPICrudAccess;
import com.garganttua.api.core.GGAPIEntity;
import com.garganttua.api.core.GGAPITenant;
import com.garganttua.api.core.IGGAPIEntityFactory;
import com.garganttua.api.core.IGGAPITenant;
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
	dto = "com.garganttua.api.example.tenants.TenantDto",
	business= "class:com.garganttua.api.example.tenants.TenantsBusiness",
	creation_access = GGAPICrudAccess.anonymous,
	count_access = GGAPICrudAccess.tenant,
	delete_one_access = GGAPICrudAccess.tenant,
	read_one_access = GGAPICrudAccess.tenant,
	update_one_access = GGAPICrudAccess.tenant,
	read_all_access = GGAPICrudAccess.tenant,
	delete_all_access = GGAPICrudAccess.tenant,
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
	delete_all_authority = true,
	unicity = {"id"},
	mandatory = {"id", "password"}
)
@NoArgsConstructor
@Getter
@GGAPIAuthenticator
@GGAPITenant
public class TenantEntity extends AbstractGGAPIEntity implements IGGAPITenant {
	
	@GGAPIAuthenticatorLogin
	@JsonProperty
	protected String email;
	@JsonInclude
	private String name;
	@JsonInclude
	private String surname;
	@JsonInclude
	@GGAPIAuthenticatorPassword
	private String password;
	@JsonInclude
	@Setter
	@Getter
	@GGAPIAuthenticatorAuthorities
	private List<String> userAuthorities;
	@JsonIgnore
	@GGAPIAuthenticatorAccountNonExpired
	@GGAPIAuthenticatorAccountNonLocked
	@GGAPIAuthenticatorCredentialsNonExpired
	@GGAPIAuthenticatorEnabled
	private boolean enabled = true;

	public TenantEntity(String uuid, String id, String name, String surname, String password) {
		super(uuid, id);
		this.uuid = uuid;
		this.email = id;
		this.name = name;
		this.surname = surname;
		this.password = password;
		this.userAuthorities = new ArrayList<String>();
	}

	@Override
	public IGGAPIEntityFactory<TenantEntity> getFactory() {
		return new IGGAPIEntityFactory<TenantEntity>() {
			
			@Override
			public TenantEntity newInstance(String uuid) {
				return new TenantEntity(uuid, null, null, null, null);
			}
			
			@Override
			public TenantEntity newInstance() {
				return new TenantEntity();
			}
		};
	}

	@Override
	@JsonIgnore
	public boolean isSuperTenant() {
		return false;
	}
	
}
