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
import java.util.ArrayList;
import java.util.List;

import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.cfg.DefaultConfig;
import org.wltea.analyzer.dic.Dictionary;

/**
 * Compatible with IK 2012FF_hf1.
 *
 * @see also {@link IKSegmenter}
 * @author wls
 */
public final class KeywordSegmenter
{
	//字符窜reader
	private Reader input;
	//分词器配置项
	private final Configuration cfg;
	//分词器上下文
	private AnalyzeContext context;
	//分词处理器列表
	private List<ISegmenter> segmenters;
	//分词歧义裁决器
	private KeywordArbitrator arbitrator;

	private final boolean removeAllCoverTerms;

	/**
	 * IK分词器构造函数
	 *
	 * @param input
	 * @param useSmart
	 *            为true，使用智能分词策略 非智能分词：细粒度输出所有可能的切分结果 智能分词： 合并数词和量词，对分词结果进行歧义判断
	 */
	public KeywordSegmenter(Reader input, boolean useSmart, boolean removeAllCoverTerms)
	{
		this.input = input;
		this.removeAllCoverTerms = removeAllCoverTerms;
		cfg = DefaultConfig.getInstance();
		cfg.setUseSmart(useSmart);
		init();
	}

	/**
	 * IK分词器构造函数
	 *
	 * @param input
	 * @param cfg
	 *            使用自定义的Configuration构造分词器
	 */
	public KeywordSegmenter(Reader input, Configuration cfg)
	{
		this.input = input;
		this.cfg = cfg;
		removeAllCoverTerms = true;
		init();
	}

	/**
	 * 分词，获取下一个词元
	 *
	 * @return Lexeme 词元对象
	 * @throws IOException
	 */
	public synchronized Lexeme next() throws IOException
	{
		Lexeme l = null;
		while ((l = context.getNextLexeme()) == null) {
			/*
			 * 从reader中读取数据，填充buffer
			 * 如果reader是分次读入buffer的，那么buffer要  进行移位处理
			 * 移位处理上次读入的但未处理的数据
			 */
			int available = context.fillBuffer(input);
			if (available <= 0) {
				//reader已经读完
				context.reset();
				return null;

			}
			else {
				//初始化指针
				context.initCursor();
				int start = -1, end = -1;
				do {
					//连续的字母和数字不分 wls
					if (context.getCurrentCharType() == CharacterUtil.CHAR_ARABIC
							|| context.getCurrentCharType() == CharacterUtil.CHAR_ENGLISH) {
						for (ISegmenter segmenter : segmenters) {
							segmenter.reset();
						}
						if (start == -1) {
							start = context.getCursor();
							end = start;
						}
						else {
							end = context.getCursor();
						}
						continue;
					}
					else if (start != -1) {
						Lexeme newLexeme = new Lexeme(context.getBufferOffset(), start, end - start + 1, Lexeme.TYPE_LETTER);
						context.addLexeme(newLexeme);
						start = -1;
						end = -1;
					}

					//遍历子分词器
					for (ISegmenter segmenter : segmenters) {
						segmenter.analyze(context);
					}
					//字符缓冲区接近读完，需要读入新的字符
					if (context.needRefillBuffer()) {
						break;
					}
					//向前移动指针
				} while (context.moveCursor());

				//连续的字母和数字不分
				if (start != -1) {
					Lexeme newLexeme = new Lexeme(context.getBufferOffset(), start, end - start + 1, Lexeme.TYPE_LETTER);
					context.addLexeme(newLexeme);
					start = -1;
					end = -1;

				}

				//重置子分词器，为下轮循环进行初始化
				for (ISegmenter segmenter : segmenters) {
					segmenter.reset();
				}
			}
			//对分词进行歧义处理
			arbitrator.process(context, cfg.useSmart(), removeAllCoverTerms);
			//将分词结果输出到结果集，并处理未切分的单个CJK字符
			context.outputToResult();
			//记录本次分词的缓冲区位移
			context.markBufferOffset();
		}
		return l;
	}

	/**
	 * 重置分词器到初始状态
	 *
	 * @param input
	 */
	public synchronized void reset(Reader input)
	{
		this.input = input;
		context.reset();
		for (ISegmenter segmenter : segmenters) {
			segmenter.reset();
		}
	}

	/**
	 * 初始化
	 */
	private void init()
	{
		//初始化词典单例
		Dictionary.initial(cfg);
		//初始化分词上下文
		context = new AnalyzeContext(cfg);
		//加载子分词器
		segmenters = loadSegmenters();
		//加载歧义裁决器
		arbitrator = new KeywordArbitrator();
	}

	/**
	 * 初始化词典，加载子分词器实现
	 *
	 * @return List<ISegmenter>
	 */
	private List<ISegmenter> loadSegmenters()
	{
		List<ISegmenter> segmenters = new ArrayList<ISegmenter>(4);
		//处理字母的子分词器
		segmenters.add(new LetterSegmenter());
		//处理中文数量词的子分词器
		segmenters.add(new CN_QuantifierSegmenter());
		//处理中文词的子分词器
		segmenters.add(new CJKSegmenter());
		return segmenters;
	}
}
