package common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * @author wls
 */
public class CommonAjaxVO
{
	/**
	 * @param vos
	 * @return
	 */
	public static JSONObject buildJSONData(Object data)
	{
		JSONObject json = new JSONObject();
		json.put("data", data);
		json.put("msg", STR_SUCC);
		json.put("code", CODE_SUCCESS);
		return json;
	}

	/**
	 * @param list
	 * @param count
	 * @param pageNum
	 * @param pageSize
	 * @return
	 */
	public static JSONObject createDataPagingJSONObject(Object data, long count, int pageNum, int pageSize)
	{
		JSONObject json = new JSONObject();
		json.put("data", data);
		json.put("totalCount", count);
		json.put("pageNum", pageNum);
		json.put("pageSize", pageSize);
		json.put("msg", STR_SUCC);
		json.put("code", CODE_SUCCESS);
		return json;
	}

	public static JSONObject createErrorJSONObject(String errorMsg)
	{
		return CommonAjaxVO.createJSONObject(errorMsg, "", CODE_FAIL);
	}

	public static JSONObject createJSONObject(String msg, Object data, int code)
	{
		JSONObject json = new JSONObject();
		json.put("data", data);
		json.put("msg", msg);
		json.put("code", code);
		return json;
	}

	public static JSONObject createSuccessJSONObject(Object data)
	{
		return CommonAjaxVO.createJSONObject(STR_SUCC, data, CODE_SUCCESS);
	}

	public static CommonAjaxVO createSuccessVO(Object msg)
	{
		return new CommonAjaxVO(CODE_SUCCESS, msg);
	}

	public static void main(String[] args)
	{
		System.out.println(CommonAjaxVO.createJSONObject("success", "data", 1).toJSONString());

		System.out.println(CommonAjaxVO.createJSONObject("success", "data", 1).toString());

		System.out.println(JSON.toJSONString(new CommonAjaxVO(1, "success", "data")));
	}

	public static final int CODE_SUCCESS = 1;

	public static final int CODE_FAIL = -1;
	public static final String STR_SUCC = "success";
	public static final int REG_FAIL_MOBILE_EXISTED = -2;
	public static final int REG_FAIL_CPAHA_FAIL = -3;

	public static final int REG_FAIL_M_TOKEN_FAIL = -5;
	public static final int RESETPWD_FAIL_NOT_EXISTED_MOBILE = -6;
	public static final int CODE_LOGIN_REQUIRED = 10;

	public static final int CODE_EXP_OCCUR = 20;

	public static final int PARAM_NULL_EXP = 30;

	private int code;

	private Object msg;

	private Object data;

	public CommonAjaxVO()
	{
	}

	/**
	 * @param code
	 * @param msg
	 */
	public CommonAjaxVO(int code, Object msg)
	{
		this.code = code;
		this.msg = msg;
	}

	public CommonAjaxVO(int code, Object msg, Object data)
	{
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	public int getCode()
	{
		return code;
	}

	/**
	 * @return the data
	 */
	public Object getData()
	{
		return data;
	}

	public Object getMsg()
	{
		return msg;
	}

	public void setCode(int code)
	{
		this.code = code;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(Object data)
	{
		this.data = data;
	}

	public void setMsg(Object msg)
	{
		this.msg = msg;
	}

}
