package com.garganttua.api.example;

import java.util.Date;

import com.garganttua.api.spec.AbstractSpringCrudifyEntity;
import com.garganttua.api.spec.ISpringCrudifyEntityFactory;
import com.garganttua.api.spec.SpringCrudifyEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SpringCrudifyEntity(dto = "org.sdc.spring.domain.crudify.example.BookingDTO", eventPublisher = "class:org.sdc.spring.domain.crudify.example.CustomEventPublisher", domain = "bookings")
public class BookingEntity extends AbstractSpringCrudifyEntity {
	
	private Date from;
	
	private Date to;
	
	private String meetingRoomUuid; 
	
	private String ownerName;
	 
	private String ownerMail;

	@Override
	public ISpringCrudifyEntityFactory<BookingEntity> getFactory() {
		return new ISpringCrudifyEntityFactory<BookingEntity>() {
			
			@Override
			public BookingEntity newInstance(String uuid) {
				BookingEntity entity = new BookingEntity();
				entity.setUuid(uuid);
				return entity;
			}
			
			@Override
			public BookingEntity newInstance() {
				return new BookingEntity();
			}
		};
	}

}
