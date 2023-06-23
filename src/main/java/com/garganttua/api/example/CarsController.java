package com.garganttua.api.example;

import org.springframework.stereotype.Controller;

import com.garganttua.api.controller.SpringCrudifyController;
import com.garganttua.api.spec.ISpringCrudifyDomain;

@Controller
public class CarsController extends SpringCrudifyController<CarEntity, CarDTO>{

	public CarsController(ISpringCrudifyDomain<CarEntity, CarDTO> domain) {
		super(domain);
	}

}
