package com.garganttua.api.example;

import org.springframework.data.mongodb.core.mapping.Document;

import com.garganttua.api.repository.dto.AbstractGGAPIDTOObject;
import com.garganttua.api.repository.dto.IGGAPIDTOFactory;
import com.garganttua.api.repository.dto.IGGAPIDTOObject;

import lombok.NoArgsConstructor;

@Document(collection = "cars")
@NoArgsConstructor
public class CarDTO extends AbstractGGAPIDTOObject<CarEntity> {

	private String name;

	public CarDTO(String tenantId, CarEntity entity) {
		super(tenantId, entity);
	}

	@Override
	public void create(CarEntity entity) {
		this.name = entity.getName();
	}

	@Override
	public CarEntity convert() {
		CarEntity entity = new CarEntity(this.name);
		super.convert(entity);
		return entity;
	}

	@Override
	public void update(IGGAPIDTOObject<CarEntity> object) {
		this.name = ((CarDTO) object).name;
		
	}

	@Override
	public IGGAPIDTOFactory<CarEntity, ? extends IGGAPIDTOObject<CarEntity>> getFactory() {
		return new IGGAPIDTOFactory<CarEntity, IGGAPIDTOObject<CarEntity>>() {

			@Override
			public IGGAPIDTOObject<CarEntity> newInstance(String tenantId, CarEntity entity) {
				return new CarDTO(tenantId, entity);
			}
		};
	}

}
