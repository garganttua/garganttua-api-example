package com.garganttua.api.example.overriding;

import org.springframework.stereotype.Controller;

import com.garganttua.api.controller.GGAPIController;
import com.garganttua.api.spec.IGGAPIDomain;

//@Controller
public class CarsController extends GGAPIController<CarEntity, CarDTO>{

	public CarsController(IGGAPIDomain<CarEntity, CarDTO> domain) {
		super(domain);
	}

}
