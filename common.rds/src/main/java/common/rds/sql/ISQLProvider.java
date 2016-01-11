package common.rds.sql;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author wls
 */
public interface ISQLProvider
{

	/**
	 * @return
	 */
	Map<Field, String> getFieldMap();

	/**
	 * @return
	 */
	Field getIdField();

	/**
	 * @return
	 */
	String getUnderscoreIdName();

	/**
	 * 提供查询count的SQL,类似:
	 * <p>
	 * select count(id) from tableName
	 *
	 * @return
	 */
	String provideCountAllSQL();

	/**
	 * 根据field条件和field范围条件提供查询COUNT的SQL,类似：
	 * <p>
	 * select count(id) from tableName where conditionField = value and rangeConditionField >(=) minVal and rangeConditionField
	 * <(=) maxVal
	 *
	 * @param fieldCondition
	 * @param fieldRangeCondition
	 * @return
	 */
	String provideCountByFieldRangeSQL(SQLFieldCondition fieldCondition, SQLFieldRangeCondition fieldRangeCondition);

	/**
	 * 根据field条件查询count的SQL,类似：
	 * <p>
	 * select count(id) from tableName where field = value
	 *
	 * @param fieldCondition
	 * @return
	 */
	String provideCountByFieldSQL(SQLFieldCondition fieldCondition);

	/**
	 * delete from tableName where id = ?
	 *
	 * @return
	 */
	String provideDeleteByIdSQL();

	/**
	 * @return
	 */
	String provideInsertSQL();

	/**
	 * 提供查询所有记录的SQL,类似:
	 * <p>
	 * select * from tableName [order by orderField desc]
	 *
	 * @param order
	 * @return
	 */
	String provideQueryAllSQL(SQLOrder order);

	/**
	 * select * from tableName where field = value and fieldRangeCondition [order by orderField limit start,num]
	 *
	 * @param fieldCondition
	 * @param fieldRangeCondition
	 * @param limit
	 * @param order
	 * @return
	 */
	String provideQueryByFieldRangeSQL(SQLFieldCondition fieldCondition, SQLFieldRangeCondition fieldRangeCondition,
			SQLLimit limit, SQLOrder order);

	/**
	 * 根据field条件查询的SQL,类似：
	 * <p>
	 * select * from tableName where field = value [order by orderField limit start,num]
	 *
	 * @param fieldCondition
	 * @return
	 */
	String provideQueryByFieldSQL(SQLFieldCondition fieldCondition, SQLLimit limit, SQLOrder order);

	/**
	 * select * from tableName where id = idValue
	 *
	 * @return
	 */
	String provideQueryByIdSQL();

	/**
	 * @return
	 */
	String provideUpdateSQL();

}
