package com.garganttua.api.example;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface KeyMongoRepository extends MongoRepository<KeyRealmDTO, String>{

	KeyRealmDTO findOneByName(String realm);

}
