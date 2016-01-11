package common.concurrent.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IDataTask运行结束回调接口，可提供打印log等功能
 *
 * @author Lisong
 */
public interface IDataTaskCompleteCallback
{

	public static final Logger logger = LoggerFactory.getLogger(IDataTaskCompleteCallback.class);

	public static IDataTaskCompleteCallback loggerCallback = new IDataTaskCompleteCallback() {
		@Override
		public void execute(IDataTask<?>... tasks)
		{
			boolean success = true;
			for (IDataTask<?> tk : tasks) {
				if (tk != null) {
					logger.info("{},{},{}", new Object[] { tk.getResult().getDesc(), tk.getResult().isSuccess(),
							tk.getResult().toString() });
					success = success && tk.getResult().isSuccess();
				}
			}
			logger.info("finally: {}", success);
		}
	};

	public void execute(IDataTask<?>... tasks);
}
