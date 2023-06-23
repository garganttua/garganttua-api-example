package com.garganttua.api.example;

import java.util.List;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garganttua.api.security.authorization.IGGAPIAuthorization;
import com.garganttua.api.spec.IGGAPIDomain;
import com.garganttua.api.ws.AbstractGGAPIService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/cars")
@ComponentScan("org.sdc")
@Tag(name = "cars", description = "The cars API")
public class CarsRestService extends AbstractGGAPIService<CarEntity, CarDTO> {

	public CarsRestService(IGGAPIDomain<CarEntity, CarDTO> domain) {
		super(domain);
	}

	@Override
	public void authorize(boolean authorize_creation, boolean authorize_read_all, boolean authorize_read_one,
			boolean authorize_update_one, boolean authorize_delete_one, boolean authorize_delete_all,
			boolean authorize_count) {
		this.AUTHORIZE_CREATION = true;
        this.AUTHORIZE_DELETE_ALL = true;
        this.AUTHORIZE_DELETE_ONE = true;
        this.AUTHORIZE_GET_ALL = true;
        this.AUTHORIZE_GET_ONE = true;
        this.AUTHORIZE_UPDATE = true;
        this.AUTHORIZE_COUNT = true;
	}

	@Override
	protected List<IGGAPIAuthorization> createCustomAuthorizations() {
		// TODO Auto-generated method stub
		return null;
	}

}
