package com.garganttua.api.example.tenants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.garganttua.api.business.IGGAPIBusiness;
import com.garganttua.api.core.GGAPIEntityException;
import com.garganttua.api.core.IGGAPICaller;
import com.garganttua.api.engine.IGGAPIEngine;

public class TenantsBusiness implements IGGAPIBusiness<TenantEntity>{

	private IGGAPIEngine engine;

	@Override
	public void setEngine(IGGAPIEngine engine) {
		this.engine = engine;
	}

	@Override
	public TenantEntity beforeCreate(IGGAPICaller caller, TenantEntity entity, Map<String, String> customParameters) throws GGAPIEntityException {
		
		//Authorize everything for a new user
		List<String> auths = new ArrayList<String>();
		
		this.engine.getAccessRulesRegistry().getAccessRules().forEach(r -> {
			if( r.getAuthority() != null ) {
				auths.add(r.getAuthority());
			}
		});
		
		List<String> listWithoutDuplicates = new ArrayList<String>(new HashSet<>(auths));
		
		entity.setUserAuthorities(listWithoutDuplicates);
		return entity;
	}

	@Override
	public TenantEntity afterCreate(IGGAPICaller caller, TenantEntity entity, Map<String, String> customParameters)
			throws GGAPIEntityException {
		// TODO Auto-generated method stub
		return entity;
	}

	@Override
	public TenantEntity beforeUpdate(IGGAPICaller caller, TenantEntity entity, Map<String, String> customParameters)
			throws GGAPIEntityException {
		// TODO Auto-generated method stub
		return entity;
	}

	@Override
	public TenantEntity afterUpdate(IGGAPICaller caller, TenantEntity entity, Map<String, String> customParameters)
			throws GGAPIEntityException {
		// TODO Auto-generated method stub
		return entity;
	}

	@Override
	public TenantEntity beforeDelete(IGGAPICaller caller, TenantEntity entity, Map<String, String> customParameters)
			throws GGAPIEntityException {
		// TODO Auto-generated method stub
		return entity;
	}

	@Override
	public TenantEntity afterDelete(IGGAPICaller caller, TenantEntity entity, Map<String, String> customParameters)
			throws GGAPIEntityException {
		// TODO Auto-generated method stub
		return entity;
	}

	@Override
	public TenantEntity afterGetOne(IGGAPICaller caller, TenantEntity entity, Map<String, String> customParameters)
			throws GGAPIEntityException {
		// TODO Auto-generated method stub
		return entity;
	}

	@Override
	public TenantEntity beforeGetOne(IGGAPICaller caller, TenantEntity entity, Map<String, String> customParameters)
			throws GGAPIEntityException {
		// TODO Auto-generated method stub
		return entity;
	}

	@Override
	public List<TenantEntity> beforeGetList(IGGAPICaller caller, List<TenantEntity> entities,
			Map<String, String> customParameters) throws GGAPIEntityException {
		// TODO Auto-generated method stub
		return entities;
	}

	@Override
	public List<?> afterGetList(IGGAPICaller caller, List<?> entities,
			Map<String, String> customParameters) throws GGAPIEntityException {
		// TODO Auto-generated method stub
		return entities;
	}

}
