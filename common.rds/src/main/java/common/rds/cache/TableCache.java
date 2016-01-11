/*
 * Copyright (C) 2015 dzyh
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package common.rds.cache;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import common.rds.annotation.PrimaryKey;
import common.rds.sql.AbstractSQLProvider;

/**
 * 数据库表数据缓存类，全表缓存，适用一些常量表或者数据量比较小的表.
 *
 * @author wls
 */
public class TableCache<T>
{

	private final String tableName;

	private final String className;

	private final Map<Object, T> cache = new HashMap<>();

	private final Lock refreshLock = new ReentrantLock();

	private Class<T> cls;

	private Field primaryKey;

	private final static Logger logger = LoggerFactory.getLogger(TableCache.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * 对应Model类全名
	 *
	 * @param className
	 */
	public TableCache(String className)
	{
		this.className = className;
		int idx = className.lastIndexOf(".");
		this.tableName = AbstractSQLProvider.underscoreName(className.substring(idx + 1));
	}

	public T get(Object key)
	{
		T bean = cache.get(key);
		if (bean != null) {
			return bean;
		}
		if (key == null) {
			return null;
		}
		T t = null;
		try {
			t = jdbcTemplate.queryForObject("select * from " + tableName + " where " + primaryKey.getName() + "=" + key,
					new BeanPropertyRowMapper<T>(cls));
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
		if (t != null) {
			cache.put(key, t);
		}
		return t;
	}

	public Collection<T> getAll()
	{
		return cache.values();
	}

	@SuppressWarnings("unchecked")
	@PostConstruct
	public void init() throws IllegalArgumentException, IllegalAccessException, ClassNotFoundException
	{
		cls = (Class<T>) Class.forName(className);

		Field[] fs = cls.getDeclaredFields();
		for (Field f : fs) {
			PrimaryKey pk = f.getAnnotation(PrimaryKey.class);
			if (pk != null) {
				primaryKey = f;
				break;
			}
		}
		if (primaryKey == null) {
			logger.error("Error: no PK in the model: {}", cls.getName());
			return;
		}

		primaryKey.setAccessible(true);
		refresh();
	}

	public void refresh() throws IllegalArgumentException, IllegalAccessException
	{
		if (refreshLock.tryLock()) {
			try {
				List<T> list = jdbcTemplate.query("select * from " + tableName, new BeanPropertyRowMapper<T>(cls));
				cache.clear();
				if (list.size() > 0) {
					for (T tt : list) {
						cache.put(primaryKey.get(tt), tt);
					}

				}
			}
			finally {
				refreshLock.unlock();
			}
		}
	}

	public void refresh(Object key)
	{
		T t = jdbcTemplate.queryForObject("select * from " + tableName + " where " + primaryKey.getName() + "=" + key,
				new BeanPropertyRowMapper<T>(cls));
		if (t != null) {
			cache.put(key, t);
		}
		else {
			cache.remove(key);
		}
	}
}
