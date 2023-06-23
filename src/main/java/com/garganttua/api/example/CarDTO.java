package com.garganttua.api.example;

import org.springframework.data.mongodb.core.mapping.Document;

import com.garganttua.api.repository.dto.AbstractSpringCrudifyDTOObject;
import com.garganttua.api.repository.dto.ISpringCrudifyDTOFactory;
import com.garganttua.api.repository.dto.ISpringCrudifyDTOObject;

import lombok.NoArgsConstructor;

@Document(collection = "cars")
@NoArgsConstructor
public class CarDTO extends AbstractSpringCrudifyDTOObject<CarEntity> {

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
	public void update(ISpringCrudifyDTOObject<CarEntity> object) {
		this.name = ((CarDTO) object).name;
		
	}

	@Override
	public ISpringCrudifyDTOFactory<CarEntity, ? extends ISpringCrudifyDTOObject<CarEntity>> getFactory() {
		return new ISpringCrudifyDTOFactory<CarEntity, ISpringCrudifyDTOObject<CarEntity>>() {

			@Override
			public ISpringCrudifyDTOObject<CarEntity> newInstance(String tenantId, CarEntity entity) {
				return new CarDTO(tenantId, entity);
			}
		};
	}

}
