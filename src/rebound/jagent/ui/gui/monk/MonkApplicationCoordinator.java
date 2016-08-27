/*#
 *# n: Monk
 *# m=Main-Class: %c
 *# SD:
 *# i=src:
 *# i=doc: doc
 *# i=res: res
 *# i=misc: misc
 */

/*
 * Created on Jun 25, 2008
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.gui.monk;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.JOptionPane;
import rebound.exceptions.ImpossibleException;
import rebound.jagent.lib.caos2pray.FriendlyTags;
import rebound.jagent.ui.gui.AboutDialog;
import rebound.jagent.ui.gui.common.GUI;
import rebound.osint.modapple.eawtproxy.ApplicationEventProxy;
import rebound.osint.modapple.eawtproxy.ApplicationListenerClone;
import rebound.osint.modapple.eawtproxy.ApplicationProxy;

/**
 * This corresponds to the application instance (Process), which can have more than one window.
 * @author RProgrammer
 */
public class MonkApplicationCoordinator
{
	public static final String ABOUT_RESOURCE = "about-monk.html";
	
	public static final boolean AppleJavaExtentionsPresent = ApplicationProxy.canGetApplication();
	
	
	public static void main(String[] args)
	{
		GUI.setupLookAndFeel();
		try
		{
			FriendlyTags.loadConfig();
		}
		catch (FileNotFoundException exc)
		{
			if (!GraphicsEnvironment.isHeadless())
				JOptionPane.showMessageDialog(null, "Could not find resource friendliness.nice", "Error Initializing", JOptionPane.ERROR_MESSAGE);
			System.err.println("Could not find resource friendliness.nice");
		}
		catch (IOException exc)
		{
			if (!GraphicsEnvironment.isHeadless())
				JOptionPane.showMessageDialog(null, "There was an error loading resource friendliness.nice: "+exc.getMessage(), "Error Initializing", JOptionPane.ERROR_MESSAGE);
			System.err.println("There was an error loading resource friendliness.nice: "+exc.getMessage());
		}
		
		
		if (args.length == 0)
		{
			MonkApplicationCoordinator coordinator = new MonkApplicationCoordinator();
			if (AppleJavaExtentionsPresent)
				coordinator.setupMacExtensions();
			coordinator.initWindow();
		}
		else
		{
			Monkifier chimp = new Monkifier();
			
			chimp.setNotifiee(new Notifee()
			{
				@Override
				public void notify(String message)
				{
					System.out.println("info: "+message);
				}
				
				@Override
				public void error(String prefix, Throwable cause)
				{
					error(prefix + " : ("+cause.getClass().getSimpleName()+")\""+cause.getMessage()+"\"");
					cause.printStackTrace();
				}
				
				@Override
				public void error(String message)
				{
					System.err.println("error: "+message);
					
					if (!GraphicsEnvironment.isHeadless())
						JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
				}
			});
			
			boolean success = chimp.monkify(args);
			System.exit(success ? 0 : 1);
		}
	}
	
	
	
	
	protected MonkWindow adrian;
	protected AboutDialog aboutDialog;
	
	
	public void initWindow()
	{
		adrian = new MonkWindow();
		adrian.setApplicationCoordinator(this);
		adrian.setVisible(true);
	}
	
	
	public AboutDialog getAboutDialog()
	{
		if (aboutDialog == null)
		{
			aboutDialog = new AboutDialog(ABOUT_RESOURCE);
		}
		return aboutDialog;
	}
	
	
	
	public void setupMacExtensions()
	{
		ApplicationProxy application = ApplicationProxy.getApplication();
		
		if (application == null)
		{
			throw new ImpossibleException("com.apple.eawt.Application exists, but it also doesn't exist...  Aren't computers are supposed to make sense?!");
		}
		
		application.removePreferencesMenuItem();
		application.addAboutMenuItem();
		application.addApplicationListener
		(
			new ApplicationListenerClone()
			{
				public void handleAbout(ApplicationEventProxy e)
				{
					e.setHandled(true);
					aboutClicked();
				}
				
				public void handleOpenFile(ApplicationEventProxy e)
				{
					e.setHandled(true);
					load(e.getFilename());
				}
				
				public void handleQuit(ApplicationEventProxy e)
				{
					e.setHandled(false); //Because Application._setTermination takes true (handled) to mean 'Quit the application' and false (unhandled) to mean leave it be (which we want so we can do it cleanly).
					quitClicked();
				}
				
				public void handleReOpenApplication(ApplicationEventProxy e)
				{
					//Nothing special
				}
				
				public void handleOpenApplication(ApplicationEventProxy e)
				{
					//Not quite sure what this does that main(String[]) doesn't...
				}
				
				
				public void handlePreferences(ApplicationEventProxy e) {}
				
				public void handlePrintFile(ApplicationEventProxy e) {}
			}
			);
	}
	
	
	
	
	
	public void load(String filename)
	{
		load(new File(filename));
	}
	
	public void load(File file)
	{
		adrian.process(file);
	}
	
	
	
	
	public void aboutClicked()
	{
		getAboutDialog().display();
	}
	
	public void quitClicked()
	{
		System.exit(0);
	}
}
