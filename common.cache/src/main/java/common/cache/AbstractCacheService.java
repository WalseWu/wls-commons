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

import java.security.NoSuchAlgorithmException;

import common.rds.exp.BugError;
import common.utils.MD5Util;

/**
 * @author wls
 */
abstract public class AbstractCacheService<T extends ICacheable> implements ICacheService<T>
{

	/* (non-Javadoc)
	 * @see common.cache.ICacheService#calculateKey(common.cache.ICacheable)
	 */
	@Override
	public String calculateKey(T t)
	{
		try {
			return MD5Util.md5Encode(t.getKeyGene());
		}
		catch (NoSuchAlgorithmException e) {
			throw new BugError(e);
		}
	}

}
