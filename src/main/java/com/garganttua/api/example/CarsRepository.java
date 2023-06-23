package com.garganttua.api.example;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Repository;

import com.garganttua.api.repository.SpringCrudifyRepository;
import com.garganttua.api.spec.ISpringCrudifyDomain;

@Repository("CarsRepository")
@ComponentScan("org.sdc")
@EnableMongoRepositories
public class CarsRepository extends SpringCrudifyRepository<CarEntity, CarDTO> {

	public CarsRepository(ISpringCrudifyDomain<CarEntity, CarDTO> domain) {
		super(domain);
	}

}
