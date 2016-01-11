package common.lucene.comp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.wltea.analyzer.cfg.DefaultConfig;
import org.wltea.analyzer.dic.Dictionary;

import com.google.common.io.Files;
import common.lucene.comp.exp.IndexNotSearchableException;
import common.lucene.comp.exp.IndexNotWritableException;
import common.rds.dao.JdbcRepository;
import common.spring.SpringContext;

/**
 * 搜索引擎抽象组件，对应{@link #indexStorePath},子类实例化之前务必调用{@link #init()}, 在系统关闭之前务必调用 {@link #destory()}方法. 该组件对索引库操作并发安全. ttt
 *
 * @author Lisong
 * @param <T>
 */
public abstract class AbstractIndexComponent<T extends Indexable> implements IDataIndexComponent<T>
{

	protected static final Logger logger = LoggerFactory.getLogger(AbstractIndexComponent.class);

	private volatile IndexSearcher searcher;

	protected volatile IndexWriter writer;

	private static final Lock loadDictSegmentLock = new ReentrantLock();

	@Value("${goods.index.path}")
	private String indexStorePath;

	private Directory indexDir;

	final Lock createIndexLock = new ReentrantLock();

	protected static final String DEFAULT_DATASOURCE = "dataSource";

	@Autowired
	protected JdbcRepository jdbcRepository;

	@Autowired
	protected SpringContext springContext;

	@Autowired
	protected IndexContext context;

	@Autowired
	private Analyzer analyzer;

	private final Method deserializeIndexableMethod;

	private final Constructor<Indexable> constructor;

	@SuppressWarnings("unchecked")
	public AbstractIndexComponent() throws NoSuchMethodException, SecurityException
	{
		ParameterizedType parameterizedType = (ParameterizedType) this.getClass().getGenericSuperclass();
		Class<Indexable> clz = (Class<Indexable>) parameterizedType.getActualTypeArguments()[0];
		deserializeIndexableMethod = clz.getDeclaredMethod(Indexable.DESERIALIZE_METHOD, Document.class);
		constructor = clz.getConstructor();
	}

	@Override
	public boolean buildIndexStore() throws Exception
	{
		if (createIndexLock.tryLock()) {
			try {
				File tmpFolder = Files.createTempDir();
				tmpFolder.deleteOnExit();
				Directory tempIndexDir = FSDirectory.open(tmpFolder);

				// 构建新索引库 到临时索引库
				IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_2, analyzer);
				iwc.setOpenMode(OpenMode.CREATE);
				IndexWriter newWriter = new IndexWriter(tempIndexDir, iwc);
				boolean r = false;

				closeWriter(writer);

				try {
					long st = System.currentTimeMillis();
					logger.info("Building Index({}) starting...", indexStorePath);
					r = buildIndexStore(newWriter);
					st = System.currentTimeMillis() - st;
					logger.info("Build Index({}) time: {}ms", indexStorePath, st);
				}
				finally {
					newWriter.commit();
					closeWriter(newWriter);
				}
				// searcher 切换到临时索引库
				IndexReader newReader = DirectoryReader.open(tempIndexDir);
				IndexSearcher tmpR = searcher;
				searcher = new IndexSearcher(newReader);
				// 关闭原索引库的reader，writer
				closeSearcher(tmpR);
				int maxDoc = searcher.getIndexReader().maxDoc();
				logger.info("{} total docs: {}", indexStorePath, maxDoc);
				// 删除原索引库，用新的覆盖之
				FileUtils.delDir(indexStorePath, false);
				try {
					String lockId = indexDir.getLockID();
					indexDir.clearLock(lockId);
				}
				finally {
					indexDir.close();
				}
				FileUtils.delDir(indexStorePath, false);
				FileUtils.copyDirectiory(tmpFolder.getCanonicalPath(), indexStorePath);
				// Searcher切换回新索引库
				indexDir = FSDirectory.open(new File(indexStorePath));
				tmpR = searcher;
				newReader = DirectoryReader.open(indexDir);
				searcher = new IndexSearcher(newReader);
				closeSearcher(tmpR);

				try {
					String lockId = tempIndexDir.getLockID();
					tempIndexDir.clearLock(lockId);
				}
				finally {
					tempIndexDir.close();
				}
				FileUtils.delDir(tmpFolder.getCanonicalPath(), true);
				writer = newIndexWriter();
				return r;
			}
			catch (Exception e) {
				logger.error("BuildIndexStore error.", e);
				throw e;
			}
			finally {
				createIndexLock.unlock();
				afterBuildIndexStore();
			}
		}
		else {
			logger.error("Get create index lock fail, which means some other thread is updating the index store.");
			return false;
		}
	}

	@Override
	public boolean createObjectIndex(T obj, boolean commitAtOnce) throws IndexNotWritableException
	{
		return createObjectIndex(obj, this.writer, commitAtOnce);
	}

	public boolean createObjectIndex(T obj, IndexWriter writer, boolean commitAtOnce) throws IndexNotWritableException
	{
		boolean r = false;
		if (createIndexLock.tryLock()) {
			try {
				Document doc = obj.serialize();
				writer.addDocument(doc);
				if (commitAtOnce) {
					writer.commit();
				}
			}
			catch (IOException e) {
				logger.error("Exception", e);
				return false;
			}
			finally {
				createIndexLock.unlock();
			}
		}
		else {
			logger.error("Get create index lock fail, which means some other thread is updating the index store.");
		}
		return r;
	}

	@PreDestroy
	public void destory() throws IOException
	{
		logger.info("{} - {} Destorying...", getClass().getSimpleName(), getIndexStorePath());
		closeWriter(writer);
		logger.info("{} - {} writer closed", getClass().getSimpleName(), getIndexStorePath());
		closeSearcher(searcher);
		logger.info("{} - {} reader closed", getClass().getSimpleName(), getIndexStorePath());
	}

	/**
	 * @return the analyzer
	 */
	@Override
	public Analyzer getAnalyzer()
	{
		return analyzer;
	}

	public String getIndexStorePath()
	{
		return indexStorePath;
	}

	@Override
	public IndexSearcher getSearcher()
	{
		if (searcher == null) {
			throw new IndexNotSearchableException("IndexSearcher not inited, this maybe the index '" + indexDir
					+ "' not created yet.");
		}
		return searcher;
	}

	@PostConstruct
	public void init() throws CorruptIndexException, LockObtainFailedException, IOException
	{
		Assert.notNull(indexStorePath);
		logger.info("index path:{}", getIndexStorePath());
		indexDir = FileUtils.openLuceneDirectory(indexStorePath);
		loadDictionary();
		try {
			IndexReader reader = DirectoryReader.open(indexDir);
			writer = newIndexWriter();
			searcher = new IndexSearcher(reader);
		}
		catch (IndexNotFoundException e) {
			logger.error("没有找到索引库，可能是索引库还没有初始化好,{}", getIndexStorePath());
		}
		catch (FileNotFoundException e) {
			logger.error("没有找到索引文件，可能是索引库还没有初始化好,{}", getIndexStorePath());
		}
	}

	public boolean isRebuilding()
	{
		if (createIndexLock.tryLock()) {
			try {
				return false;
			}
			finally {
				createIndexLock.unlock();
			}
		}
		return true;
	}

	@Override
	public SearchResult<T> search(Query query, int topN, boolean returnScoreInfo)
	{
		return search(query, null, topN, returnScoreInfo);
	}

	@Override
	public SearchResult<T> search(Query query, Sort sort, int topN, boolean returnScoreInfo)
	{

		return null;
	}

	/* (non-Javadoc)
	 * @see com.chachazhan.qian.index.comp.IDataIndexComponent#search(org.apache.lucene.search.Query, int, int, boolean)
	 */
	@Override
	public SearchResult<T> search(Query query, Sort sort, int start, int limit, boolean returnScoreInfo)
	{
		int max = start + limit;
		SearchResult<T> rt = new SearchResult<>();
		try {
			TopDocs tds = sort == null ? searcher.search(query, max) : searcher.search(query, max, sort);
			rt.setTotalNum(tds.totalHits);
			ScoreDoc[] sds = tds.scoreDocs;
			int len = sds.length;
			if (start >= len) {
				return rt;
			}
			for (int i = start; i < max; i++) {
				if (i >= len) {
					break;
				}
				ScoreDoc sd = sds[i];
				Document doc = searcher.doc(sd.doc);
				T t = (T) constructor.newInstance();
				deserializeIndexableMethod.invoke(t, doc);
				rt.add(t);
			}

		}
		catch (IOException e) {
			logger.error("Oops, Search Error: ", e);
		}
		catch (Exception e) {
			logger.error("Oops, Search Error: ", e);
		}
		return rt;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T searchObject(Number id)
	{
		NumericRangeQuery<Long> query = NumericRangeQuery.newLongRange("id", id.longValue(), id.longValue(), true, true);
		try {
			TopDocs topDoc = searcher.search(query, 100);
			if (topDoc.totalHits == 0) {
				return null;
			}
			ScoreDoc docs[] = topDoc.scoreDocs;
			int len = docs.length;
			if (len > 0) {
				ScoreDoc sdoc = docs[0];
				Document doc = searcher.doc(sdoc.doc);
				Indexable rt = constructor.newInstance();
				return (T) deserializeIndexableMethod.invoke(rt, doc);
			}
			return null;
		}
		catch (IOException e) {
			logger.error("IOError.", e);
			return null;
		}
		catch (Exception e) {
			logger.error("Error.", e);
			return null;
		}
	}

	public void setIndexStorePath(String indexStorePath)
	{
		this.indexStorePath = indexStorePath;
		logger.info("Set index path: {}", indexStorePath);
	}

	public void setSearcher(IndexSearcher searcher)
	{
		this.searcher = searcher;
	}

	@Override
	public boolean swithIndexStorePath(String path) throws IOException
	{
		if (createIndexLock.tryLock()) {
			try {
				File f = new File(path);
				Directory tempIndexDir = FSDirectory.open(f);
				// searcher 切换到临时索引库
				IndexReader newReader = DirectoryReader.open(tempIndexDir);
				IndexSearcher tmpR = searcher;
				searcher = new IndexSearcher(newReader);

				// 关闭原索引库的reader，writer
				closeSearcher(tmpR);
				closeWriter(writer);
				// 删除原索引库，用新的覆盖之
				FileUtils.delDir(indexStorePath, false);
				try {
					String lockId = indexDir.getLockID();
					indexDir.clearLock(lockId);
				}
				catch (Exception e) {
					//ignore
				}
				finally {
					try {
						indexDir.close();
					}
					catch (Exception e) {
						//						ignore
					}
				}
				FileUtils.delDir(indexStorePath, false);
				FileUtils.copyDirectiory(f.getCanonicalPath(), indexStorePath);
				// Searcher切换回新索引库
				indexDir = FSDirectory.open(new File(indexStorePath));
				tmpR = searcher;
				newReader = DirectoryReader.open(indexDir);
				searcher = new IndexSearcher(newReader);
				closeSearcher(tmpR);
				writer = newIndexWriter();
			}
			catch (Exception e) {
				logger.error("Switch index error:{}", e);
				e.printStackTrace();
			}
			finally {
				createIndexLock.unlock();
			}

			return true;
		}
		return false;
	}

	private void closeSearcher(IndexSearcher isearcher) throws IOException
	{
		if (isearcher != null) {
			isearcher.getIndexReader().close();
		}
	}

	private void closeWriter(IndexWriter iwriter) throws IOException
	{
		if (iwriter != null) {
			iwriter.close();
		}
	}

	private void loadDictionary()
	{
		if (loadDictSegmentLock.tryLock()) {
			try {
				Dictionary dictionary = Dictionary.initial(DefaultConfig.getInstance());
				logger.info(dictionary.getClass().getName());
				dictionary.refreshDictSegment(false);
			}
			finally {
				loadDictSegmentLock.unlock();
			}
		}
	}

	protected void afterBuildIndexStore()
	{

	}

	/**
	 * 全量构建新索引,@see {@link IDataIndexComponent}{@link #buildIndexStore()}
	 *
	 * @param writer
	 * @return
	 */
	abstract protected boolean buildIndexStore(final IndexWriter writer);

	protected IndexWriter newIndexWriter() throws IOException
	{
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_2, analyzer);
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		return new IndexWriter(FileUtils.openLuceneDirectory(indexStorePath), iwc);
	}

	protected void reopenIndexSearcher() throws IOException
	{
		logger.info("reopen index store '{}'", indexStorePath);

		Directory tmpD = FileUtils.openLuceneDirectory(indexStorePath);

		IndexSearcher tmp = searcher;
		IndexReader newReader = DirectoryReader.open(tmpD);
		searcher = new IndexSearcher(newReader);
		closeSearcher(tmp);

		try {
			String lockId = indexDir.getLockID();
			indexDir.clearLock(lockId);
		}
		finally {
			indexDir.close();

			indexDir = tmpD;
		}
	}
}
