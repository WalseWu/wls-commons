package common.lucene.comp;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

@Component
public class IndexContext
{

	private class ContextVariable
	{
		Map<Object, Object> map = new HashMap<Object, Object>();

		public Object get(Object key)
		{
			return map.get(key);
		}

		public void set(Object key, Object value)
		{
			map.put(key, value);
		}
	}

	public static IndexContext getContext()
	{
		return context;
	}

	private final ThreadLocal<String> indexTable = new ThreadLocal<String>();

	private final ThreadLocal<ContextVariable> contextVariable = new ThreadLocal<ContextVariable>();

	private static IndexContext context;

	public Object get(Object key)
	{
		ContextVariable v = contextVariable.get();
		if (v != null) {
			return v.get(key);
		}
		return null;
	}

	public String getIndexTable()
	{
		return indexTable.get();
	}

	@PostConstruct
	public void init()
	{
		context = this;
	}

	public void set(Object key, Object value)
	{
		if (contextVariable.get() == null) {
			contextVariable.set(new ContextVariable());
		}
		contextVariable.get().set(key, value);
	}

	public void setIndexTable(String table)
	{
		indexTable.set(table);
	}
}
