package common.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

public class FileUtil
{
	/**
	 * 在dir目录下创建子目录 resultFoldername
	 *
	 * @param dir
	 * @param resultFoldername
	 * @return
	 * @throws IOException
	 */
	public static File createChildFolder(File dir, String resultFoldername) throws IOException
	{
		File f = new File(dir.getCanonicalPath() + File.separator + resultFoldername);
		if (!f.exists() || !f.isDirectory()) {
			f.mkdir();
		}
		return f;
	}

	public static File createDirs(String dirPath)
	{
		File f = new File(dirPath);
		if (!f.exists() || !f.isDirectory()) {
			f.mkdirs();
		}
		return f;
	}

	public static File createFile(String filePath) throws IOException
	{
		File f = new File(filePath);
		if (!f.exists() || f.isDirectory()) {
			f.createNewFile();
		}
		return f;
	}

	public static void deleteDir(String dirPath)
	{
		File f = new File(dirPath);
		if (f.exists() && f.isDirectory()) {
			File[] fs = f.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname)
				{
					return pathname.getName().endsWith("java");
				}
			});
			for (File file : fs) {
				if (!file.isDirectory()) {
					file.delete();
				}
				else {
					FileUtil.deleteDir(file.getAbsolutePath());
				}
			}
			f.delete();
		}
	}

	public static String[] getAllSortedFilenamesUnderFolder(File folder)
	{
		String[] files = folder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name)
			{
				try {
					File f = new File(dir.getCanonicalPath() + File.separator + name);
					if (f.exists() && !f.isDirectory()) {
						return true;
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}
		});
		Arrays.sort(files);
		return files;
	}

	public static String getProjectRootPath()
	{
		String dir = System.getProperty("user.dir");
		File f = new File(dir);
		return f.getParent();
	}

	public static File[] listFilesByFilter(String parentFolderStr, FilenameFilter filter)
	{
		File emFolder = new File(parentFolderStr);
		return emFolder.listFiles(filter);
	}

	/**
	 * 去除invalidChars中包含的所有字符
	 *
	 * @param filename
	 * @return
	 */
	public static String parseFilename(String filename)
	{
		char[] fChars = filename.toCharArray();
		char[] resultChar = new char[fChars.length];
		int i = 0;
		outer: for (char fc : fChars) {
			for (char c : invalidChars) {
				if (fc == c) {
					continue outer;
				}
			}
			resultChar[i++] = fc;
		}
		return new String(resultChar, 0, i);
	}

	private static char[] invalidChars = new char[] { '>' };
}
