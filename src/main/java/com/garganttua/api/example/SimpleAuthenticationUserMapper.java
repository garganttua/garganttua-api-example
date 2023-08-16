package com.garganttua.api.example;

import javax.inject.Inject;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.garganttua.api.engine.IGGAPIDynamicDomainEngine;
import com.garganttua.api.repository.IGGAPIRepository;
import com.garganttua.api.security.authentication.IGGAPISecurityException;
import com.garganttua.api.security.authentication.dao.AbstractGGAPIAuthenticationUserMapper;

@Service
public class SimpleAuthenticationUserMapper extends AbstractGGAPIAuthenticationUserMapper<UserEntity> {
	
	@Inject
	private IGGAPIDynamicDomainEngine engine;
	private String magicTenantId = "0";


	@SuppressWarnings("unchecked")
	@Override
	protected UserEntity getEntity(String login) throws IGGAPISecurityException {
		
		IGGAPIRepository<UserEntity, UserDto> repository = (IGGAPIRepository<UserEntity, UserDto>) this.engine.getRepository("users_repository");
		
		UserEntity user = repository.getOneById(this.magicTenantId , login);
		
		if(user == null) {
			throw new IGGAPISecurityException("User not found or invalid password");
		}
		
		return user;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected UserDetails mapUser(UserEntity entity) {
		IGGAPIRepository<UserEntity, UserDto> repository = (IGGAPIRepository<UserEntity, UserDto>) this.engine.getRepository("users_repository");
		
		UserInfoUserDetails infos = new UserInfoUserDetails(entity);
		infos.setTenantId(repository.getTenant(entity));
		
		return infos;
	}

}
