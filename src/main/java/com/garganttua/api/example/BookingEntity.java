package com.garganttua.api.example;

import java.util.Date;

import com.garganttua.api.core.AbstractGGAPIEntity;
import com.garganttua.api.core.GGAPIEntity;
import com.garganttua.api.core.IGGAPIEntityFactory;

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
@GGAPIEntity(dto = "com.garganttua.api.example.BookingDTO", eventPublisher = "class:com.garganttua.api.example.CustomEventPublisher", domain = "bookings")
public class BookingEntity extends AbstractGGAPIEntity {
	
	private Date from;
	
	private Date to;
	
	private String meetingRoomUuid; 
	
	private String ownerName;
	 
	private String ownerMail;

	@Override
	public IGGAPIEntityFactory<BookingEntity> getFactory() {
		return new IGGAPIEntityFactory<BookingEntity>() {
			
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
