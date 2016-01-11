/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 *
 *
 */
package org.wltea.analyzer.dic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wltea.analyzer.cfg.Configuration;

import common.spring.SpringContext;

/**
 * IK Dictionary patch，主要是为了支持动态加载字典词库，每天更新词库中180w的字典，IK中自带的27w字典屏蔽掉，并且能够动态丢弃 老字典切换到新字典
 */
public class Dictionary
{

	/**
	 * 获取词典单子实例
	 *
	 * @return Dictionary 单例对象
	 */
	public static Dictionary getSingleton()
	{
		if (singleton == null) {
			throw new IllegalStateException("词典尚未初始化，请先调用initial方法");
		}
		return singleton;
	}

	/**
	 * 词典初始化 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化 只有当Dictionary类被实际调用时，才会开始载入词典， 这将延长首次分词操作的时间 该方法提供了一个在应用加载阶段就初始化字典的手段
	 *
	 * @return Dictionary
	 */
	public static Dictionary initial(Configuration cfg)
	{
		if (singleton == null) {
			synchronized (Dictionary.class) {
				if (singleton == null) {
					singleton = new Dictionary(cfg);
					return singleton;
				}
			}
		}
		return singleton;
	}

	private final AtomicBoolean loaded = new AtomicBoolean(false);

	private final static Logger logger = LoggerFactory.getLogger(Dictionary.class);

	Callable<DictSegment> task = new Callable<DictSegment>() {
		@Override
		public DictSegment call()
		{
			DictSegment ds = null;
			ds = dictSupport.refreshDictSegment();
			return ds;
		}
	};

	private Future<DictSegment> f = null;

	/*
	 * 词典单子实例
	 */
	private static Dictionary singleton;
	/*
	 * 主词典对象
	 */
	private DictSegment _MainDict = new DictSegment((char) 0);

	/*
	 * 停止词词典
	 */
	private DictSegment _StopWordDict;

	/*
	 * 量词词典
	 */
	private DictSegment _QuantifierDict = new DictSegment((char) 0);

	private final IKDictionarySupport dictSupport;

	//	public void addWord(String word){
	//		singleton._MainDict.fillSegment(word.trim().toLowerCase().toCharArray());
	//	}

	/**
	 * 配置对象
	 */
	private final Configuration cfg;

	private Dictionary(Configuration cfg)
	{
		dictSupport = SpringContext.getBean(IKDictionarySupport.class);//new MainDictSupport();
		this.cfg = cfg;
		//		this.loadMainDict(); 屏蔽自带字典
		loadStopWordDict();
		//		loadQuantifierDict();
	}

	/**
	 * 批量加载新词条
	 *
	 * @param words
	 *            Collection<String>词条列表
	 */
	public void addWords(Collection<String> words)
	{
		if (words != null) {
			for (String word : words) {
				if (word != null) {
					//批量加载词条到主内存词典中
					singleton._MainDict.fillSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}

	/**
	 * 批量移除（屏蔽）词条
	 *
	 * @param words
	 */
	public void disableWords(Collection<String> words)
	{
		if (words != null) {
			for (String word : words) {
				if (word != null) {
					//批量屏蔽词条
					singleton._MainDict.disableSegment(word.trim().toLowerCase().toCharArray());
				}
			}
		}
	}

	/**
	 * 判断是否是停止词
	 *
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return boolean
	 */
	public boolean isStopWord(char[] charArray, int begin, int length)
	{
		return singleton._StopWordDict.match(charArray, begin, length).isMatch();
	}

	/**
	 * 检索匹配主词典
	 *
	 * @param charArray
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray)
	{
		return singleton._MainDict.match(charArray);
	}

	/**
	 * 检索匹配主词典
	 *
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInMainDict(char[] charArray, int begin, int length)
	{
		return singleton._MainDict.match(charArray, begin, length);
	}

	/**
	 * 检索匹配量词词典
	 *
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 匹配结果描述
	 */
	public Hit matchInQuantifierDict(char[] charArray, int begin, int length)
	{
		return singleton._QuantifierDict.match(charArray, begin, length);
	}

	/**
	 * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
	 *
	 * @param charArray
	 * @param currentIndex
	 * @param matchedHit
	 * @return Hit
	 */
	public Hit matchWithHit(char[] charArray, int currentIndex, Hit matchedHit)
	{
		DictSegment ds = matchedHit.getMatchedDictSegment();
		return ds.match(charArray, currentIndex, 1, matchedHit);
	}

	public void refreshDictSegment(boolean needBlock)
	{
		if (loaded.get()) {
			return;
		}
		if (f == null) {
			synchronized (task) {
				if (f == null) {
					ExecutorService es = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
							new LinkedBlockingQueue<Runnable>()) {
						@Override
						protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable)
						{
							return new FutureTask<T>(callable) {
								@Override
								public void done()
								{
									logger.info("Refreshing DS successful finished.");
									try {
										DictSegment ds = f.get();
										if (ds != null) {
											_MainDict = ds;
										}

									}
									catch (InterruptedException | ExecutionException e) {
										logger.error("加载词典失败!");
									}
									loaded.set(true);
								}

							};
						}

					};
					f = es.submit(task);
					es.shutdown();
				}
			}
		}
		if (needBlock && !loaded.get()) {
			try {
				if (f != null && !f.isDone()) {
					DictSegment ds = f.get();
					if (ds != null) {
						singleton._MainDict = ds;
					}
				}
			}
			catch (InterruptedException | ExecutionException e) {
			}
		}
	}

	/**
	 * 加载用户配置的扩展词典到主词库表
	 */
	private void loadExtDict()
	{
		//加载扩展词典配置
		List<String> extDictFiles = cfg.getExtDictionarys();
		if (extDictFiles != null) {
			InputStream is = null;
			for (String extDictName : extDictFiles) {
				//读取扩展词典文件
				logger.info("加载扩展词典：" + extDictName);
				is = this.getClass().getClassLoader().getResourceAsStream(extDictName);
				//如果找不到扩展的字典，则忽略
				if (is == null) {
					continue;
				}
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
					String theWord = null;
					do {
						theWord = br.readLine();
						if (theWord != null && !"".equals(theWord.trim())) {
							//加载扩展词典数据到主内存词典中
							//logger.info(theWord);
							_MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
						}
					} while (theWord != null);

				}
				catch (IOException ioe) {
					System.err.println("Extension Dictionary loading exception.");
					ioe.printStackTrace();

				}
				finally {
					try {
						if (is != null) {
							is.close();
							is = null;
						}
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * 加载主词典及扩展词典
	 */
	private void loadMainDict()
	{
		//建立一个主词典实例
		_MainDict = new DictSegment((char) 0);
		//读取主词典文件
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(cfg.getMainDictionary());
		if (is == null) {
			throw new RuntimeException("Main Dictionary not found!!!");
		}

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
			String theWord = null;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					_MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
				}
			} while (theWord != null);

		}
		catch (IOException ioe) {
			System.err.println("Main Dictionary loading exception.");
			ioe.printStackTrace();

		}
		finally {
			try {
				if (is != null) {
					is.close();
					is = null;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		//加载扩展词典
		loadExtDict();
	}

	/**
	 * 加载量词词典
	 */
	private void loadQuantifierDict()
	{
		//建立一个量词典实例
		_QuantifierDict = new DictSegment((char) 0);
		//读取量词词典文件
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(cfg.getQuantifierDicionary());
		if (is == null) {
			throw new RuntimeException("Quantifier Dictionary not found!!!");
		}
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
			String theWord = null;
			do {
				theWord = br.readLine();
				if (theWord != null && !"".equals(theWord.trim())) {
					_QuantifierDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
				}
			} while (theWord != null);

		}
		catch (IOException ioe) {
			System.err.println("Quantifier Dictionary loading exception.");
			ioe.printStackTrace();

		}
		finally {
			try {
				if (is != null) {
					is.close();
					is = null;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 加载用户扩展的停止词词典
	 */
	private void loadStopWordDict()
	{
		//建立一个主词典实例
		_StopWordDict = new DictSegment((char) 0);
		//加载扩展停止词典
		List<String> extStopWordDictFiles = cfg.getExtStopWordDictionarys();
		if (extStopWordDictFiles != null) {
			InputStream is = null;
			for (String extStopWordDictName : extStopWordDictFiles) {
				logger.info("加载扩展停止词典：" + extStopWordDictName);
				//读取扩展词典文件
				is = this.getClass().getClassLoader().getResourceAsStream(extStopWordDictName);
				//如果找不到扩展的字典，则忽略
				if (is == null) {
					continue;
				}
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"), 512);
					String theWord = null;
					do {
						theWord = br.readLine();
						if (theWord != null && !"".equals(theWord.trim())) {
							//logger.info(theWord);
							//加载扩展停止词典数据到内存中
							_StopWordDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
						}
					} while (theWord != null);

				}
				catch (IOException ioe) {
					System.err.println("Extension Stop word Dictionary loading exception.");
					ioe.printStackTrace();

				}
				finally {
					try {
						if (is != null) {
							is.close();
							is = null;
						}
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

}
