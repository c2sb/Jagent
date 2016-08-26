/*
 * Created on Jul 9, 2008
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.gui.edos.sedpane;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public interface DnDErrorHandler
{
	/*
	 * Rule for error notifiers is, return true if you handled the error, or false if you didn't and Sedpane needs to
	 */
	
	public boolean errorInDrop(IOException exc);
	
	public boolean errorInDrop(UnsupportedFlavorException exc);
}
