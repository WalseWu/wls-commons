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
 * 通用数据库写任务，从queue中批量获取batchSize数据交给handler进行save到数据库
 *
 * @author Lisong
 * @param <T>
 */
public class CommonWriteDBTask<T> extends AbstractDataTask<String>
{

	private final static Logger logger = LoggerFactory.getLogger(CommonWriteDBTask.class);

	private final BlockingQueue<T> queue;

	private final int batchSize;

	private final AtomicLong count = new AtomicLong(0);

	private static final Lock beforeFinishLock = new ReentrantLock();

	List<T> list = new ArrayList<>();

	private final CommonTaskDataHandler<T>[] handlers;

	@SafeVarargs
	public CommonWriteDBTask(BlockingQueue<T> queue, int batchSize, CommonTaskDataHandler<T>... handlers)
	{
		this.queue = queue;
		this.batchSize = batchSize;
		this.handlers = handlers;
	}

	public void beforeRunningFinish()
	{
		logger.info("beforeRunningFinish...");
		logger.info("Processing remaining in batch array: {}", list.size());
		if (list.size() > 0) {
			try {
				doHandle(list);
			}
			catch (Exception e) {
				logger.error("Hander error!", e);
			}
			finally {
				list.clear();
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
						try {
							doHandle(tArr);
						}
						finally {
							tArr.clear();
						}
					}
				}
				doHandle(tArr);
				tArr.clear();
			}
			finally {
				queue.clear();
				beforeFinishLock.unlock();
			}
		}
		else {
			logger.info("Failed to get lock in beforeRunningFinish to handle the queue!!!!Over!!");
		}
	}

	@Override
	public String runInternal()
	{
		AtomicInteger recordCounter = new AtomicInteger(0);
		while (super.isRunning()) {
			if (recordCounter.compareAndSet(batchSize, 0)) {
				try {
					doHandle(list);
					//					logger.info("handle records {}", list.size());
				}
				finally {
					list.clear();
				}
			}
			T fromQueue = null;
			try {
				fromQueue = queue.poll(1200, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
			}
			if (fromQueue != null) {
				list.add(fromQueue);
				recordCounter.incrementAndGet();
			}
		}
		beforeRunningFinish();
		return super.getName() + " finished:" + count.get();
	}

	private void doHandle(List<T> list)
	{
		if (handlers == null || handlers.length == 0) {
			return;
		}
		for (CommonTaskDataHandler<T> handler : handlers) {
			try {
				handler.handle(list);
			}
			catch (Exception e) {
				logger.error("", e);
			}
		}
	}

}
