package common.lucene.comp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class SearchResult<T> implements Serializable
{

	private static final long serialVersionUID = 1L;

	private Collection<T> objects;

	private int totalNum;

	public SearchResult()
	{
		objects = new ArrayList<T>();
	}

	public void add(T obj)
	{
		objects.add(obj);
	}

	public Collection<T> getObjects()
	{
		return objects;
	}

	public int getTotalNum()
	{
		return totalNum;
	}

	public void setObjects(Collection<T> objects)
	{
		this.objects = objects;
	}

	public void setTotalNum(int totalNum)
	{
		this.totalNum = totalNum;
	}
}
