package com.feinno.limiter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流器:
 * 1.限制最大流量 & 每妙最大流量
 * 2.定时重置 - 默认最大流量10分钟重置一次
 *  
 * @author lichunlei
 *
 */
public class Limiter {
	
	private int maxCount;
	private AtomicInteger counter;
	
	private int maxCountPerSecond;
	private AtomicInteger counterPerSecond;
	
	/**
	 * 构造函数
	 * @param maxCount 最大限流数量-10分钟内积压直
	 * @param maxCountPerSecond 每秒最大限流数量
	 */
	public Limiter(int maxCount,int maxCountPerSecond)
	{
		this.maxCount = maxCount;
		this.maxCountPerSecond = maxCountPerSecond;
		counter = new AtomicInteger(0);
		counterPerSecond = new AtomicInteger(0);
	}
		
	public void increment()
	{
		counter.incrementAndGet();
		counterPerSecond.incrementAndGet();
	}
	
	public void decrement()
	{
		counter.decrementAndGet();
		//counterPerSecond.decrementAndGet();
	}
	
	public int getCount()
	{
		return counter.get();
	}
	
	public void setCount(int v)
	{
		counter.set(v);
	}
	
	public int getCountPerSecond()
	{
		return counterPerSecond.get();
	}
	
	public void setCountPerSecond(int v)
	{
		counterPerSecond.set(v);
	}

	
	public void resetCounter()
	{
		counter.set(0);
	}
	
	public void resetCounterPerSecond()
	{
		counterPerSecond.set(0);
	}
	
	/**
	 * 总量超过限定数量
	 * @return
	 */
	public boolean isOverFlow()
	{
		return counter.get()>=maxCount;
	}
	/**
	 * 每秒超过限定数量
	 * @return
	 */
	public boolean isOverFlowPerSecond()
	{
		return counterPerSecond.get()>= maxCountPerSecond ;
	}

}

