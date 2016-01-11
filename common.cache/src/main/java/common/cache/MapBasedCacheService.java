/*
 * Copyright (C) 2015 dzyh
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package common.cache;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wls
 */
public class MapBasedCacheService<T extends ICacheable> extends AbstractCacheService<T>
{
	private class CacheableWrapper<K extends ICacheable>
	{
		K target;
		short operation = 0;

		/**
		 * @param timestamp
		 * @param target
		 */
		public CacheableWrapper(K target, short operation)
		{
			this.target = target;
			this.operation = operation;
		}
	}

	public static void main(String[] args)
	{
		String key = "djaflkal;fa;edfaqfq:123";
		long id = new Long(key.substring(key.lastIndexOf(':') + 1));
		System.out.println(id);
	}

	static short NON_OPERATION = -1;
	static short CREATE_OPERATION = 1;

	static short UPDATE_OPERATION = 2;

	private final ConcurrentHashMap<String, CacheableWrapper<T>> cacheMap = new ConcurrentHashMap<>();

	private final ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();

	private ScheduledFuture<?> expireFuture;

	private long expireMillisecond = 1000 * 60 * 60 * 24 * 7; //24 hour * 7

	private final Lock refreshLock = new ReentrantLock();

	private CacheDBOperator<T> dbOperator;

	private long schedlueTimeSecond = 30;

	private static final Logger logger = LoggerFactory.getLogger(MapBasedCacheService.class);

	/* (non-Javadoc)
	 * @see common.cache.ICacheService#deleteByKey(java.lang.String)
	 */
	@Override
	public boolean deleteByKey(String key)
	{
		cacheMap.remove(key);
		return true;
	}

	/**
	 * @return the dbOperator
	 */
	public CacheDBOperator<T> getDbOperator()
	{
		return dbOperator;
	}

	/**
	 * @return the expireMillisecond
	 */
	@Override
	public long getExpireMillisecond()
	{
		return expireMillisecond;
	}

	/**
	 * @return the schedlueTimeSecond
	 */
	public long getSchedlueTimeSecond()
	{
		return schedlueTimeSecond;
	}

	@Override
	public boolean isCached(String key)
	{
		return cacheMap.contains(key);
	}

	/* (non-Javadoc)
	 * @see common.cache.ICacheService#readObject(java.lang.String, boolean)
	 */
	@Override
	public T readObject(String key, boolean resetExpireTime)
	{
		logger.debug("read: '{}', resetExpireTime: {}", key, resetExpireTime);
		CacheableWrapper<T> w = cacheMap.get(key);

		if (w == null) {
			//缓存没有命中，从数据库中读取
			//			long id = new Long(key.substring(key.lastIndexOf(':') + 1));
			logger.debug("Read key '{}' from DB", key);
			T target = this.dbOperator.getByCachedKey(key);
			if (target != null) {
				target.setCachedTime(System.currentTimeMillis());
				cacheMap.put(key, new CacheableWrapper<>(target, UPDATE_OPERATION));
			}
			return target;
		}

		T t = w.target;
		if (t != null && resetExpireTime) {
			t.setCachedTime(System.currentTimeMillis());
			w.operation = UPDATE_OPERATION;
		}
		return t;
	}

	/**
	 * @param dbOperator
	 *            the dbOperator to set
	 */
	public void setDbOperator(CacheDBOperator<T> dbOperator)
	{
		this.dbOperator = dbOperator;
	}

	/**
	 * @param expireMillisecond
	 *            the expireMillisecond to set
	 */
	public void setExpireMillisecond(long expireMillisecond)
	{
		this.expireMillisecond = expireMillisecond;
	}

	/**
	 * @param schedlueTimeSecond
	 *            the schedlueTimeSecond to set
	 */
	public void setSchedlueTimeSecond(long schedlueTimeSecond)
	{
		this.schedlueTimeSecond = schedlueTimeSecond;
	}

	@PostConstruct
	public void start() throws IllegalArgumentException, IllegalAccessException
	{
		refresh();
		startScheduleTask();
	}

	public void startScheduleTask()
	{
		expireFuture = ex.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run()
			{
				try {
					Set<String> keys = cacheMap.keySet();
					for (String key : keys) {
						logger.debug("refresh key: {}", key);
						CacheableWrapper<T> w = cacheMap.get(key);
						if (w == null || w.target == null) {
							continue;
						}
						if (System.currentTimeMillis() - w.target.getCachedTime() > getExpireMillisecond()) {
							cacheMap.remove(key);
						}
						else if (w.operation > 0) {
							doDBSaveUpdate(w);
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, schedlueTimeSecond, schedlueTimeSecond, TimeUnit.SECONDS);
	}

	@PreDestroy
	public void stop()
	{
		try {
			ex.awaitTermination(15, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
		}
		if (expireFuture != null) {
			expireFuture.cancel(true);
		}
		ex.shutdown();
	}

	/* (non-Javadoc)
	 * @see common.cache.ICacheService#writeObject(common.cache.ICacheable)
	 */
	@Override
	public String writeObject(T t)
	{
		String key = calculateKey(t);
		t.setCachedTime(System.currentTimeMillis());
		try {
			dbOperator.getCacheKeyField().set(t, key);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error("Set cache key field value error !");
		}
		cacheMap.put(key, new CacheableWrapper<>(t, CREATE_OPERATION));
		return key;
	}

	private void doDBSaveUpdate(final CacheableWrapper<T> w) throws IllegalArgumentException, IllegalAccessException
	{
		if (w.operation == CREATE_OPERATION) {
			logger.debug("create key: {}", dbOperator.getCacheKeyField().get(w.target));
			dbOperator.createCacheable(w.target);
		}
		else if (w.operation == UPDATE_OPERATION) {
			logger.debug("update key: {}", dbOperator.getCacheKeyField().get(w.target));
			dbOperator.updateCacheable(w.target);
		}
		w.operation = NON_OPERATION;
	}

	/**
	 * 刷新整个缓存
	 *
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private void refresh() throws IllegalArgumentException, IllegalAccessException
	{
		if (refreshLock.tryLock()) {
			try {
				List<T> list = dbOperator.loadAllCacheable();
				cacheMap.clear();
				if (list.size() > 0) {
					for (T tt : list) {
						if (System.currentTimeMillis() - tt.getCachedTime() <= expireMillisecond) {
							cacheMap.put(calculateKey(tt), new CacheableWrapper<>(tt, NON_OPERATION));
						}
					}
				}
			}
			finally {
				refreshLock.unlock();
			}
		}
	}
}
