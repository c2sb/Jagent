/*
 * Created on Feb 15, 2010
 * 	by the great Eclipse(c)
 */
package rebound.jagent.lib;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PathBoss
{
	protected static final PathBoss INST = new PathBoss();
	
	public static PathBoss getInstance()
	{
		return INST;
	}
	
	
	
	protected static final Map<Character, String> charactersToEscapePosix;
	static {
		charactersToEscapePosix = new HashMap<>();
		charactersToEscapePosix.put('/', "slash");
	}

	protected static final Map<Character, String> charactersToEscapeMac;
	static {
		charactersToEscapeMac = new HashMap<>();
		charactersToEscapeMac.put('/', "slash");
		charactersToEscapeMac.put(':', "colon");
	}

	protected static final Map<Character, String> charactersToEscapeWindows;
	static {
		charactersToEscapeWindows = new HashMap<>();
		charactersToEscapeWindows.put('\\', "backslash");
		charactersToEscapeWindows.put('/', "slash");
		charactersToEscapeWindows.put(':', "colon");
		charactersToEscapeWindows.put('*', "asterisk");
		charactersToEscapeWindows.put('?', "question");
		charactersToEscapeWindows.put('"', "dblquote");
		charactersToEscapeWindows.put('<', "lessthan");
		charactersToEscapeWindows.put('>', "greaterthan");
		charactersToEscapeWindows.put('|', "pipe");
	}
	protected Map<Character, String> charactersToEscapeUnionCombinationOfAll = charactersToEscapeWindows;  //The Windows set just happens to be a superset of all the others XD
	
	
	
	
	protected boolean hostIsWindows = System.getProperty("os.name").toLowerCase().contains("windows");
	protected boolean hostIsMac = System.getProperty("os.name").toLowerCase().contains("mac os");
	protected Map<Character, String> charactersToEscapeHost;
	
	
	
	{
		if (hostIsWindows)
			charactersToEscapeHost = charactersToEscapeWindows;
		else if (hostIsMac)
			charactersToEscapeHost = charactersToEscapeMac;
		else
			charactersToEscapeHost = charactersToEscapePosix;
		
		
		
		char s = File.pathSeparatorChar;
		if (!charactersToEscapeHost.containsKey(s))
		{
			charactersToEscapeHost.put(s, "pathsep");
			System.out.println("Your system has a very unusual path separator character!  '"+File.pathSeparator+"'  Do you think you could maybe email me (rprogrammer@gmail.com) about this iydmma? It's interesting! :D");
		}
	}
	
	
	
	
	
	
	public boolean isNameCrossplatformFriendly(String name)
	{
		for (Character c : charactersToEscapeUnionCombinationOfAll.keySet())
			if (name.indexOf((char)c) != -1)
				return false;
		return true;
	}
	
	
	
	public String getEscapedNameOnCurrentHostOS(String name)
	{
		final char openEscape = '(';
		final char closeEscape = ')';
		final String openEscapeEscape = "(paren)";
		
		
		//Escape the escapes! \o/
		{
			for (String n : charactersToEscapeHost.values())
			{
				String esc = openEscape+n+closeEscape;
				
				if (name.contains(esc))
				{
					name = name.replace(n, openEscapeEscape+n+closeEscape);
				}
			}
		}
		
		
		//Escape the actual things! ^^'
		{
			for (Entry<Character, String> e : charactersToEscapeHost.entrySet())
			{
				name = name.replace(e.getKey().toString(), openEscape+e.getValue()+closeEscape);
			}
		}
		
		
		return name;
	}
}
