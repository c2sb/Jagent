/*
 * Created on May 20, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.ui.gui.edos.sedpane;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * The reason this class exists is to allow the drag and drop subsystem of a Sedpane to support more flavors.<br>
 * @author RProgrammer
 */
public interface DnDiplomat
{
	public DataFlavor[] getSupportedDropFlavors();
	
	/**
	 * Note: dcursor is absolute, i.e. use it as an insertion point
	 */
	public void drop(Object data, DataFlavor flav, int dcursor);
	
	
	
	public DataFlavor[] getSupportedDragFlavors();
	
	/**
	 * If {@link #getDragData(DataFlavor, int, BufferedImage) getDragData()} is invoked multiple times with the same arguments, is it guaranteed to return the same results?<br>
	 */
	public boolean isDragDataCachingAllowed();
	
	public Object getDragData(DataFlavor flav, int index, BufferedImage img) throws UnsupportedFlavorException, IOException;
	
	public void freeDragResources();
}
