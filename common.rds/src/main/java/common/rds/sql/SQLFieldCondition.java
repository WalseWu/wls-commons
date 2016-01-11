package common.rds.sql;

import java.text.MessageFormat;

/**
 * @author wls
 */
public class SQLFieldCondition
{

	public static SQLFieldCondition create(String fieldName, Object value)
	{
		return new SQLFieldCondition(fieldName, value);
	}

	private final String fieldName;
	private final Object value;

	private final static String CONDITION_FOMAT = "{0} = ?";

	/**
	 * @param fieldName
	 * @param value
	 */
	private SQLFieldCondition(String fieldName, Object value)
	{
		this.fieldName = fieldName;
		this.value = value;
	}

	public String getFieldCondition()
	{
		return MessageFormat.format(CONDITION_FOMAT, fieldName);
	}

	public Object getValue()
	{
		return value;
	}
}
