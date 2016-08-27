/*#
 *# n: EdosTool
 *# m=Main-Class: %c
 *# SD:
 *# i=src:
 *# i=doc: doc
 *# i=res: res
 *# i=misc: misc
 */

/*
 * Created on Aug 12, 2009
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.cli;

import rebound.hci.graphics2d.ImageUtilities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import rebound.bits.Endianness;
import rebound.file.FSUtilities;
import rebound.jagent.lib.FormatMismatchException;
import rebound.jagent.lib.blk.FromBLKConverter;
import rebound.jagent.lib.c16.FromC16Converter;
import rebound.jagent.lib.s16.FromS16Converter;
import rebound.text.StringUtilities;
//import rebound.util.cli.StandardArguments;

public class EdosTool
{
	public static final String HELP = 
		""; //Todo Write CLI help
	
	public static void main(String[] args) throws IOException, FormatMismatchException
	{
		//StandardArguments sargs = new StandardArguments();
		//sargs.bake(args);
		
		EdosTool tool = new EdosTool();
		
		/*if (!sargs.getLongSuffices("type=").isEmpty())
		{
			tool.formatName = sargs.getLongSuffices("type=").get(0);
		}
		
		if (!sargs.getLongSuffices("suffix=").isEmpty())
		{
			tool.suffix = sargs.getLongSuffices("suffix=").get(0);
		}
		
		tool.verbose = sargs.isSwitchPresent('v');
		
		
		for (String t : sargs.getTargets())
		{
			tool.processExtract(new File(t));
		}*/
	}
	
	
	
	
	public static final String INTERNAL_FORMAT_RAW_ARGB32_BE = "raw32-be";
	public static final String INTERNAL_FORMAT_RAW_ARGB32_LE = "raw32-le";
	public static final String INTERNAL_FORMAT_RAW_RGB16_565_BE = "raw16-565-be";
	public static final String INTERNAL_FORMAT_RAW_RGB16_565_LE = "raw16-565-le";
	public static final String INTERNAL_FORMAT_RAW_RGB16_555_BE = "raw16-555-be";
	public static final String INTERNAL_FORMAT_RAW_RGB16_555_LE = "raw16-555-le";
	
	protected String formatName = "raw32-be";
	protected String suffix = ".png";
	
	protected boolean verbose = false;
	
	public void processExtract(File f) throws IOException, FormatMismatchException
	{
		if (f.isDirectory())
		{
			for (File c : f.listFiles())
				processExtract(c);
		}
		else if (f.isFile())
		{
			String t = f.getPath();
			if (verbose)
				System.out.println("Processing "+t);
			try
			{
				if (t.toLowerCase().endsWith(".c16"))
				{
					processC16(t);
				}
				else if (t.toLowerCase().endsWith(".s16"))
				{
					processS16(t);
				}
				else if (t.toLowerCase().endsWith(".blk"))
				{
					processBLK(t);
				}
			}
			catch (Exception exc)
			{
				if (!verbose)
					System.err.println("Error on "+t);
				exc.printStackTrace();
			}
		}
		else
		{
			System.out.println("Not a file or directory: "+f);
		}
	}
	
	public void processC16(String file) throws IOException, FormatMismatchException
	{
		FromC16Converter conv = new FromC16Converter();
		conv.setC16File(file);
		conv.read();
		
		String stem = StringUtilities.rsplit(file, '.', 1)[0];
		new File(stem).mkdir();
		for (int i = 0; i < conv.getFrames().length; i++)
		{
			File dest = new File(stem, i+suffix);
			writeImage(conv.getFrames()[i], dest);
		}
	}
	
	public void processS16(String file) throws IOException, FormatMismatchException
	{
		FromS16Converter conv = new FromS16Converter();
		conv.setS16File(file);
		conv.read();
		
		String stem = StringUtilities.rsplit(file, '.', 1)[0];
		new File(stem).mkdir();
		for (int i = 0; i < conv.getFrames().length; i++)
		{
			File dest = new File(stem, i+suffix);
			writeImage(conv.getFrames()[i], dest);
		}
	}
	
	public void processBLK(String file) throws IOException, FormatMismatchException
	{
		FromBLKConverter conv = new FromBLKConverter();
		conv.setBLKFile(file);
		conv.read();
		
		String stem = StringUtilities.rsplit(file, '.', 1)[0];
		File dest = new File(stem+suffix);
		writeImage(conv.getBackground(), dest);
	}
	
	
	
	public void writeImage(BufferedImage image, File dest) throws IOException
	{
		if (verbose)
			System.out.println("\t"+dest);
		
		if (FSUtilities.lexists(dest))
		{
			System.err.println("Destination file extsts: "+dest);
			return;
		}
		
		if (INTERNAL_FORMAT_RAW_ARGB32_BE.equals(formatName))
		{
			//TODO This doesn't do the alpha correctly..
			writeFile(dest, ImageUtilities.extractRawImageData_ARGB32_bytes(image, Endianness.BIG));
		}
		else if (INTERNAL_FORMAT_RAW_ARGB32_LE.equals(formatName))
		{
			//TODO This doesn't do the alpha correctly..
			writeFile(dest, ImageUtilities.extractRawImageData_ARGB32_bytes(image, Endianness.LITTLE));
		}
		else if (INTERNAL_FORMAT_RAW_RGB16_565_BE.equals(formatName))
		{
			writeFile(dest, ImageUtilities.extractRawImageData_5X5_bytes(image, true, Endianness.BIG));
		}
		else if (INTERNAL_FORMAT_RAW_RGB16_565_LE.equals(formatName))
		{
			writeFile(dest, ImageUtilities.extractRawImageData_5X5_bytes(image, true, Endianness.LITTLE));
		}
		else if (INTERNAL_FORMAT_RAW_RGB16_555_BE.equals(formatName))
		{
			writeFile(dest, ImageUtilities.extractRawImageData_5X5_bytes(image, false, Endianness.BIG));
		}
		else if (INTERNAL_FORMAT_RAW_RGB16_555_LE.equals(formatName))
		{
			writeFile(dest, ImageUtilities.extractRawImageData_5X5_bytes(image, false, Endianness.LITTLE));
		}
		
		else
		{
			boolean success = ImageIO.write(image, formatName, dest);
			if (!success)
				System.err.println("Unable to write format '"+formatName+"' : "+dest);
		}
	}
	
	
	protected void writeFile(File file, byte[] data) throws IOException
	{
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(data);
		}
	}
}
