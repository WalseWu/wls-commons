package common.concurrent.tasks;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import common.concurrent.handler.ReadTaskDataHandler;
import common.rds.dbutils.JdbcOperator;

/**
 * {@link CommonReadDBTask} 的升级版，主要是解决CommonReadDBTask每个线程读取数据库记录不均匀的问题，该任务能够保证每个线程读取数据库记录数量是均匀的. 基于SQL limit实现分片读取
 *
 * @author Lisong
 */
public class CommonReadDBOnEverageTask<T> extends AbstractDataTask<String>
{

	/**
	 * 根据总的记录数和每个线程数分配任务，返回二维数据int[threadNumber][2] result，其中result[i][0]为第i个线程处理记录的开始，result[i][1]表示第i个线程需要处理的记录数量
	 *
	 * @param totalCount
	 * @param threadNumber
	 */
	public static long[][] calculateTasksAccordingTotalCountAndThreadNumber(long totalCount, int threadNumber)
	{
		long[][] tasks = new long[threadNumber][2];
		long readCountPerThread = totalCount / threadNumber;

		for (int i = 0; i < threadNumber - 1; i++) {
			tasks[i][0] = i * readCountPerThread;
			tasks[i][1] = readCountPerThread;
		}

		tasks[threadNumber - 1][0] = (threadNumber - 1) * readCountPerThread;
		tasks[threadNumber - 1][1] = totalCount - tasks[threadNumber - 1][0];

		return tasks;
	}

	public static final String QUERY_TOTAL_COUNT = "SELECT COUNT(*) FROM %TABLE_NAME%";

	private final static Logger logger = LoggerFactory.getLogger(CommonReadDBOnEverageTask.class);

	private final Class<T> genericType;

	private final JdbcOperator jdbcOperator;

	private final BlockingQueue<T> queue;

	private final AtomicLong count = new AtomicLong(0);
	private long batchSize = 2000;

	/**
	 * select **** from table_name limit ?,?
	 */
	private final String sql;

	protected final String tableName;

	/**
	 * 该任务需要读取的总记录数
	 */
	private final int readCount;

	/**
	 * 从哪个记录开始
	 */
	private final int limitStart;

	private final ReadTaskDataHandler<T> handler;

	public CommonReadDBOnEverageTask(JdbcOperator jdbcOperator, String sql, int readCount, int limitStart, int batchSize,
			BlockingQueue<T> queue, Class<T> genericType, String tableName, ReadTaskDataHandler<T> handler)
	{
		this.jdbcOperator = jdbcOperator;
		this.readCount = readCount;
		this.sql = sql;
		this.batchSize = batchSize;
		this.queue = queue;
		this.genericType = genericType;
		this.tableName = tableName;
		this.handler = handler;
		this.limitStart = limitStart;
		super.setName("(Read " + tableName + " from: " + limitStart + ", readCount:" + readCount + ")");
		logger.info(toString());
	}

	@Override
	public String runInternal()
	{
		long st = limitStart;
		long batchSizeUsed = Math.min(readCount - count.get(), batchSize);
		while (super.isRunning()) {
			@SuppressWarnings("unchecked")
			List<T> tlist = (List<T>) (genericType == String.class ? jdbcOperator.columnQuery(sql, st, batchSizeUsed)
					: jdbcOperator.executeQuery(sql, genericType, st, batchSizeUsed));
			for (T t : tlist) {
				try {
					if (t != null) {
						if (handler != null) {
							T[] ts = handler.handle(t);
							if (null != ts) {
								for (T tt : ts) {
									queue.put(tt);
								}
							}
						}
						else {
							queue.put(t);
						}
					}
				}
				catch (InterruptedException e) {
					//ignore
				}
				count.incrementAndGet();
			}
			st = st + tlist.size();
			batchSizeUsed = Math.min(readCount - count.get(), batchSize);
			logger.info("{} read {} record, and reach to ({},{}) row", new Object[] { getName(), tlist.size(), count.get(), st });
			if (count.get() >= readCount || tlist.isEmpty()) {
				break;
			}
			tlist.clear();
		}
		return getName() + " finished:" + count.get();
	}

	@Override
	public String toString()
	{
		return getName();
	}
}
