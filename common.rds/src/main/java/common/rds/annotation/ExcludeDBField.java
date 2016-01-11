/*
 * Copyright (C) 2015
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package common.rds.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author wls
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcludeDBField
{

	public static enum FieldType
	{
		/**
		 * 包含的字段
		 */
		MUST,

		/**
		 * 可选的字段
		 */
		SHOULD,

		/**
		 * 排除的字段种类
		 */
		MUST_NOT;
	}

	FieldType value() default FieldType.MUST_NOT;
}
