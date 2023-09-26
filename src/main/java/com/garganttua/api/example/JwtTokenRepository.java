package com.garganttua.api.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.garganttua.api.security.authorization.token.jwt.IGGAPIDBTokenKeeper;

@Repository
public class JwtTokenRepository implements IGGAPIDBTokenKeeper {

	@Autowired
	private TokenMongoRepository rep;

	@Override
	public String findOne(String uuid) {
		TokenEntity token = this.rep.findOneByUserUuid(uuid);

		if (token != null) {
			return token.getToken();
		} else {
			return null;
		}

	}

	@Override
	public void store(String uuid, String token) {
		this.rep.save(new TokenEntity(uuid, token));
	}

}
