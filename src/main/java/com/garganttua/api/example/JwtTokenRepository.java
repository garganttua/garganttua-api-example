package com.garganttua.api.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.garganttua.api.security.authorization.token.GGAPIToken;
import com.garganttua.api.security.authorization.token.jwt.IGGAPIDBTokenKeeper;

@Repository
public class JwtTokenRepository implements IGGAPIDBTokenKeeper {

	@Autowired
	private TokenMongoRepository rep;

	@Override
	public GGAPIToken findOne(GGAPIToken example) {
		TokenDTO token = this.rep.findOneByUserUuid(example.getUserId());
		if( token != null ) {
			return new GGAPIToken(token.getUuid(), token.getUserUuid(), token.getCreationDate(), token.getExpirationDate(), token.getToken(), token.getSigningKeyId());
		}
		return null;
	}

	@Override
	public void store(GGAPIToken token) {
		TokenDTO tokendto = this.rep.findOneByUserUuid(token.getUserId());
		if( tokendto != null ) {
			token.setUuid(tokendto.getUuid());
		}
		this.rep.save(new TokenDTO(token.getUuid(), token.getUserId(), token.getCreationDate(), token.getExpirationDate(), token.getToken(), token.getSigningKeyId()));
	}
}
