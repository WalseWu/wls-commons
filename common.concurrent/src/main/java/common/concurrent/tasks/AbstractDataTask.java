package common.concurrent.tasks;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wls
 * @param <T>
 */
public abstract class AbstractDataTask<T> implements IDataTask<T>
{

	private final Lock lock = new ReentrantLock();

	private volatile AtomicBoolean stop = new AtomicBoolean(true);

	private final DataTaskResult<T> result = new DataTaskResult<T>();

	private String name = getClass().getSimpleName();

	protected static final Logger logger = LoggerFactory.getLogger(AbstractDataTask.class);

	public void afterRun()
	{
		logger.info("Task '{}' after run!", getName());
	}

	public void beforeRun()
	{
		logger.info("Task '{}' before run!", getName());
	}

	@Override
	public T call()
	{
		T obj = null;
		try {
			beforeRun();
			logger.info("Task '{}' starting!", getName());
			try {
				obj = runInternal();
				result.setResult(obj);
				result.setSuccess(true);
				result.setDesc(Thread.currentThread().getName() + "-" + getName());
			}
			catch (Exception e) {
				result.setError(e);
				callExceptionHandler(e);
			}
		}
		finally {
			try {
				afterRun();
			}
			catch (Exception e) {
				callExceptionHandler(e);
			}
		}
		return obj;
	}

	public void callExceptionHandler(Exception e)
	{
		if (e instanceof SQLException) {
			SQLException t = (SQLException) e;
			String message = t.getMessage();
			int errorCode = t.getErrorCode();
			String sqlState = t.getSQLState();
			logger.error("SQLException({}): errorCode='{}', state='{}', message='{}'", new Object[] { getName(), errorCode,
					sqlState, message });
		}
		else if (e.getCause() instanceof SQLException) {
			SQLException t = (SQLException) e.getCause();
			String message = t.getMessage();
			int errorCode = t.getErrorCode();
			String sqlState = t.getSQLState();
			logger.error("SQLException({}): errorCode='{}', state='{}', message='{}'", new Object[] { getName(), errorCode,
					sqlState, message });
		}
		else {
			logger.error("Exception({}):", getName(), e);
		}
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public DataTaskResult<T> getResult()
	{
		return result;
	}

	@Override
	public boolean isRunning()
	{
		return !stop.get();
	}

	public abstract T runInternal();

	@Override
	public void selfStop()
	{
		stop.set(true);
		logger.info("Task '{}' stop!", getName());
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public void start()
	{
		stop.set(false);
	}

	/**
	 * Acquires the lock only if it is free at the time of invocation. Acquires the lock if it is available and returns
	 * immediately with the value true. If the lock is not available then this method will return immediately with the value
	 * false.
	 *
	 * @return true if could get the lock
	 */
	protected boolean tryScheduleLock()
	{
		return lock.tryLock();
	}

	protected void unScheduleLock()
	{
		lock.unlock();
	}

}
