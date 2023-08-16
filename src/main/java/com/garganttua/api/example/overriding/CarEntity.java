package com.garganttua.api.example.overriding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.garganttua.api.spec.AbstractGGAPIEntity;
import com.garganttua.api.spec.IGGAPIEntityFactory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CarEntity extends AbstractGGAPIEntity {
	
	@JsonProperty
	private String name; 

	@Override
	public IGGAPIEntityFactory<CarEntity> getFactory() {
		return new IGGAPIEntityFactory<CarEntity>() {
			
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
