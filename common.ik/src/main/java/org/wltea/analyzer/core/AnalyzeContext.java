/*
 * Copyright (C) 2015 MC
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package org.wltea.analyzer.core;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Dictionary;

/**
 * (non-Javadoc)
 *
 * @see IK AnalyzeContext @author wls
 */
public class AnalyzeContext
{
	//默认缓冲区大小
	private static final int BUFF_SIZE = 4096;
	//缓冲区耗尽的临界值
	private static final int BUFF_EXHAUST_CRITICAL = 100;

	//字符窜读取缓冲
	private char[] segmentBuff;
	//字符类型数组
	private int[] charTypes;

	//记录Reader内已分析的字串总长度
	//在分多段分析词元时，该变量累计当前的segmentBuff相对于reader起始位置的位移
	private int buffOffset;
	//当前缓冲区位置指针
	private int cursor;
	//最近一次读入的,可处理的字串长度
	private int available;

	//子分词器锁
	//该集合非空，说明有子分词器在占用segmentBuff
	private final Set<String> buffLocker;

	//原始分词结果集合，未经歧义处理
	private QuickSortSet orgLexemes;
	//LexemePath位置索引表
	private final Map<Integer, LexemePath> pathMap;
	//最终分词结果集
	private final LinkedList<Lexeme> results;

	//分词器配置项
	private final Configuration cfg;

	public AnalyzeContext(Configuration cfg)
	{
		this.cfg = cfg;
		segmentBuff = new char[BUFF_SIZE];
		charTypes = new int[BUFF_SIZE];
		buffLocker = new HashSet<String>();
		orgLexemes = new QuickSortSet();
		pathMap = new HashMap<Integer, LexemePath>();
		results = new LinkedList<Lexeme>();
	}

	/**
	 * 组合词元
	 */
	private void compound(Lexeme result)
	{
		if (!cfg.useSmart()) {
			return;
		}
		//数量词合并处理
		if (!results.isEmpty()) {

			if (Lexeme.TYPE_ARABIC == result.getLexemeType()) {
				Lexeme nextLexeme = results.peekFirst();
				boolean appendOk = false;
				if (Lexeme.TYPE_CNUM == nextLexeme.getLexemeType()) {
					//合并英文数词+中文数词
					appendOk = result.append(nextLexeme, Lexeme.TYPE_CNUM);
				}
				else if (Lexeme.TYPE_COUNT == nextLexeme.getLexemeType()) {
					//合并英文数词+中文量词
					appendOk = result.append(nextLexeme, Lexeme.TYPE_CQUAN);
				}
				if (appendOk) {
					//弹出
					results.pollFirst();
				}
			}

			//可能存在第二轮合并
			if (Lexeme.TYPE_CNUM == result.getLexemeType() && !results.isEmpty()) {
				Lexeme nextLexeme = results.peekFirst();
				boolean appendOk = false;
				if (Lexeme.TYPE_COUNT == nextLexeme.getLexemeType()) {
					//合并中文数词+中文量词
					appendOk = result.append(nextLexeme, Lexeme.TYPE_CQUAN);
				}
				if (appendOk) {
					//弹出
					results.pollFirst();
				}
			}

		}
	}

	/**
	 * 对CJK字符进行单字输出
	 *
	 * @param index
	 */
	//	private void outputSingleCJK(int index)
	//	{
	//		if (CharacterUtil.CHAR_CHINESE == charTypes[index]) {
	//			Lexeme singleCharLexeme = new Lexeme(buffOffset, index, 1, Lexeme.TYPE_CNCHAR);
	//			results.add(singleCharLexeme);
	//		}
	//		else if (CharacterUtil.CHAR_OTHER_CJK == charTypes[index]) {
	//			Lexeme singleCharLexeme = new Lexeme(buffOffset, index, 1, Lexeme.TYPE_OTHER_CJK);
	//			results.add(singleCharLexeme);
	//		}
	//	}

	/**
	 * 向分词结果集添加词元
	 *
	 * @param lexeme
	 */
	void addLexeme(Lexeme lexeme)
	{
		orgLexemes.addLexeme(lexeme);
	}

	/**
	 * 添加分词结果路径 路径起始位置 ---> 路径 映射表
	 *
	 * @param path
	 */
	void addLexemePath(LexemePath path)
	{
		if (path != null) {
			pathMap.put(path.getPathBegin(), path);
		}
	}

	/**
	 * 根据context的上下文情况，填充segmentBuff
	 *
	 * @param reader
	 * @return 返回待分析的（有效的）字串长度
	 * @throws IOException
	 */
	int fillBuffer(Reader reader) throws IOException
	{
		int readCount = 0;
		if (buffOffset == 0) {
			//首次读取reader
			readCount = reader.read(segmentBuff);
		}
		else {
			int offset = available - cursor;
			if (offset > 0) {
				//最近一次读取的>最近一次处理的，将未处理的字串拷贝到segmentBuff头部
				System.arraycopy(segmentBuff, cursor, segmentBuff, 0, offset);
				readCount = offset;
			}
			//继续读取reader ，以onceReadIn - onceAnalyzed为起始位置，继续填充segmentBuff剩余的部分
			readCount += reader.read(segmentBuff, offset, BUFF_SIZE - offset);
		}
		//记录最后一次从Reader中读入的可用字符长度
		available = readCount;
		//重置当前指针
		cursor = 0;
		return readCount;
	}

	int getBufferOffset()
	{
		return buffOffset;
	}

	char getCurrentChar()
	{
		return segmentBuff[cursor];
	}

	int getCurrentCharType()
	{
		return charTypes[cursor];
	}

	int getCursor()
	{
		return cursor;
	}

	//
	//	    void setCursor(int cursor){
	//	    	this.cursor = cursor;
	//	    }

	/**
	 * 返回lexeme 同时处理合并
	 *
	 * @return
	 */
	Lexeme getNextLexeme()
	{
		//从结果集取出，并移除第一个Lexme
		Lexeme result = results.pollFirst();
		while (result != null) {
			//数量词合并
			compound(result);
			if (Dictionary.getSingleton().isStopWord(segmentBuff, result.getBegin(), result.getLength())) {
				//是停止词继续取列表的下一个
				result = results.pollFirst();
			}
			else {
				//不是停止词, 生成lexeme的词元文本,输出
				result.setLexemeText(String.valueOf(segmentBuff, result.getBegin(), result.getLength()));
				break;
			}
		}
		return result;
	}

	/**
	 * 返回原始分词结果
	 *
	 * @return
	 */
	QuickSortSet getOrgLexemes()
	{
		return orgLexemes;
	}

	char[] getSegmentBuff()
	{
		return segmentBuff;
	}

	/**
	 * 初始化buff指针，处理第一个字符
	 */
	void initCursor()
	{
		cursor = 0;
		segmentBuff[cursor] = CharacterUtil.regularize(segmentBuff[cursor]);
		charTypes[cursor] = CharacterUtil.identifyCharType(segmentBuff[cursor]);
	}

	/**
	 * 判断当前segmentBuff是否已经用完 当前执针cursor移至segmentBuff末端this.available - 1
	 *
	 * @return
	 */
	boolean isBufferConsumed()
	{
		return cursor == available - 1;
	}

	/**
	 * 只要buffLocker中存在segmenterName 则buffer被锁定
	 *
	 * @return boolean 缓冲去是否被锁定
	 */
	boolean isBufferLocked()
	{
		return buffLocker.size() > 0;
	}

	/**
	 * 设置当前segmentBuff为锁定状态 加入占用segmentBuff的子分词器名称，表示占用segmentBuff
	 *
	 * @param segmenterName
	 */
	void lockBuffer(String segmenterName)
	{
		buffLocker.add(segmenterName);
	}

	/**
	 * 累计当前的segmentBuff相对于reader起始位置的位移
	 */
	void markBufferOffset()
	{
		buffOffset += cursor;
	}

	/**
	 * 指针+1 成功返回 true； 指针已经到了buff尾部，不能前进，返回false 并处理当前字符
	 */
	boolean moveCursor()
	{
		if (cursor < available - 1) {
			cursor++;
			segmentBuff[cursor] = CharacterUtil.regularize(segmentBuff[cursor]);
			charTypes[cursor] = CharacterUtil.identifyCharType(segmentBuff[cursor]);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 判断segmentBuff是否需要读取新数据 满足一下条件时， 1.available == BUFF_SIZE 表示buffer满载 2.buffIndex < available - 1 && buffIndex > available -
	 * BUFF_EXHAUST_CRITICAL表示当前指针处于临界区内 3.!context.isBufferLocked()表示没有segmenter在占用buffer 要中断当前循环（buffer要进行移位，并再读取数据的操作）
	 *
	 * @return
	 */
	boolean needRefillBuffer()
	{
		return available == BUFF_SIZE && cursor < available - 1 && cursor > available - BUFF_EXHAUST_CRITICAL
				&& !isBufferLocked();
	}

	/**
	 * 推送分词结果到结果集合 1.从buff头部遍历到this.cursor已处理位置 2.将map中存在的分词结果推入results 3.将map中不存在的CJDK字符以单字方式推入results
	 */
	void outputToResult()
	{
		int index = 0;
		for (; index <= cursor;) {
			//跳过非CJK字符
			if (CharacterUtil.CHAR_USELESS == charTypes[index]) {
				index++;
				continue;
			}
			//从pathMap找出对应index位置的LexemePath
			LexemePath path = pathMap.get(index);
			if (path != null) {
				//输出LexemePath中的lexeme到results集合
				Lexeme l = path.pollFirst();
				while (l != null) {
					results.add(l);
					//将index移至lexeme后
					index = l.getBegin() + l.getLength();
					l = path.pollFirst();
					if (l != null) {
						//输出path内部，词元间遗漏的单字
						for (; index < l.getBegin(); index++) {
							//							outputSingleCJK(index);
						}
					}
				}
			}
			else {//pathMap中找不到index对应的LexemePath
				//单字输出
				//				outputSingleCJK(index);
				index++;
			}
		}
		//清空当前的Map
		pathMap.clear();
	}

	/**
	 * 重置分词上下文状态
	 */
	void reset()
	{
		buffLocker.clear();
		orgLexemes = new QuickSortSet();
		available = 0;
		buffOffset = 0;
		charTypes = new int[BUFF_SIZE];
		cursor = 0;
		results.clear();
		segmentBuff = new char[BUFF_SIZE];
		pathMap.clear();
	}

	/**
	 * 移除指定的子分词器名，释放对segmentBuff的占用
	 *
	 * @param segmenterName
	 */
	void unlockBuffer(String segmenterName)
	{
		buffLocker.remove(segmenterName);
	}
}
