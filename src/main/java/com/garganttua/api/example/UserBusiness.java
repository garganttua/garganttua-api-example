package com.garganttua.api.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.api.business.IGGAPIBusiness;
import com.garganttua.api.engine.IGGAPIDynamicDomainEngine;
import com.garganttua.api.repository.IGGAPIRepository;
import com.garganttua.api.security.IGGAPISecurityHelper;
import com.garganttua.api.spec.GGAPIEntityException;
import com.garganttua.api.spec.filter.GGAPILiteral;

@Service
public class UserBusiness implements IGGAPIBusiness<UserEntity> {

	@Inject
	private Optional<PasswordEncoder> passwordEncoder;
	
	@Inject 
	private IGGAPIDynamicDomainEngine engine;
	
	@Value("${com.garganttua.api.magicTenantId}")
	private String magicTenantId;
	
	@Inject
	private Optional<IGGAPISecurityHelper> helper;
	
	@SuppressWarnings("unchecked")
	@Override
	public void beforeCreate(String tenantId, UserEntity entity) throws GGAPIEntityException {
		
		//Encrypt password
		PasswordEncoder encodedPassword = this.passwordEncoder.orElseGet( () -> getPasswordEncoder() );
		entity.setPassword(encodedPassword.encode(entity.getPassword()));
		
		String fiterString = "{\"name\":\"$field\", \"value\":\"id\",\"literals\":[{\"name\":\"$eq\",\"value\":\""+entity.getId()+"\"}]}";
		
		ObjectMapper mapper = new ObjectMapper();
		GGAPILiteral filter = null;
		try {
			filter = mapper.readValue(fiterString, GGAPILiteral.class);
		} catch (JsonProcessingException e) {
			
		}
		
		IGGAPIRepository<UserEntity, UserDto> repository = (IGGAPIRepository<UserEntity, UserDto>) this.engine.getRepository("users_repository");
		
		List<UserEntity> users = repository .getEntities(this.magicTenantId, 0, 0, filter, null);
		
		if( users.size() > 0 ) {
			throw new GGAPIEntityException(GGAPIEntityException.ENTITY_ALREADY_EXISTS, "User with mail "+entity.getId()+" already exists.");
		}

		this.helper.ifPresent(h -> {
			h.getAuthorizations().forEach(a -> {
				if (entity.getAuthorizations() == null) {
					List<String> roles = new ArrayList<String>();
					entity.setAuthorizations(roles);
				}
				if( a.getAuthorization() != null && !a.getAuthorization().isEmpty() ) {
					entity.getAuthorizations().add(a.getAuthorization());
				}
			});
		});
	}
	
	private PasswordEncoder getPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void beforeUpdate(String tenantId, UserEntity entity) throws GGAPIEntityException {
		String fiterString = "{\"name\":\"$field\", \"value\":\"id\",\"literals\":[{\"name\":\"$eq\",\"value\":\""+entity.getId()+"\"}]}";
		
		ObjectMapper mapper = new ObjectMapper();
		GGAPILiteral filter = null;
		try {
			filter = mapper.readValue(fiterString, GGAPILiteral.class);
		} catch (JsonProcessingException e) {
			
		}
		
		IGGAPIRepository<UserEntity, UserDto> repository = (IGGAPIRepository<UserEntity, UserDto>) this.engine.getRepository("users_repository");
		
		List<UserEntity> users = repository .getEntities(this.magicTenantId, 0, 0, filter, null);
		
		if( users.size() > 0 ) {
			throw new GGAPIEntityException(GGAPIEntityException.ENTITY_ALREADY_EXISTS, "User with mail "+entity.getId()+" already exists.");
		}
	}

	@Override
	public void beforeDelete(String tenantId, UserEntity entity) throws GGAPIEntityException {
		
	}

	@Override
	public void afterCreate(String tenantId, UserEntity entity) throws GGAPIEntityException {
		
	}

	@Override
	public void afterUpdate(String tenantId, UserEntity entity) throws GGAPIEntityException {
		
	}

	@Override
	public void afterDelete(String tenantId, UserEntity entity) throws GGAPIEntityException {
		
	}

}
