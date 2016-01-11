package common.tools.codegenerator;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import common.rds.annotation.PrimaryKey;

import freemarker.template.Template;

/**
 * @author wls
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/dzyh/config/spring-common.xml", "/dzyh/config/spring-rds-common.xml" })
public class DaoGenerator extends AbstractCodeGenerator
{
	private final String templateFile = "dao.html";

	private final String modelPackage = "dzyh.common.beans";

	@Test
	public void generateCode() throws Exception
	{
		List<Class<?>> clzs = getClassesUnderPackage(modelPackage);
		File targetDir = prepareTargetFolder("/dao");
		for (Class<?> model : clzs) {
			logger.info("start to process {}...", model.getSimpleName());
			generateDaoForModel(model, targetDir);
		}
	}

	/**
	 * 从packgname下扫描所有类
	 *
	 * @param packgname
	 * @throws ClassNotFoundException
	 */
	public List<Class<?>> getClassesUnderPackage(String packgname) throws ClassNotFoundException
	{
		List<Class<?>> list = new ArrayList<>();

		final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		// add include filters which matches all the classes (or use your own)
		provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

		// get matching classes defined in the package
		final Set<BeanDefinition> classes = provider.findCandidateComponents(packgname);

		// this is how you can load the class type from BeanDefinition instance
		for (BeanDefinition bean : classes) {
			String name = bean.getBeanClassName();
			list.add(Class.forName(name));
		}
		return list;
	}

	protected void generateDaoForModel(Class<?> model, File targetDir) throws Exception
	{
		Field[] fs = model.getDeclaredFields();
		Field pf = null;
		for (Field f : fs) {
			PrimaryKey pk = f.getAnnotation(PrimaryKey.class);
			if (pk != null) {
				pf = f;
				break;
			}
		}

		if (pf == null) {
			logger.error("Error: no PK in the model: {}", model.getName());
			return;
		}

		Template t = getCodeTemplate(templatePath, templateFile);
		Map<String, Object> data = new HashMap<>();
		data.put("package", "dao");
		data.put("fullBeanName", model.getName());
		data.put("beanName", model.getSimpleName());
		data.put("primaryKeyType", pf.getType().getSimpleName());
		data.put("primaryKeyName", pf.getName());

		codeToFile(targetDir, model.getSimpleName() + "Dao", t, data);
	}
}
