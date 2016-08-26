/*
 * Created on May 14, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.lib.pray.blocks.parsers;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import rebound.bits.Bytes;
import rebound.io.IOUtilities;
import rebound.jagent.lib.FormatMismatchException;
import rebound.jagent.lib.pray.Block;
import rebound.jagent.lib.pray.blocks.BlockParser;
import rebound.jagent.lib.pray.template.Group;
import rebound.jagent.lib.pray.template.PrayTemplate;

public class TagBlockParser
implements BlockParser
{
	public void parseBlock(Block b, InputStream data, PrayTemplate template) throws IOException, FormatMismatchException
	{
		Group g = new Group();
		template.addGroup(g); //Let the template configure it a bit
		
		g.setID(new String(b.getId(), "ASCII"));
		g.setName(b.getName());
		
		int intTagCount = Bytes.getLittleInt(data);
		int ival = 0;
		String name = null;
		for (int i = 0; i < intTagCount; i++)
		{
			//Read int val
			name = readLString(data);
			ival = Bytes.getLittleInt(data);
			g.addIntTag(name, ival);
		}
		
		
		int strTagCount = Bytes.getLittleInt(data);
		String sval = null;
		boolean hasAtLeastOneScript = false;
		boolean hasOtherScripts = false; //scripts other than "Script 1", that is.
		for (int i = 0; i < strTagCount; i++)
		{
			//Read str val
			name = readLString(data);
			
			if (Group.isScriptTag(name))
			{
				hasAtLeastOneScript = true;
				int scriptNumber = Group.getScriptNumber(name);
				if (scriptNumber != 1)
					hasOtherScripts = true;
				g.writeScriptToFile(data, scriptNumber);
			}
			else
			{
				sval = readLString(data);
				g.addStringTag(name, sval);
			}
		}
		
		
		if (hasAtLeastOneScript && !hasOtherScripts)
		{
			//We can only do this after all scripts have been written.
			g.recognizeSingletonScript();
		}
	}
	
	protected String readLString(InputStream in) throws IOException, EOFException
	{
		int len = Bytes.getLittleInt(in);
		byte[] raw = new byte[len];
		IOUtilities.forceRead(in, raw);
		return new String(raw, "ascii");
	}
	
	
	public boolean canHandle(Block b)
	{
		//Todo blacklist blocks instead of whitelisting??  (ie, what is the behavior of 'unknown' block id's; assume they're tag blocks or something else or assume nothing? :> )
		//TODO allow configurability by the user :>  (eg, from a text file placed next to the .jar or in their home folder for overrides :> )
		
		return
			b.getID() == Block.ID_AGNT ||
			b.getID() == Block.ID_DSAG ||
			b.getID() == Block.ID_LIVE ||
			b.getID() == Block.ID_EGGS ||
			b.getID() == Block.ID_SFAM ||
			b.getID() == Block.ID_DFAM ||
			b.getID() == Block.ID_EXPC ||
			b.getID() == Block.ID_DSEX ||
			
			b.getID() == Block.ID_DSGB;
	}
}
