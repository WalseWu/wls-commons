package common.rds.exp;

/**
 * An error that indicates a developer bug that is fatal to the continued execution of the application and MUST be remedied at
 * once.
 * 
 * @author Lisong
 */
public class BugError extends Error
{
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public BugError()
	{
		super();
	}

	/**
	 * @param message
	 */
	public BugError(String message)
	{
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public BugError(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * @param cause
	 */
	public BugError(Throwable cause)
	{
		super(cause);
	}
}
