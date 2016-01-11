package common.spring;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringContext implements ApplicationContextAware
{

	public static <T> T getBean(Class<T> requiredType)
	{
		return context.getBean(requiredType);
	}

	public static boolean isReady()
	{
		return context != null;
	}

	private static ApplicationContext context;

	public ApplicationContext getApplicationContext()
	{
		return context;
	}

	@SuppressWarnings("unchecked")
	public <T> T getBean(String name)
	{
		return (T) context.getBean(name);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> requiredType)
	{
		return context.getBeansOfType(requiredType);
	}

	@Override
	public void setApplicationContext(ApplicationContext acx)
	{
		synchronized (SpringContext.class) {
			context = acx;
		}
	}

}
