/*
 * Created on May 27, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.lib.caos2pray.exc;

public class DependencyException
extends Exception
{
	public DependencyException()
	{
		super();
	}
	
	public DependencyException(String dep)
	{
		super(dep);
	}
	
	
	public String getDependency()
	{
		return super.getMessage();
	}
	
	@Override
	public String getMessage()
	{
		return "Dependency type not supported for: "+getDependency();
	}
}
