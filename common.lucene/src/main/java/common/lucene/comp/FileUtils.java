package common.lucene.comp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 引入了lucene index专用
 *
 * @author
 */
public class FileUtils
{

	// 复制文件夹
	public static void copyDirectiory(String sourceDir, String targetDir) throws IOException
	{
		logger.info("Switch index: copy {} to {}", sourceDir, targetDir);
		// 新建目标目录
		new File(targetDir).mkdirs();
		// 获取源文件夹当前下的文件或目录
		File[] file = new File(sourceDir).listFiles();
		for (File sourceFile : file) {
			if (sourceFile.isFile()) {
				// 目标文件
				File targetFile = new File(new File(targetDir).getAbsolutePath() + File.separator + sourceFile.getName());
				FileUtils.copyFile(sourceFile, targetFile);
			}
			if (sourceFile.isDirectory()) {
				// 准备复制的源文件夹
				String dir1 = sourceDir + File.separator + sourceFile.getName();
				// 准备复制的目标文件夹
				String dir2 = targetDir + File.separator + sourceFile.getName();
				FileUtils.copyDirectiory(dir1, dir2);
			}
		}
	}

	// 复制文件
	public static void copyFile(File sourceFile, File targetFile) throws IOException
	{
		try (// 新建文件输入流并对它进行缓冲
				FileInputStream input = new FileInputStream(sourceFile);
				BufferedInputStream inBuff = new BufferedInputStream(input);
				// 新建文件输出流并对它进行缓冲
				FileOutputStream output = new FileOutputStream(targetFile);
				BufferedOutputStream outBuff = new BufferedOutputStream(output);) {
			// 缓冲数组
			byte[] b = new byte[1024 * 5];
			int len;
			while ((len = inBuff.read(b)) != -1) {
				outBuff.write(b, 0, len);
			}
			// 刷新此缓冲的输出流
			outBuff.flush();
		}
		catch (IOException e) {
			throw e;
		}
	}

	public static void delDir(String filepath, boolean deleteFolder) throws IOException
	{
		logger.info("Switch index: delete {}", filepath);
		File f = new File(filepath);
		if (f.exists() && f.isDirectory()) {
			if (f.listFiles().length == 0 && deleteFolder) {
				f.delete();
			}
			else {
				File delFile[] = f.listFiles();
				int i = f.listFiles().length;
				for (int j = 0; j < i; j++) {
					if (delFile[j].isDirectory()) {
						FileUtils.delDir(delFile[j].getAbsolutePath(), deleteFolder);
					}
					delFile[j].delete();
				}
			}
		}
		if (deleteFolder) {
			f.delete();
		}
	}

	public static FSDirectory openLuceneDirectory(final String path) throws IOException
	{
		File dir = new File(path);
		if (!dir.exists() || !dir.isDirectory()) {
			dir.mkdirs();
		}
		return FSDirectory.open(dir);
	}

	private static Logger logger = LoggerFactory.getLogger(FileUtils.class);
}
