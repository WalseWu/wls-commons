/*
 * Copyright (C) 2015 dzyh
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author wls
 */
public final class FileReadWriteUtil
{

	/**
	 * 追加文件：使用FileOutputStream，在构造FileOutputStream时，把第二个参数设为true
	 *
	 * @param fileName
	 * @param content
	 */
	public static void appendToFile(File file, String conent)
	{
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			out.write(conent);
			out.write("\r\n");
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("写(追加)文件失败!");
		}
		finally {
			try {
				if (out != null) {
					out.close();
				}
			}
			catch (IOException e) {
			}
		}
	}

	/**
	 * Calculate the total size of file
	 *
	 * @param filePath
	 * @return
	 */
	public static long calcFileSize(String filePath)
	{
		RandomAccessFile read = null;
		try {
			read = new RandomAccessFile(filePath, "r");
			long totalSize = read.length();
			return totalSize;
		}
		catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		finally {
			if (read != null) {
				try {
					read.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	/**
	 * @return root path
	 * @throws IOException
	 */
	public static String calcRootPath(String src)
	{
		if (rootPath == null) {
			synchronized (FileReadWriteUtil.class) {
				if (rootPath == null) {
					File directory = new File(src).getParentFile();
					try {
						rootPath = directory.getCanonicalPath();
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return rootPath;
	}

	/**
	 * 读取size大小的数据块，然后去除最后一个“回车”以后的数据，并计算去除以后的有效 大小
	 *
	 * @param path
	 * @param start
	 * @param size
	 * @return
	 * @throws IOException
	 */
	public static long calcSizeBeforeLastReturn(String path, long start, long size)
	{
		RandomAccessFile read = null;
		long result = 0;
		try {
			read = new RandomAccessFile(path, "r");
			ByteBuffer out = read.getChannel().map(FileChannel.MapMode.READ_ONLY, start, size);
			byte[] tmp = new byte[out.limit()];//out.array();
			out.get(tmp);
			out.clear();
			//			String tempStr = new String(tmp);
			//			System.out.println(tempStr);
			int point = tmp.length - 1;
			while (tmp[point] != '\n' && tmp[point] != '\r') {
				point--;
			}
			result = point + 1;
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException("找不到文件:" + path);
		}
		catch (Exception e) {
			throw new RuntimeException(path + "文件读取失败!");
		}
		finally {
			try {
				read.close();
			}
			catch (IOException e) {
			}
		}
		return result;
	}

	/**
	 * 将 源文件 copy 到 目标文件
	 *
	 * @param srcFile
	 *            源文件
	 * @param destFile
	 *            目标文件
	 * @throws IOException
	 */
	public static void copyFile(File srcFile, File destFile) throws IOException
	{
		if (!destFile.exists() || destFile.isDirectory()) {
			destFile.createNewFile();
		}
		BufferedInputStream inBuff = null;
		BufferedOutputStream outBuff = null;
		try {
			inBuff = new BufferedInputStream(new FileInputStream(srcFile));

			outBuff = new BufferedOutputStream(new FileOutputStream(destFile));

			byte[] b = new byte[1024 * 5];
			int len;
			while ((len = inBuff.read(b)) != -1) {
				outBuff.write(b, 0, len);
			}
			outBuff.flush();
		}
		finally {
			if (inBuff != null) {
				inBuff.close();
			}
			if (outBuff != null) {
				outBuff.close();
			}
		}
	}

	public static void main(String[] args)
	{
		FileReadWriteUtil.readFirstLine("D:\\CPA\\testFiles\\test1.txt");
	}

	/**
	 * 从文件指定的指定位置开始读取 readSize 大小的数据
	 *
	 * @param path
	 * @param start
	 * @param readSize
	 * @return
	 * @throws IOException
	 */
	public static BufferedReader readByStartAndSize(String path, long start, long readSize)
	{
		RandomAccessFile read = null;
		try {
			read = new RandomAccessFile(path, "r");
			ByteBuffer bb = read.getChannel().map(FileChannel.MapMode.READ_ONLY, start, readSize);
			byte[] buffer = new byte[bb.limit()];//out.array();
			bb.get(buffer);
			BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer)));
			bb.clear();

			return br;
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException("找不到文件:" + path);
		}
		catch (IOException e) {
			throw new RuntimeException(path + " 文件读取失败!");
		}
		finally {
			if (read != null) {
				try {
					read.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Read the first line of file
	 *
	 * @param filePath
	 * @return
	 */
	public static String readFirstLine(String filePath)
	{
		BufferedReader br = null;
		String result = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
			result = br.readLine();
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException("找不到指定文件：" + filePath);
		}
		catch (IOException e) {
			throw new RuntimeException("文件读取出错!" + filePath);
		}
		finally {
			try {
				if (br != null) {
					br.close();
				}
			}
			catch (IOException e) {
			}
		}
		return result;
	}

	private static String rootPath = null;
}
