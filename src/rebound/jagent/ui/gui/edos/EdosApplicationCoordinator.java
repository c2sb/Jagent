/*#
 *# n: Edos
 *# m=Main-Class: %c
 *# SD:
 *# i=src:
 *# i=doc: doc
 *# i=res: res
 *# i=misc: misc
 */

/*
 * Created on Jun 14, 2008
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.gui.edos;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;
import rebound.jagent.ui.gui.AboutDialog;
import rebound.jagent.ui.gui.common.GUI;
import rebound.osint.modapple.eawtproxy.ApplicationEventProxy;
import rebound.osint.modapple.eawtproxy.ApplicationListenerClone;
import rebound.osint.modapple.eawtproxy.ApplicationProxy;

/**
 * This corresponds to the application instance (Process), which can have more than one window.
 * @author RProgrammer
 */
public class EdosApplicationCoordinator
implements WindowListener
{
	public static final String ABOUT_RESOURCE = "about-edos.html";
	
	public static final boolean AppleJavaExtentionsPresent = ApplicationProxy.canGetApplication();
	
	
	public static void main(String[] args)
	{
		GUI.setupLookAndFeel();
		EdosApplicationCoordinator coordinator = new EdosApplicationCoordinator();
		if (AppleJavaExtentionsPresent)
			coordinator.setupMacExtensions();
		
		if (args.length == 0)
		{
			coordinator.newWindow();
		}
		else
		{
			coordinator.load(args);
		}
	}
	
	
	
	
	
	protected final List<EdosWindow> windowRegistry = new ArrayList<>();
	protected final AboutDialog aboutDialog = new AboutDialog(ABOUT_RESOURCE);
	
	public EdosApplicationCoordinator()
	{
		super();
	}
	
	
	
	public void setupMacExtensions()
	{
		ApplicationProxy application = ApplicationProxy.getApplication();
		application.removePreferencesMenuItem();
		application.addAboutMenuItem();
		application.addApplicationListener
		(
			new ApplicationListenerClone()
			{
				@Override
				public void handleAbout(ApplicationEventProxy e)
				{
					e.setHandled(true);
					aboutClicked();
				}

				@Override
				public void handleOpenFile(ApplicationEventProxy e)
				{
					System.out.println("Open Document("+e.getFilename()+")");
					e.setHandled(true);
					load(e.getFilename());
				}

				@Override
				public void handleQuit(ApplicationEventProxy e)
				{
					safeQuit();
					e.setHandled(false); //Because Application._setTermination takes true (handled) to mean 'Quit the application' and false (unhandled) to mean leave it be.
				}

				@Override
				public void handleReOpenApplication(ApplicationEventProxy e)
				{
					//Nothing special
				}

				@Override
				public void handleOpenApplication(ApplicationEventProxy e)
				{
					//Not quite sure what this does that main(String[]) doesn't...
				}

				@Override
				public void handlePreferences(ApplicationEventProxy e) {}
				@Override
				public void handlePrintFile(ApplicationEventProxy e) {}
			}
		);
	}
	
	
	//<General use-file commands
	public void load(String[] filenames)
	{
		File[] files = new File[filenames.length];
		for (int i = 0; i < files.length; i++)
			files[i] = new File(filenames[i]);
		load(files);
	}
	
	public void load(File[] files)
	{
		EdosWindow looseImageTarget = null;
		
		boolean atleastOneNonSprite = Arrays.stream(files).anyMatch(file -> !isSpriteFile(file));

		if (atleastOneNonSprite)
			looseImageTarget = newWindow(); //Make sure it's first so that subsequent image loads will load into this window
		
		for (File file : files)
		{
			if (isSpriteFile(file))
			{
				EdosWindow fresh = newWindow();
				fresh.openSafe(file);
			}
			else
			{
				if (atleastOneNonSprite)
					looseImageTarget.importImageSafe(file);
			}
		}
	}
	
	public void load(String filename)
	{
		load(new File(filename));
	}
	
	
	public void load(File file)
	{
		if (isSpriteFile(file))
		{
			EdosWindow window = newWindow();
			window.openSafe(file);
		}
		else
		{
			synchronized (windowRegistry)
			{
				EdosWindow target;
				if (windowRegistry.isEmpty())
				{
					target = newWindow(); //I can't imagine this path would be used, but just to be safe...
				}
				else
				{
					target = windowRegistry.get(0);
				}

				target.importImageSafe(file);
			}
		}
	}
	//General use-file commands>
	
	
	
	/**
	 * Makes sure the user is on board.<br>
	 * @return <code>true</code> if System.exit() was called (although this shouldn't technically return), or <code>false</code> if it was cancelled.
	 */
	public boolean safeQuit()
	{
		if (JOptionPane.showConfirmDialog(null, "Are you sure you wish to quit?", "Quit, really?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			System.exit(0);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	

	public void aboutClicked()
	{
		aboutDialog.display();
	}
	
	
	public EdosWindow newWindow()
	{
		EdosWindow e = new EdosWindow();
		e.setApplicationCoordinator(this);
		registerWindow(e);
		e.addWindowListener(this);
		e.setVisible(true);
		return e;
	}
	
	public boolean closeWindow(EdosWindow window)
	{
		synchronized (windowRegistry)
		{
			if (windowRegistry.size() == 1 && windowRegistry.contains(window))
			{
				if (!safeQuit())
				{
					return false;
				}
			}
			
			window.setVisible(false);
			windowRegistry.remove(window);
			return true;
		}
	}
	
	public void registerWindow(EdosWindow window)
	{
		synchronized (windowRegistry)
		{
			windowRegistry.add(window);
		}
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		if (e.getWindow() instanceof EdosWindow)
		{
			EdosWindow window = (EdosWindow)e.getWindow();
			closeWindow(window);
		}
	}
	
	
	
	
	@Override
	public void windowOpened(WindowEvent e)
	{
	}
	@Override
	public void windowClosed(WindowEvent e)
	{
	}
	@Override
	public void windowIconified(WindowEvent e)
	{
	}
	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}
	@Override
	public void windowActivated(WindowEvent e)
	{
	}
	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}
	
	
	
	
	
	
	
	//<Logic / Utils
	public static boolean isSpriteFile(File file)
	{
		String ln = file.getName().toLowerCase();
		return ln.endsWith(".c16") || ln.endsWith(".s16") || ln.endsWith(".spr") || ln.endsWith(".blk");
	}
	//Logic / Utils>
}
