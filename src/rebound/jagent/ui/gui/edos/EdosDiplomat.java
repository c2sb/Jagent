/*
 * Created on May 20, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.ui.gui.edos;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JOptionPane;
import rebound.jagent.ui.gui.DropData;
import rebound.jagent.ui.gui.edos.sedpane.DnDiplomat;
import rebound.util.Compass;

public class EdosDiplomat
implements DnDiplomat
{
	public static final DataFlavor BIMAGEFLAVOR = new DataFlavor("image/x-java-image; class=java.awt.image.BufferedImage", null);
	public static final DataFlavor[] SUPPORTED_DROPFLAVORS = {BIMAGEFLAVOR, DataFlavor.javaFileListFlavor, DropData.URILIST_FLAVOR};
	public static final DataFlavor[] SUPPORTED_DRAGFLAVORS = {BIMAGEFLAVOR, DataFlavor.imageFlavor, DataFlavor.javaFileListFlavor, DropData.URILIST_FLAVOR};
	protected static final String[] TEMP_DRAGIMAGE_STORAGE_PATHS = {"temp", "tmp", "/tmp", "C:\\Windows\\Temp", "."};
	
	protected EdosWindow edos;
	protected String dragExt = ".png";
	protected String dragMIME = "image/png";
	
	public EdosDiplomat(EdosWindow e)
	{
		super();
		edos = e;
	}
	
	
	public DataFlavor[] getSupportedDropFlavors()
	{
		return SUPPORTED_DROPFLAVORS;
	}
	
	
	
	
	
	public void drop(Object data, DataFlavor flav, int dcursor)
	{
		if (flav == DataFlavor.javaFileListFlavor || flav == DropData.URILIST_FLAVOR)
		{
			List<File> l;
			if (flav == DropData.URILIST_FLAVOR)
				l = DropData.parseURIList((String)data);
			else
				l = (List<File>)data;

			try
			{
				for (File f : l)
					handleDropFile(f, dcursor);
			}
			catch (IOException exc)
			{
				JOptionPane.showMessageDialog(null, "Some images didn't get loaded, here's the first error message: "+exc.getMessage(), "Import image error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (flav == BIMAGEFLAVOR)
			edos.importImage((BufferedImage)data, dcursor);
	}
	
	
	protected void handleDropFile(File f, int i) throws IOException
	{
		f = f.getAbsoluteFile();
		String lname = f.getName().toLowerCase();
		
		if (lname.endsWith(".c16") || lname.endsWith(".s16") || lname.endsWith(".blk"))
			edos.open(f);
		else
			edos.importImage(f, i);
	}
	
	
	
	
	
	
	
	
	public DataFlavor[] getSupportedDragFlavors()
	{
		return SUPPORTED_DRAGFLAVORS;
	}
	
	public boolean isDragDataCachingAllowed()
	{
		return true;
	}
	
	public Object getDragData(DataFlavor flav, int index, BufferedImage img) throws UnsupportedFlavorException, IOException
	{
		if (BIMAGEFLAVOR.equals(flav))
			return img;
		else if (DataFlavor.imageFlavor.equals(flav))
			return img;
		else if (DataFlavor.javaFileListFlavor.equals(flav) || DropData.URILIST_FLAVOR.equals(flav))
		{
			File created;
			
			try
			{
				created = createTempDragFile(img, index);
			}
			catch (IOException exc)
			{
				System.err.println("DnD Failed: Could not create temporary housing for image #"+index+":");
				System.err.println(exc.getMessage());
				System.err.println();
				throw new IOException("Could not create temporary housing for image.");
			}
			
			if (created == null)
			{
				System.err.println("DnD Failed: Could not create temporary housing for image #"+index+".");
				throw new IOException("Could not create temporary housing for image.");
			}
			else
			{
				setOffPendingDelete(created);
				
				if (DataFlavor.javaFileListFlavor.equals(flav))
					return Collections.singletonList(created);
				else
					//URI List
					return created.toURI().toString();
			}
		}
		else
			throw new UnsupportedFlavorException(flav);
	}
	
	
	//This is used instead of File.createTempFile() because I need control over the file name.
	protected File createTempDragFile(BufferedImage img, int index) throws IOException
	{
		File f;
		String name = "Frame_"+index+getDragExt();
		
		for (String path : TEMP_DRAGIMAGE_STORAGE_PATHS)
		{
			f = new File(path);
			if (f.exists())
			{
				f = new File(path, name);
				if (!f.exists())
				{
					try
					{
						f.deleteOnExit();
						writeImage(img, f);
						return f;
					}
					catch (IOException exc)
					{
						System.err.println("Tried writing temp drag file to "+f.getAbsolutePath()+" but got this: \""+exc.getMessage()+"\", sigh, trying another.");
					}
				}
			}
		}
		
		
		//Try not on DefaultDnDiplomat.class's home, not user.dir
		File home = Compass.getLocalHabitat(getClass());
		for (String path : TEMP_DRAGIMAGE_STORAGE_PATHS)
		{
			f = new File(home, path);
			if (f.exists())
			{
				f = new File(f, name);
				if (!f.exists())
				{
					try
					{
						f.deleteOnExit();
						writeImage(img, f);
						return f;
					}
					catch (IOException exc)
					{
						System.err.println("Tried writing temp drag file to "+f.getAbsolutePath()+" but got this: \""+exc.getMessage()+"\", sigh, trying another.");
					}
				}
			}
		}
		
		
		home = new File(System.getProperty("user.home"));
		for (String path : TEMP_DRAGIMAGE_STORAGE_PATHS)
		{
			f = new File(home, path);
			if (f.exists())
			{
				f = new File(f, name);
				if (!f.exists())
				{
					try
					{
						f.deleteOnExit();
						writeImage(img, f);
						return f;
					}
					catch (IOException exc)
					{
						System.err.println("Tried writing temp drag file to "+f.getAbsolutePath()+" but got this: \""+exc.getMessage()+"\", sigh, trying another.");
					}
				}
			}
		}
		
		
		
		//Figure out where the system stores its tmp files
		{
			File sampleTemp = File.createTempFile("EdosProbe", null);
			File tempDir = sampleTemp.getParentFile();
			
			if (tempDir.isDirectory())
			{
				f = new File(tempDir, name);
				
				if (!f.exists())
				{
					f.deleteOnExit();
					writeImage(img, f);
					return f;
				}
			}
		}
		
		
		
		//Can't make a nice file, so just make an ugly system tmp file
		{
			File tmp = File.createTempFile("Frame_"+index+" %", getDragExt());
			writeImage(img, tmp);
			return tmp;
		}
	}
	
	
	
	protected void writeImage(BufferedImage img, File dest) throws IOException
	{
		//Find an instantiate an ImageWriter for a given mime type ($dragMIME)
		ImageWriter writer = null;
		{
			Iterator<ImageWriterSpi> spis = IIORegistry.getDefaultInstance().getServiceProviders(ImageWriterSpi.class, true);
			while (spis.hasNext()) {
				ImageWriterSpi spi = spis.next();
				for (String mimeType : spi.getMIMETypes()) {
					if (mimeType.startsWith(getDragMIME())) {
						writer = spi.createWriterInstance();
						break;
					}
				}
			}
		}
		
		if (writer == null)
			throw new IOException("This system's Java ImageIO does not have any plugins which support the mime type " + dragMIME);
		
		FileImageOutputStream out = new FileImageOutputStream(dest);
		writer.setOutput(out);
		writer.write(img);
		out.close();
		writer.dispose();
	}
	
	
	public void freeDragResources()
	{
		//On Mac I could use this to delete the stupid temp files I have to make
		//On X (linux/unix) this is called before the move even on successful drags, so it would delete the temp file before it is moved away
		
		//So I just set off a pending delete whenever you start dragging (QUICK you've got 300 seconds to finish the drag and move!)
	}
	public void setOffPendingDelete(final File f)
	{
		new Thread(() -> {
            try
            {
                Thread.sleep(300*1000);
            }
            catch (InterruptedException ignored)
            {
            }

            f.delete(); //Tied to a path; the temp path, won't delete the file if it's moved out in time
        }).start();
	}
	
	
	
	
	
	
	
	public String getDragExt()
	{
		return this.dragExt;
	}
	
	public String getDragMIME()
	{
		return this.dragMIME;
	}
	
	public void setDragExt(String dragExt)
	{
		this.dragExt = dragExt;
	}
	
	public void setDragMIME(String dragMIME)
	{
		this.dragMIME = dragMIME;
	}
}
