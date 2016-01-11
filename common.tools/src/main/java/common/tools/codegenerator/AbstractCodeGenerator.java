/*
 * Copyright (C) 2015 dzyh
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package common.tools.codegenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import common.utils.FileUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * @author wls
 */
public abstract class AbstractCodeGenerator
{

	String templatePath = "/common/tools/codegenerator/beantemplate";

	protected static final Logger logger = LoggerFactory.getLogger(AbstractCodeGenerator.class);

	public void codeToFile(File dir, String className, Template t, Map<?, ?> data) throws Exception
	{
		try (FileOutputStream fos = new FileOutputStream(new File(dir.getAbsolutePath() + "/" + className + ".java"))) {
			t.process(data, new OutputStreamWriter(fos, "utf-8"));//
			fos.flush();
		}
		catch (Exception e) {
			throw e;
		}
	}

	public Map<String, Object> generateTempalteData(String packageName, String className)
	{
		Map<String, Object> data = new HashMap<>();
		data.put("package", packageName);
		data.put("className", className);
		return data;
	}

	public Template getCodeTemplate(String templateClasspath, String templateFile) throws Exception
	{
		Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
		cfg.setClassForTemplateLoading(getClass(), templateClasspath);// 指定模板所在的classpath目录
		cfg.setSharedVariable("upperFC", new UpperFirstCharacter());// 添加一个"宏"共享变量用来将属性名首字母大写
		return cfg.getTemplate(templateFile);// 指定模板
	}

	public File prepareTargetFolder(String cPath)
	{
		String projectPath = FileUtil.getProjectRootPath();
		String javaFilePath = projectPath + cPath;
		FileUtil.deleteDir(javaFilePath);
		File dir = FileUtil.createDirs(javaFilePath);
		return dir;
	}
}
