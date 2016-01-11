package common.rds.dbutils;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * @author wls
 */
public interface JdbcOperator
{

	public Map<String, Object> executeQueryRow(String sql, Object... params);

	public <T> T executeQueryObject(String sql, Class<T> clazz, Object... params);

	/**
	 * 单列数据查询 Creates a new instance of ColumnListHandler. The first column of each row will be returned from handle().
	 * 
	 * @param sql
	 *            SQL查询语句
	 * @param params
	 *            SQL参数
	 * @return List<Object>
	 */
	public <T> List<T> columnQuery(String sql, Object... params);

	/**
	 * 执行SQL查询，列数据以Map形式返回
	 * 
	 * @param sql
	 *            SQL查询语句
	 * @param params
	 *            SQL参数
	 * @return List<Map<String,Object>>
	 */
	public List<Map<String, Object>> executeQuery(String sql, Object... params);

	public <T> T columnQuerySingle(String sql, Object... params);

	public <T> List<T> executeQuery(String sql, Class<T> clazz);

	public <T> List<T> executeQuery(String sql, Class<T> clazz, Object... params);

	public <T> Queue<T> columnQuery2Queue(String sql);

	public <T> Queue<T> columnQuery2Queue(String sql, Object... params);

	public <T> T query(String sql, ResultSetHandler<T> rsh, Object... params);
}