package com.garganttua.api.example;

import com.garganttua.api.events.ISpringCrudifyEventPublisher;
import com.garganttua.api.events.SpringCrudifyEntityEvent;
import com.garganttua.api.spec.ISpringCrudifyEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomEventPublisher implements ISpringCrudifyEventPublisher {

	@Override
	public void publishEntityEvent(SpringCrudifyEntityEvent event, ISpringCrudifyEntity entity) {
		log.info("Event [{}], [{}]", event, entity);
	}

}
