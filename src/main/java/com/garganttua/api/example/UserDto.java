package com.garganttua.api.example;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.garganttua.api.repository.dto.AbstractGGAPIDTOObject;
import com.garganttua.api.repository.dto.IGGAPIDTOFactory;
import com.garganttua.api.repository.dto.IGGAPIDTOObject;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Document(collection = "users")
@NoArgsConstructor
public class UserDto extends AbstractGGAPIDTOObject<UserEntity>{

	private String password;
	
	private List<String> authorizations;

	public UserDto(String tenantId, UserEntity entity) {
		super(tenantId, entity);
	}

	@Override
	public void create(UserEntity entity) {
		this.password = entity.getPassword();
		this.authorizations = entity.getAuthorizations();
	}

	@Override
	public UserEntity convert() {
		return new UserEntity(this.uuid, this.password, this.id, this.authorizations);
	}

	@Override
	public void update(IGGAPIDTOObject<UserEntity> object) {
		this.authorizations = ((UserDto) object).getAuthorizations();
		this.id = ((UserDto) object).getId();
	}

	@Override
	public IGGAPIDTOFactory<UserEntity, UserDto> getFactory() {
		return new IGGAPIDTOFactory<UserEntity, UserDto>() {
			
			@Override
			public UserDto newInstance(String tenantId, UserEntity entity) {
				return new UserDto(tenantId, entity);
			}
		};
	}

}
