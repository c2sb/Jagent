/*
 * Created on Aug 15, 2009
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import rebound.exceptions.ImpossibleException;
import rebound.text.StringUtilities;

//This has some DnD logic
public class DropData
{
	public static final DataFlavor URILIST_FLAVOR;
	static
	{
		try
		{
			URILIST_FLAVOR = new DataFlavor("text/uri-list; class=java.lang.String");
		}
		catch (ClassNotFoundException exc)
		{
			throw new ImpossibleException("java.lang.String not found!!", exc);
		}
	}
	
	
	
	
	public static List<File> getFileList(DropTargetDropEvent dtde) throws IOException
	{
		Transferable t = dtde.getTransferable();
		
		if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
		{
			dtde.acceptDrop(dtde.getDropAction());
			try
			{
				List<File> data = (java.util.List)t.getTransferData(DataFlavor.javaFileListFlavor);
				
				dtde.dropComplete(true);
				
				return data;
			}
			catch (UnsupportedFlavorException exc)
			{
				throw new ImpossibleException(exc); //We just checked that the flavor would work!
			}
		}
		else if (t.isDataFlavorSupported(URILIST_FLAVOR))
		{
			dtde.acceptDrop(dtde.getDropAction());
			try
			{
				String data = (String)t.getTransferData(URILIST_FLAVOR);
				
				List<File> files = parseURIList(data);
				
				dtde.dropComplete(true);
				
				return files;
			}
			catch (UnsupportedFlavorException exc)
			{
				throw new ImpossibleException(exc); //We just checked that the flavor would work!
			}
		}
		else
		{
			System.out.println("Unsupported drop object; with the flavors:");
			
			for (DataFlavor f : t.getTransferDataFlavors())
				System.out.println("\t"+f.getMimeType());
			
			return null;
		}
	}
	
	
	
	public static List<File> parseURIList(String data)
	{
		data = StringUtilities.getUniversalNewlines(data);
		
		String[] uris = StringUtilities.split(data, '\n');
		
		List<File> files = new ArrayList<>();
		{
			for (String rawuri : uris)
			{
				if (!rawuri.equals(""))
				{
					URI uri = URI.create(rawuri);
					if ("file".equalsIgnoreCase(uri.getScheme()))
						files.add(new File(uri));
				}
			}
		}
		
		return files;
	}
}
