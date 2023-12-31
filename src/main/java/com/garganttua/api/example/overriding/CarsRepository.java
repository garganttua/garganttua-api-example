package com.garganttua.api.example.overriding;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.stereotype.Repository;

import com.garganttua.api.repository.GGAPIRepository;
import com.garganttua.api.spec.IGGAPIDomain;

//@Repository("CarsRepository")
@ComponentScan("com.garganttua")
//@EnableMongoRepositories
public class CarsRepository extends GGAPIRepository<CarEntity, CarDTO> {

	public CarsRepository(IGGAPIDomain<CarEntity, CarDTO> domain) {
		super(domain);
	}

}
