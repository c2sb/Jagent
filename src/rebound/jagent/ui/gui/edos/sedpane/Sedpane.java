/*
 * Created on Jan 27, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.ui.gui.edos.sedpane;

import static rebound.hci.graphics2d.Direction2D.*;
import static rebound.hci.graphics2d.java2d.Java2DUtilities.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import javax.swing.JComponent;
import rebound.hci.graphics2d.Direction2D;


//Todo switch model from repaint-on-property-update to explicit-repaint
//Todo add support for more than one SelectionChangeListener
//Todo add support for multi-selection
public class Sedpane
extends JComponent
implements KeyListener
{
	public static final Color Default_Background = new Color(0, 0, 192);
	public static final Color Default_Foreground = new Color(64, 64, 255);
	
	
	//<Persistent state
	//Todo Make these properties mutable
	public static final int Thumbnail_Width = 48;
	public static final int Thumbnail_Height = 48;
	protected static final int Absurdly_Large = 256; //Maximum size of unused label cache
	public static final int XPadding = 10; //Padding around each image, measured in pixels
	public static final int YPadding = 10; //Padding around each image, measured in pixels
	public static final int Selected_XPadding = XPadding - 1; //Padding between thumbnail and 'selected' indicator
	public static final int Selected_YPadding = YPadding - 1; //Padding between thumbnail and 'selected' indicator
	public static final Color Default_Infomask_BG = new Color(240, 240, 240, 128);
	public static final Color Default_Infomask_FG = new Color(0, 0, 0, 255);
	public static final int IDragCursor_Height = Thumbnail_Height + YPadding;
	protected Font infomaskFont;
	
	
	protected boolean wrapSideways = true;
	
	protected SedpaneSelectionChangeListener selectionChangeListener = null;
	protected SedpaneImageMutationListener mutationListener = null;
	protected DnDErrorHandler dndErrorHandler = null;
	protected MutableImageStorage storage = null;
	
	protected DragSource odragSource;
	protected DragGestureRecognizer odragRecognizer;
	protected DropTarget idropTarget;
	protected DnDiplomat diplomat = null;
	//Persistent state>
	
	//<Transient State
	protected int matrixWidth = 0; //Columns of tiles, not width in pixels
	protected int matrixHeight = 0; //Rows of tiles, not height in pixels
	protected int matrixOffset = 0;
	
	protected int selected = -1;
	
	protected int infomask = -1;
	
	protected int idragCursorX = -1;
	protected int idragCursorY = -1;
	//Transient State>
	
	
	
	//<Init
	public Sedpane()
	{
		super();
		
		if (getBackground() == null)
			setBackground(Default_Background);
		if (getForeground() == null)
			setForeground(Default_Foreground);
		
		infomaskFont = new Font("SansSerif", Font.PLAIN, 12);
		
		registerEventListeners();
	}
	//Init>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<Persistent state controllers
	//Persistent state is state which can only be changed by these accessors
	
	//<Matrix
	public void add(BufferedImage image)
	{
		add(image, getImageCount());
	}
	
	public void add(BufferedImage original, int insertionPoint)
	{
		storage.add(original, insertionPoint);
		
		if (hasSelection() && getSelectedImageIndex() >= insertionPoint)
		{
			setSelected(getSelectedMatrixIndex()+1);
		}
		
		if (getImageMutationListener() != null)
			getImageMutationListener().imageAdded(insertionPoint);
		
		repaint();
	}
	
	public void delete(int index)
	{
		if (index >= 0 && index < getImageCount())
		{
			storage.delete(index);
			
			
			if (hasSelection() && index < getSelectedImageIndex())
			{
				setSelected(getSelectedMatrixIndex()-1);
			}
			
			
			int emptyRowCount = getEmptyTileCount() / getMatrixWidth();
			
			if (emptyRowCount > 0)
			{
				if (shiftRows(-1) != 0) //Only one bad empty row could be created by deleting one image
				{
					if (hasSelection())
					{
						setSelected(getSelectedMatrixIndex()+getMatrixWidth());
					}
				}
			}
			
			
			killInfomask();
			
			if (getImageMutationListener() != null)
				getImageMutationListener().imageDeleted(index);
			
			repaint();
		}
	}
	
	public void setImages(BufferedImage[] imgs)
	{
		clear();
		for (int i = 0; i < imgs.length; i++)
			add(imgs[i]);
	}
	
	/**
	 * This moves the frame identified by <code>old</code> to the position <code>fresh</code>.<br>
	 * @param old corresponds to an entire tile
	 * @param fresh corresponds to a position between tiles, the beginning (<code>0</code>), or the end (<code>length</code>)
	 */
	public void reorder(int old, int fresh)
	{
		if (fresh != old && fresh != old+1)
		{
			storage.reorder(old, fresh);
			
			if (hasSelection())
			{
				if (old == getSelectedMatrixIndex())
				{
					if (fresh < old)
						setSelected(fresh);
					else
						setSelected(fresh-1);
				}
				else if (old > getSelectedImageIndex() && fresh <= getSelectedImageIndex())
				{
					setSelected(getSelectedMatrixIndex()+1);
				}
				else if (old < getSelectedImageIndex() && fresh > getSelectedImageIndex())
				{
					setSelected(getSelectedMatrixIndex()-1);
				}
			}
			
			if (getImageMutationListener() != null)
				getImageMutationListener().imagesReordered(old, fresh);
			
			repaint();
		}
	}
	
	public void clear()
	{
		storage.clear();
		if (hasIDragCursor())
			setIDragCursor(0, 0);
		killInfomask();
		killSelection();
		
		if (getImageMutationListener() != null)
			getImageMutationListener().imagesCleared();
		
		repaint();
	}
	
	
	public int getImageCount()
	{
		return getImageStorage().getCount();
	}
	
	public boolean isEmpty()
	{
		return getImageCount() == 0;
	}
	//Images/Matrix>
	
	
	
	
	//<Simple properties
	public boolean isWrappingSideways()
	{
		return this.wrapSideways;
	}
	
	public void setWrapSideways(boolean wrapSideways)
	{
		this.wrapSideways = wrapSideways;
	}
	
	public Font getInfomaskFont()
	{
		return this.infomaskFont;
	}
	
	public void setInfomaskFont(Font infomaskFont)
	{
		this.infomaskFont = infomaskFont;
	}
	//Simple properties>
	
	
	
	
	
	
	
	
	
	//<Beans
	public DnDiplomat getDnDiplomat()
	{
		return this.diplomat;
	}
	
	public void setDnDiplomat(DnDiplomat diplomat)
	{
		this.diplomat = diplomat;
	}
	
	
	public SedpaneSelectionChangeListener getSelectionChangeListener()
	{
		return this.selectionChangeListener;
	}
	
	public void setSelectionChangeListener(SedpaneSelectionChangeListener notifee)
	{
		this.selectionChangeListener = notifee;
	}
	
	public SedpaneImageMutationListener getImageMutationListener()
	{
		return this.mutationListener;
	}
	
	public void setImageMutationListener(SedpaneImageMutationListener notifee)
	{
		this.mutationListener = notifee;
	}
	
	
	public DnDErrorHandler getDnDErrorHandler()
	{
		return this.dndErrorHandler;
	}
	
	public void setDnDErrorHandler(DnDErrorHandler dndErrorHandler)
	{
		this.dndErrorHandler = dndErrorHandler;
	}
	
	
	/**
	 * A read-only interface is returned because all mutations should go through the delegates in the owning Sedpane.<br>
	 */
	public ImageStorage getImageStorage()
	{
		return this.storage;
	}
	
	public void setImageStorage(MutableImageStorage storage)
	{
		this.storage = storage;
	}
	//Beans>
	//Persistent state controllers>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<Transient state controllers
	//Transient state is state that can be changed by AWT events (ie the user can change it in the background)
	
	//<Matrix
	/**
	 * The number of columns of tiles (even if there are no image thumbnails there now).<br>
	 */
	public int getMatrixWidth()
	{
		return matrixWidth;
	}
	
	/**
	 * The number of rows of tiles.<br>
	 */
	public int getMatrixHeight()
	{
		return matrixHeight;
	}
	
	public int getMatrixSize()
	{
		return matrixWidth * matrixHeight;
	}
	
	public int getMatrixOffset()
	{
		if (matrixOffset >= getImageCount())
			matrixOffset = getImageCount() - 1;
		if (matrixOffset < 0)
			matrixOffset = 0;
		
		return this.matrixOffset;
	}
	
	
	
	
	/**
	 * Resizes the matrix.<br>
	 * Note: This does not implicitly call <code>repaint()</code>
	 */
	protected void resizeMatrix(int newWidth, int newHeight)
	{
		if (newWidth < 0)
			newWidth = 0;
		if (newHeight < 0)
			newHeight = 0;
		
		matrixWidth = newWidth;
		matrixHeight = newHeight;
		
		//Preserve offset being in multiples of rows
		{
			if (getMatrixSize() > 0)
			{
				int newoff = getMatrixOffset();
				newoff = newoff / matrixWidth * matrixWidth;
				setMatrixOffset(newoff);
			}
		}
		
		//Preserve no bad empty rows
		{
			if (getMatrixSize() > 0)
			{
				int actual = shiftRows(-(getEmptyTileCount() / matrixWidth));
				
				if (hasSelection())
				{
					setSelected(getSelectedMatrixIndex() + (-actual * matrixWidth));
				}
			}
		}
	}
	
	
	/**
	 * Hard-scrolls up or down by shifting the offset in multiples of {@link #getMatrixWidth()}.<br>
	 * @param rows The number of rows to shift by
	 * @return Number of rows actually shifted
	 */
	public int shiftRows(int rows)
	{
		if (rows != 0 && getMatrixSize() > 0)
		{
			//Prevent scrolling away by ensuring there is never >= one empty row, unless of course there aren't enough images to fill the matrix at any offset
			int empty = Math.max(0, getMatrixSize() - (getImageCount() - (getMatrixOffset() + rows * getMatrixWidth())));
			rows -= (empty / getMatrixWidth());
			
			int newoff = getMatrixOffset() + rows * getMatrixWidth();
			
			if (newoff >= 0)
			{
				setMatrixOffset(newoff);
				
				return rows;
			}
			else
			{
				return 0;
			}
		}
		else
		{
			return 0;
		}
	}
	
	public void setMatrixOffset(int newoff)
	{
		if (getImageCount() == 0)
		{
			if (matrixOffset != 0)
			{
				matrixOffset = 0;
				repaint();
			}
		}
		else
		{
			if (newoff < 0)
				newoff = 0;
			else if (newoff >= getImageCount())
				newoff = getImageCount()-1;
			
			
			/*
			 * ImageCount = 18
			 * MatrixSize = 16
			 * 
			 * 
			 * 
			 * Offset = 0
			 * 			---------
			 * 			|* * * *|
			 * 			|* * * *|
			 * 			|* * * *|
			 * 			|* * * *|
			 * 			---------
			 * 			 * *    
			 * 			
			 * 			
			 * 			
			 * Offset = 4
			 * 			 * * * *
			 * 			---------
			 * 			|* * * *|
			 * 			|* * * *|
			 * 			|* * * *|
			 * 			|* *    |
			 * 			---------
			 * 			
			 * 			
			 * 
			 * 
			 * Offset = 5
			 * 			 * * * *
			 * 			 *
			 * 			---------
			 * 			|* * * *|
			 * 			|* * * *|
			 * 			|* * * *|
			 * 			|*      |
			 * 			---------
			 * 			
			 * 			
			 * 			
			 * Offset = 8
			 * 			 * * * *
			 * 			 * * * *
			 * 			---------
			 * 			|* * * *|
			 * 			|* * * *|
			 * 			|* *    |
			 * 			|       |
			 * 			---------
			 */
			
			if (matrixOffset != newoff)
			{
				matrixOffset = newoff;
				repaint();
			}
		}
	}
	//Matrix>
	
	
	
	
	
	
	
	//<Selection
	/**
	 * This returns the index of the selection in the matrix's coordinate space.<br>
	 * @return The matrix index of the selection, or -1 if there is no selection.
	 */
	public int getSelectedMatrixIndex()
	{
		if (selected == -1)
			return -1;
		else if (selected < -1)
			selected = -1;
		else
		{
			if (selected >= getMatrixSize())
				selected = getMatrixSize() - 1;
			else if (selected + getMatrixOffset() >= getImageCount())
				selected = getImageCount() - 1 - getMatrixOffset();
		}
		
		return selected;
	}
	
	public boolean hasSelection()
	{
		return getSelectedMatrixIndex() != -1;
	}
	
	/**
	 * This returns an index in the {@link #getImageStorage() ImageStorage}'s coordinate space.<br>
	 * @return The image index of the selection (matrix-index + {@link #getMatrixOffset()}), or -1 if there is no selection.
	 */
	public int getSelectedImageIndex()
	{
		if (hasSelection())
			return getMatrixOffset() + getSelectedMatrixIndex();
		else
			return -1;
	}
	
	public int getSelectedColumn()
	{
		if (!hasSelection())
			return -1;
		else
			return getSelectedMatrixIndex() % getMatrixWidth();
	}
	
	public int getSelectedRow()
	{
		if (!hasSelection())
			return -1;
		else
			return getSelectedMatrixIndex() / getMatrixWidth();
	}
	
	
	
	
	/**
	 * This performs the act of 'moving' the selection.<br>
	 * This means that if an invalid (or unfilled) destination tile is selected, certain actions may take place:<br>
	 * <br>
	 * For a destination off the left or right side of the matrix, wrapping may occur, assuming it is {@link #isWrappingSideways() enabled}.<br>
	 * For a destination off the top or bottom side of the matrix, the offset may shift (ie hard-scrolling, as opposed to smooth scrolling).<br>
	 * @param dir The direction in which to move the selection 1 tile.
	 */
	public void moveSelected(Direction2D dir) //- for left/up, + for down/right
	{
		if (getMatrixSize() > 0)
		{
			if (hasSelection())
			{
				int cx = getSelectedColumn();
				int cy = getSelectedRow();
				
				int dx = cx;
				int dy = cy;
				
				
				if (dir == Down)
				{
					if (cy == getMatrixHeight()-1)
					{
						shiftRows(1);
					}
					else
					{
						dy++;
					}
					
					
					if (!isMatrixTileFilled(dx, dy))
					{
						dy--;
					}
				}
				
				
				else if (dir == Up)
				{
					if (cy == 0)
					{
						shiftRows(-1);
						//Moving off the top validation is handled by shiftRows
					}
					else
					{
						dy--;
						//We don't have to verify it being a filled tile, because moving up from a filled tile will always move to a filled tile
					}
				}
				
				
				
				else if (dir == Right)
				{
					if (cx == getMatrixWidth()-1)
					{
						if (isWrappingSideways())
						{
							if (cy == getMatrixHeight()-1)
							{
								if (shiftRows(1) != 0) //If it actually moved
								{
									dx = 0;
								}
							}
							else
							{
								if (isMatrixTileFilled(0, dy+1))
								{
									dx = 0;
									dy++;
								}
							}
						}
					}
					else
					{
						if (isMatrixTileFilled(dx+1, dy))
						{
							dx++;
						}
					}
				}
				
				
				else if (dir == Left)
				{
					if (cx == 0)
					{
						if (isWrappingSideways())
						{
							if (cy == 0)
							{
								if (shiftRows(-1) != 0)
								{
									dx = getMatrixWidth()-1;
								}
							}
							else
							{
								dx = getMatrixWidth()-1;
								dy--;
							}
						}
					}
					else
					{
						dx--;
					}
				}
				
				
				
				setSelected(dx, dy);
			}
		}
	}
	
	
	/**
	 * Sets the selection within the matrix.<br>
	 * Column and row coordinates are simply truncated if out of bounds.<br>
	 */
	public void setSelected(int col, int row)
	{
		if (getMatrixSize() > 0)
		{
			if (col < 0 || row < 0)
			{
				killSelection();
			}
			else
			{
				//Truncate
				if (col >= getMatrixWidth())
					col = getMatrixWidth() - 1;
				if (row >= getMatrixHeight())
					row = getMatrixHeight() - 1;
				
				int index = col + row * getMatrixWidth();
				
				if (index + getMatrixOffset() >= getImageCount())
					index = getImageCount() - 1 - getMatrixOffset();
				
				setSelected(index);
			}
		}
	}
	
	public void killSelection()
	{
		if (selected != -1)
		{
			selected = -1;
			repaint();
		}
	}
	
	/**
	 * Sets the selection with a matrix index (not an image index).<br>
	 * @param index The index in the matrix, or -1 to remove the selection
	 */
	public void setSelected(int index)
	{
		if (index < -1)
			index = -1;
		else if (index != -1)
		{
			//Truncate it to the matrix
			if (index >= getMatrixSize())
				index = getMatrixSize() - 1;
			
			//Truncate it to the available images
			else if (getMatrixOffset() + index >= getImageCount())
				index = getImageCount() - getMatrixOffset() - 1;
		}
		
		if (index != selected)
		{
			//Notify event listeners
			if (selectionChangeListener != null)
				selectionChangeListener.selectedChange(selected, index);
			
			selected = index;
			
			//Update
			repaint();
		}
	}
	//Selection>
	
	
	
	
	
	
	//<Infomask
	public int getInfomaskMatrixIndex()
	{
		if (infomask < -1)
			infomask = -1;
		else if (infomask + getMatrixOffset() >= getImageCount())
			infomask = -1;
		
		return infomask;
	}
	
	public boolean isInfomaskVisible()
	{
		return getInfomaskMatrixIndex() != -1;
	}
	
	public void setInfomask(int matrixIndex)
	{
		if (matrixIndex < -1)
			matrixIndex = -1;
		else if (matrixIndex + getMatrixOffset() >= getImageCount())
			matrixIndex = -1;
		
		if (infomask != matrixIndex)
		{
			infomask = matrixIndex;
			repaint();
		}
	}
	
	public void killInfomask()
	{
		if (isInfomaskVisible())
		{
			infomask = -1;
			repaint();
		}
	}
	//Infomask>
	
	
	
	
	
	//<IDrag Cursor
	/**
	 * The column of the drag n drop cursor, which shows the current drop target for dnd operations initiated elsewhere.<br>
	 * Note that since the idragCursor exists between elements of the matrix, it has one more potential x coordinate than tiles in the matrix.<br>
	 * Therefore while tiles end at <code>{@link #getMatrixWidth()} - 1</code>, this ends at <code>{@link #getMatrixWidth()}</code>.<br>
	 */
	public int getIDragCursorColumn()
	{
		if (idragCursorX != -1 && idragCursorY != -1)
		{
			validateIDragCursor();
			return idragCursorX;
		}
		else
		{
			return -1;
		}
	}
	
	public int getIDragCursorRow()
	{
		if (idragCursorX != -1 && idragCursorY != -1)
		{
			validateIDragCursor();
			return idragCursorY;
		}
		else
		{
			return -1;
		}
	}
	
	public boolean hasIDragCursor()
	{
		validateIDragCursor();
		return idragCursorX != -1 && idragCursorY != -1;
	}
	
	/**
	 * Gets the logical matrix index that a drop would take place in.<br>
	 * It represents the insertion point to the left of the tile at the given index.<br>
	 * Note that this can be 0, <code>length</code> or anywhere in between  where <code>length</code> is either the {@link #getMatrixSize() matrix size} or however many images are currently in the matrix if it is not full.<br>
	 * The column and row are ambiguous, but this is canonical.<br>
	 * <br>
	 * @return The current position to insert the drug item if and when it is dropped   as a matrix index.<br>
	 */
	public int getIDragCursorLogicalIndex()
	{
		return getIDragCursorRow() * getMatrixWidth() + getIDragCursorColumn();
	}
	
	public void killIDragCursor()
	{
		setIDragCursor(-1, -1);
	}
	
	public void setIDragCursorToEnd()
	{
		setIDragCursor(getMatrixWidth(), getMatrixHeight()-1);
	}
	
	public void setIDragCursor(int col, int row)
	{
		if (col < 0 || row < 0)
		{
			boolean had = hasIDragCursor();
			idragCursorX = -1;
			idragCursorY = -1;
			if (had)
				repaint();
		}
		else
		{
			int oldx = idragCursorX;
			int oldy = idragCursorY;
			idragCursorX = col;
			idragCursorY = row;
			validateIDragCursor();
			
			if (oldx == -1 || oldy == -1)
			{
				if (idragCursorX != -1 && idragCursorY != -1)
					repaint();
			}
			else
			{
				if (idragCursorX == -1 || idragCursorY == -1)
					repaint();
			}
		}
	}
	
	
	//This assumes neither are -1
	protected void validateIDragCursor()
	{
		if (idragCursorX == -1 || idragCursorY == -1)
		{
			idragCursorX = -1;
			idragCursorY = -1;
		}
		else
		{
			if (getImageCount() == 0 || getMatrixSize() == 0)
			{
				idragCursorX = 0;
				idragCursorY = 0;
			}
			else
			{
				int x = idragCursorX;
				int y = idragCursorY;
				
				if (x > getMatrixWidth())
					x = getMatrixWidth();
				if (y >= getMatrixHeight())
					y = getMatrixHeight()-1;
				
				int lastMatrixImage = getImageCount() - getMatrixOffset() - 1;
				if (y * getMatrixWidth() + x > lastMatrixImage+1)
				{
					x = lastMatrixImage % getMatrixWidth() + 1;
					y = lastMatrixImage / getMatrixWidth();
				}
				
				idragCursorX = x;
				idragCursorY = y;
			}
		}
	}
	//IDrag Cursor>
	//Transient state controllers>
	
	
	
	
	
	
	
	//<Utilities
	/**
	 * Calculates the matrix index of the tile which contains this point.<br>
	 * Note: A tile contains a point only if the point lies inside the thumbnail, not the padding.<br>
	 * 
	 * Note: This does not return a filled matrix index, so you may want to run it through {@link #getFilledMatrixIndex(int)} to convert it to such.<br>
	 * 
	 * @return A raw matrix index or -1 if the point does not lie inside any tile's thumbnail boundaries.
	 */
	public int getTileIndex(int x, int y)
	{
		//Calculate the index of the tile by the x and y inside this component
		
		int col = x / (Thumbnail_Width + XPadding*2);
		int relx = x % (Thumbnail_Width + XPadding*2);
		int row = y / (Thumbnail_Height + YPadding*2);
		int rely = y % (Thumbnail_Height + YPadding*2);
		
		
		//Validate the row & col
		if (col < 0 || row < 0 || col >= getMatrixWidth() || row >= getMatrixHeight())
			return -1;
		
		
		//Validate that they clicked in the thumbnail, not the padding
		if
		(
			(relx >= XPadding && relx < XPadding+Thumbnail_Width)
			&&
			(rely >= YPadding && rely < YPadding+Thumbnail_Height)
			)
		{
			return col + row*getMatrixWidth();
		}
		else
		{
			return -1;
		}
	}
	
	
	/**
	 * Calculates the number of empty tiles.<br>
	 * You can use this to find the number of filled tiles easily.<br>
	 */
	public int getEmptyTileCount()
	{
		return getMatrixSize() - getLastFilledTileIndex() - 1;
	}
	
	
	
	/**
	 * This simply performs validation on a matrixIndex, converting it to -1 (null) if invalid, or not filled.<br>
	 * A "filled matrix index" is a matrix index of a tile which has an image.<br>
	 * @return A filled matrix index of a tile that is not empty, or -1
	 */
	public int getFilledMatrixIndex(int matrixIndex)
	{
		if (matrixIndex < 0)
			return -1;
		else if (matrixIndex >= getMatrixSize())
			return -1;
		else if (matrixIndex + getMatrixOffset() >= getImageCount())
			return -1;
		else
			return matrixIndex;
	}
	
	public boolean isMatrixIndexFilled(int matrixIndex)
	{
		return getFilledMatrixIndex(matrixIndex) != -1;
	}
	
	public boolean isMatrixTileFilled(int col, int row)
	{
		return getFilledMatrixIndex(col + row*getMatrixWidth()) != -1;
	}
	
	
	
	/**
	 * The last filled tile (if it exists) is the tile in the matrix with the highest index that is filled (ie it does not represent an image).<br>
	 * @return The matrix index of the last filled tile or -1 if there are no filled tiles, or any tiles at all.
	 */
	public int getLastFilledTileIndex()
	{
		return Math.min(getImageCount() - 1 - getMatrixOffset(), getMatrixSize()-1);
	}
	
	public int getLastFilledColumn()
	{
		return getLastFilledTileIndex() % getMatrixWidth();
	}
	
	public int getLastFilledRow()
	{
		return getLastFilledTileIndex() / getMatrixWidth();
	}
	//Utilities>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<Graphics
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g); //Background
		
		Color originalColor = g.getColor();
		Font originalFont = g.getFont();
		
		paintThumbnails((Graphics2D)g);
		paintSelectionBox((Graphics2D)g);
		paintInfomask((Graphics2D)g);
		paintIDragCursor((Graphics2D)g);
		
		g.setColor(originalColor);
		g.setFont(originalFont);
	}
	
	
	
	
	
	protected void paintThumbnails(Graphics2D g)
	{
		if (getMatrixSize() > 0)
		{
			for (int row = 0; row < getMatrixHeight(); row++)
			{
				for (int col = 0; col < getMatrixWidth(); col++)
				{
					int x = col*(Thumbnail_Width+XPadding*2) + XPadding;
					int y = row*(Thumbnail_Height+YPadding*2) + YPadding;
					
					Image thumb = null;
					{
						int index = getMatrixOffset() + (row*getMatrixWidth()) + col;
						if (index >= getImageCount())
							return;
						thumb = getImageStorage().getThumbnail(index);
					}
					
					int verticalPadding = 0;
					int horizontalPadding = 0;
					{
						int thumbWidth = getWidthNow(thumb);
						int thumbHeight = getHeightNow(thumb);
						
						horizontalPadding = (Thumbnail_Width - thumbWidth) / 2;
						verticalPadding = (Thumbnail_Height - thumbHeight) / 2;
					}
					
					//Todo make the next paint() wait until these are all finished, but allow them to happen in parallel
					g.drawImage(thumb, x+horizontalPadding, y+verticalPadding, null);
				}
			}
		}
	}
	
	
	
	
	
	protected void paintSelectionBox(Graphics2D g)
	{
		if (hasSelection())
		{
			//Paint selected thumb's border
			int row = getSelectedRow();
			int col = getSelectedColumn();
			
			int left = 0;
			int top = 0;
			int right = 0;
			int bottom = 0;
			{
				left = col * (Thumbnail_Width + XPadding*2) + XPadding - Selected_XPadding;
				top = row * (Thumbnail_Height + YPadding*2) + YPadding - Selected_YPadding;
				right = left + Selected_XPadding + Thumbnail_Width + Selected_XPadding;
				bottom = top + Selected_YPadding + Thumbnail_Height + Selected_YPadding;
				
				left--;
				top--;
			}
			
			
			//Paint the selection box
			{
				Color dark = getForeground().darker();
				Color light = getForeground().brighter();
				
				
				g.setColor(light);
				g.drawLine(left, top, left, bottom); //L
				
				g.setColor(light);
				g.drawLine(left, top, right, top); //T
				
				g.setColor(dark);
				g.drawLine(right, top, right, bottom); //R
				
				g.setColor(dark);
				g.drawLine(left, bottom, right, bottom); //B
			}
		}
	}
	
	
	
	
	protected void paintInfomask(Graphics2D g)
	{
		/*
		 * This is example text displayed in the infomask:
		 * 		"[ 13 ]"
		 * 		"W9021"
		 * 		"H8993"
		 * 
		 * The lines should each be individually horizontally centered.
		 * The text as a whole should be vertically centered within the infomask, even if it doesn't vertically fit with the given font.
		 */
		
		if (isInfomaskVisible() && getMatrixSize() > 0)
		{
			Font font = getInfomaskFont();
			
			String[] lines = null;
			{
				int imageIndex = getInfomaskMatrixIndex()+getMatrixOffset();
				
				lines = new String[3];
				lines[0] = "[ "+(imageIndex)+" ]";
				lines[1] = "W"+getImageStorage().getWidth(imageIndex);
				lines[2] = "H"+getImageStorage().getHeight(imageIndex);
			}
			
			TextLayout[] layouts = null;
			{
				layouts = new TextLayout[lines.length];
				for (int i = 0; i < lines.length; i++)
					layouts[i] = new TextLayout(lines[i], font, g.getFontRenderContext());
			}
			
			
			
			int verticalPadding = 0;
			{
				int totalHeight = 0;
				{
					for (int i = 0; i < lines.length; i++)
					{
						TextLayout layout = layouts[i];
						totalHeight += layout.getAscent() + layout.getDescent();
						if (i < layouts.length-1)
							totalHeight += layout.getLeading();
					}
				}
				
				verticalPadding = (Thumbnail_Height - totalHeight) / 2;
			}
			
			
			int infomaskX = (infomask % getMatrixWidth()) * (Thumbnail_Width + XPadding*2) + XPadding;
			int infomaskY = (infomask / getMatrixWidth()) * (Thumbnail_Height + YPadding*2) + YPadding;
			
			
			
			
			//Paint the mask
			{
				g.setColor(Default_Infomask_BG);
				g.fillRect(infomaskX, infomaskY, Thumbnail_Width, Thumbnail_Height);
			}
			
			
			//Paint the info
			{
				int y = infomaskY+verticalPadding;
				
				g.setColor(Default_Infomask_FG);
				g.setFont(font);
				
				for (int i = 0; i < lines.length; i++)
				{
					int horizontalPadding = (int)((Thumbnail_Width - layouts[i].getVisibleAdvance()) / 2);
					
					y += layouts[i].getAscent();
					
					g.drawString(lines[i], infomaskX+horizontalPadding, y);
					
					y += layouts[i].getDescent();
					y += layouts[i].getLeading();
				}
			}
		}
	}
	
	
	
	
	
	protected void paintIDragCursor(Graphics2D g)
	{
		if (hasIDragCursor())
		{
			int x = getIDragCursorColumn() * (Thumbnail_Width + XPadding*2);
			int y = getIDragCursorRow() * (Thumbnail_Height + YPadding*2);
			
			int verticalPadding = ((Thumbnail_Height + YPadding*2) - IDragCursor_Height) / 2;
			
			Color baseColor = null;
			{
				baseColor = getForeground();
			}
			
			
			if (x > 0)
			{
				g.setColor(baseColor.brighter());
				int y1 = y+verticalPadding;
				int y2 = y+verticalPadding+IDragCursor_Height-1;
				g.drawLine(x-1, y1, x-1, y2);
			}
			
			if (x < getWidth())
			{
				g.setColor(baseColor.darker());
				int y1 = y+verticalPadding;
				int y2 = y+verticalPadding+IDragCursor_Height-1;
				g.drawLine(x, y1, x, y2);
			}
		}
	}
	//Graphics>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<Events
	
	protected void registerEventListeners()
	{
		//<Component
		addComponentListener
		(
			new ComponentListener()
			{
				public void componentResized(ComponentEvent e)
				{
					resized(getWidth(), getHeight());
				}
				
				public void componentShown(ComponentEvent e){}
				public void componentMoved(ComponentEvent e){}
				public void componentHidden(ComponentEvent e){}
			}
			);
		//Component>
		
		//<DnD
		odragSource = new DragSource();
		odragRecognizer = odragSource.createDefaultDragGestureRecognizer
			(
				this,
				DnDConstants.ACTION_COPY,
				new DragGestureListener()
				{
					public void dragGestureRecognized(DragGestureEvent dge)
					{
						odragBegin(dge, dge.getDragOrigin().x, dge.getDragOrigin().y);
					}
				}
				);
		
		idropTarget = new DropTarget
			(
				this,
				new DropTargetListener()
				{
					public void dragEnter(DropTargetDragEvent dtde)
					{
						idragUpdate(true, dtde.getLocation().x, dtde.getLocation().y);
					}
					
					public void dragOver(DropTargetDragEvent dtde)
					{
						idragUpdate(false, dtde.getLocation().x, dtde.getLocation().y);
					}
					
					public void drop(DropTargetDropEvent dtde)
					{
						idragDropped(dtde);
					}
					
					public void dragExit(DropTargetEvent dte)
					{
						idragCancelled();
					}
					
					public void dropActionChanged(DropTargetDragEvent dtde) {}
				}
				);
		//DnD>
		
		
		
		
		//<Mouse
		addMouseListener
		(
			new MouseListener()
			{
				public void mouseExited(MouseEvent e)
				{
					mouseLeave();
					e.consume();
				}
				
				public void mouseEntered(MouseEvent e)
				{
					mouseUpdate(e.getX(), e.getY(), e.getModifiersEx());
					e.consume();
				}
				
				public void mouseReleased(MouseEvent e)
				{
					Sedpane.this.mouseClicked(e.getX(), e.getY(), e.getButton(), e.getModifiersEx());
					e.consume();
				}
				
				public void mouseClicked(MouseEvent e){}
				public void mousePressed(MouseEvent e){}
			}
			);
		
		addMouseMotionListener
		(
			new MouseMotionListener()
			{
				public void mouseMoved(MouseEvent e)
				{
					mouseUpdate(e.getX(), e.getY(), e.getModifiersEx());
					e.consume();
				}
				
				public void mouseDragged(MouseEvent e)
				{
				}
			}
			);
		//Mouse>
		
		
		//<Keyboard
		addKeyListener(this);
		//Keyboard>
	}
	
	
	
	
	
	//<Resize
	protected void resized(int newWidth, int newHeight)
	{
		killIDragCursor();
		killInfomask();
		resizeMatrix
		(
			newWidth / (Thumbnail_Width+XPadding*2),
			newHeight / (Thumbnail_Height+YPadding*2)
			);
		repaint();
	}
	//Resize>
	
	
	
	
	//<Drag and Drop
	private DataFlavor uniqueDataFlavor;
	public DataFlavor getUniqueInternalReorderFlavor()
	{
		if (uniqueDataFlavor == null)
		{
			String uniqueExtension = null;
			{
				int objectCode = 0;
				String vmCode = null;
				{
					objectCode = System.identityHashCode(this);
					vmCode = ManagementFactory.getRuntimeMXBean().getName();
				}
				
				
				//Escape it so that it fits in a MIME Type
				{
					StringBuilder buff = new StringBuilder();
					for (char c : vmCode.toCharArray())
					{
						if
						(
							//Only strict ASCII Alphanum
							(c >= 0x30 && c <= 0x39)
							||
							(c >= 0x41 && c <= 0x5A)
							||
							(c >= 0x61 && c <= 0x7A)
							)
						{
							buff.append(c);
						}
						else
						{
							buff.append(Integer.toHexString(c).toUpperCase());
						}
					}
					
					vmCode = buff.toString();
				}
				
				uniqueExtension = objectCode + "-" + vmCode;
			}
			
			uniqueDataFlavor = new DataFlavor("application/x-sedpane-reorder-"+uniqueExtension+"; class=java.lang.Integer", "Internal Sedpane Reorder");
		}
		return uniqueDataFlavor;
	}
	
	
	
	protected class Sedragdata
	implements Transferable
	{
		protected int index;
		protected BufferedImage image;
		protected DataFlavor[] flavors;
		protected Hashtable<DataFlavor, Object> cache;
		
		public Sedragdata(int i, BufferedImage img)
		{
			super();
			index = i;
			image = img;
			
			
			DataFlavor[] dipd = getDnDiplomat().getSupportedDragFlavors();
			
			//Append the internal reorder flavor
			flavors = new DataFlavor[dipd.length+1];
			System.arraycopy(dipd, 0, flavors, 0, dipd.length);
			flavors[dipd.length] = getUniqueInternalReorderFlavor();
			
			if (getDnDiplomat().isDragDataCachingAllowed())
				cache = new Hashtable<DataFlavor, Object>();
		}
		
		
		public DataFlavor[] getTransferDataFlavors()
		{
			return flavors;
		}
		
		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			for (DataFlavor f : flavors)
				if (f.equals(flavor))
					return true;
			return false;
		}
		
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
		{
			//Basically delegate to the diplomat
			//exceptions are	Special internal reorder	and	caching.
			
			if (getUniqueInternalReorderFlavor().equals(flavor))
				return index;
			else
			{
				if (cache != null)
				{
					if (cache.containsKey(flavor))
						return cache.get(flavor);
					else
					{
						Object d = getDnDiplomat().getDragData(flavor, index, image);
						cache.put(flavor, d);
						return d;
					}
				}
				else
				{
					return getDnDiplomat().getDragData(flavor, index, image);
				}
			}
		}
	}
	
	
	
	
	
	//<Drag side (output-drag)
	//This is called when the user starts dragging a tile
	protected synchronized void odragBegin(DragGestureEvent e, int x, int y)
	{
		int index = getFilledMatrixIndex(getTileIndex(x, y));
		
		if (index != -1)
		{
			index += getMatrixOffset();
			//Todo make this image translucent
			Image img = getImageStorage().getThumbnail(index);
			Transferable t = new Sedragdata(index, storage.load(index));
			odragSource.startDrag
			(
				e,
				null,
				img,
				new Point(-getWidthNow(img)/2, -getHeightNow(img)/2),
				t,
				new DragSourceListener()
				{
					public void dragDropEnd(DragSourceDropEvent dsde)
					{
						odragEnd();
					}
					
					public void dropActionChanged(DragSourceDragEvent dsde) {}
					public void dragOver(DragSourceDragEvent dsde) {}
					public void dragExit(DragSourceEvent dse) {}
					public void dragEnter(DragSourceDragEvent dsde) {}
				}
				);
		}
	}
	
	//Called when the item drug from us has been dropped and finished
	protected void odragEnd()
	{
		killInfomask();
		getDnDiplomat().freeDragResources();
	}
	//Drag side (output-drag)>
	
	
	
	
	//<Drop side (input-drag)
	protected void idragUpdate(boolean fresh, int x, int y)
	{
		//		System.out.println("new:"+fresh+"  ("+x+","+y+")");
		
		//Set the idrag cursor to be under the given point
		
		int col = 0;
		int row = 0;
		{
			int tilewidth = Thumbnail_Width + XPadding*2;
			col = (x+(tilewidth/2)) / tilewidth;
			row = y / (Thumbnail_Height+YPadding*2);
		}
		
		setIDragCursor(col, row);
		repaint(); //Todo why do I have to have this explicitly?
	}
	
	/**
	 * IE: Mouse left component
	 */
	protected void idragCancelled()
	{
		//		System.out.println("Cancelled");
		killIDragCursor();
		killInfomask();
	}
	
	
	
	
	protected synchronized void idragDropped(DropTargetDropEvent e)
	{
		//		System.out.println("Drop. Mouse at ("+e.getLocation().getX()+","+e.getLocation().getY()+")");
		
		int dc = getIDragCursorLogicalIndex();
		killIDragCursor();
		killInfomask();
		
		Transferable t = e.getTransferable();
		
		
		//Check for dataflavor meaning it was dragged from here
		if (t.isDataFlavorSupported(getUniqueInternalReorderFlavor()))
		{
			//Since it was from this very window, we dont need to do the whole export shebang.
			e.acceptDrop(e.getDropAction());
			
			int index = -1;
			
			try
			{
				index = (Integer)t.getTransferData(getUniqueInternalReorderFlavor());
				reorder(index, dc);
			}
			catch (UnsupportedFlavorException exc)
			{
			}
			catch (IOException exc)
			{
			}
			
			e.dropComplete(index != -1);
			return;
		}
		
		if (diplomat == null)
			e.rejectDrop();
		else
		{
			for (DataFlavor d : diplomat.getSupportedDropFlavors())
			{
				if (t.isDataFlavorSupported(d))
				{
					e.acceptDrop(e.getDropAction());
					
					try
					{
						diplomat.drop(t.getTransferData(d), d, dc+getMatrixOffset());
						
						e.dropComplete(true);
					}
					catch (UnsupportedFlavorException exc)
					{
						e.dropComplete(false);
						if (dndErrorHandler == null)
							dndErrorHandler.errorInDrop(exc);
					}
					catch (IOException exc)
					{
						e.dropComplete(false);
						if (dndErrorHandler == null)
							dndErrorHandler.errorInDrop(exc);
					}
					
					return;
				}
				
			}
			e.rejectDrop();
		}
		
		
		e.dropComplete(false);
	}
	//Drop side (input-drag)>
	//Drag and Drop>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<Mouse control
	protected void mouseClicked(int x, int y, int buttons, int modifiers)
	{
		//This includes -1, meaning no valid tile clicked
		int tileIndex = getFilledMatrixIndex(getTileIndex(x, y));
		setSelected(tileIndex);
	}
	
	protected void mouseUpdate(int x, int y, int modifiers)
	{
		int tileIndex = getFilledMatrixIndex(getTileIndex(x, y));
		setInfomask(tileIndex);
	}
	
	/**
	 * Meaning: The mouse left the whole Sedpane. (not left a tile)
	 */
	protected void mouseLeave()
	{
		killInfomask();
	}
	//Mouse control>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<Keyboard control
	public synchronized void keyPressed(KeyEvent e)
	{
		boolean actionDown = (e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0;
		
		Direction2D dir = null;
		
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_UP:
			{
				if (actionDown)
					shiftRows(-1);
				else
					dir = Up;
				break;
			}
			
			case KeyEvent.VK_DOWN:
			{
				if (actionDown)
					shiftRows(1);
				else
					dir = Down;
				break;
			}
			
			case KeyEvent.VK_LEFT:
			{
				dir = Left;
				break;
			}
			
			case KeyEvent.VK_RIGHT:
			{
				dir = Right;
				break;
			}
			
			case KeyEvent.VK_TAB:
			{
				dir = Right;
				break;
			}
			
			case KeyEvent.VK_DELETE:
			case KeyEvent.VK_BACK_SPACE:
			{
				if (hasSelection())
				{
					delete(getSelectedImageIndex());
				}
				break;
			}
			
			default:
			{
				//Skip e.consume()
				return;
			}
		}
		e.consume();
		
		
		if (dir != null)
		{
			if (hasSelection())
			{
				moveSelected(dir);
			}
			else
			{
				setSelected(0);
			}
		}
	}
	
	
	//Unused
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}
	//Keyboard control>
	
	
	//Events>
}
