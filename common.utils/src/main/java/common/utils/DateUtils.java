package common.utils;

import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateUtils
{

	/**
	 * convert the date type to java.sql.Timestamp class
	 */
	public static Timestamp convertToTimestamp(Date date)
	{
		if (date == null) {
			return null;
		}
		else {
			return new Timestamp(date.getTime());
		}
	}

	/**
	 * format date to string
	 *
	 * @param date
	 *            the date
	 * @param format
	 *            the format
	 * @return the formatted string
	 */
	public static String formatDateToStr(Date date, String format)
	{
		if (date == null || format == null) {
			return null;
		}

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format, new DateFormatSymbols());
			return sdf.format(date);
		}
		catch (Exception e) {
			return format;
		}
	}

	/**
	 * 得到两个日期相差的天数
	 */
	public static int getBetweenDay(Date date1, Date date2)
	{
		Calendar d1 = new GregorianCalendar();
		d1.setTime(date1);
		Calendar d2 = new GregorianCalendar();
		d2.setTime(date2);
		int days = d2.get(Calendar.DAY_OF_YEAR) - d1.get(Calendar.DAY_OF_YEAR);
		// System.out.println("days="+days);
		int y2 = d2.get(Calendar.YEAR);
		if (d1.get(Calendar.YEAR) != y2) {
			// d1 = (Calendar) d1.clone();
			do {
				days += d1.getActualMaximum(Calendar.DAY_OF_YEAR);
				d1.add(Calendar.YEAR, 1);
			} while (d1.get(Calendar.YEAR) != y2);
		}
		return Math.abs(days);
	}

	/**
	 * get current time string yyyy means year MM means month dd means date HH means hour mm means minute ss means second SS means
	 * million second 'E' - DAY_OF_WEEK 'G' - ERA 'k' - HOUR_OF_DAY: 1-based. eg, 23:59 + 1 hour =>> 24:59 'a' - AM_PM 'F' -
	 * DAY_OF_WEEK_IN_MONTH 'w' - WEEK_OF_YEAR 'W' - WEEK_OF_MONTH 'K' - HOUR: 0-based. eg, 11PM + 1 hour =>> 0 AM
	 *
	 * @param style
	 *            the style string like 'yyyyMMdd'
	 * @return current time string in style mode
	 */
	public static String getCurrentTimeStr(String style)
	{
		if (style == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(style, new DateFormatSymbols());
		return sdf.format(new Date());
	}

	public static int getDistanceBetween0()
	{
		Calendar curDate = Calendar.getInstance();
		Calendar tommorowDate = new GregorianCalendar(curDate.get(Calendar.YEAR), curDate.get(Calendar.MONTH),
				curDate.get(Calendar.DATE) + 1, 0, 0, 0);
		int diffSec = (int) (tommorowDate.getTimeInMillis() - curDate.getTimeInMillis()) / 1000;
		return diffSec;
	}

	public static Date getDNdayAgo(Date currentDay, int n)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(currentDay);
		cal.add(Calendar.DAY_OF_MONTH, -n);
		return cal.getTime();
	}

	/**
	 * 获取n月前的时间 格式yyyy-MM-dd,n如果为负表示n天后的时间
	 */
	public static Date getDNdayAgo(int n)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -n);
		return cal.getTime();
	}

	/**
	 * 获取n小时前的时间 格式yyyy-MM-dd,n如果为负表示n小时后的时间
	 */
	public static Date getDNHourAgo(int n)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, -n);
		return cal.getTime();
	}

	/**
	 * 获取n分钟前的时间 格式yyyy-MM-dd,n如果为负表示n小时后的时间
	 */
	public static Date getDNMinuteAgo(int n)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -n);
		return cal.getTime();
	}

	/**
	 * 获取n秒前的时间 格式yyyy-MM-dd,n如果为负表示n秒后的时间
	 */
	public static Date getDNSecondAgo(int n)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, -n);
		return cal.getTime();
	}

	/**
	 * get the last day of the given month
	 *
	 * @param year
	 *            Year
	 * @param month
	 *            Month from 1 to 12
	 * @return the last day of given month
	 */
	public static int getLastDayInMonth(int year, int month)
	{
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, year);
		// month: 1~12
		c.set(Calendar.MONTH, month - 1);
		return c.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	public static String getNdayAgo(Date currentDay, int n, String formatStr)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(currentDay);
		cal.add(Calendar.DAY_OF_MONTH, -n);
		return DateUtils.formatDateToStr(cal.getTime(), formatStr);
	}

	/**
	 * 获取n天前的时间 格式yyyy-MM-dd,n如果为负表示n天后的时间
	 *
	 * @param n
	 * @return
	 */
	public static String getNdayAgo(int n)
	{
		return DateUtils.getNdayAgo(n, DATESTYLEA_A);
	}

	/**
	 * 获取n天前的时间 格式自定义
	 *
	 * @param n
	 * @param formatStr
	 * @return
	 */
	public static String getNdayAgo(int n, String formatStr)
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -n);
		return DateUtils.formatDateToStr(cal.getTime(), formatStr);
	}

	/**
	 * @param n
	 * @return n ago day value in long type as 'yyyyMMdd', eg: 20140523
	 */
	public static long getNdayAgoLong(int n)
	{
		String s = DateUtils.getNdayAgo(n, DATESTYLED_A);
		return Long.valueOf(s);
	}

	public static Date getSystemMaxDate()
	{
		return MAX_DATE;
	}

	/**
	 * get current date object
	 */
	public static Date nowDate()
	{
		return new Date();
	}

	public static Date parseStrToDate(String dateStr)
	{
		return DateUtils.parseStrToDate(dateStr, DATESTYLEA_A);
	}

	/**
	 * parse string to date format
	 *
	 * @param dateStr
	 *            the date string
	 * @param format
	 *            the format string
	 * @return date object or null
	 */
	public static Date parseStrToDate(String dateStr, String format)
	{
		if (dateStr == null || format == null) {
			return null;
		}

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format, new DateFormatSymbols());
			return sdf.parse(dateStr);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Converts a <code>String</code> object in JDBC timestamp escape format to a <code>Timestamp</code> value.
	 *
	 * @param timeStr
	 *            timestamp in format yyyy-MM-dd hh:mm:ss[.f...]. The fractional seconds may be omitted.
	 * @return corresponding <code>Timestamp</code> value
	 * @exception java.lang.IllegalArgumentException
	 *                if the given argument does not have the format <code>yyyy-MM-dd hh:mm:ss[.f...]</code>
	 */
	public static Timestamp parseStrToTimestamp(String timeStr)
	{
		if (timeStr == null) {
			return null;
		}
		try {
			return Timestamp.valueOf(timeStr);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * 'yyyy-MM-dd HH:mm:ss' style data format 例：1988-11-14 10:10:10
	 */
	public static final String DATESTYLEA = "yyyy-MM-dd HH:mm:ss";

	/**
	 * 'yyyy-MM-dd' style data format 例：1988-11-14
	 */
	public static final String DATESTYLEA_A = "yyyy-MM-dd";

	/**
	 * 'yyyy-MM-dd HH:mm' style data format 例：1988-11-14 10:10
	 */
	public static final String DATESTYLEA_B = "yyyy-MM-dd HH:mm";

	/**
	 * 'yyyy-MM-dd HH:mm:ss.SSS' style data format 例：1988-11-14 10:10:10.222
	 */
	public static final String DATESTYLEA_C = "yyyy-MM-dd HH:mm:ss.SSS";

	/**
	 * 'yyyy/MM/dd HH:mm:ss' style data format 例：1988/11/14 10:10:10
	 */
	public static final String DATESTYLEB = "yyyy/MM/dd HH:mm:ss";

	/**
	 * 'yyyy/MM/dd' style data format 例：1988/11/14
	 */
	public static final String DATESTYLEB_A = "yyyy/MM/dd";

	/**
	 * 'yyyy/MM/dd HH:mm' style data format 例：1988/11/14 10:10
	 */
	public static final String DATESTYLEB_B = "yyyy/MM/dd HH:mm";

	/**
	 * 'yyyy/MM/dd HH:mm:ss.SSS' style data format 例：1988/11/14 10:10:10.222
	 */
	public static final String DATESTYLEB_C = "yyyy/MM/dd HH:mm:ss.SSS";

	/**
	 * 'yyyy/MM/dd/HH' style data format 例：1988/11/14/10
	 */
	public static final String DATESTYLEB_D = "yyyy/MM/dd/HH";

	/**
	 * 'yyyy年MM月dd日 HH时mm分ss秒' style data format 例：1988年11月14日 10时10分10秒
	 */
	public static final String DATESTYLEC = "yyyy年MM月dd日 HH时mm分ss秒";

	/**
	 * 'yyyy年MM月dd日' style data format 例：1988年11月14日
	 */
	public static final String DATESTYLEC_A = "yyyy年MM月dd日";

	/**
	 * 'yyyy年MM月dd日 HH时mm分' style data format 例：1988年11月14日 10时10分
	 */
	public static final String DATESTYLEC_B = "yyyy年MM月dd日 HH时mm分";

	/**
	 * 'yyyy年MM月dd日 HH时mm分ss秒.SSS' style data format 例：1988年11月14日 10时10分10秒.222
	 */
	public static final String DATESTYLEC_C = "yyyy年MM月dd日 HH时mm分ss秒.SSS";

	/**
	 * 'yyyy年MM月dd日 HH时' style data format 例：1988年11月14日 10时
	 */
	public static final String DATESTYLEC_D = "yyyy年MM月dd日 HH时";

	/**
	 * 'yyyy年MM月dd日 HH时mm分ss秒SSS' style data format 例：1988年11月14日 10时10分10秒222
	 */
	public static final String DATESTYLEC_E = "yyyy年MM月dd日 HH时mm分ss秒SSS";

	/**
	 * 'yyyyMMddHHmmss' style data format 例：19881114101010
	 */
	public static final String DATESTYLED = "yyyyMMddHHmmss";

	/**
	 * 'yyyyMMdd' style data format 例：19881114
	 */
	public static final String DATESTYLED_A = "yyyyMMdd";

	/**
	 * 'yyyyMMddHHmm' style data format 例：198811141010
	 */
	public static final String DATESTYLED_B = "yyyyMMddHHmm";

	/**
	 * 'yyyyMMddHHmmssSSS' style data format 例：19881114101010222
	 */
	public static final String DATESTYLED_C = "yyyyMMddHHmmssSSS";

	/**
	 * 'yyyyMMddHH' style data format 例：1988111410
	 */
	public static final String DATESTYLED_D = "yyyyMMddHH";

	/**
	 * 'HH:mm:ss' style data format 例：10:10:10
	 */
	public static final String DATESTYLEO = "HH:mm:ss";

	/**
	 * 'HH时mm分ss秒' style data format 例：10时10分10秒
	 */
	public static final String DATESTYLEO_A = "HH时mm分ss秒";

	/**
	 * 'yy年MM月dd日' style data format 例：88年11月14日
	 */
	public static final String DATESTYLEO_B = "yy年MM月dd日";

	/**
	 * 'HH/mm/ss' style data format 例：10/10/10
	 */
	public static final String DATESTYLEO_C = "HH/mm/ss";

	/**
	 * 'yyMMddHHmmssSSS' style data format 例：881114101010222
	 */
	public static final String DATESTYLEO_D = "yyMMddHHmmssSSS";

	public static final Date MAX_DATE = new Date(Long.MAX_VALUE);
}
