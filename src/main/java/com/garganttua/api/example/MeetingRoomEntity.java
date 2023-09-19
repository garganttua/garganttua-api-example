package com.garganttua.api.example;

import org.geojson.Point;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.garganttua.api.spec.AbstractGGAPIEntity;
import com.garganttua.api.spec.GGAPICrudAccess;
import com.garganttua.api.spec.GGAPIEntity;
import com.garganttua.api.spec.IGGAPIEntityFactory;
import com.garganttua.api.spec.IGGAPIEntityWithTenant;

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
@GGAPIEntity(dto = "com.garganttua.api.example.MeetingRoomDTO", 
eventPublisher = "class:com.garganttua.api.example.CustomEventPublisher", 
domain = "meetingRooms", 
publicEntity = true, 
showTenantId = true,
update_one_access = GGAPICrudAccess.owner,
creation_access =  GGAPICrudAccess.authenticated,
delete_all_access = GGAPICrudAccess.owner,
count_access = GGAPICrudAccess.anonymous,
read_all_access = GGAPICrudAccess.anonymous,
read_one_access = GGAPICrudAccess.anonymous,
geolocialized = "location")
public class MeetingRoomEntity extends AbstractGGAPIEntity implements IGGAPIEntityWithTenant {

	@JsonProperty
	private String name;
	
	@JsonProperty
	private Point location;
	
	@JsonProperty
	private String[] facilities;

	private String tenantId;

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

	@Override
	public String getTenantId() {
		return this.tenantId;
	}

	@Override
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

}