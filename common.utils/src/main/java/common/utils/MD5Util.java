package common.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util
{

	public static void main(String[] args) throws NoSuchAlgorithmException
	{
		System.out.println(MD5Util.md5Encode("wilson123"));
	}

	public static String md5Encode(String input) throws NoSuchAlgorithmException
	{
		if (input != null) {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] result = md.digest(input.getBytes());
			String resultStr = MD5Util.byteArrayToHexString(result);
			return resultStr.toUpperCase();
		}
		return null;
	}

	/**
	 * 转换字节数组为十六进制字符串
	 *
	 * @param 字节数组
	 * @return 十六进制字符串
	 */
	private static String byteArrayToHexString(byte[] b)
	{
		StringBuffer resultSb = new StringBuffer();
		for (byte element : b) {
			resultSb.append(MD5Util.byteToHexString(element));
		}
		return resultSb.toString();
	}

	/** 将一个字节转化成十六进制形式的字符串 */
	private static String byteToHexString(byte b)
	{
		int n = b;
		if (n < 0) {
			n = 256 + n;
		}
		int d1 = n / 16;
		int d2 = n % 16;
		return hexDigits[d1] + hexDigits[d2];
	}

	//十六进制下数字到字符的映射数组
	private final static String[] hexDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

}
