package common.rds.exp;

public class SQLRuntimeException extends RuntimeException
{

	private static final long serialVersionUID = 1L;

	public SQLRuntimeException(Throwable e)
	{
		super(e);
	}

}
