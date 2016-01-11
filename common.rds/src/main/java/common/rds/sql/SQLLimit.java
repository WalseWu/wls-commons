/*
 * Copyright (C) 2015 Oracle
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package common.rds.sql;

import java.text.MessageFormat;

/**
 * @author wls
 */
public class SQLLimit
{

	public static SQLLimit createSQLLimit(int start, int limit)
	{
		return new SQLLimit(start, limit);
	}

	private static final String MYSQL_LIMIT_CLAUSE = "limit {0},{1}";
	int start;

	int limit;

	private SQLLimit(int start, int limit)
	{
		this.start = start;
		this.limit = limit;
	}

	public String getMySqlLimitClause()
	{
		return MessageFormat.format(MYSQL_LIMIT_CLAUSE, start, limit);
	}
}
