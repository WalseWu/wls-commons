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

import java.util.Stack;
import java.util.TreeSet;

/**
 * Compatible with IK 2012FF_hf1
 *
 * @see also {@link IKArbitrator}
 * @author wls
 */
public class KeywordArbitrator
{
	/**
	 * 回滚词元链，直到它能够接受指定的词元
	 *
	 * @param lexeme
	 * @param l
	 */
	private void backPath(Lexeme l, LexemePath option)
	{
		while (option.checkCross(l)) {
			option.removeTail();
		}

	}

	/**
	 * 向前遍历，添加词元，构造一个无歧义词元组合
	 *
	 * @param LexemePath
	 *            path
	 * @return
	 */
	private Stack<QuickSortSet.Cell> forwardPath(QuickSortSet.Cell lexemeCell, LexemePath option)
	{
		//发生冲突的Lexeme栈
		Stack<QuickSortSet.Cell> conflictStack = new Stack<QuickSortSet.Cell>();
		QuickSortSet.Cell c = lexemeCell;
		//迭代遍历Lexeme链表
		while (c != null && c.getLexeme() != null) {
			if (!option.addNotCrossLexeme(c.getLexeme())) {
				//词元交叉，添加失败则加入lexemeStack栈
				conflictStack.push(c);
			}
			c = c.getNext();
		}
		return conflictStack;
	}

	/**
	 * 歧义识别
	 *
	 * @param lexemeCell
	 *            歧义路径链表头
	 * @param fullTextLength
	 *            歧义路径文本长度
	 * @param option
	 *            候选结果路径
	 * @return
	 */
	private LexemePath judge(QuickSortSet.Cell lexemeCell, int fullTextLength)
	{
		//候选路径集合
		TreeSet<LexemePath> pathOptions = new TreeSet<LexemePath>();
		//候选结果路径
		LexemePath option = new LexemePath();

		//对crossPath进行一次遍历,同时返回本次遍历中有冲突的Lexeme栈
		Stack<QuickSortSet.Cell> lexemeStack = forwardPath(lexemeCell, option);

		//当前词元链并非最理想的，加入候选路径集合
		pathOptions.add(option.copy());

		//存在歧义词，处理
		QuickSortSet.Cell c = null;
		while (!lexemeStack.isEmpty()) {
			c = lexemeStack.pop();
			//回滚词元链
			backPath(c.getLexeme(), option);
			//从歧义词位置开始，递归，生成可选方案
			forwardPath(c, option);
			pathOptions.add(option.copy());
		}

		//返回集合中的最优方案
		return pathOptions.first();

	}

	/**
	 * TODO:若真的必须去除完全覆盖term，可以修改词方法
	 *
	 * @desc 分词歧义处理
	 * @param orgLexemes
	 * @param useSmart
	 * @param removeALlCoverTerms
	 *            当前没有使用该参数，保证query的分词算法和索引的分词算法一致就可以保证全覆盖和部分覆盖的评分.
	 */
	void process(AnalyzeContext context, boolean useSmart, boolean removeALlCoverTerms)
	{
		QuickSortSet orgLexemes = context.getOrgLexemes();
		Lexeme orgLexeme = orgLexemes.pollFirst();

		LexemePath crossPath = new LexemePath();
		while (orgLexeme != null) {

			/**
			 * wls:单字去除在构建字典这一步完成，白名单也在字典构建时候应用 if (orgLexeme.getLength() <= 1) { orgLexeme = orgLexemes.pollFirst(); continue; }
			 */
			if (!crossPath.addCrossLexeme(orgLexeme)) {
				//找到与crossPath不相交的下一个crossPath
				if (crossPath.size() == 1 || !useSmart) {
					//crossPath没有歧义 或者 不做歧义处理
					//直接输出当前crossPath
					//					if (crossPath.getPathLength() > 1) {
					context.addLexemePath(crossPath);
					//					}
				}
				else {
					//对当前的crossPath进行歧义处理
					QuickSortSet.Cell headCell = crossPath.getHead();
					LexemePath judgeResult = judge(headCell, crossPath.getPathLength());
					//输出歧义处理结果judgeResult
					context.addLexemePath(judgeResult);
					/**
					 * wls:单字去除在构建字典这一步完成，白名单也在字典构建时候应用 if (judgeResult.getPathLength() > 1) { //单个字符的term不加入结果集 }
					 */
				}

				//把orgLexeme加入新的crossPath中
				crossPath = new LexemePath();
				crossPath.addCrossLexeme(orgLexeme);
				/**
				 * wls:单字去除在构建字典这一步完成，白名单也在字典构建时候应用 if (orgLexeme.getLength() > 1) { //单个字符的term不加入结果集 }
				 */
			}
			orgLexeme = orgLexemes.pollFirst();
		}

		//处理最后的path
		if (crossPath.size() == 1 || !useSmart) {
			//crossPath没有歧义 或者 不做歧义处理
			//直接输出当前crossPath
			context.addLexemePath(crossPath);
			/**
			 * wls:单字去除在构建字典这一步完成，白名单也在字典构建时候应用 if (crossPath.getPathLength() > 1) { //单个字符的term不加入结果集 }
			 */
		}
		else {
			//对当前的crossPath进行歧义处理
			QuickSortSet.Cell headCell = crossPath.getHead();
			LexemePath judgeResult = judge(headCell, crossPath.getPathLength());
			//输出歧义处理结果judgeResult
			context.addLexemePath(judgeResult);

			/**
			 * wls:单字去除在构建字典这一步完成，白名单也在字典构建时候应用 if (judgeResult.getPathLength() > 1) { //单个字符的term不加入结果集 }
			 */
		}
	}

}
