/*
 * Created on Jun 2, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.ui.gui.monk;

import static rebound.jagent.ui.gui.monk.Monkifier.OutputType.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import rebound.exceptions.ImpossibleException;
import rebound.jagent.lib.FormatMismatchException;
import rebound.jagent.lib.caos2pray.CAOS2PRAY;
import rebound.jagent.lib.caos2pray.exc.ConfigReadException;
import rebound.jagent.lib.caos2pray.exc.DependencyException;
import rebound.jagent.lib.caos2pray.exc.ScanningException;
import rebound.jagent.lib.caos2pray.exc.ScriptReadException;
import rebound.jagent.lib.pray.InvalidNameException;
import rebound.jagent.lib.pray.PrayMaker;
import rebound.jagent.lib.pray.PrayParser;
import rebound.jagent.lib.pray.template.PrayTemplate;
import rebound.jagent.lib.pray.template.TemplateConstructor;
import rebound.jagent.lib.pray.template.TemplateParser;

public class Monkifier
{
	public static enum OutputType
	{
		STANDARD_PRAY_TEMPLATE_SOURCE,
		PRAY,
	}
	
	protected Notifee notifiee;
	protected OutputType desiredOutput = null;
	
	private PrayParser parser = new PrayParser();
	private TemplateParser temparser = new TemplateParser();
	private PrayMaker maker = new PrayMaker();
	private TemplateConstructor constructor = new TemplateConstructor();
	private PrayTemplate template;
	
	public Monkifier(Notifee n)
	{
		super();
		notifiee = n;
	}
	
	public Monkifier()
	{
		this(new Notifee()
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
			}
		});
	}
	
	
	public synchronized boolean monkify(String[] files)
	{
		if (files != null)
		{
			for (String s : files)
				if (!monkify(new File(s)))
					return false;
			return true;
		}
		else
			return false;
	}
	
	public synchronized boolean monkify(Collection<File> files)
	{
		if (files != null)
		{
			for (File f : files)
				if (!monkify(f))
					return false;
			return true;
		}
		else
			return false;
	}
	
	public synchronized boolean monkify(File[] files)
	{
		if (files != null)
		{
			for (File f : files)
				if (!monkify(f))
					return false;
			return true;
		}
		else
			return false;
	}
	
	public synchronized boolean monkify(File sourceFile)
	{
		sourceFile = sourceFile.getAbsoluteFile();
		String lowercaseSourceFileName = sourceFile.getName().toLowerCase();
		String basename = sourceFile.getName();
		String preferredOutputFile = null;
		int extdot = sourceFile.getName().lastIndexOf(".");
		if (extdot != -1)
			basename = sourceFile.getName().substring(0, extdot);
		
		if (lowercaseSourceFileName.endsWith(".cos"))
		{
			//CAOS2PRAY
			
			notifiee.notify("Analyzing "+sourceFile.getName());
			
			try
			{
				template = CAOS2PRAY.convert(sourceFile);
			}
			catch (DependencyException exc)
			{
				notifiee.error("Could not categorize dependency "+exc.getDependency(), exc);
				return false;
			}
			catch (ScriptReadException exc)
			{
				if (exc.isFileNotFound())
					notifiee.error("Could not find "+(exc.isRemove() ? "remove " : "")+"script "+exc.getScript(), exc);
				else
					notifiee.error("notifier.error analyzing "+(exc.isRemove() ? "remove " : "")+"script "+exc.getScript()+": \""+exc.getIOExc().getMessage()+"\"", exc);
				return false;
			}
			catch (ScanningException exc)
			{
				notifiee.error("CAOS2PRAY Analysis notifier.error: "+exc.getDetailMessage(), exc);
				return false;
			}
			catch (ConfigReadException exc)
			{
				throw new ImpossibleException("The config should have been read at main()!\t\tMsg:"+exc.getCause().getMessage());
			}
			
			preferredOutputFile = template.getDesiredOutputFile();
			
			String out = null;
			if (getDesiredOutputType() == PRAY || getDesiredOutputType() == null)  //default
			{
				if (preferredOutputFile == null)
					out = basename + ".agents";
				else
					out = preferredOutputFile;
				
				notifiee.notify("Generating "+out);
				maker.setPrayTemplate(template);
				
				FileOutputStream cout = null;
				try
				{
					cout = new FileOutputStream(new File(sourceFile.getParent(), out));
				}
				catch (FileNotFoundException exc)
				{
					notifiee.error("Strange, FileNotFound notifier.error from "+out, exc);
					return false;
				}
				maker.setOut(cout);
				
				try
				{
					maker.make();
					cout.close();
				}
				catch (IOException exc)
				{
					notifiee.error("IOError making PRAY chunk file "+out+": \""+exc.getMessage()+"\"", exc);
					return false;
				}
				catch (InvalidNameException exc)
				{
					notifiee.error("Error making PRAY chunk file "+out+": "+exc.getMessage(), exc);
					return false;
				}
				catch (OutOfMemoryError err)
				{
					notifiee.error("Error making PRAY chunk file: program ran out of memory; it will now terminate.", err);
					throw err;
				}
			}
			else if (getDesiredOutputType() == STANDARD_PRAY_TEMPLATE_SOURCE)
			{
				if (preferredOutputFile == null)
				{
					out = basename + ".txt";
				}
				else
				{
					extdot = preferredOutputFile.lastIndexOf(".");
					if (extdot != -1)
						out = preferredOutputFile.substring(0, extdot)+".txt";
					else
						out = preferredOutputFile;
				}
				
				notifiee.notify("Generating "+out);
				constructor.setTemplate(template);
				
				FileOutputStream cout = null;
				try
				{
					cout = new FileOutputStream(new File(sourceFile.getParent(), out));
				}
				catch (FileNotFoundException exc)
				{
					notifiee.error("That's strange, we get a FileNotFound from "+out, exc);
					return false;
				}
				
				constructor.setOut(cout);
				
				try
				{
					constructor.construct();
					cout.close();
				}
				catch (IOException exc)
				{
					notifiee.error("IOError constructing "+out+": \""+exc.getMessage()+"\"", exc);
					return false;
				}
			}
			else
			{
				throw new AssertionError("enum escape");
			}
		}
		else if (lowercaseSourceFileName.endsWith(".txt") || lowercaseSourceFileName.endsWith(".ps"))
		{
			//PRAY Source
			
			if (getDesiredOutputType() == STANDARD_PRAY_TEMPLATE_SOURCE)
			{
				notifiee.notify("Nothing to do.");
				return true;
			}
			else if (getDesiredOutputType() == PRAY || getDesiredOutputType() == null)  //default!
			{
				notifiee.notify("Parsing "+sourceFile.getName());
				
				FileInputStream in = null;
				try
				{
					in = new FileInputStream(sourceFile);
				}
				catch (FileNotFoundException exc)
				{
					notifiee.error("Could not find "+sourceFile.getName(), exc);
					return false;
				}
				
				temparser.setIn(in);
				
				try
				{
					temparser.parse();
					temparser.setContext(sourceFile);
					in.close();
				}
				catch (IOException exc)
				{
					notifiee.error("IOError parsing "+sourceFile.getName()+": \""+exc.getMessage()+"\"", exc);
					return false;
				}
				
				template = temparser.getTemplate();
				
				if (getDesiredOutputType() == PRAY)
				{
					notifiee.notify("Generating "+basename+".agents");
					maker.setPrayTemplate(template);
					
					FileOutputStream cout = null;
					try
					{
						cout = new FileOutputStream(new File(sourceFile.getParent(), basename+".agents"));
					}
					catch (FileNotFoundException exc)
					{
						notifiee.error("Strange, FileNotFound notifier.error from "+basename+".agents", exc);
						return false;
					}
					
					maker.setOut(cout);
					
					try
					{
						maker.make();
					}
					catch (IOException exc)
					{
						notifiee.error("IOError making PRAY chunk file "+basename+".agents: \""+exc.getMessage()+"\"", exc);
						return false;
					}
					catch (InvalidNameException exc)
					{
						notifiee.error("Error making PRAY chunk file "+basename+".agents: "+exc.getMessage(), exc);
						return false;
					}
					catch (OutOfMemoryError err)
					{
						notifiee.error("Error making PRAY chunk file: program ran out of memory; it will now terminate.", err);
						throw err;
					}
				}
			}
			else
			{
				throw new AssertionError("enum escape");
			}
		}
		else
		{
			//PRAY Chunk
			
			if (getDesiredOutputType() == PRAY)
			{
				notifiee.notify("Nothing to do.");
				return true;
			}
			else if (getDesiredOutputType() == STANDARD_PRAY_TEMPLATE_SOURCE || getDesiredOutputType() == null)  //default! :>
			{
				notifiee.notify("Parsing "+sourceFile.getName());
				
				File dir = new File(sourceFile.getParentFile(), basename + " files");
				if (!dir.exists())
					dir.mkdirs();
				
				if (!dir.isDirectory())
				{
					notifiee.error("Could not create destination directory "+dir);
					return false;
				}
				
				try
				{
					try
					{
						parser.setDir(dir.getAbsoluteFile());
						parser.setIn(sourceFile);
					}
					catch (FileNotFoundException exc)
					{
						notifiee.error("Could not find "+sourceFile.getName(), exc);
						dir.delete();
						return false;
					}
					
					parser.parse();
					parser.getIn().close();
				}
				catch (FormatMismatchException exc)
				{
					notifiee.error(sourceFile.getName()+" is not a supported PRAY chunk file.\n"+exc.getMessage(), exc);
					dir.delete();
					return false;
				}
				catch (IOException exc)
				{
					notifiee.error("IOError parsing "+sourceFile.getName(), exc);
					dir.delete();
					return false;
				}
				
				template = parser.getTemplate();
				
				if (getDesiredOutputType() == STANDARD_PRAY_TEMPLATE_SOURCE || getDesiredOutputType() == null)  //deeeefffaauullltttttt ><
				{
					notifiee.notify("Generating "+basename+".txt");
					File out = new File(dir, basename+".txt");
					
					TemplateConstructor constructor = new TemplateConstructor();
					constructor.setTemplate(template);
					
					FileOutputStream cout = null;
					try
					{
						cout = new FileOutputStream(out);
					}
					catch (FileNotFoundException exc)
					{
						notifiee.error("Strange, FileNotFound notifier.error from "+basename+".txt", exc);
						return false;
					}
					
					constructor.setOut(cout);
					
					try
					{
						constructor.construct();
						cout.close();
					}
					catch (IOException exc)
					{
						notifiee.error("IOError constructing "+basename+".txt: \""+exc.getMessage()+"\"", exc);
						return false;
					}
				}
				// this is where this would go, theoretically speaking :>       //else if (getDesiredOutputType() == CAOS2PRAY_PRAY_TEMPLATE_SOURCE)
				else
				{
					throw new AssertionError("should be unreachable!");
				}
			}
			else
			{
				throw new AssertionError("enum escape");
			}
		}
		
		notifiee.notify("Done.");
		return true;
	}
	
	
	
	
	
	
	public synchronized Notifee getNotifiee()
	{
		return this.notifiee;
	}
	
	public synchronized void setNotifiee(Notifee notifier)
	{
		this.notifiee = notifier;
	}
	
	public synchronized OutputType getDesiredOutputType()
	{
		return this.desiredOutput;
	}
	
	public synchronized void setDesiredOutput(OutputType desiredOutput)
	{
		this.desiredOutput = desiredOutput;
	}
	
	
	
	
	
	
	
	//PRAY Compilation settings
	public boolean isMergeScripts()
	{
		return maker.isMergeScripts();
	}
	
	public void setMergeScripts(boolean value)
	{
		maker.setMergeScripts(value);
	}
}
