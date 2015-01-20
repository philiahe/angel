/*
 * FAE, Feinno App Engine
 *  
 * Create by windcraft 2011-2-9
 * 
 * Copyright (c) 2011 北京新媒传信科技有限公司
 */
package com.feinno.appengine.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 用于包含一组资源
 * 
 * @author 高磊 gaolei@feinno.com
 */
public class ResourceGroup<E extends Resource>
{
	private Resource[] resources;

	public ResourceGroup(List<Resource> resources)
	{
		int max = -1;
		for (Resource r: resources) {
			if (max < r.index())
				max = r.index();
		}
		
		this.resources = new Resource[max + 1];

		for (Resource r: resources) {
			this.resources[r.index()] = r;
		}
	}
	
	public ResourceGroup(List<Resource> resources, ResourceType type){
		if(!(ResourceType.NONE == type))
			throw new IllegalArgumentException("Unknown resourceType:" + type);
		
		this.resources = new Resource[1];
		this.resources[0] = resources.get(0);
	}
	
	@SuppressWarnings("unchecked")
	public E getResource(int index)
	{
		return (E)resources[index];
	}
	
	@SuppressWarnings("unchecked")
	public Collection<E> resources()
	{
		ArrayList<E> ret = new ArrayList<E>();
		for (int i = 0; i < resources.length; i++) {
			if (resources[i] != null)
				ret.add((E) resources[i]);
		}
		return ret;
	}
}
