package common.rds.dao;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import common.rds.dbutils.JdbcOperator;
import common.rds.sql.IRDSQuery;

/**
 * @author wls
 */
public abstract class AbstractDao
{

	public static String toSQLClauseStr(List<IRDSQuery> qs)
	{
		if (qs == null || qs.size() == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer("where ");
		for (IRDSQuery q : qs) {
			String cl = q.toQueryString();
			if (cl != null && !"".equals(cl)) {
				sb.append(cl).append(" and ");
			}
		}

		return sb.substring(0, sb.lastIndexOf(" and "));
	}

	@Autowired
	private JdbcRepository jdbcRepository;

	protected String dataSource = "dataSource";

	protected JdbcOperator jdbcReader;

	protected JdbcTemplate jdbcTemplate;

	/**
	 * 生成一个 SQL的in语句， 比如(?,?,?)
	 *
	 * @param num
	 * @return
	 */
	public String generateInClause(int num)
	{
		if (num <= 0) {
			return "";
		}
		StringBuffer s = new StringBuffer("(");
		for (int i = 0; i < num - 1; i++) {
			s.append("?,");
		}
		s.append("?)");
		return s.toString();
	}

	public String getDataSource()
	{
		return dataSource;
	}

	public JdbcOperator getJdbcReader()
	{
		return jdbcReader;
	}

	public JdbcTemplate getJdbcTemplate()
	{
		return jdbcTemplate;
	}

	@PostConstruct
	public void init()
	{
		jdbcReader = jdbcRepository.getOperator(dataSource);
		Assert.notNull(jdbcReader);

		jdbcTemplate = jdbcRepository.getJdbcTemplate(dataSource);
		Assert.notNull(jdbcTemplate);
	}

	public <T> List<T> query(String sql, Class<T> clz, Object... args)
	{
		return jdbcReader.executeQuery(sql, clz, args);
	}

	public void setDataSource(String dataSource)
	{
		this.dataSource = dataSource;
	}
}
