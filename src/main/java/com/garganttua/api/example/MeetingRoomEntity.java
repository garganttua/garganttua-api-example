package com.garganttua.api.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.garganttua.api.spec.AbstractGGAPIEntity;
import com.garganttua.api.spec.IGGAPIEntityFactory;
import com.garganttua.api.spec.GGAPIEntity;

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
@GGAPIEntity(dto = "org.sdc.spring.domain.crudify.example.MeetingRoomDTO", eventPublisher = "class:org.sdc.spring.domain.crudify.example.CustomEventPublisher", domain = "meetingRooms")
public class MeetingRoomEntity extends AbstractGGAPIEntity {

	@JsonProperty
	private String name;
	
	@JsonProperty
	private String location;
	
	@JsonProperty
	private String[] facilities;

	@Override
	public IGGAPIEntityFactory<MeetingRoomEntity> getFactory() {
		IGGAPIEntityFactory<MeetingRoomEntity> factory = new IGGAPIEntityFactory<MeetingRoomEntity>() {

			@Override
			public MeetingRoomEntity newInstance() {
				return new MeetingRoomEntity();
			}

			@Override
			public MeetingRoomEntity newInstance(String uuid) {
				MeetingRoomEntity entity = new MeetingRoomEntity();
				entity.setUuid(uuid);
				return entity;
			}
		};
		return factory ;
	}

}