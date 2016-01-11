package common.rds.dao;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import common.rds.exp.BugError;
import common.rds.sql.AbstractSQLProvider;
import common.rds.sql.ISQLProvider;
import common.rds.sql.MySQLProvider;
import common.rds.sql.SQLFieldCondition;
import common.rds.sql.SQLFieldRangeCondition;
import common.rds.sql.SQLLimit;
import common.rds.sql.SQLOrder;

public abstract class GenericDao<T> extends AbstractDao
{
	protected static final Logger logger = LoggerFactory.getLogger(GenericDao.class);

	protected ISQLProvider sqlProvider;

	protected Class<T> persistentClass;

	/**
	 * 指定对应主键名字
	 *
	 * @param idName
	 */
	@SuppressWarnings("unchecked")
	public GenericDao(String idName)
	{
		this.persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		sqlProvider = new MySQLProvider(idName, persistentClass);
	}

	/**
	 * 批量插入新纪录，如果autoIncrememntId为true，则id必须是<0的值，否则会认为该对象数据已经存在，无法做insert
	 *
	 * @param objs
	 * @param autoIncrememntId
	 *            id是否自增值
	 */
	public void batchInsert(List<T> objs, boolean autoIncrememntId)
	{
		List<Object[]> params = new ArrayList<>();
		String sql = null;
		for (T obj : objs) {
			Assert.notNull(obj);
			Object idVal = null;
			try {
				idVal = sqlProvider.getIdField().get(obj);
			}
			catch (IllegalArgumentException | IllegalAccessException e2) {
				throw new BugError();
			}
			Object[] args = null;
			int i = 0;
			if (autoIncrememntId) {
				//id是自增类型
				if (idVal != null) {
					if (idVal instanceof Number) {
						Number idNum = (Number) idVal;
						if (idNum.longValue() > 0) {
							throw new IllegalStateException("对于id自增对象,做insert时候id必须小于等于0");
						}
					}
				}
				args = new Object[sqlProvider.getFieldMap().size()];
				if (sql == null) {
					sql = MessageFormat.format(sqlProvider.provideInsertSQL(), "", "");
				}
			}
			else {
				args = new Object[sqlProvider.getFieldMap().size() + 1];
				args[i++] = idVal;
				if (sql == null) {
					sql = MessageFormat.format(sqlProvider.provideInsertSQL(), sqlProvider.getUnderscoreIdName() + ",", "?,");
				}
			}

			for (Map.Entry<Field, String> entry : sqlProvider.getFieldMap().entrySet()) {
				try {
					args[i++] = entry.getKey().get(obj);
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
					throw new BugError();
				}
			}
			params.add(args);
		}

		jdbcTemplate.batchUpdate(sql, params);
	}

	/**
	 * 所有记录总数
	 *
	 * @return
	 */
	public long countAll()
	{
		String sql = sqlProvider.provideCountAllSQL();
		logger.debug(sql);
		return jdbcReader.columnQuerySingle(sql);
	}

	/**
	 * 根据字段条件查询记录数量
	 *
	 * @param filedName
	 * @param value
	 * @return count
	 */
	public long countByField(String filedName, Object value)
	{
		String fieldUnderscore = AbstractSQLProvider.underscoreName(filedName);
		String sql = sqlProvider.provideCountByFieldSQL(SQLFieldCondition.create(fieldUnderscore, value));
		logger.debug(sql);
		return jdbcReader.columnQuerySingle(sql, value);
	}

	/**
	 * 根据字段区间查询
	 *
	 * @param rangeFieldName
	 *            字段名字
	 * @param rangeValueMin
	 *            字段的范围最小值
	 * @param rangeValueMax
	 *            字段范围最大值
	 * @param includeMin
	 *            是否包含最小值
	 * @param includeMax
	 *            是否包含最大值
	 * @return
	 */
	public long countByFieldRange(String rangeFieldName, Object rangeValueMin, Object rangeValueMax, boolean includeMin,
			boolean includeMax)
	{
		return countByFieldRange(rangeFieldName, rangeValueMin, rangeValueMax, includeMin, includeMax, null, null);
	}

	/**
	 * 根据字段区间,以及其他条件查询
	 *
	 * @param rangeFieldName
	 *            字段名字
	 * @param rangeValueMin
	 *            字段的范围最小值
	 * @param rangeValueMax
	 *            字段范围最大值
	 * @param includeMin
	 *            是否包含最小值
	 * @param includeMax
	 *            是否包含最大值
	 * @param fieldConditionName
	 *            其他条件的字段名
	 * @param fieldConditionValue
	 *            其他条件的值
	 * @return
	 */
	public long countByFieldRange(String rangeFieldName, Object rangeValueMin, Object rangeValueMax, boolean includeMin,
			boolean includeMax, String fieldConditionName, Object fieldConditionValue)
	{
		if (fieldConditionName == null) {
			fieldConditionName = "1";
			fieldConditionValue = 1;
		}
		String sql = sqlProvider.provideCountByFieldRangeSQL(SQLFieldCondition.create(
				AbstractSQLProvider.underscoreName(fieldConditionName), fieldConditionValue), SQLFieldRangeCondition.create(
						AbstractSQLProvider.underscoreName(rangeFieldName), rangeValueMin, rangeValueMax, includeMin, includeMax));
		logger.debug(sql);
		return jdbcReader.columnQuerySingle(sql, fieldConditionValue, rangeValueMin, rangeValueMax);
	}

	/**
	 * @param id
	 * @return the number of rows deleted
	 */
	public int delete(Long id)
	{
		return jdbcTemplate.update(sqlProvider.provideDeleteByIdSQL(), id);
	}

	public <K> List<K> executeQuery(String sql, Class<K> type, Object... params)
	{
		return jdbcReader.executeQuery(sql, type, params);
	}

	public List<T> executeQuery(String sql, Object... params)
	{
		return jdbcReader.executeQuery(sql, persistentClass, params);
	}

	public <K> K executeQueryObject(String sql, Class<K> type, Object... params)
	{
		return jdbcReader.columnQuerySingle(sql, params);
	}

	/**
	 * 执行客户端提供的SQL和参数
	 *
	 * @param updateSQL
	 * @param args
	 */
	public int executeUpdateSQL(String updateSQL, Object... args)
	{
		logger.debug(updateSQL);
		return jdbcTemplate.update(updateSQL, args);
	}

	/**
	 * 根据id获取记录
	 *
	 * @return
	 */
	public T get(long id)
	{
		String sql = sqlProvider.provideQueryByIdSQL();
		logger.debug(sql);
		return jdbcReader.executeQueryObject(sql, persistentClass, id);
	}

	/**
	 * @return all record from table
	 */
	public List<T> getAll()
	{
		return getAll(null, null);
	}

	/**
	 * @return all record from table
	 */
	public List<T> getAll(String orderField, String orderType)
	{
		SQLOrder order = orderField == null ? null : SQLOrder.createOrder(AbstractSQLProvider.underscoreName(orderField),
				orderType);
		String sql = sqlProvider.provideQueryAllSQL(order);
		logger.debug(sql);
		return jdbcReader.executeQuery(sql, persistentClass);
	}

	/**
	 * 根据字段查询
	 *
	 * @param filedName
	 * @param value
	 * @return
	 */
	public List<T> getByField(String filedName, Object value)
	{
		return getByField(filedName, value, -1, -1, null, null);
	}

	/**
	 * 用于分页查询，根据字段查询，从第 @param start 条记录开始，查询 @param limit条记录
	 *
	 * @param filedName
	 * @param value
	 * @param start
	 * @param limit
	 * @param orderField
	 * @param orderType
	 * @return
	 */
	public List<T> getByField(String filedName, Object value, int start, int limit)
	{
		return getByField(filedName, value, start, limit, null, null);
	}

	/**
	 * 用于分页查询，根据字段查询，从第 @param start 条记录开始，查询 @param limit条记录
	 *
	 * @param filedName
	 * @param value
	 * @param start
	 * @param limit
	 * @param orderField
	 * @param orderType
	 * @return
	 */
	public List<T> getByField(String filedName, Object value, int start, int limit, String orderField, String orderType)
	{
		SQLFieldCondition fieldCondition = SQLFieldCondition.create(AbstractSQLProvider.underscoreName(filedName), value);
		SQLLimit sqlLimit = null;
		if (start >= 0 && limit > 0) {
			sqlLimit = SQLLimit.createSQLLimit(start, limit);
		}

		SQLOrder sqlOrder = null;
		if (orderField != null) {
			sqlOrder = SQLOrder.createOrder(AbstractSQLProvider.underscoreName(orderField), orderType);
		}

		String sql = sqlProvider.provideQueryByFieldSQL(fieldCondition, sqlLimit, sqlOrder);
		logger.debug(sql);
		return jdbcReader.executeQuery(sql, persistentClass, value);
	}

	/**
	 * 根据字段区间查询
	 *
	 * @param rangeFieldName
	 *            字段名字
	 * @param rangeValueMin
	 *            字段的范围最小值
	 * @param rangeValueMax
	 *            字段范围最大值
	 * @param includeMin
	 *            是否包含最小值
	 * @param includeMax
	 *            是否包含最大值
	 * @return
	 */
	public List<T> getByFieldRange(String rangeFieldName, Object rangeValueMin, Object rangeValueMax, boolean includeMin,
			boolean includeMax)
			{
		return getByFieldRange(rangeFieldName, rangeValueMin, rangeValueMax, includeMin, includeMax, null, null);
			}

	/**
	 * 用于分页查询，根据字段区间查询
	 *
	 * @param rangeFieldName
	 *            字段名字
	 * @param rangeValueMin
	 *            字段的范围最小值
	 * @param rangeValueMax
	 *            字段范围最大值
	 * @param includeMin
	 *            是否包含最小值
	 * @param includeMax
	 *            是否包含最大值
	 * @param start
	 *            从第几条记录开始查询
	 * @param limit
	 *            总共查询多少条记录
	 * @return
	 */
	public List<T> getByFieldRange(String rangeFieldName, Object rangeValueMin, Object rangeValueMax, boolean includeMin,
			boolean includeMax, int start, int limit)
			{
		return getByFieldRange(rangeFieldName, rangeValueMin, rangeValueMax, includeMin, includeMax, null, null, start, limit);
			}

	/**
	 * 根据字段区间,以及其他条件查询
	 *
	 * @param rangeFieldName
	 *            字段名字
	 * @param rangeValueMin
	 *            字段的范围最小值
	 * @param rangeValueMax
	 *            字段范围最大值
	 * @param includeMin
	 *            是否包含最小值
	 * @param includeMax
	 *            是否包含最大值
	 * @param fieldConditionName
	 *            其他条件的字段名
	 * @param fieldConditionValue
	 *            其他条件的值
	 * @return
	 */
	public List<T> getByFieldRange(String rangeFieldName, Object rangeValueMin, Object rangeValueMax, boolean includeMin,
			boolean includeMax, String fieldConditionName, Object fieldConditionValue)
			{
		return getByFieldRange(rangeFieldName, rangeValueMin, rangeValueMax, includeMin, includeMax, fieldConditionName,
				fieldConditionValue, -1, -1);
			}

	/**
	 * 根据字段区间,以及其他条件查询
	 *
	 * @param rangeFieldName
	 *            字段名字
	 * @param rangeValueMin
	 *            字段的范围最小值
	 * @param rangeValueMax
	 *            字段范围最大值
	 * @param includeMin
	 *            是否包含最小值
	 * @param includeMax
	 *            是否包含最大值
	 * @param fieldConditionName
	 *            其他条件的字段名
	 * @param fieldConditionVal
	 *            其他条件的值
	 * @param start
	 *            从第几条记录开始查询
	 * @param limit
	 *            总共查询多少条记录
	 * @return
	 */
	public List<T> getByFieldRange(String rangeFieldName, Object rangeValueMin, Object rangeValueMax, boolean includeMin,
			boolean includeMax, String fieldConditionName, Object fieldConditionVal, int start, int limit)
			{
		return getByFieldRange(rangeFieldName, rangeValueMin, rangeValueMax, includeMin, includeMax, fieldConditionName,
				fieldConditionVal, start, limit, null, null);
			}

	/**
	 * 根据字段区间,以及其他条件查询
	 *
	 * @param rangeFieldName
	 *            字段名字
	 * @param rangeValueMin
	 *            字段的范围最小值
	 * @param rangeValueMax
	 *            字段范围最大值
	 * @param includeMin
	 *            是否包含最小值
	 * @param includeMax
	 *            是否包含最大值
	 * @param fieldConditionName
	 *            其他条件的字段名
	 * @param fieldConditionVal
	 *            其他条件的值
	 * @param start
	 *            从第几条记录开始查询
	 * @param limit
	 *            总共查询多少条记录
	 * @param orderField
	 * @param orderType
	 * @return
	 */
	public List<T> getByFieldRange(String rangeFieldName, Object rangeValueMin, Object rangeValueMax, boolean includeMin,
			boolean includeMax, String fieldConditionName, Object fieldConditionVal, int start, int limit, String orderField,
			String orderType)
			{
		String ocn = fieldConditionName;
		Object ocv = fieldConditionVal;
		if (StringUtils.isEmpty(ocn)) {
			ocn = "1";
			ocv = 1;
		}
		else {
			ocn = AbstractSQLProvider.underscoreName(fieldConditionName);
		}

		SQLFieldCondition fieldCondition = SQLFieldCondition.create(ocn, ocv);
		SQLFieldRangeCondition rangeCondition = SQLFieldRangeCondition.create(AbstractSQLProvider.underscoreName(rangeFieldName),
				rangeValueMin, rangeValueMax, includeMin, includeMax);

		SQLLimit sqlLimit = null;
		if (start >= 0 && limit > 0) {
			sqlLimit = SQLLimit.createSQLLimit(start, limit);
		}

		SQLOrder order = null;
		if (!StringUtils.isEmpty(orderField)) {
			order = SQLOrder.createOrder(orderField, orderType);
		}
		String sql = sqlProvider.provideQueryByFieldRangeSQL(fieldCondition, rangeCondition, sqlLimit, order);
		logger.debug(sql);
		return jdbcReader.executeQuery(sql, persistentClass, ocv, rangeValueMin, rangeValueMax);
			}

	public ISQLProvider getSqlProvider()
	{
		return sqlProvider;
	}

	/**
	 * 插入一条新纪录，如果autoIncrememntId为true，则id必须是<0的值，否则会认为该对象数据已经存在，无法做insert
	 *
	 * @param obj
	 * @param autoIncrememntId
	 *            id是否自增值
	 */
	public Long insert(T obj, boolean autoIncrememntId)
	{
		return doInsert(obj, autoIncrememntId, sqlProvider.provideInsertSQL());
	}

	/**
	 * 和{@link #insert(Object, boolean)}功能一样，不同的是对于主键或者唯一索引冲突的此方法会忽略，即insert into ignore
	 *
	 * @param obj
	 * @param autoIncrememntId
	 * @return
	 */
	public Long insertIgnore(T obj, boolean autoIncrememntId)
	{
		return doInsert(obj, autoIncrememntId, sqlProvider.provideInsertSQL().replace("insert into", "insert ignore into"));
	}

	public List<T> queryBySQL(String sql, Object... args)
	{
		return jdbcReader.executeQuery(sql, persistentClass, args);
	}

	/**
	 * 更新obj对象
	 *
	 * @param obj
	 */
	public void update(T obj)
	{
		Object[] args = new Object[sqlProvider.getFieldMap().size() + 1];
		int i = 0;
		try {
			for (Map.Entry<Field, String> entry : sqlProvider.getFieldMap().entrySet()) {
				args[i++] = entry.getKey().get(obj);
			}
			args[i++] = sqlProvider.getIdField().get(obj);
		}
		catch (IllegalArgumentException | IllegalAccessException e) {
			throw new BugError();
		}

		jdbcTemplate.update(sqlProvider.provideUpdateSQL(), args);
	}

	/**
	 * 插入一条新纪录，如果autoIncrememntId为true，则id必须是<0的值，否则会认为该对象数据已经存在，无法做insert
	 *
	 * @param obj
	 * @param autoIncrememntId
	 *            id是否自增值
	 */
	private Long doInsert(T obj, boolean autoIncrememntId, String insertSQL)
	{
		Assert.notNull(obj);
		Object idVal = null;
		try {
			idVal = sqlProvider.getIdField().get(obj);
		}
		catch (IllegalArgumentException | IllegalAccessException e2) {
			throw new BugError();
		}
		String sql = null;
		Object[] args = null;
		int i = 0;
		if (autoIncrememntId) {
			//id是自增类型
			if (idVal != null) {
				if (idVal instanceof Number) {
					Number idNum = (Number) idVal;
					if (idNum.longValue() > 0) {
						throw new IllegalStateException("对于id自增对象,做insert时候id必须小于等于0");
					}
				}
			}
			args = new Object[sqlProvider.getFieldMap().size()];
			sql = MessageFormat.format(/*sqlProvider.provideInsertSQL()*/insertSQL, "", "");
		}
		else {
			args = new Object[sqlProvider.getFieldMap().size() + 1];
			args[i++] = idVal;
			sql = MessageFormat.format(/*sqlProvider.provideInsertSQL()*/insertSQL, sqlProvider.getUnderscoreIdName() + ",",
					"?,");
		}

		for (Map.Entry<Field, String> entry : sqlProvider.getFieldMap().entrySet()) {
			try {
				args[i++] = entry.getKey().get(obj);
			}
			catch (IllegalArgumentException | IllegalAccessException e) {
				throw new BugError();
			}
		}
		logger.debug(sql);
		if (autoIncrememntId) {
			final String usql = sql;
			final ArgumentPreparedStatementSetter pss = new ArgumentPreparedStatementSetter(args);
			Long newId = jdbcTemplate.execute(new ConnectionCallback<Long>() {

				@Override
				public Long doInConnection(Connection con) throws SQLException, DataAccessException
				{
					PreparedStatement ps = con.prepareStatement(usql, Statement.RETURN_GENERATED_KEYS);
					pss.setValues(ps);
					ps.executeUpdate();
					ResultSet rs = ps.getGeneratedKeys();
					if (rs.next()) {
						return rs.getLong(1);
					}
					return -1L;
				}
			});

			if (newId > 0) {
				sqlProvider.getIdField().setAccessible(true);
				try {
					if (sqlProvider.getIdField().getType() == int.class || sqlProvider.getIdField().getType() == Integer.class) {
						sqlProvider.getIdField().set(obj, newId.intValue());
					}
					else {
						sqlProvider.getIdField().set(obj, newId);
					}
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
					logger.error("", e);
				}
			}

			return newId;
		}
		else {
			jdbcTemplate.update(sql, args);
			return (Long) idVal;
		}
	}

}
