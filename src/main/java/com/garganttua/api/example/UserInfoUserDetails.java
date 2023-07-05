package com.garganttua.api.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.garganttua.api.security.authentication.dao.AbstractGGAPIUserDetails;

public class UserInfoUserDetails extends AbstractGGAPIUserDetails {
	
	public UserInfoUserDetails(UserEntity entity) {
    	super(
    			entity.getId(),
    			entity.getUuid(), 
    			true,
    			entity.getPassword(),
    			null, 
    			getAuthorities(entity)
    			);
    }

	private static Collection<? extends GrantedAuthority> getAuthorities(UserEntity entity) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		
		if( entity.getAuthorizations() != null ) {
			entity.getAuthorizations().forEach(r -> {
				SimpleGrantedAuthority authority = new SimpleGrantedAuthority(r);
				authorities.add(authority);
			}); 
		}
		
		return authorities;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -169122733886162397L;

}
