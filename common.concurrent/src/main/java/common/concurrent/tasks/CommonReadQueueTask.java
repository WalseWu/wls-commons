package common.concurrent.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import common.concurrent.handler.CommonTaskDataHandler;

/**
 * 从指定的BlockingQueue中读取对象, 并根据设定的batchSize批量处理
 * 
 * @author Lisong
 */
public class CommonReadQueueTask<T> extends AbstractDataTask<String>
{

	private final static Logger logger = LoggerFactory.getLogger(CommonReadQueueTask.class);

	private final Class<T> genericType;

	private final BlockingQueue<T> queue;

	private final int batchSize;

	private final CommonTaskDataHandler<T> handler;

	private AtomicLong count = new AtomicLong(0);

	private static final Lock beforeFinishLock = new ReentrantLock();

	private List<T> batchArray;

	public CommonReadQueueTask(BlockingQueue<T> queue, int batchSize, CommonTaskDataHandler<T> handler, Class<T> genericClz)
	{
		this.queue = queue;
		this.batchSize = batchSize;
		this.genericType = genericClz;
		batchArray = new ArrayList<T>();
		this.handler = handler;
	}

	@Override
	public String runInternal()
	{
		AtomicInteger recordCounter = new AtomicInteger(0);
		while (super.isRunning()) {
			if (recordCounter.compareAndSet(batchSize, 0)) {
				logger.info("process {}: {}]", this.genericType.getName(), batchSize);
				try {
					handler.handle(batchArray);
				}
				catch (Exception e) {
					logger.error("", e);
				}
				finally {
					batchArray.clear();
				}
			}
			T t = null;
			try {
				t = queue.poll(1200, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
			}
			if (t != null) {
				batchArray.add(t);
				recordCounter.incrementAndGet();
			}
		}
		beforeRunningFinish();
		return super.getName() + " finished:" + count.get();
	}

	public void beforeRunningFinish()
	{
		logger.info("beforeRunningFinish...");
		logger.info("Processing remaining in batch array: {}", batchArray.size());
		if (batchArray.size() > 0) {
			try {
				handler.handle(batchArray);
			}
			catch (Exception e) {
				logger.error("", e);
			}
			finally {
				batchArray.clear();
			}
		}

		if (beforeFinishLock.tryLock()) {
			try {
				List<T> tArr = new ArrayList<T>();
				logger.info("Processing remaining in queue: {}", queue.size());
				for (T t : queue) {
					if (t != null) {
						tArr.add(t);
					}
					if (tArr.size() >= batchSize) {
						handler.handle(tArr);
						tArr.clear();
					}
				}
				handler.handle(tArr);
				tArr.clear();
			}
			finally {
				queue.clear();
				beforeFinishLock.unlock();
			}
		}
		else {
			logger.info("Failed to get lock in beforeRunningFinish to handle the shared queue!!!!Over!!");
		}

	}
}
