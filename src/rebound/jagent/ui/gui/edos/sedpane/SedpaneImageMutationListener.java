/*
 * Created on Jul 25, 2008
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.gui.edos.sedpane;

/**
 * These events are called after the fact.<br>
 * @author RProgrammer
 */
public interface SedpaneImageMutationListener
{
	public void imageAdded(int index);
	
	public void imageDeleted(int index);
	
	public void imagesCleared();
	
	public void imagesReordered(int source, int dest);
}
