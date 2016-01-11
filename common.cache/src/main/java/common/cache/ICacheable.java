package common.cache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 可缓存进行序列化的对象
 *
 * @author wls
 * @param <T>
 */
public interface ICacheable
{

	/**
	 * 返回进入缓存的时间
	 *
	 * @return
	 */
	long getCachedTime();

	/**
	 * 返回ID
	 *
	 * @return
	 */
	long getId();

	/**
	 * 返回缓存对象的密码因子，一般在业务上独一无二
	 *
	 * @return
	 */
	String getKeyGene();

	/**
	 * 从in中读取T类型的对象并返回
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	<T extends ICacheable> void readStream(ObjectInput in) throws IOException, ClassNotFoundException;

	void setCachedTime(long time);

	/**
	 * 写入out中
	 *
	 * @param out
	 * @throws IOException
	 */
	void writeStream(ObjectOutput out) throws IOException;
}
