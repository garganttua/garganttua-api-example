package com.garganttua.api.example;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TokenMongoRepository extends MongoRepository<TokenDTO, String> {
	
	TokenDTO findOneByUserUuid(String userId);
}