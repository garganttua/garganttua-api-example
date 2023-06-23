package com.garganttua.api.example;

import com.garganttua.api.events.IGGAPIEventPublisher;
import com.garganttua.api.events.GGAPIEntityEvent;
import com.garganttua.api.spec.IGGAPIEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomEventPublisher implements IGGAPIEventPublisher {

	@Override
	public void publishEntityEvent(GGAPIEntityEvent event, IGGAPIEntity entity) {
		log.info("Event [{}], [{}]", event, entity);
	}

}
