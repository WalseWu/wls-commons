package common.rds.sql;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import common.rds.annotation.ExcludeDBField;
import common.rds.annotation.ExcludeDBField.FieldType;

/**
 * @author wls
 */
abstract public class AbstractSQLProvider implements ISQLProvider
{
	/**
	 * 将一个字符串转成驼峰格式
	 *
	 * @param name
	 * @param firstUpperCase
	 *            true则首字母大写，false则首字母小写
	 * @return
	 */
	public static String camelCaseName(String name, boolean firstUpperCase)
	{
		if (!StringUtils.hasLength(name)) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		String[] nms = name.split("_");

		if (firstUpperCase) {
			result.append(nms[0].substring(0, 1).toUpperCase());
		}
		else {
			result.append(nms[0].substring(0, 1).toLowerCase());
		}
		result.append(nms[0].substring(1).toLowerCase());

		for (int i = 1; i < nms.length; i++) {
			String nm = nms[i];
			result.append(nm.substring(0, 1).toUpperCase());
			result.append(nm.substring(1).toLowerCase());
		}
		return result.toString();
	}

	public static String prepareQueryAllSQL(Class<?> clz)
	{
		Field[] fields = clz.getDeclaredFields();
		StringBuffer sb = new StringBuffer("select ");
		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			ExcludeDBField ecl = f.getAnnotation(ExcludeDBField.class);
			if (ecl != null && ecl.value() == FieldType.MUST_NOT) {
				//排除
				continue;
			}
			String fieldName = f.getName();
			String underscoreName = AbstractSQLProvider.underscoreName(fieldName);
			sb.append(underscoreName).append(" as ").append(fieldName).append(",");
		}

		sb.deleteCharAt(sb.length() - 1);
		sb.append(" from ").append(AbstractSQLProvider.underscoreName(clz.getSimpleName())); //Order
		return sb.toString();
	}

	/**
	 * Convert a name in camelCase to an underscored name in lower case. Any upper case letters are converted to lower case with a
	 * preceding underscore.
	 *
	 * @param name
	 *            the string containing original name
	 * @return the converted name
	 */
	public static String underscoreName(String name)
	{
		if (!StringUtils.hasLength(name)) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		result.append(name.substring(0, 1).toLowerCase());
		for (int i = 1; i < name.length(); i++) {
			String s = name.substring(i, i + 1);
			String slc = s.toLowerCase();
			if (!s.equals(slc)) {
				result.append("_").append(slc);
			}
			else {
				result.append(s);
			}
		}
		return result.toString();
	}

	protected static final Logger logger = LoggerFactory.getLogger(AbstractSQLProvider.class);

	protected final Class<?> persistentClass;

	/**
	 * <Field,underscoreName>, No Id Field
	 */
	private final Map<Field, String> fieldMap = new LinkedHashMap<>();

	protected String underscoreIdName;

	private Field idField;

	protected final String tableName;

	//SQL
	/**
	 * select * from tableName {order}
	 */
	protected final String queryAllSQL;
	/**
	 * select count(id) from tableName
	 */
	protected final String countAllSQL;

	/**
	 * select * from tableName where {fieldCondition} [{order} {limit}]
	 */
	protected final String queryByFieldSQL;

	/**
	 * select count(id) from tableName where {fieldCondition}
	 */
	protected final String countByFieldSQL;

	/**
	 * select * from tableName where id = idValue
	 */
	protected final String queryByIdSQL;
	/**
	 * select * from tableName where {fieldCondition} and {fieldRangeRondition} {order} {limit}
	 */
	protected final String queryByFieldRangeSQL;
	/**
	 * select count(id) from tableName where {fieldCondition} and {fieldRangeRondition}
	 */
	protected final String countByFieldRangeSQL;
	protected final String insertSQL;

	protected final String updateSQL;

	protected final String deleteByIdSQL;

	public AbstractSQLProvider(String idName, Class<?> persistentClass)
	{
		this.persistentClass = persistentClass;
		String clzName = persistentClass.getSimpleName();
		tableName = AbstractSQLProvider.underscoreName(clzName);

		prepareFields(idName);
		Assert.notNull(idField);

		//Prepare SQL
		queryAllSQL = prepareQueryAllSQL();
		countAllSQL = prepareCountAllSQL();

		String queryAllSqlNoOrder = MessageFormat.format(queryAllSQL, "");

		queryByIdSQL = queryAllSqlNoOrder + "where " + underscoreIdName + " = ?";
		logger.info("cache query by id sql: {}", queryByIdSQL);

		queryByFieldSQL = queryAllSqlNoOrder + "where {0} {1} {2}";//fieldCondition order limit
		logger.info("cache query by field sql: {}", queryByFieldSQL);

		countByFieldSQL = countAllSQL + " where {0}";//fieldCondition
		logger.info("cache count by field sql: {}", countByFieldSQL);

		//fieldCondition, fieldRangeRondition, order, limit
		queryByFieldRangeSQL = queryAllSqlNoOrder + "where {0} and {1} {2} {3}";
		logger.info("cache query by field range sql: {}", queryByFieldRangeSQL);
		//fieldCondition, fieldRangeRondition
		countByFieldRangeSQL = countAllSQL + " where {0} and {1}";
		logger.info("cache count by field range sql: {}", countByFieldRangeSQL);

		insertSQL = prepareInsertSQL();
		updateSQL = prepareUpdateSQL();

		deleteByIdSQL = prepareDeleteByIdSQL();
	}

	@Override
	public Map<Field, String> getFieldMap()
	{
		return fieldMap;
	}

	@Override
	public Field getIdField()
	{
		return idField;
	}

	@Override
	public String getUnderscoreIdName()
	{
		return underscoreIdName;
	}

	/* (non-Javadoc)
	 * @see com.huishi.sz.common.rds.sql.ISQLProvider#provideCountAllSQL()
	 */
	@Override
	public String provideCountAllSQL()
	{
		return countAllSQL;
	}

	/* (non-Javadoc)
	 * @see com.huishi.sz.common.rds.sql.ISQLProvider#provideCountByFieldRangeSQL()
	 */
	@Override
	public String provideCountByFieldRangeSQL(SQLFieldCondition fieldCondition, SQLFieldRangeCondition fieldRangeCondition)
	{
		return MessageFormat.format(countByFieldRangeSQL, fieldCondition.getFieldCondition(),
				fieldRangeCondition.getFieldRangeCondition());
	}

	/* (non-Javadoc)
	 * @see com.huishi.sz.common.rds.sql.ISQLProvider#provideCountByFieldSQL()
	 */
	@Override
	public String provideCountByFieldSQL(SQLFieldCondition fieldCondition)
	{
		return MessageFormat.format(countByFieldSQL, fieldCondition.getFieldCondition());
	}

	/*
	 * @see com.huishi.sz.common.rds.sql.ISQLProvider#provideDeleteByIdSQL()
	 */
	@Override
	public String provideDeleteByIdSQL()
	{
		return deleteByIdSQL;
	}

	@Override
	public String provideInsertSQL()
	{
		return insertSQL;
	}

	/* (non-Javadoc)
	 * @see com.huishi.sz.common.rds.sql.ISQLProvider#provideQueryAllSQL()
	 */
	@Override
	public String provideQueryAllSQL(SQLOrder order)
	{
		return MessageFormat.format(queryAllSQL, order == null ? "" : order.getOrderClause());
	}

	@Override
	public String provideQueryByIdSQL()
	{
		return queryByIdSQL;
	}

	@Override
	public String provideUpdateSQL()
	{
		return updateSQL;
	}

	private String prepareCountAllSQL()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("select count(").append(underscoreIdName).append(")");
		sb.append(" from ").append(tableName);
		String sql = sb.toString();
		logger.info("cache count all sql: {}", sql);
		return sql;
	}

	/**
	 * @return
	 */
	private String prepareDeleteByIdSQL()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("delete from ").append(tableName);
		sb.append(" where ").append(underscoreIdName).append(" = ?");
		return sb.toString();
	}

	private void prepareFields(String idName)
	{
		Field[] fields = persistentClass.getDeclaredFields();

		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			ExcludeDBField ecl = f.getAnnotation(ExcludeDBField.class);
			if (ecl != null) {
				continue;
			}
			f.setAccessible(true);
			String fieldName = f.getName();
			String underscoreName = AbstractSQLProvider.underscoreName(fieldName);
			if (idName.equals(fieldName)) {
				idField = f;
				underscoreIdName = underscoreName;
			}
			else {
				fieldMap.put(f, underscoreName);
			}
		}
	}

	private String prepareInsertSQL()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("insert into ").append(tableName).append("({0}");
		for (Map.Entry<Field, String> entry : fieldMap.entrySet()) {
			sb.append(entry.getValue()).append(",");
		}
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append(") values ({1}");
		int fieldNum = fieldMap.size();
		for (int i = 0; i < fieldNum; i++) {
			sb.append("?,");
		}
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append(")");
		String sql = sb.toString();
		logger.info("cache insertSQL: {}", sql);
		return sql;
	}

	private String prepareQueryAllSQL()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("select ").append(underscoreIdName).append(" as ").append(idField.getName());
		for (Map.Entry<Field, String> entry : fieldMap.entrySet()) {
			sb.append(", ").append(entry.getValue()).append(" as ").append(entry.getKey().getName());
		}
		sb.append(" from ").append(tableName).append(" {0}"); //Order
		String sql = sb.toString();
		logger.info("cache query all sql: {}", sql);
		return sql;
	}

	/**
	 * @return
	 */
	private String prepareUpdateSQL()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("update ").append(tableName).append(" set ");
		for (Map.Entry<Field, String> entry : fieldMap.entrySet()) {
			sb.append(entry.getValue()).append(" = ?,");
		}
		sb.deleteCharAt(sb.lastIndexOf(","));
		sb.append(" where ").append(underscoreIdName).append(" = ?");
		return sb.toString();
	}

}
