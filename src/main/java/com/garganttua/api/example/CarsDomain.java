package com.garganttua.api.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.garganttua.api.spec.ISpringCrudifyDomain;

@Configuration
public class CarsDomain  {

	@Bean
	public ISpringCrudifyDomain<CarEntity, CarDTO> carDomain(){

		return new ISpringCrudifyDomain<CarEntity, CarDTO>() {

			@Override
			public Class<CarEntity> getEntityClass() {
				return CarEntity.class;
			}

			@Override
			public Class<CarDTO> getDtoClass() {
				return CarDTO.class;
			}

			@Override
			public String getDomain() {
				return "cars";
			}

		};
	}

}
