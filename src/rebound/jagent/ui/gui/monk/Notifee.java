/*
 * Created on Jun 2, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.ui.gui.monk;

public interface Notifee
{
	public void error(String message);
	public void error(String prefix, Throwable cause);
	
	public void notify(String message);
}
