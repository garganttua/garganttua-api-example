package com.garganttua.api.example;

import org.springframework.stereotype.Repository;

import com.garganttua.api.repository.dao.mongodb.SpringCrudifyMongoRepository;
import com.garganttua.api.spec.ISpringCrudifyDomain;

@Repository
public class CarsDAO extends SpringCrudifyMongoRepository<CarEntity, CarDTO>{

	public CarsDAO(ISpringCrudifyDomain<CarEntity, CarDTO> domain) {
		super(domain);
	}

}
