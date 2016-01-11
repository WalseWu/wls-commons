package common.lucene.comp;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import common.lucene.comp.exp.IndexNotWritableException;

/**
 * 搜索引擎组件，具备构建、更新、搜索、索引库，以及切换索引库目录的功能，一般一个索引库对应一个组件.
 *
 * @author Lisong
 * @param <T>
 */
public interface IDataIndexComponent<T extends Indexable>
{

	/**
	 * 创建索引库，创建完成之后原索引库会丢弃，一般在初始化启动时候需要做此操作
	 *
	 * @return
	 * @throws IndexNotWritableException
	 * @throws IOException
	 * @throws CorruptIndexException
	 * @throws Exception
	 */
	public boolean buildIndexStore() throws IndexNotWritableException, CorruptIndexException, IOException, Exception;

	/**
	 * 将对象创建到索引库
	 *
	 * @param obj
	 * @return
	 */
	public boolean createObjectIndex(T obj, boolean commitAtOnce) throws IndexNotWritableException;

	public Analyzer getAnalyzer();

	public IndexSearcher getSearcher();

	/**
	 * 根据提供的query搜索，返回topN个对象
	 *
	 * @param query
	 * @return
	 */
	public SearchResult<T> search(Query query, int topN, boolean returnScoreInfo);

	/**
	 * 搜索并根据sort排序
	 *
	 * @param query
	 * @param sort
	 * @param topN
	 * @return
	 */
	public SearchResult<T> search(Query query, Sort sort, int topN, boolean returnScoreInfo);

	/**
	 * 分页搜索接口
	 *
	 * @param query
	 * @param start
	 * @param limit
	 * @param returnScoreInfo
	 * @return
	 */
	public SearchResult<T> search(Query query, Sort sort, int start, int limit, boolean returnScoreInfo);

	/**
	 * 根据对象标识从索引库搜索
	 *
	 * @param id
	 *            对象标识
	 * @return id对应的对象，null如果索引库没有该对象信息，如果匹配到多个，则返回第一个
	 */
	public T searchObject(Number id);

	/**
	 * 切换索引库目录
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public boolean swithIndexStorePath(String path) throws IOException;
}
