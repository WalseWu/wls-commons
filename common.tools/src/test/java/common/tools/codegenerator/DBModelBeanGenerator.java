package common.tools.codegenerator;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import common.rds.dao.JdbcRepository;
import common.rds.sql.AbstractSQLProvider;

import freemarker.template.Template;

/**
 * @author wls
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/dzyh/config/spring-common.xml", "/dzyh/config/spring-rds-common.xml" })
public class DBModelBeanGenerator extends AbstractCodeGenerator
{
	@Autowired
	private JdbcRepository jr;

	private final String templateFile = "javabean.html";

	@Test
	public void generateCode() throws Exception
	{
		File targetDir = prepareTargetFolder("/beans");
		Collection<DataSource> dss = jr.getAllDataSource();
		for (DataSource ds : dss) {
			String dsName = jr.getDataSourceBeanName(ds);
			JdbcTemplate jt = jr.getJdbcTemplate(dsName);
			Set<String> tables = getTables(jt);
			for (String table : tables) {
				logger.info("start process table: {}.........", table);
				generateCodeForTable(table, targetDir, jt);
			}
		}
	}

	/**
	 * 获取表的每一列名字和对应java类型
	 *
	 * @param tableName
	 * @return
	 */
	public Map<String, String> getTableColumns(final String tableName, final JdbcTemplate jt)
	{
		return jt.execute(new StatementCallback<Map<String, String>>() {
			@Override
			public Map<String, String> doInStatement(Statement stmt) throws SQLException, DataAccessException
			{
				Map<String, String> colNames = new HashMap<>();
				ResultSetMetaData rsd = stmt.executeQuery("select * from " + tableName + " limit 1").getMetaData();
				for (int i = 0; i < rsd.getColumnCount(); i++) {
					int index = i + 1;
					String columnName = rsd.getColumnName(index), javaType = rsd.getColumnClassName(index), dbType = rsd
							.getColumnTypeName(index);
					int columnSize = rsd.getColumnDisplaySize(index);
					logger.info("{} - {} - {} - {}", columnName, javaType, dbType, columnSize);
					colNames.put(columnName.toLowerCase(), javaType);
				}
				return colNames;
			}
		});
	}

	/**
	 * 获取数据库中所有table的名字
	 *
	 * @return
	 */
	public Set<String> getTables(final JdbcTemplate jt)
	{
		return jt.execute(new ConnectionCallback<Set<String>>() {
			@Override
			public Set<String> doInConnection(Connection con) throws SQLException, DataAccessException
			{
				ResultSet rs = con.getMetaData().getTables(null, "%", "%", new String[] { "TABLE" });
				Set<String> tableNames = new HashSet<>();
				while (rs.next()) {
					String tableName = rs.getString("TABLE_NAME");
					tableNames.add(tableName);
				}
				return tableNames;
			}
		});
	}

	protected void generateCodeForTable(String table, File targetDir, final JdbcTemplate jt) throws Exception
	{
		Template t = getCodeTemplate(templatePath, templateFile);
		String className = AbstractSQLProvider.camelCaseName(table, true);
		Map<String, Object> data = generateTempalteData("beans", className);
		Map<String, String> columnAndType = getTableColumns(table, jt);

		List<Map<String, String>> pros = new ArrayList<>();
		for (Map.Entry<String, String> entry : columnAndType.entrySet()) {
			String name = entry.getKey();
			String type = "java.sql.Timestamp".equals(entry.getValue()) ? "java.util.Date" : Class.forName(entry.getValue())
					.getSimpleName();
			Map<String, String> pro = new HashMap<>();
			pro.put("proType", type);
			pro.put("proName", AbstractSQLProvider.camelCaseName(name, false));
			pros.add(pro);
		}
		data.put("properties", pros);
		codeToFile(targetDir, className, t, data);
	}
}
