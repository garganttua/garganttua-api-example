package com.garganttua.api.example;

import org.springframework.data.mongodb.core.mapping.Document;

import com.garganttua.api.repository.dto.AbstractGGAPIDTOObject;
import com.garganttua.api.repository.dto.IGGAPIDTOFactory;
import com.garganttua.api.repository.dto.IGGAPIDTOObject;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "bookings")
public class BookingDTO extends AbstractGGAPIDTOObject<BookingEntity> {

	public BookingDTO(String tenantId, BookingEntity entity) {
		super(tenantId, entity);
	}
	
	@Override
	public void create(BookingEntity entity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public BookingEntity convert() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(IGGAPIDTOObject<BookingEntity> object) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IGGAPIDTOFactory<BookingEntity, BookingDTO> getFactory() {
		return new IGGAPIDTOFactory<BookingEntity, BookingDTO>() {
			
			@Override
			public BookingDTO newInstance(String tenantId, BookingEntity entity) {
				BookingDTO dto = new BookingDTO(tenantId, entity);
				return dto;
			}
		};
	}

}
