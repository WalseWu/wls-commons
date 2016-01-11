package common.concurrent.tasks;

import java.util.concurrent.Callable;

/**
 * Callable task 接口
 * 
 * @author Lisong
 * @param <V>
 */
public interface IDataTask<V> extends Callable<V>
{

	public static class DataTaskResult<V>
	{
		private V result;
		private boolean success = false;
		private Exception error;
		private String desc = "Null";

		public V getResult()
		{
			return result;
		}

		public void setResult(V result)
		{
			this.result = result;
		}

		public boolean isSuccess()
		{
			return success;
		}

		public void setSuccess(boolean success)
		{
			this.success = success;
		}

		public Exception getError()
		{
			return error;
		}

		public void setError(Exception error)
		{
			this.error = error;
		}

		public String getDesc()
		{
			return desc;
		}

		public void setDesc(String desc)
		{
			this.desc = desc;
		}

		@Override
		public String toString()
		{
			return result != null ? result.toString() : "Null";
		}

	}

	public boolean isRunning();

	public void selfStop();

	public void start();

	public String getName();

	public DataTaskResult<V> getResult();
}
