package com.garganttua.api.example;

import java.util.List;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garganttua.api.security.authorization.IGGAPIAuthorization;
import com.garganttua.api.spec.GGAPICrudAccess;
import com.garganttua.api.spec.IGGAPIDomain;
import com.garganttua.api.ws.AbstractGGAPIService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/cars")
@ComponentScan("com.garganttua")
@Tag(name = "cars", description = "The cars API")
public class CarsRestService extends AbstractGGAPIService<CarEntity, CarDTO> {

	public CarsRestService(IGGAPIDomain<CarEntity, CarDTO> domain) {
		super(domain);
	}

	@Override
	public void allow(boolean allow_creation, boolean allow_read_all, boolean allow_read_one,
			boolean allow_update_one, boolean allow_delete_one, boolean allow_delete_all,
			boolean allow_count) {
		this.ALLOW_CREATION = true;
        this.ALLOW_DELETE_ALL = true;
        this.ALLOW_DELETE_ONE = true;
        this.ALLOW_GET_ALL = true;
        this.ALLOW_GET_ONE = true;
        this.ALLOW_UPDATE = true;
        this.ALLOW_COUNT = true;
	}

	@Override
	protected List<IGGAPIAuthorization> createCustomAuthorizations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAccesses(GGAPICrudAccess creation_access, GGAPICrudAccess read_all_access,
			GGAPICrudAccess read_one_access, GGAPICrudAccess update_one_access, GGAPICrudAccess delete_one_access,
			GGAPICrudAccess delete_all_access, GGAPICrudAccess count_access) {
		this.CREATION_ACCESS = creation_access;
		this.GET_ALL_ACCESS = read_all_access;
		this.GET_ONE_ACCESS = read_one_access;
		this.UPDATE_ACCESS = update_one_access;
		this.DELETE_ONE_ACCESS = delete_one_access;
		this.DELETE_ALL_ACCESS = delete_all_access;
		this.COUNT_ACCESS = count_access;
	}

}
