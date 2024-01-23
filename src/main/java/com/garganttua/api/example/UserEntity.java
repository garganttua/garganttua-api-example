package com.garganttua.api.example;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.garganttua.api.core.AbstractGGAPIEntity;
import com.garganttua.api.core.GGAPICrudAccess;
import com.garganttua.api.core.GGAPIEntity;
import com.garganttua.api.core.IGGAPIEntityFactory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@GGAPIEntity(domain = "users", dto = "com.garganttua.api.example.UserDto", creation_access = GGAPICrudAccess.anonymous, tenantEntity = true, business = "bean:com.garganttua.api.example.UserBusiness", read_all_authority = true)
public class UserEntity extends AbstractGGAPIEntity {

	@JsonProperty
	private String password;

	@JsonProperty
	private List<String> authorizations;
	
	public UserEntity(String uuid, String password, String email, List<String> authorizations) {
		super(uuid, email);
		this.password = password;
		this.authorizations = authorizations;
	}

	public UserEntity() {
		super();
	}

	@Override
	public IGGAPIEntityFactory<UserEntity> getFactory() {
		return new IGGAPIEntityFactory<UserEntity>() {

			@Override
			public UserEntity newInstance() {
				return new UserEntity();
			}

			@Override
			public UserEntity newInstance(String uuid) {
				return new UserEntity(uuid, null, null, null);
			}
			
		};
	}
}
