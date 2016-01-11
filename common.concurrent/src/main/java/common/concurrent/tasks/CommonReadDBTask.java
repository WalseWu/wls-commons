package common.concurrent.tasks;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import common.concurrent.handler.ReadTaskDataHandler;
import common.rds.dbutils.JdbcOperator;

/**
 * 普通读任务，根据指定SQL,字段和范围，从数据库读取数据之后放入队列
 *
 * @author Lisong
 */
public class CommonReadDBTask<T> extends AbstractDataTask<String>
{

	private final static Logger logger = LoggerFactory.getLogger(CommonReadDBTask.class);

	private final Class<T> genericType;

	private final JdbcOperator jdbcOperator;

	private final BlockingQueue<T> queue;

	private final String sql; // "select id as keywordId, word as word, pv,click,avg_price as avgPrice,competition,ctr from tuici_insight_1W20140510"; //读取数据表语句

	private final String countSQL; //读取数据总记录数

	private final String propertyName;// 指定的数据库表字段

	private long batchSize = 2000;

	private long totalCount = 0;

	private final AtomicLong count = new AtomicLong(0);

	private ReadTaskDataHandler<T> handler;

	public CommonReadDBTask(BlockingQueue<T> queue, JdbcOperator jdbcOperator, String sql, String countSQL, String propertyName,
			long min, long max, Class<T> genericClz)
	{
		this(queue, jdbcOperator, sql, countSQL, propertyName, min, max, genericClz, null, 20000);
	}

	public CommonReadDBTask(BlockingQueue<T> queue, JdbcOperator jdbcOperator, String sql, String countSQL, String propertyName,
			long min, long max, Class<T> genericClz, ReadTaskDataHandler<T> handler, int batchReadSize)
	{
		this.queue = queue;
		this.jdbcOperator = jdbcOperator;
		this.propertyName = propertyName;
		this.genericType = genericClz;

		StringBuffer sb = new StringBuffer(sql);
		sb.append(" order by ").append(this.propertyName).append(" desc limit ?,?");
		this.sql = sb.toString();
		logger.info("{}--{}, SQL: {}", new Object[] { min, max, this.sql });
		super.setName(this.getClass().getName() + "." + propertyName + ": " + min + " TO " + max);
		this.countSQL = countSQL;
		this.batchSize = batchReadSize;
		this.handler = handler;
	}

	@Override
	public void beforeRun()
	{
		super.beforeRun();
		totalCount = jdbcOperator.columnQuerySingle(countSQL);
		logger.info("Total Count: {}", totalCount);
	}

	@Override
	public String runInternal()
	{
		long st = 0;
		while (super.isRunning()) {
			List<T> tlist = jdbcOperator.executeQuery(sql, genericType, st, batchSize);
			logger.info("{}: {}-{} ==> {}", new Object[] { sql, st, batchSize, tlist.size() });
			for (T t : tlist) {
				try {
					if (t != null) {
						if (handler != null) {
							T[] ts = handler.handle(t);
							for (T tt : ts) {
								queue.put(tt);
								count.incrementAndGet();
							}
						}
						else {
							queue.put(t);
							count.incrementAndGet();
						}
					}
				}
				catch (InterruptedException e) {
					//ignore
				}
			}
			logger.info("read {} records and put into the queue.", tlist.size());
			st = st + tlist.size();
			tlist.clear();
			if (st >= totalCount) {
				break;
			}
		}
		return super.getName() + " finished:" + count.get();
	}

}
