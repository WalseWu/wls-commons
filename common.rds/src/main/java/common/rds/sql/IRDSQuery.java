/*
 * Copyright (C) 2015
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package common.rds.sql;

/**
 * @author wls
 */
public interface IRDSQuery
{
	String COMMON_CLAUSE_FORMATTER = "{0}={1}";

	String COMMON_AND_FORMATTER = "{0} AND {1}";

	String toQueryString();
}
