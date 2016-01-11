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

import java.lang.reflect.Field;
import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import common.rds.dao.GenericDao;

/**
 * @author wls
 */
public class CacheDBOperator<T extends ICacheable> extends GenericDao<T>
{

	private Field cacheKeyField;

	/**
	 * @param idName
	 */
	public CacheDBOperator(String idName)
	{
		super(idName);
		Field[] fields = persistentClass.getDeclaredFields();
		inner: for (Field f : fields) {
			CacheKeyField ckf = f.getAnnotation(CacheKeyField.class);
			if (ckf != null) {
				cacheKeyField = f;
				cacheKeyField.setAccessible(true);
				break inner;
			}
		}

		if (cacheKeyField == null) {
			throw new IllegalStateException("No cache key field in " + persistentClass.getName());
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void createCacheable(T t)
	{
		try {
			T tt = getByCachedKey(cacheKeyField.get(t));
			if (tt == null) {
				super.insertIgnore(t, true);
			}
			else {
				Object idValue = sqlProvider.getIdField().get(tt);
				sqlProvider.getIdField().set(t, idValue);
				super.update(t);
			}
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Cache key field value get error:{}", e);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void deleteCacheable(T t) throws IllegalArgumentException, IllegalAccessException
	{
		//TODO: 暂时不删除了
		//		Long key = (Long) sqlProvider.getIdField().get(t);
		//		super.delete(key);
	}

	public T getByCachedKey(Object value)
	{
		List<T> list = super.getByField(cacheKeyField.getName(), value);
		return list == null || list.size() == 0 ? null : list.get(0);
	}

	/**
	 * @return the cacheKeyField
	 */
	public Field getCacheKeyField()
	{
		return cacheKeyField;
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<T> loadAllCacheable()
	{
		return super.getAll();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateCacheable(T t)
	{
		super.update(t);
	}
}
