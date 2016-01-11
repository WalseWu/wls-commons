package common.lucene.comp.exp;

public class IndexNotSearchableException extends RuntimeException
{

	private static final long serialVersionUID = 1L;

	final String message;

	public IndexNotSearchableException(String message)
	{
		super(message);
		this.message = message;
	}

	@Override
	public String getMessage()
	{
		return message;
	}

}
