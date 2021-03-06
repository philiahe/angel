/*
 * FAE, Feinno App Engine
 *  
 * Create by gaolei Apr 7, 2012
 * 
 * Copyright (c) 2012 北京新媒传信科技有限公司
 */
package com.feinno.rpc.channel;

import com.feinno.diagnostic.perfmon.SmartCounter;

/**
 * 客户端调用方法缓存, 绑定在ep-service-method上
 * @see RpcClientMethodKey
 * 
 * @author 高磊 gaolei@feinno.com
 */
public class RpcClientMethodCache
{
	private RpcClientMethodKey key;
	private SmartCounter counter;
	
	public RpcClientMethodCache(RpcClientMethodKey key)
	{
		this.key = key;
	}
	
	public RpcClientMethodKey getKey()
	{
		return key;
	}

	public SmartCounter getCounter()
	{
		return counter;
	}

	public void setCounter(SmartCounter counter)
	{
		this.counter = counter;
	}	
}
