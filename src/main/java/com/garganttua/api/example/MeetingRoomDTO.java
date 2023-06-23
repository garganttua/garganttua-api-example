package com.garganttua.api.example;

import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.garganttua.api.repository.dto.AbstractSpringCrudifyDTOObject;
import com.garganttua.api.repository.dto.ISpringCrudifyDTOFactory;
import com.garganttua.api.repository.dto.ISpringCrudifyDTOObject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "meetingRooms")
public class MeetingRoomDTO extends AbstractSpringCrudifyDTOObject<MeetingRoomEntity> {
	
	@JsonProperty
	private String name;
	
	@JsonProperty
	private String location;
	
	@JsonProperty
	private String[] facilities;
	
	public MeetingRoomDTO(String tenantId, MeetingRoomEntity entity) {
		super(tenantId, entity);
	}

	@Override
	public void create(MeetingRoomEntity entity) {
		this.name = entity.getName();
		this.location = entity.getLocation();
		this.facilities = entity.getFacilities();
	}

	@Override
	public MeetingRoomEntity convert() {
		MeetingRoomEntity mre = new MeetingRoomEntity(this.name, this.location, this.facilities);
		super.convert(mre);
		mre.setId(this.id);
		mre.setUuid(this.uuid);
		return mre;
	}

	@Override
	public void update(ISpringCrudifyDTOObject<MeetingRoomEntity> object) {
		this.name = ((MeetingRoomDTO) object).getName();
		this.location = ((MeetingRoomDTO) object).getLocation();
		this.facilities = ((MeetingRoomDTO) object).getFacilities();
	}

	@Override
	public ISpringCrudifyDTOFactory<MeetingRoomEntity, MeetingRoomDTO> getFactory() {
		ISpringCrudifyDTOFactory<MeetingRoomEntity, MeetingRoomDTO> factory = new ISpringCrudifyDTOFactory<MeetingRoomEntity, MeetingRoomDTO>() {
			@Override
			public MeetingRoomDTO newInstance(String tenantId, MeetingRoomEntity entity) {
				return new MeetingRoomDTO(tenantId, entity);
			}
		};
		return factory;
	}

}
