/*
 * Created on Jul 9, 2008
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.gui.edos.sedpane;

import java.awt.image.BufferedImage;

public interface MutableImageStorage
extends ImageStorage
{
	public void add(BufferedImage img, int index);
	
	public void clear();
	
	public void delete(int index);
	
	public void reorder(int old, int fresh);
}
