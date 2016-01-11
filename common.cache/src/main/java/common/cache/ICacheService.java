package common.cache;

/**
 * @author wls
 */
public interface ICacheService<T extends ICacheable>
{
	/**
	 * 计算Key
	 *
	 * @param t
	 * @return
	 */
	String calculateKey(T t);

	/**
	 * @param key
	 * @return
	 */
	boolean deleteByKey(String key);

	/**
	 * 获取本缓存对象过期的时长 in毫秒
	 *
	 * @return
	 */
	long getExpireMillisecond();

	/**
	 * @param key
	 * @return
	 */
	boolean isCached(String key);

	/**
	 * @param key
	 *            读取的键
	 * @param resetExpireTime
	 *            是否重新设置过期时间
	 * @return
	 */
	T readObject(String key, boolean resetExpireTime);

	/**
	 * 将对象写入缓存
	 *
	 * @param t
	 * @return key
	 */
	String writeObject(T t);
}
