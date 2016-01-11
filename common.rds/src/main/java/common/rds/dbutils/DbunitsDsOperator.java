package common.rds.dbutils;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import common.rds.exp.SQLRuntimeException;

/**
 * 不支持事务，只提供读取功能
 * 
 * @author wls
 */
public class DbunitsDsOperator implements JdbcOperator
{

	Logger logger = LoggerFactory.getLogger(DbunitsDsOperator.class);

	private QueryRunner qr;

	public void setDataSource(DataSource dataSource)
	{
		this.qr = new QueryRunner(dataSource);
	}

	public DbunitsDsOperator()
	{
		super();
	}

	public DbunitsDsOperator(DataSource ds)
	{
		super();
		setDataSource(ds);
	}

	@Override
	public <T> List<T> columnQuery(String sql, Object... params)
	{
		List<T> rst = Collections.emptyList();

		try {
			rst = qr.query(sql, new ColumnListHandler<T>(), params);
		}
		catch (SQLException se) {
			logger.error("", se);
			throw new SQLRuntimeException(se);
		}

		return rst;
	}

	@Override
	public <T> T columnQuerySingle(String sql, Object... params)
	{
		List<T> rst = columnQuery(sql, params);
		if (!rst.isEmpty()) {
			return rst.get(0);
		}
		else {
			return null;
		}
	}

	@Override
	public List<Map<String, Object>> executeQuery(String sql, Object... params)
	{
		List<Map<String, Object>> rst = Collections.emptyList();

		try {
			rst = qr.query(sql, new MapListHandler(), params);
		}
		catch (SQLException se) {
			logger.error("", se);
			throw new SQLRuntimeException(se);
		}

		return rst;
	}

	@Override
	public <T> List<T> executeQuery(String sql, Class<T> clazz)
	{

		List<T> rst = Collections.emptyList();

		try {
			rst = qr.query(sql, new BeanListHandler<T>(clazz));
		}
		catch (SQLException se) {
			logger.error("", se);
			throw new SQLRuntimeException(se);
		}

		return rst;
	}

	@Override
	public <T> List<T> executeQuery(String sql, Class<T> clazz, Object... params)
	{

		List<T> rst = Collections.emptyList();

		try {
			rst = qr.query(sql, new BeanListHandler<T>(clazz), params);
		}
		catch (SQLException se) {
			logger.error("", se);
			throw new SQLRuntimeException(se);
		}

		return rst;
	}

	@Override
	public <T> Queue<T> columnQuery2Queue(String sql)
	{

		Queue<T> rst = new LinkedList<T>();

		try {
			rst = qr.query(sql, new ColumnQueueHandler<T>());
		}
		catch (SQLException se) {
			logger.error("", se);
			throw new SQLRuntimeException(se);
		}

		return rst;
	}

	@Override
	public <T> Queue<T> columnQuery2Queue(String sql, Object... params)
	{

		Queue<T> rst = new LinkedList<T>();

		try {
			rst = qr.query(sql, new ColumnQueueHandler<T>(), params);
		}
		catch (SQLException se) {
			logger.error("", se);
			throw new SQLRuntimeException(se);
		}

		return rst;
	}

	@Override
	public Map<String, Object> executeQueryRow(String sql, Object... params)
	{
		Map<String, Object> resMap = Collections.emptyMap();
		try {
			resMap = qr.query(sql, new MapHandler(), params);
		}
		catch (SQLException se) {
			logger.error("", se);
			throw new SQLRuntimeException(se);
		}
		return resMap;
	}

	@Override
	public <T> T executeQueryObject(String sql, Class<T> clazz, Object... params)
	{
		try {
			return qr.query(sql, new BeanHandler<T>(clazz), params);
		}
		catch (SQLException se) {
			logger.error("", se);
			throw new SQLRuntimeException(se);
		}
	}

	@Override
	public <T> T query(String sql, ResultSetHandler<T> rsh, Object... params)
	{
		T result = null;
		try {
			result = qr.query(sql, rsh, params);
		}
		catch (SQLException e) {
			logger.error("SQL Error:", e);
			throw new SQLRuntimeException(e);
		}
		return result;
	}

	//	public long insertAndReturnAutoGeneratedID(String sql, Object... params)
	//	{
	//		PreparedStatement ps = null;
	//		ResultSet rs = null;
	//		Connection conn = null;
	//		try {
	//			conn = qr.getDataSource().getConnection();
	//			ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
	//			qr.fillStatement(ps, params);
	//			ps.executeUpdate();
	//			rs = ps.getGeneratedKeys();
	//			return rs.next() ? rs.getLong(1) : -1;
	//		}
	//		catch (SQLException e) {
	//			logger.error("SQL Error:", e);
	//			throw new SQLRuntimeException(e);
	//		}
	//		finally {
	//			try {
	//				DbUtils.closeQuietly(rs);
	//				DbUtils.closeQuietly(ps);
	//			}
	//			finally {
	//				DbUtils.closeQuietly(conn);
	//			}
	//		}
	//	}
}