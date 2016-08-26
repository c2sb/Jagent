/*
 * Created on Sep 12, 2009
 * 	by the great Eclipse(c)
 */
package rebound.jagent.lib.caos2pray.exc;

public class MissingLinkedScriptException
extends ScanningException
{
	
	public MissingLinkedScriptException()
	{
	}
	
	public MissingLinkedScriptException(String script, Throwable cause)
	{
		super(script, cause);
	}
	
	public MissingLinkedScriptException(String script)
	{
		super(script);
		
	}
	
	public MissingLinkedScriptException(Throwable cause)
	{
		super(cause);
	}
	
	@Override
	public String getDetailMessage()
	{
		return "Couldn't find linked script "+getMessage();
	}
}
