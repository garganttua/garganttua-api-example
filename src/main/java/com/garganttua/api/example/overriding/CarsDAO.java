package com.garganttua.api.example.overriding;

import org.springframework.stereotype.Repository;

import com.garganttua.api.repository.dao.mongodb.GGAPIMongoRepository;
import com.garganttua.api.spec.IGGAPIDomain;

@Repository
public class CarsDAO extends GGAPIMongoRepository<CarEntity, CarDTO>{

	public CarsDAO(IGGAPIDomain<CarEntity, CarDTO> domain) {
		super(domain);
	}

}
