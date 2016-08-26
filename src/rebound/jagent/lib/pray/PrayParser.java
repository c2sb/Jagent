/*
 * Created on Jan 12, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.lib.pray;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;
import rebound.bits.Bytes;
import rebound.io.IOUtilities;
import rebound.jagent.lib.FormatMismatchException;
import rebound.jagent.lib.pray.blocks.MetaBlockParser;
import rebound.jagent.lib.pray.template.PrayTemplate;

/**
 * Note: This class is NOT asynchronal, and not synchronized which means a client app any other use MUST synchronize it completely! 
 * @author Sean
 */

public class PrayParser
{
	protected InputStream in;
	protected MetaBlockParser blars;
	protected PrayTemplate template;
	protected String originalFile; //Can be null
	
	public PrayParser()
	{
		super();
		blars = new MetaBlockParser();
		template = new PrayTemplate();
	}
	
	protected static final byte[] PRAYMAGIC = new byte[]{0x50,0x52,0x41,0x59};
	
	public void parse() throws IOException, EOFException, FileNotFoundException, FormatMismatchException
	{
		//Read and check magic
		{
			byte[] magic = new byte[PRAYMAGIC.length];
			try
			{
				IOUtilities.forceRead(in, magic);
			}
			catch (EOFException exc)
			{
				throw new FormatMismatchException("File is not a PRAY file: wrong magic.");
			}
			
			if (!Arrays.equals(magic, PRAYMAGIC))
				throw new FormatMismatchException("File is not a PRAY file: wrong magic.");
		}
		
		template = new PrayTemplate(template.getDir());
		
		Block currBlock = null;
		int bindex = 0;
		while (true)
		{
			currBlock = readBlockHeader(bindex);
			
			if (currBlock == null)
				//Clean EOF
				break;
			
			ProtectiveInputStream vishnishtee = null ;
			InputStream blockDataStream = null;
			{
				//This will prevent the block parser from reading past the end of the block, and tell me how much it read, in case it reads less than it's supposed to.
				vishnishtee = new ProtectiveInputStream(in, currBlock.getLengthInFile());
				
				if (currBlock.compressed)
					blockDataStream = getInflaterInputStream(vishnishtee); //This will uncompress the block data on-the-fly
				else
					blockDataStream = vishnishtee;
			}
			
			//Give to BlockParser
			boolean supported = blars.parseBlock(currBlock, blockDataStream, template);
			//Historical Note: The return value of parseBlock() used to be used to determine whether or not the block was actually consumed, but I switched the model so that even a "supported" isn't trusted to have read it to completion.
			
			//Complain if there's data left over, or it's unsupported (ie, all of the data left over)
			{
				if (!supported)
					System.err.println("Warning: Unsupported block type '"+currBlock.getIdText()+"', ignoring...");
				else
					if (vishnishtee.getTally() != currBlock.getLengthInFile())
						System.err.println("Warning: Less block data was read than is present in file.  "+currBlock.getIdText()+" block \""+currBlock.getName()+"\"  specified that "+currBlock.lengthInFile+" bytes were present in the file"+(currBlock.isCompressed() ? " ("+currBlock.originalLength+" before compression)" : "")+", but only "+vishnishtee.getTally()+" were read.  This is either a bug in Jagent, or the block (if compressed) has extra data (and thus a mismatch between the Uncompressed-Length and the true length of the data once decompressed).");
			}
			
			//Skip any unread data (or all of it, if the block wasn't supported)
			IOUtilities.forceSkip(in, currBlock.lengthInFile - vishnishtee.getTally());
			
			bindex++;
		}
	}
	
	protected Block readBlockHeader(int bindex) throws IOException, FormatMismatchException
	{
		Block b = new Block();
		
		//Index
		b.index = bindex;
		
		//Read ID
		b.id = new byte[4];
		int c = in.read(); //This is the only place EOF is allowed (and indeed the only way to know when the PRAY file properly terminates)
		if (c == -1)
			return null;
		b.id[0] = (byte)c;
		IOUtilities.forceRead(in, b.id, 1, 3);
		
		//Read Name
		{
			byte[] rawname = new byte[128];
			IOUtilities.forceRead(in, rawname);
			
			//Search for the first 0x00 (nul) byte which indicates a name shorter than 128 chars
			int actualLen = 128;
			for (int i = 0; i < 128; i++)
			{
				if (rawname[i] == 0)
				{
					actualLen = i;
					break;
				}
			}
			
			//Wrap the meaningful characters in a String 
			b.name = new String(rawname, 0, actualLen, "ASCII");
		}
		
		//Length in file
		b.lengthInFile = Bytes.getLittleInt(in);
		
		//Original length
		b.originalLength = Bytes.getLittleInt(in);
		
		//Flags
		int flags = Bytes.getLittleInt(in);
		b.compressed = (flags & 1) != 0;
		
		return b;
	}
	
	protected static InputStream getInflaterInputStream(InputStream in)
	{
		InflaterInputStream iis = new InflaterInputStream(in);
		return iis;
	}
	
	
	
	/**
	 * This stream watches its usage very carefully and will return EOF (just as a normal {@link InputStream} would) if more than the specified {@link #limit} is attempted to be read.
	 * Also, once the stream is finished being used, the {@link #getTally()} method will report how many bytes were read from the stream.
	 * @author RProgrammer
	 */
	protected static class ProtectiveInputStream
	extends FilterInputStream
	{
		protected long tally = 0;
		protected long limit;
		
		public ProtectiveInputStream(InputStream in, long limit)
		{
			super(in);
			this.limit = limit;
		}
		
		@Override
		public int read() throws IOException
		{
			if (tally >= limit)
				return -1;
			
			int c = super.read();
			tally++;
			return c;
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException
		{
			if (tally >= limit)
				return -1;
			
			if (tally+len > limit)
				len = (int)(limit - tally);
			
			int amt = super.read(b, off, len);
			tally += amt;
			return amt;
		}
		
		@Override
		public int read(byte[] b) throws IOException
		{
			return this.read(b, 0, b.length);
		}
		
		/**
		 * @return The number of bytes read with this stream
		 */
		public long getTally()
		{
			return tally;
		}
	};	
	
	
	
	
	
	public InputStream getIn()
	{
		return this.in;
	}
	
	public void setIn(InputStream in)
	{
		this.in = in;
	}
	
	public void setIn(File file) throws FileNotFoundException
	{
		if (getDir() == null)
			setDir(file.getAbsoluteFile().getParentFile());
		setIn(new FileInputStream(file));
		originalFile = file.getName();
	}
	
	public void setIn(String file) throws FileNotFoundException
	{
		setIn(new File(file));
	}
	
	public void setDir(File dir)
	{
		getTemplate().setDir(dir);
	}
	
	public File getDir()
	{
		return getTemplate().getDir();
	}
	
	public PrayTemplate getTemplate()
	{
		return this.template;
	}
}
