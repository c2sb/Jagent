/*
 * Created on May 28, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.lib.caos2pray.exc;

public class NotIntegerTagException
extends ScanningException
{
	public NotIntegerTagException(String tagname)
	{
		super(tagname);
	}
	
	
	public String getTagName()
	{
		return getMessage();
	}


	@Override
	public String getDetailMessage()
	{
		return "You forgot quotes on the (apparently textual) tag "+getTagName();
	}
}
