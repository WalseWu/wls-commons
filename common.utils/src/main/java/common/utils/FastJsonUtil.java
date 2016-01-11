/*
 * Copyright (C) 2015 dzyh
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package common.utils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;

/**
 * @author wls
 */
public class FastJsonUtil
{
	public static FastJsonUtil getDefaultInstance()
	{
		return defaultInstance;
	}

	public static FastJsonUtil getInstanceWithDataFormat(String dateFormat)
	{
		if (dateFormat == null || dateFormat.isEmpty()) {
			return defaultInstance;
		}

		FastJsonUtil f = map.get(dateFormat);
		if (f != null) {
			return f;
		}

		map.put(dateFormat, new FastJsonUtil(dateFormat));
		return map.get(dateFormat);
	}

	private final static FastJsonUtil defaultInstance = new FastJsonUtil("yyyy-MM-dd HH:mm:ss");

	private final static Map<String, FastJsonUtil> map = new HashMap<>();

	static {
		map.put(defaultInstance.dateFormat, defaultInstance);
	}

	private final String dateFormat;

	/**
	 * @param dateFormat
	 */
	public FastJsonUtil(String dateFormat)
	{
		this.dateFormat = dateFormat;
	}

	@SuppressWarnings("unchecked")
	public final <T> T parseObject(String text, Class<T> clazz, Feature... features)
	{
		return (T) parseObject(text, (Type) clazz, ParserConfig.getGlobalInstance(), JSON.DEFAULT_PARSER_FEATURE, features);
	}

	@SuppressWarnings("unchecked")
	public final <T> T parseObject(String input, Type clazz, Feature... features)
	{
		return (T) parseObject(input, clazz, ParserConfig.getGlobalInstance(), JSON.DEFAULT_PARSER_FEATURE, features);
	}

	@SuppressWarnings("unchecked")
	public final <T> T parseObject(String input, Type clazz, int featureValues, Feature... features)
	{
		if (input == null) {
			return null;
		}

		for (Feature featrue : features) {
			featureValues = Feature.config(featureValues, featrue, true);
		}

		DefaultJSONParser parser = new DefaultJSONParser(input, ParserConfig.getGlobalInstance(), featureValues);
		parser.setDateFormat(dateFormat);
		T value = (T) parser.parseObject(clazz);

		JSON.handleResovleTask(parser, value);

		parser.close();

		return value;
	}

	@SuppressWarnings("unchecked")
	public final <T> T parseObject(String input, Type clazz, ParserConfig config, int featureValues, Feature... features)
	{
		if (input == null) {
			return null;
		}

		for (Feature featrue : features) {
			featureValues = Feature.config(featureValues, featrue, true);
		}

		DefaultJSONParser parser = new DefaultJSONParser(input, config, featureValues);
		parser.setDateFormat(dateFormat);
		T value = (T) parser.parseObject(clazz);

		JSON.handleResovleTask(parser, value);

		parser.close();

		return value;
	}

	@SuppressWarnings("unchecked")
	public final <T> T parseObject(String text, TypeReference<T> type, Feature... features)
	{
		return (T) parseObject(text, type.getType(), ParserConfig.getGlobalInstance(), JSON.DEFAULT_PARSER_FEATURE, features);
	}
}
