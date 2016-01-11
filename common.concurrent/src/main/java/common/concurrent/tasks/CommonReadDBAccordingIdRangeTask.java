package common.concurrent.tasks;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.Assert;

import common.concurrent.handler.ReadTaskDataHandler;
import common.rds.dbutils.JdbcOperator;

/**
 * @author Lisong
 * @param <T>
 */
public class CommonReadDBAccordingIdRangeTask<T> extends AbstractDataTask<String>
{

	/**
	 * 根据ID范围数和线程数分配每个线程的ID区间，返回二维数据int[threadNumber][2] result，其中result[i][0]为第i个线程处理的最小ID，result[i][1]表示第i个线程需要处理的最大ID
	 *
	 * @param minId
	 * @param maxId
	 * @param threadNum
	 */
	public static long[][] calculateTaskAccordingIdRangeAndThreadNumber(long minId, long maxId, int threadNum)
	{
		if (minId > maxId || threadNum < 1) {
			return null;
		}
		long[][] taskIdRanges = new long[threadNum][2];
		if (threadNum == 1) {
			taskIdRanges[0][0] = minId;
			taskIdRanges[0][1] = maxId;
			return taskIdRanges;
		}

		long count = (maxId - minId) / threadNum;

		if (count <= 10) {
			throw new IllegalStateException("Too Many thread num for the read tasks!!!");
		}

		for (int i = 0; i < threadNum - 1; i++) {
			taskIdRanges[i][0] = minId + i * count;
			taskIdRanges[i][1] = taskIdRanges[i][0] + count;
		}
		taskIdRanges[threadNum - 1][0] = taskIdRanges[threadNum - 2][1];
		taskIdRanges[threadNum - 1][1] = maxId;
		return taskIdRanges;
	}

	public static long queryMaxId(JdbcOperator jdbcOperator, String tableName)
	{
		Object min = jdbcOperator.columnQuerySingle(QUERY_MAX_ID.replace("#tablename", tableName));
		if (min instanceof Integer) {
			Integer minId = (Integer) min;
			return minId.longValue();
		}
		else if (min instanceof BigInteger) {
			return ((BigInteger) min).longValue();
		}
		return (Long) min;
	}

	public static long queryMinId(JdbcOperator jdbcOperator, String tableName)
	{
		Object min = jdbcOperator.columnQuerySingle(QUERY_MIN_ID.replace("#tablename", tableName));
		if (min instanceof Integer) {
			Integer minId = (Integer) min;
			return minId.longValue();
		}
		else if (min instanceof BigInteger) {
			return ((BigInteger) min).longValue();
		}
		return (Long) min;
	}

	public static final String QUERY_MIN_ID = "select MIN(id) from #tablename";

	public static final String QUERY_MAX_ID = "select MAX(id) from #tablename";

	/** 本Task读取的id空间 */
	private final long minId, maxId;

	private final String sql = "select #data from #tablename where id >= ? and id < ?";

	private final int batchSize;

	private final String usingSql;

	private final BlockingQueue<T> queue;

	private final Class<T> genericType;

	private final JdbcOperator jdbcOperator;

	private final ReadTaskDataHandler<T> handler;

	private final AtomicLong count = new AtomicLong(0);

	public CommonReadDBAccordingIdRangeTask(String tableName, String returnData, long minId, long maxId, int batchSize,
			JdbcOperator jdbcOperator, BlockingQueue<T> queue, Class<T> genericType, ReadTaskDataHandler<T> handler)
	{
		usingSql = sql.replace("#data", returnData).replace("#tablename", tableName);
		this.minId = minId;
		this.maxId = maxId;

		this.batchSize = batchSize;

		this.jdbcOperator = jdbcOperator;
		this.queue = queue;
		this.genericType = genericType;
		this.handler = handler;

		Assert.notNull(queue);
		Assert.notNull(jdbcOperator);

		Assert.isTrue(minId <= maxId);

		super.setName("Read " + tableName + " id:" + minId + "--" + maxId);
	}

	@Override
	public String runInternal()
	{
		long sid = minId;
		long eid = Math.min(minId + batchSize, maxId);
		while (super.isRunning() && sid < maxId && eid <= maxId) {
			@SuppressWarnings("unchecked")
			List<T> tlist = (List<T>) (genericType == String.class ? jdbcOperator.columnQuery(usingSql, sid, eid) : jdbcOperator
					.executeQuery(usingSql, genericType, sid, eid));
			for (T t : tlist) {
				try {
					if (t != null) {
						count.incrementAndGet();
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
			}

			sid = eid;
			eid = Math.min(eid + batchSize, maxId);
		}

		return getName() + " finished:" + count.get();
	}
}
