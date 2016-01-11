package common.rds.sql;

import java.text.MessageFormat;

import org.springframework.util.Assert;

/**
 * @author wls
 */
public class MySQLProvider extends AbstractSQLProvider
{

	/**
	 * @param idName
	 */
	public MySQLProvider(String idName, Class<?> clz)
	{
		super(idName, clz);
	}

	/* (non-Javadoc)
	 * @see com.huishi.sz.common.rds.sql.ISQLProvider#provideQueryByFieldRangeSQL()
	 */
	@Override
	public String provideQueryByFieldRangeSQL(SQLFieldCondition fieldCondition, SQLFieldRangeCondition fieldRangeCondition,
			SQLLimit limit, SQLOrder order)
	{
		return MessageFormat.format(queryByFieldRangeSQL, fieldCondition.getFieldCondition(), fieldRangeCondition
				.getFieldRangeCondition(), order == null ? "" : order.getOrderClause(),
				limit == null ? "" : limit.getMySqlLimitClause());
	}

	/* (non-Javadoc)
	 * @see com.huishi.sz.common.rds.sql.ISQLProvider#provideQueryByFieldSQL()
	 */
	@Override
	public String provideQueryByFieldSQL(SQLFieldCondition fieldCondition, SQLLimit limit, SQLOrder order)
	{
		Assert.notNull(fieldCondition);
		return MessageFormat.format(queryByFieldSQL, fieldCondition.getFieldCondition(),
				order == null ? "" : order.getOrderClause(), limit == null ? "" : limit.getMySqlLimitClause());
	}

}
