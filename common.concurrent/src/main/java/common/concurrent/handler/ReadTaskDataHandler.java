package common.concurrent.handler;

/**
 * @author wls
 * @param <T>
 */
public interface ReadTaskDataHandler<T>
{

	public T[] handle(T obj);
}
