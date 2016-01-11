package common.rds.sql;

import java.text.MessageFormat;

/**
 * @author wls
 */
public class SQLFieldRangeCondition
{
	public static SQLFieldRangeCondition create(String filedName, Object minVal, Object maxVal, boolean includeMin,
			boolean includeMax)
	{
		return new SQLFieldRangeCondition(filedName, minVal, maxVal, includeMin, includeMax);
	}

	public static final String GT_SYMBOL = ">";

	public static final String GTE_SYMBOL = ">=";

	public static final String LT_SYMBOL = "<";

	public static final String LTE_SYMBOL = "<=";

	private final String fieldName;
	private final Object minVal;
	private final Object maxVal;
	private final boolean includeMin, includeMax;

	private static final String LEFT_OR_RIGHT_CONDITION = "{0} {1} ?";

	private static final String CONDITION = "{0} AND {1}";

	/**
	 * @param filedName
	 * @param minVal
	 * @param maxVal
	 * @param includeMin
	 * @param includeMax
	 */
	private SQLFieldRangeCondition(String filedName, Object minVal, Object maxVal, boolean includeMin, boolean includeMax)
	{
		fieldName = filedName;
		this.minVal = minVal;
		this.maxVal = maxVal;
		this.includeMin = includeMin;
		this.includeMax = includeMax;
	}

	public String getFieldRangeCondition()
	{
		String leftCondition = minVal == null ? null : MessageFormat.format(LEFT_OR_RIGHT_CONDITION, fieldName,
				includeMin ? GTE_SYMBOL : GT_SYMBOL);
		String rightCondition = maxVal == null ? null : MessageFormat.format(LEFT_OR_RIGHT_CONDITION, fieldName,
				includeMax ? LTE_SYMBOL : LT_SYMBOL);

		if (leftCondition == null) {
			return rightCondition;
		}

		if (rightCondition == null) {
			return leftCondition;
		}

		return MessageFormat.format(CONDITION, leftCondition, rightCondition);
	}

	public Object getMaxVal()
	{
		return maxVal;
	}

	public Object getMinVal()
	{
		return minVal;
	}
}
