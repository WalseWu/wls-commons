package common.concurrent.handler;

import java.util.List;

/**
 * @author wls
 * @param <T>
 */
public interface CommonTaskDataHandler<T>
{

	public void handle(List<T> list);
}
