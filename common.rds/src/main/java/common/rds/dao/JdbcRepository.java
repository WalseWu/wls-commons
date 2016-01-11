package common.rds.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import common.rds.dbutils.DbunitsDsOperator;
import common.rds.dbutils.JdbcOperator;
import common.spring.SpringContext;

/**
 * 数据基本组件获取, 如果需要获取支持事务的组件请使用{@link #getJdbcTemplate(String)}, 否则可以使用 {@link #getOperator(String)}
 *
 * @author wls
 */
public class JdbcRepository
{

	public static JdbcRepository getInstance()
	{
		return instance;
	}

	private final Map<String, DataSource> nameDsMap = new HashMap<>();

	private final Map<DataSource, String> dsNameMap = new HashMap<>();

	private final Map<String, JdbcOperator> dbutilsMap = new HashMap<String, JdbcOperator>();

	private final Map<String, JdbcTemplate> jdbcTemplateMap = new HashMap<>();

	protected final static Logger logger = LoggerFactory.getLogger(JdbcRepository.class);

	@Autowired
	private SpringContext context;

	private static JdbcRepository instance;

	public Collection<DataSource> getAllDataSource()
	{
		return nameDsMap.values();
	}

	public String getDataSourceBeanName(DataSource dataSource)
	{
		return dsNameMap.get(dataSource);
	}

	public JdbcTemplate getJdbcTemplate(String dataSource)
	{
		return jdbcTemplateMap.get(dataSource);
	}

	public JdbcOperator getOperator(DataSource dataSource)
	{
		return dbutilsMap.get(dataSource);
	}

	public JdbcOperator getOperator(String dataSource)
	{
		return dbutilsMap.get(dataSource);
	}

	@PostConstruct
	public void postConstruct()
	{
		Map<String, DataSource> dateSourceMap = context.getBeansOfType(DataSource.class);
		for (Map.Entry<String, DataSource> entry : dateSourceMap.entrySet()) {
			logger.info("Cache DataSource {} and DbunitsDsOperator on start!", entry.getKey());
			String dsBeanName = entry.getKey();
			DataSource dsBean = entry.getValue();
			nameDsMap.put(dsBeanName, dsBean);
			dsNameMap.put(dsBean, dsBeanName);
			dbutilsMap.put(dsBeanName, new DbunitsDsOperator(dsBean));

			JdbcTemplate jt = new JdbcTemplate(dsBean, false);
			jdbcTemplateMap.put(dsBeanName, jt);
		}
		instance = this;
	}
}
