package com.garganttua.api.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.garganttua.api.engine.IGGAPIEngine;
import com.garganttua.api.repository.IGGAPIRepository;
import com.garganttua.api.security.GGAPISecurityException;
import com.garganttua.api.security.authentication.dao.AbstractGGAPIAuthenticationUserMapper;

@Service
public class SimpleAuthenticationUserMapper extends AbstractGGAPIAuthenticationUserMapper<UserEntity> {
	
	@Autowired
	private IGGAPIEngine engine;
	private String magicTenantId = "0";


	@SuppressWarnings("unchecked")
	@Override
	protected UserEntity getEntity(String login) throws GGAPISecurityException {
		
		IGGAPIRepository<UserEntity, UserDto> repository = (IGGAPIRepository<UserEntity, UserDto>) this.engine.getRepository("users_repository");
		
		UserEntity user = repository.getOneById(this.magicTenantId, null, login);
		
		if(user == null) {
			throw new GGAPISecurityException("User not found or invalid password");
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
