/*
 * Created on May 20, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.ui.gui.edos.sedpane;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class MemoryImageStorage
implements MutableImageStorage
{
	//Vectors are used for the setSize method
	protected Vector<BufferedImage> images;
	protected Vector<Image> thumbnails;
	protected int thumbnailWidth, thumbnailHeight;
	
	
	public MemoryImageStorage()
	{
		images = new Vector<BufferedImage>();
		thumbnails = new Vector<Image>();
		thumbnailWidth = 48;
		thumbnailHeight = 48;
	}
	
	
	
	protected int getValidIndex(int index)
	{
		if (index < 0)
			index = 0;
		else if (index >= getCount())
			index = getCount()-1;
		return index;
	}
	
	protected int getValidInsertionIndex(int index)
	{
		if (index < 0)
			index = 0;
		else if (index > getCount())
			index = getCount();
		return index;
	}
	
	
	
	
	
	public int getCount()
	{
		return images.size();
	}
	
	public BufferedImage load(int index)
	{
		return images.get(getValidIndex(index));
	}
	
	public int getHeight(int index)
	{
		return images.get(getValidIndex(index)).getHeight();
	}
	
	public int getWidth(int index)
	{
		return images.get(getValidIndex(index)).getWidth();
	}
	
	public BufferedImage[] loadAll()
	{
		return images.toArray(new BufferedImage[images.size()]);
	}
	
	
	
	
	public void add(BufferedImage img, int index)
	{
		index = getValidInsertionIndex(index);
		images.add(index, img);
		thumbnails.add(index, null);
	}
	
	public void clear()
	{
		images.clear();
		thumbnails.clear();
	}
	
	public void delete(int index)
	{
		index = getValidIndex(index);
		images.remove(index);
		thumbnails.remove(index);
	}
	
	public void reorder(int old, int fresh)
	{
		old = getValidIndex(old);
		fresh = getValidInsertionIndex(fresh);
		if (fresh != old && fresh != old+1)
		{
			images.add(fresh < old ? fresh : fresh-1, images.remove(old));
			thumbnails.add(fresh < old ? fresh : fresh-1, thumbnails.remove(old));
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<Thumbnails
	protected void ensureThumbnail(int index)
	{
		if (thumbnails.size() <= index)
		{
			thumbnails.setSize(index+1);
		}
		
		
		if (thumbnails.get(index) == null)
		{
			BufferedImage source = load(index);
			
			int thumbwidth = 0;
			int thumbheight = 0;
			{
				int width = source.getWidth();
				int height = source.getHeight();
				
				thumbwidth = getThumbnailWidth();
				thumbheight = (int)(thumbwidth * ((float)height/(float)width));
				
				if (thumbheight > getThumbnailHeight())
				{
					thumbheight = getThumbnailHeight();
					thumbwidth = (int)(thumbheight * ((float)width/(float)height));
				}
				
				if (thumbwidth <= 0)
					thumbwidth = 1;
				if (thumbheight <= 0)
					thumbheight = 1;
			}
			
			Image thumbnail = source.getScaledInstance(thumbwidth, thumbheight, Image.SCALE_AREA_AVERAGING);
			
			thumbnails.set(index, thumbnail);
		}
	}
	
	
	
	public Image getThumbnail(int index)
	{
		index = getValidIndex(index);
		ensureThumbnail(index);
		return thumbnails.get(index);
	}
	
	
	
	public int getThumbnailHeight()
	{
		return thumbnailHeight;
	}
	
	public int getThumbnailWidth()
	{
		return thumbnailWidth;
	}
	
	public void setThumbnailSizes(int width, int height)
	{
		if (width <= 0)
			width = 1;
		if (height <= 0)
			height = 1;
		
		if (thumbnailWidth != width || thumbnailHeight != height)
		{
			thumbnails.clear();
			thumbnailWidth = width;
			thumbnailHeight = height;
			
			for (int i = getCount()-1; i >= 0; i--)
				ensureThumbnail(i);
		}
	}
	//Thumbnails>
}
