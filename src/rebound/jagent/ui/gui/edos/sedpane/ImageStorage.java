/*
 * Created on May 20, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.ui.gui.edos.sedpane;

import java.awt.Image;
import java.awt.image.BufferedImage;

public interface ImageStorage
{
	public int getCount();
	
	public int getWidth(int index);
	
	public int getHeight(int index);
	
	public BufferedImage load(int index);
	
	public BufferedImage[] loadAll();
	
	
	public Image getThumbnail(int index);
	
	public int getThumbnailWidth();
	public int getThumbnailHeight();
	
	public void setThumbnailSizes(int width, int height);
}
