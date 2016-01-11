package common.rds.sql;

import java.text.MessageFormat;

/**
 * @author wls
 */
public class SQLOrder
{
	public static SQLOrder createDescOrder(String field)
	{
		if (field == null || "".equals(field)) {
			return null;
		}
		return new SQLOrder(field, DESC_TYPE);
	}

	public static SQLOrder createOrder(String field)
	{
		if (field == null || "".equals(field)) {
			return null;
		}
		return new SQLOrder(field, INC_TYPE);
	}

	public static SQLOrder createOrder(String field, String orderType)
	{
		if (field == null || "".equals(field)) {
			return null;
		}
		return DESC_TYPE.equals(orderType) ? SQLOrder.createDescOrder(field) : SQLOrder.createOrder(field);
	}

	public static final String DESC_TYPE = "desc";
	public static final String INC_TYPE = "";

	private static final String ORDER_CLAUSE = " order by {0} {1}";

	String orderField;

	String orderType;

	private SQLOrder(String orderField, String orderType)
	{
		this.orderField = orderField;
		this.orderType = orderType;
	}

	public String getOrderClause()
	{
		return MessageFormat.format(ORDER_CLAUSE, orderField, orderType == null ? "" : orderType);
	}

	public String getOrderField()
	{
		return orderField;
	}

	public String getOrderType()
	{
		return orderType;
	}

	public void setOrderField(String orderField)
	{
		this.orderField = orderField;
	}

	public void setOrderType(String orderType)
	{
		this.orderType = orderType;
	}
}
