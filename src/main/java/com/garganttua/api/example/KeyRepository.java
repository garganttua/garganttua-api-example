package com.garganttua.api.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.garganttua.api.security.keys.GGAPIKey;
import com.garganttua.api.security.keys.GGAPIKeyExpiredException;
import com.garganttua.api.security.keys.GGAPIKeyRealms;
import com.garganttua.api.security.keys.IGGAPIDBKeyKeeper;
import com.garganttua.api.security.keys.IGGAPIKeyRealm;

import io.jsonwebtoken.SignatureAlgorithm;

@Repository
public class KeyRepository implements IGGAPIDBKeyKeeper {

	@Autowired
	private KeyMongoRepository rep;
	
	@Override
	public IGGAPIKeyRealm getRelam(String realmStr) {
		KeyRealmDTO realm = this.rep.findOneByName(realmStr);
		if( realm != null) {
			return GGAPIKeyRealms.createRealm(
					realm.getName(), 
					realm.getAlgo(), 
					new GGAPIKey(realm.getCipheringKey().getUuid(), realm.getCipheringKey().getRealm(), realm.getCipheringKey().getAlgorithm(), realm.getCipheringKey().getExpiration(), realm.getCipheringKey().getType(), realm.getCipheringKey().getEncoded()),
					new GGAPIKey(realm.getUncipheringKey().getUuid(), realm.getUncipheringKey().getRealm(), realm.getUncipheringKey().getAlgorithm(), realm.getUncipheringKey().getExpiration(), realm.getUncipheringKey().getType(), realm.getUncipheringKey().getEncoded()));
		}
		return null;
	}

	@Override
	public void createRealm(IGGAPIKeyRealm realm) throws GGAPIKeyExpiredException {
		this.rep.save(new KeyRealmDTO(realm.getName(), realm.getAlgo(),
				new KeyDTO(realm.getCipheringKey().getUuid(), realm.getCipheringKey().getRealm(), realm.getCipheringKey().getAlgorithm(), realm.getCipheringKey().getExpiration(), realm.getCipheringKey().getType(), realm.getCipheringKey().getEncoded()), 
				new KeyDTO(realm.getUncipheringKey().getUuid(), realm.getUncipheringKey().getRealm(), realm.getUncipheringKey().getAlgorithm(), realm.getUncipheringKey().getExpiration(), realm.getUncipheringKey().getType(), realm.getUncipheringKey().getEncoded())));
	}

}
