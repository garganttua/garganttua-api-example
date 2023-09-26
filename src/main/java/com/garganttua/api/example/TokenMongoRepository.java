package com.garganttua.api.example;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TokenMongoRepository extends MongoRepository<TokenEntity, String> {
	
	TokenEntity findOneByUserUuid(String userId);
}