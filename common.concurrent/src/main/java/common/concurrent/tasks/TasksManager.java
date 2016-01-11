package common.concurrent.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IDataTask调度管理器，一个管理器可调度管理一组完成共同任务的IDataTask
 * 
 * @author Lisong
 */
@SuppressWarnings("rawtypes")
public class TasksManager
{

	static class TaskThreadFactory implements ThreadFactory
	{
		static final AtomicInteger poolNumber = new AtomicInteger(1);
		final ThreadGroup group;
		final AtomicInteger threadNumber = new AtomicInteger(1);
		final String namePrefix;

		TaskThreadFactory(String name)
		{
			SecurityManager s = System.getSecurityManager();
			group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "pool-" + name + "-" + poolNumber.getAndIncrement() + "-thread-";
		}

		@Override
		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			if (t.isDaemon()) {
				t.setDaemon(false);
			}
			if (t.getPriority() != Thread.NORM_PRIORITY) {
				t.setPriority(Thread.NORM_PRIORITY);
			}
			return t;
		}
	}

	private final ConcurrentHashMap<IDataTask, Future> taskMap = new ConcurrentHashMap<IDataTask, Future>();

	private static int nThreads = Runtime.getRuntime().availableProcessors() + 1;

	private static final Logger logger = LoggerFactory.getLogger(TasksManager.class);

	private AbstractExecutorService executors;

	private final static int REJECT = 2;

	private final static int ACCEPT = 1;

	private volatile int state = ACCEPT;

	private final String name;

	private final Lock mainLock = new ReentrantLock();

	public TasksManager(String name, IDataTaskCompleteCallback... callbacks)
	{
		this(name, nThreads, nThreads, new LinkedBlockingQueue<Runnable>(), callbacks);
	}

	public TasksManager(final String name, int coreThreadNum, int maxThreadNum, BlockingQueue<Runnable> queue,
			final IDataTaskCompleteCallback... callbacks)
	{
		this.name = name;
		executors = new ThreadPoolExecutor(coreThreadNum, maxThreadNum, 10L, TimeUnit.MINUTES, queue, new TaskThreadFactory(name)) {
			@Override
			protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable)
			{
				return new FutureTask<T>(callable) {
					@Override
					protected void done()
					{
						taskMap.remove(callable);
						super.done();
						if (callbacks == null || callbacks.length == 0) {
							return;
						}
						if (callable instanceof IDataTask) {
							for (IDataTaskCompleteCallback callback : callbacks) {
								callback.execute((IDataTask) callable);
							}
						}
					}
				};
			}

			@Override
			protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, T value)
			{
				return new FutureTask<T>(runnable, value) {
					@Override
					protected void done()
					{
						taskMap.remove(runnable);
						super.done();
						if (callbacks == null || callbacks.length == 0) {
							return;
						}
						if (runnable instanceof IDataTask) {
							for (IDataTaskCompleteCallback callback : callbacks) {
								callback.execute((IDataTask) runnable);
							}
						}
					}
				};
			}
		};
	}

	public TasksManager(String name, int coreThreadNum, int maxThreadNum, IDataTaskCompleteCallback... callbacks)
	{
		this(name, coreThreadNum, maxThreadNum, new LinkedBlockingQueue<Runnable>(), callbacks);
	}

	public boolean couldSchedule()
	{
		mainLock.lock();
		try {
			return state == ACCEPT;
		}
		finally {
			mainLock.unlock();
		}
	}

	public void go2AcceptStatus()
	{
		mainLock.lock();
		try {
			state = ACCEPT;
		}
		finally {
			mainLock.unlock();
		}
	}

	public void go2RejectStatus()
	{
		mainLock.lock();
		try {
			state = REJECT;
		}
		finally {
			mainLock.unlock();
		}
	}

	public boolean isScheduled(IDataTask task)
	{
		return taskMap.contains(task);
	}

	public void schedule(AbstractDataTask<?> task)
	{
		if (state == REJECT) {
			logger.warn("TasksManager is in REJECT STATUS!!");
			return;
		}
		if (task.tryScheduleLock()) {
			try {
				if (taskMap.get(task) != null) {
					logger.warn("{} is running, could not schedule again!", task);
				}
				else {
					task.start();
					Future<?> f = executors.submit(task);
					taskMap.put(task, f);
				}
			}
			finally {
				task.unScheduleLock();
			}
		}
	}

	public void shutdown()
	{
		executors.shutdown();
	}

	public void stopAllTasks()
	{
		logger.info("{} stop all tasks.", name);
		Set<IDataTask> taskSet = taskMap.keySet();
		List<Future> flist = new ArrayList<Future>();
		for (IDataTask task : taskSet) {
			try {
				task.selfStop();
				Future f = taskMap.remove(task);
				if (f != null) {
					flist.add(f);
				}
			}
			catch (Exception e) {
				logger.error("", e);
			}
		}
		for (Future f : flist) {
			if (f != null) {
				try {
					f.get();
				}
				catch (InterruptedException e) {
				}
				catch (ExecutionException e) {
					logger.error("", e);
				}
			}
		}
		logger.info("{} all tasks stopped.", name);
	}

	public boolean unSchedule(IDataTask task)
	{
		boolean result = false;
		try {
			Future f = taskMap.remove(task);
			if (f != null) {
				result = true;
				f.cancel(true);
			}
		}
		catch (Exception e) {
			logger.error("", e);
		}
		return result;
	}

	public void waitAllComplete()
	{
		logger.info("{} wait all tasks to complete.", name);
		Set<IDataTask> taskSet = taskMap.keySet();
		try {
			for (IDataTask t : taskSet) {
				try {
					logger.info("{} wait to complete 259.", t.getName());
					Future<?> f = taskMap.get(t);
					if (f != null) {
						logger.info("{} wait to complete 262.", t.getName());
						f.get();
						logger.info("{} complete 264.", t.getName());
					}
					else {
						logger.info("{} is null 266.", t.getName());
					}
				}
				catch (Exception e) {
					logger.error("Error while waitAllComplete", e);
				}
			}
		}
		catch (Exception e) {
			logger.error("waitAllComplete", e);
		}

		logger.info("{} all tasks completed.", name);
	}
}
