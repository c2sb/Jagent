/*
 * Created on Jan 15, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.lib.pray.blocks.makers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import rebound.io.IOUtilities;
import rebound.jagent.lib.PathBoss;
import rebound.jagent.lib.pray.Block;
import rebound.jagent.lib.pray.InvalidNameException;
import rebound.jagent.lib.pray.blocks.MetaBlockMaker;

public class InlineFileBlockMaker
{
	public void make(OutputStream out, File sourceFile, String prayID, String prayFilename) throws IOException, InvalidNameException
	{
		if (!PathBoss.getInstance().isNameCrossplatformFriendly(prayFilename))
			throw new InvalidNameException(prayFilename, "characters in the name are illegal on some platforms");
		
		Block header = new Block();
		header.setCompressed(false);
		header.setOriginalLength((int)sourceFile.length());
		header.setLengthInFile((int)sourceFile.length());
		header.setName(prayFilename);
		header.setId(prayID.getBytes("ASCII"));
		
		MetaBlockMaker.writeHeader(out, header);
		
		FileInputStream filein = new FileInputStream(sourceFile);
		IOUtilities.pump(filein, out);
		filein.close();
	}
}
