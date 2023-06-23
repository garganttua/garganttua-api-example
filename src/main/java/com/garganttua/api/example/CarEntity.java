package com.garganttua.api.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.garganttua.api.spec.AbstractSpringCrudifyEntity;
import com.garganttua.api.spec.ISpringCrudifyEntityFactory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CarEntity extends AbstractSpringCrudifyEntity {
	
	@JsonProperty
	private String name; 

	@Override
	public ISpringCrudifyEntityFactory<CarEntity> getFactory() {
		return new ISpringCrudifyEntityFactory<CarEntity>() {
			
			@Override
			public CarEntity newInstance(String uuid) {
				CarEntity entity = new CarEntity();
				entity.setUuid(uuid);
				return entity;
			}
			
			@Override
			public CarEntity newInstance() {
				return new CarEntity();
			}
		};
	}

}
