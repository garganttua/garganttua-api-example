package com.garganttua.api.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.api.events.GGAPIEvent;
import com.garganttua.api.events.IGGAPIEventPublisher;
import com.garganttua.api.spec.IGGAPIEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomEventPublisher implements IGGAPIEventPublisher {


	@Override
	public <Entity extends IGGAPIEntity> void publishEvent(GGAPIEvent<Entity> event) {
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			String eventSring = mapper.writeValueAsString(event);
			log.info("Sending event "+eventSring);
			
			
		} catch (JsonProcessingException e) {
			log.error("Error during publishing event", e);
		}
	}
}
