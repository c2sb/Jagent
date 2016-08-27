/*
 * Created on Jan 27, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.ui.gui.edos;

import static rebound.text.StringUtilities.*;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import rebound.concurrency.threads.Scheduler;
import rebound.hci.graphics2d.gui.components.ExtJFCFileFilter;
import rebound.jagent.lib.FormatMismatchException;
import rebound.jagent.lib.blk.FromBLKConverter;
import rebound.jagent.lib.blk.ToBLKConverter;
import rebound.jagent.lib.c16.FromC16Converter;
import rebound.jagent.lib.c16.ToC16Converter;
import rebound.jagent.lib.s16.FromS16Converter;
import rebound.jagent.lib.s16.ToS16Converter;
import rebound.jagent.ui.gui.edos.sedpane.MemoryImageStorage;
import rebound.jagent.ui.gui.edos.sedpane.Sedpane;
import rebound.jagent.ui.gui.edos.sedpane.SedpaneImageMutationListener;
import rebound.text.IndependentCursor;
import rebound.text.StringUtilities;

public class EdosWindow
extends JFrame
{
	public static final long DEFAULT_STATUS_CLEAR_DELAY = 3000; //The delay in resetting the status bar after something is displayed there.
	
	public static final boolean UseAboutMenuItem;
	public static final boolean UseQuitMenuItem;
	
	static
	{
		UseAboutMenuItem = !EdosApplicationCoordinator.AppleJavaExtentionsPresent;
		UseQuitMenuItem = !EdosApplicationCoordinator.AppleJavaExtentionsPresent;
	}
	
	
	//<Logic fields
	protected EdosApplicationCoordinator edosApplicationCoordinator;
	protected boolean addAtSelection = true;
	//Logic fields>
	
	
	
	//<GUI
	//Main gui
	protected JPanel pane;
	protected Sedpane sedpane;
	protected JLabel statusLabel;
	
	//Menu
	protected JMenuBar mnu;
	protected JMenu mnuFile, mnuEdit, mnuHelp;
	protected JMenuItem mnuFileNew, mnuFileSave, mnuFileOpen, mnuFileClose, mnuFileQuit, mnuFileImport, mnuFileImportSet, mnuFileImportContactSheet, mnuFileExport, mnuFileExportSet, mnuFileExportContactSheet;
	protected JMenuItem mnuEditClearAll;
	protected JCheckBoxMenuItem mnuEdit565, mnuEditTransparencyEmulation;
	protected JMenuItem mnuHelpAbout;
	
	//Misc gui
	protected Color statusInfoColor = Color.blue;
	protected Color statusErrorColor = Color.red.darker();
	protected Scheduler statusClearer = new Scheduler();
	protected long statusClearDelay = DEFAULT_STATUS_CLEAR_DELAY;
	
	
	public EdosWindow()
	{
		super("Edos");
		setJMenuBar(getMnu());
		setSize(500, 500);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		doLayout();
		setContentPane(getPane());
		
		clearStatusText();
	}
	
	
	//<Main gui
	public JPanel getPane()
	{
		if (pane == null)
		{
			pane = new JPanel(true)
			{
				@Override
				public void doLayout()
				{
					super.doLayout();
					
					int width = getWidth();
					int height = getHeight();
					
					getSedpane().setSize(width, height-20);
					getSedpane().setLocation(0, 0);
					getStatusLabel().setSize(width, 20);
					getStatusLabel().setLocation(0, height - 20);
				}
			};
			pane.add(getSedpane());
			pane.add(getStatusLabel());
		}
		return pane;
	}
	
	
	public Sedpane getSedpane()
	{
		if (sedpane == null)
		{
			sedpane = new Sedpane();
			sedpane.setImageStorage(new MemoryImageStorage());
			sedpane.setDnDiplomat(new EdosDiplomat(this));
			
			sedpane.setImageMutationListener(new SedpaneImageMutationListener()
			{
				public void imagesReordered(int source, int dest)
				{
					clearStatusText();
				}
				
				public void imagesCleared()
				{
					clearStatusText();
				}
				
				public void imageDeleted(int index)
				{
					clearStatusText();
				}
				
				public void imageAdded(int index)
				{
				}
			});
			
			this.addKeyListener(sedpane);
		}
		return sedpane;
	}
	
	public JLabel getStatusLabel()
	{
		if (statusLabel == null)
		{
			statusLabel = new JLabel();
			statusLabel.addMouseListener(new MouseListener()
			{
				public void mouseClicked(MouseEvent e)
				{
					clearStatusText();
				}
				
				public void mouseReleased(MouseEvent e){}
				public void mousePressed(MouseEvent e){}
				public void mouseExited(MouseEvent e){}
				public void mouseEntered(MouseEvent e){}
			});
		}
		return statusLabel;
	}
	//Main gui>
	
	
	
	//<Menus
	public JMenuBar getMnu()
	{
		if (mnu == null)
		{
			mnu = new JMenuBar();
			mnu.add(getMnuFile());
			mnu.add(getMnuEdit());
			
			if (UseAboutMenuItem) //Only About is in the Help menu, so if you hide that, hide the whole help menu
				mnu.add(getMnuHelp());
		}
		return mnu;
	}
	
	public JMenu getMnuFile()
	{
		if (mnuFile == null)
		{
			mnuFile = new JMenu("File");
			mnuFile.add(getMnuFileNew());
			mnuFile.add(getMnuFileOpen());
			mnuFile.add(getMnuFileSave());
			mnuFile.add(getMnuFileImport());
			mnuFile.add(getMnuFileExport());
			mnuFile.add(getMnuFileImportSet());
			mnuFile.add(getMnuFileExportSet());
			mnuFile.add(getMnuFileImportContactSheet());
			mnuFile.add(getMnuFileExportContactSheet());
			mnuFile.add(getMnuFileClose());
			
			if (UseQuitMenuItem)
				mnuFile.add(getMnuFileQuit());
		}
		return mnuFile;
	}
	
	public JMenuItem getMnuFileNew()
	{
		if (mnuFileNew == null)
		{
			mnuFileNew = new JMenuItem("New");
			mnuFileNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileNew.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					newClicked();
				}
			});
		}
		return mnuFileNew;
	}
	
	public JMenuItem getMnuFileOpen()
	{
		if (mnuFileOpen == null)
		{
			mnuFileOpen = new JMenuItem("Open");
			mnuFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileOpen.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					openClicked();
				}
			});
		}
		return mnuFileOpen;
	}
	
	public JMenuItem getMnuFileSave()
	{
		if (mnuFileSave == null)
		{
			mnuFileSave = new JMenuItem("Save");
			mnuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileSave.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					saveClicked();
				}
			});
		}
		return mnuFileSave;
	}
	
	public JMenuItem getMnuFileImport()
	{
		if (mnuFileImport == null)
		{
			mnuFileImport = new JMenuItem("Import Images");
			mnuFileImport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileImport.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					importImageClicked();
				}
			});
		}
		return mnuFileImport;
	}
	
	public JMenuItem getMnuFileImportSet()
	{
		if (mnuFileImportSet == null)
		{
			mnuFileImportSet = new JMenuItem("Import Set");
			mnuFileImportSet.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileImportSet.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					importSetClicked();
				}
			});
		}
		return mnuFileImportSet;
	}
	
	public JMenuItem getMnuFileImportContactSheet()
	{
		if (mnuFileImportContactSheet == null)
		{
			mnuFileImportContactSheet = new JMenuItem("Import Contact Sheet");
			mnuFileImportContactSheet.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.ALT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileImportContactSheet.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					importContactSheetClicked();
				}
			});
		}
		return mnuFileImportContactSheet;
	}
	
	public JMenuItem getMnuFileExport()
	{
		if (mnuFileExport == null)
		{
			mnuFileExport = new JMenuItem("Export Selected Image");
			mnuFileExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileExport.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					exportImageClicked();
				}
			});
		}
		return mnuFileExport;
	}
	
	public JMenuItem getMnuFileExportSet()
	{
		if (mnuFileExportSet == null)
		{
			mnuFileExportSet = new JMenuItem("Export Set");
			mnuFileExportSet.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileExportSet.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					exportSetClicked();
				}
			});
		}
		return mnuFileExportSet;
	}
	
	public JMenuItem getMnuFileExportContactSheet()
	{
		if (mnuFileExportContactSheet == null)
		{
			mnuFileExportContactSheet = new JMenuItem("Export Contact Sheet");
			mnuFileExportContactSheet.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileExportContactSheet.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					exportContactSheetClicked();
				}
			});
		}
		return mnuFileExportContactSheet;
	}
	
	public JMenuItem getMnuFileClose()
	{
		if (mnuFileClose == null)
		{
			mnuFileClose = new JMenuItem("Close window");
			mnuFileClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileClose.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					closeClicked();
				}
			});
		}
		return mnuFileClose;
	}
	
	public JMenuItem getMnuFileQuit()
	{
		if (mnuFileQuit == null)
		{
			mnuFileQuit = new JMenuItem("Quit");
			mnuFileQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileQuit.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					quitClicked();
				}
			});
		}
		return mnuFileQuit;
	}
	
	
	
	public JMenu getMnuEdit()
	{
		if (mnuEdit == null)
		{
			mnuEdit = new JMenu("Edit");
			mnuEdit.add(getMnuEditClearAll());
			mnuEdit.add(getMnuEdit565());
			mnuEdit.add(getMnuEditTransparencyEmulation());
		}
		return mnuEdit;
	}
	
	public JCheckBoxMenuItem getMnuEdit565()
	{
		if (mnuEdit565 == null)
		{
			mnuEdit565 = new JCheckBoxMenuItem("565 Mode?");
			mnuEdit565.setSelected(true);
			mnuEdit565.addItemListener(new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					clearStatusText();
				}
			});
		}
		return mnuEdit565;
	}
	
	public JCheckBoxMenuItem getMnuEditTransparencyEmulation()
	{
		if (mnuEditTransparencyEmulation == null)
		{
			mnuEditTransparencyEmulation = new JCheckBoxMenuItem("Transparency emulation (pure black)?");
			mnuEditTransparencyEmulation.setSelected(false);
			mnuEditTransparencyEmulation.addItemListener(new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					clearStatusText();
				}
			});
		}
		return mnuEditTransparencyEmulation;
	}
	
	public JMenuItem getMnuEditClearAll()
	{
		if (mnuEditClearAll == null)
		{
			mnuEditClearAll = new JMenuItem("Clear All");
			mnuEditClearAll.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					clearAllClicked();
				}
			});
		}
		return mnuEditClearAll;
	}
	
	
	
	public JMenu getMnuHelp()
	{
		if (mnuHelp == null)
		{
			mnuHelp = new JMenu("Help");
			mnuHelp.add(getMnuHelpAbout());
		}
		return mnuHelp;
	}
	
	public JMenuItem getMnuHelpAbout()
	{
		if (mnuHelpAbout == null)
		{
			mnuHelpAbout = new JMenuItem("About");
			mnuHelpAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuHelpAbout.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					aboutClicked();
				}
			});
		}
		return mnuHelpAbout;
	}
	//Menus>
	
	
	
	
	
	
	
	
	
	
	
	
	//<GUI Logic
	protected JFileChooser spriteJFC, imageJFC, singletonImageJFC;
	protected ExportSetDialog exportSetDialog;
	protected ExportContactSheetDialog exportContactSheetDialog;
	
	public JFileChooser getSpriteFileChooser()
	{
		if (spriteJFC == null)
		{
			spriteJFC = new JFileChooser();
			spriteJFC.setAcceptAllFileFilterUsed(true);
			spriteJFC.addChoosableFileFilter(new ExtJFCFileFilter("Meta-Room background", true, "blk"));
			spriteJFC.addChoosableFileFilter(new ExtJFCFileFilter("Uncompressed Sprite File", true, "s16"));
			spriteJFC.addChoosableFileFilter(new ExtJFCFileFilter("Compressed Sprite File", true, "c16"));
			spriteJFC.addChoosableFileFilter(new ExtJFCFileFilter("All Creatures Graphics Files", true, "c16","s16", "blk"));
		}
		return spriteJFC;
	}
	
	public JFileChooser getImageFileChooser()
	{
		if (imageJFC == null)
		{
			imageJFC = new JFileChooser();
			imageJFC.setMultiSelectionEnabled(true);
			imageJFC.addChoosableFileFilter(new FileFilter()
			{
				@Override
				public boolean accept(File f)
				{
					return !f.isDirectory();
				}
				
				@Override
				public String getDescription()
				{
					return "Most image files";
				}
			});
		}
		return imageJFC;
	}
	
	public JFileChooser getSingletonImageFileChooser()
	{
		if (singletonImageJFC == null)
		{
			singletonImageJFC = new JFileChooser();
			singletonImageJFC.setMultiSelectionEnabled(false);
			singletonImageJFC.addChoosableFileFilter(new FileFilter()
			{
				@Override
				public boolean accept(File f)
				{
					return !f.isDirectory();
				}
				
				@Override
				public String getDescription()
				{
					return "Most image files";
				}
			});
		}
		return singletonImageJFC;
	}
	
	public ExportSetDialog getExportSetDialog()
	{
		if (exportSetDialog == null)
		{
			exportSetDialog = new ExportSetDialog();
			exportSetDialog.setCallback(this);
		}
		return this.exportSetDialog;
	}
	
	public ExportContactSheetDialog getExportContactSheetDialog()
	{
		if (exportContactSheetDialog == null)
		{
			exportContactSheetDialog = new ExportContactSheetDialog();
			exportContactSheetDialog.setCallback(this);
		}
		return this.exportContactSheetDialog;
	}
	
	
	
	
	
	
	public synchronized void newClicked()
	{
		getApplicationCoordinator().newWindow();
	}
	
	public synchronized void saveClicked()
	{
		if (getSpriteFileChooser().showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			saveSafe(getSpriteFileChooser().getSelectedFile());
		}
	}
	
	/**
	 * This aggregates all the error handling logic in one place.<br>
	 */
	public synchronized void saveSafe(File f)
	{
		try
		{
			save(f);
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(null, "An error has occurred during writing of this file, here's a message: "+exc.getMessage(), "!", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public synchronized void openClicked()
	{
		//Todo Try to fix the vanishing menu bug   (edit: 2012-10-06; is this still a bug?)
		this.repaint();
		this.getMnu().repaint();
		this.getMnuFile().repaint();
		this.getMnuFileOpen().repaint();
		
		
		if (getSpriteFileChooser().showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			openSafe(getSpriteFileChooser().getSelectedFile());
		}
		
		
		this.repaint();
		this.getMnu().repaint();
		this.getMnuFile().repaint();
		this.getMnuFileOpen().repaint();
		
		SwingUtilities.invokeLater
		(
			new Runnable()
			{
				public void run()
				{
					repaint();
					getMnu().repaint();
					getMnuFile().repaint();
					getMnuFileOpen().repaint();
				}
			}
			);
	}
	
	/**
	 * This aggregates all the error handling logic in one place.<br>
	 */
	public synchronized void openSafe(File f)
	{
		try
		{
			open(f);
		}
		catch (FileNotFoundException exc)
		{
			JOptionPane.showMessageDialog(null, "The file ceased to exist!", "!", JOptionPane.ERROR_MESSAGE);
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(null, "An error has occurred during opening of this file, here's a message: "+exc.getMessage(), "!", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public synchronized void importImageClicked()
	{
		if (getImageFileChooser().showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			importImageSafe(getImageFileChooser().getSelectedFiles());
		}
	}
	
	public synchronized void importImageSafe(File... f)
	{
		int index = 0;
		if (isAddAtSelection())
			index = getSedpane().getSelectedImageIndex();
		else
			index = getSedpane().getImageCount();
		
		
		importImageSafe(index, f);
	}
	
	public synchronized void importImageSafe(int index, File... f)
	{
		try
		{
			importImages(f, index);
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(null, "An error has occurred during writing of this file, here's a message: "+exc.getMessage(), "!", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	public synchronized void exportImageClicked()
	{
		BufferedImage selectedImage = getSelectedImage();
		
		if (selectedImage != null)
		{
			if (getImageFileChooser().showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			{
				try
				{
					exportImage(getImageFileChooser().getSelectedFile(), selectedImage);
				}
				catch (IOException exc)
				{
					JOptionPane.showMessageDialog(null, "An error has occurred during writing of this file, here's a message: "+exc.getMessage(), "!", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}
	
	
	
	public synchronized void importSetClicked()
	{
		if (getSingletonImageFileChooser().showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				int index = 0;
				if (isAddAtSelection())
					index = getSedpane().getSelectedImageIndex();
				else
					index = getSedpane().getImageCount();
				
				importSet(getSingletonImageFileChooser().getSelectedFile(), index);
			}
			catch (IOException exc)
			{
				JOptionPane.showMessageDialog(null, "An error has occurred during writing of this file, here's a message: "+exc.getMessage(), "!", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	
	public synchronized void exportSetClicked()
	{
		if (getSedpane().getImageCount() == 0)
		{
			setErrorText("No images to export.");
		}
		else
		{
			getExportSetDialog().display();
		}
	}
	
	
	
	public synchronized void importContactSheetClicked()
	{
		if (getSingletonImageFileChooser().showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				int index = 0;
				if (isAddAtSelection())
					index = getSedpane().getSelectedImageIndex();
				else
					index = getSedpane().getImageCount();
				
				
				File file = getSingletonImageFileChooser().getSelectedFile();
				
				//Prompt for tile dimentions
				String raw = null;
				int tilewidth = 0, tileheight = 0;
				boolean validTileDimensions = false;
				{
					raw = JOptionPane.showInputDialog(null, new String[]{"And what are the tile sizes?", "Example: 100x100"}, "Enter tile size", JOptionPane.QUESTION_MESSAGE);
					if (raw == null)
						return;
					
					
					
					char[] rawdata = raw.toCharArray();
					
					IndependentCursor cursor = new IndependentCursor();
					
					while (cursor.getCursor() < rawdata.length && !isDigit(rawdata[cursor.getCursor()], 10))
					{
						cursor.setCursor(cursor.getCursor()+1); //Skip to the first number
					}
					
					tilewidth = (int)parseBasicNumber(rawdata, 0, rawdata.length, cursor, 10);
					
					while (cursor.getCursor() < rawdata.length && !isDigit(rawdata[cursor.getCursor()], 10))
					{
						cursor.setCursor(cursor.getCursor()+1); //Skip the 'x' or whatever delimiter is used
					}
					
					tileheight = (int)StringUtilities.parseBasicNumber(rawdata, 0, rawdata.length, cursor, 10);
					
					if (tilewidth == 0 || tileheight == 0)
					{
						validTileDimensions = false;
					}
					else
					{
						validTileDimensions = true;
					}
				}
				
				
				if (!validTileDimensions)
				{
					popupError("Sorry, but "+raw+" are invalid dimensions.");
					return;
				}
				
				
				//Actually do it
				importContactSheet(file, index, tilewidth, tileheight);
			}
			catch (IOException exc)
			{
				popupError("An error has occurred during writing of this file, here's a message: "+exc.getMessage());
			}
		}
	}
	
	
	public synchronized void exportContactSheetClicked()
	{
		if (getSedpane().getImageCount() == 0)
		{
			setErrorText("No images to export.");
		}
		else
		{
			//Validate that all images are the same size.
			boolean allSameSize = false;
			{
				allSameSize = true;
				int width = 0, height = 0;
				boolean dimensionsSet = false;
				for (int i = 0; i < getSedpane().getImageCount(); i++)
				{
					BufferedImage image = getSedpane().getImageStorage().load(i);
					
					if (!dimensionsSet)
					{
						width = image.getWidth();
						height = image.getHeight();
						dimensionsSet = true;
					}
					else
					{
						if (width != image.getWidth() || height != image.getHeight())
						{
							allSameSize = false;
							break;
						}
					}
				}
			}
			
			
			if (!allSameSize)
			{
				popupError("Cannot export sheet: not all images are the same size.");
				return;
			}
			
			
			getExportContactSheetDialog().display();
		}
	}
	
	
	
	
	
	
	
	public synchronized void exportSetCallback(File dir, String base, int firstIndex, ImageWriterSpi format)
	{
		try
		{
			exportSet(dir, base, firstIndex, format);
		}
		catch (IOException exc)
		{
			System.err.println("Error occurred exporting set:\n"+exc.toString());
			exc.printStackTrace();
			popupError("An IO error occurred exporting the set: "+exc.getMessage());
		}
	}
	
	
	public synchronized void exportContactSheetCallback(File file, int width, int height, ImageWriterSpi format)
	{
		try
		{
			exportContactSheet(file, width, height, format);
		}
		catch (IOException exc)
		{
			System.err.println("Error occurred exporting contact sheet:\n"+exc.toString());
			exc.printStackTrace();
			popupError("An IO error occurred exporting the set: "+exc.getMessage());
		}
	}
	
	
	
	
	
	
	
	public synchronized void clearAllClicked()
	{
		getSedpane().clear();
	}
	
	
	public synchronized void aboutClicked()
	{
		getApplicationCoordinator().aboutClicked();
	}
	
	
	
	public synchronized void closeClicked()
	{
		getApplicationCoordinator().closeWindow(this);
	}
	
	public synchronized void quitClicked()
	{
		getApplicationCoordinator().safeQuit();
	}
	//GUI Logic>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<GUI Controllers
	public synchronized boolean is565()
	{
		return getMnuEdit565().isSelected();
	}
	
	public synchronized boolean isTransparencyEmulationPureBlackSet()
	{
		return getMnuEditTransparencyEmulation().isSelected();
	}
	
	public synchronized BufferedImage getSelectedImage()
	{
		if (getSedpane().hasSelection())
			return getSedpane().getImageStorage().load(getSedpane().getSelectedImageIndex());
		else
			return null;
	}
	
	public synchronized boolean isAddAtSelection()
	{
		if (getSedpane().getSelectedImageIndex() != -1)
			return this.addAtSelection;
		else
			return false;
	}
	
	public synchronized void setAddAtSelection(boolean addAtSelection)
	{
		this.addAtSelection = addAtSelection;
	}
	
	public synchronized long getStatusClearDelay()
	{
		return this.statusClearDelay;
	}
	
	public synchronized void setStatusClearDelay(long delay)
	{
		this.statusClearDelay = delay;
	}
	
	public synchronized void clearStatusText()
	{
		getStatusLabel().setForeground(statusInfoColor);
		
		getStatusLabel().setText(getSedpane().getImageCount() + " images,  565 mode "+(is565() ? "enabled" : "disabled")+",  transparency em. "+(isTransparencyEmulationPureBlackSet() ? "pure black" : "off"));
		
		repaint();
	}
	
	public synchronized void setStatusText(String text)
	{
		getStatusLabel().setForeground(statusInfoColor);
		getStatusLabel().setText(text);
		repaint();
		resetClearTask();
	}
	
	public synchronized void setErrorText(String text)
	{
		getStatusLabel().setForeground(statusErrorColor);
		getStatusLabel().setText(text);
		repaint();
		resetClearTask();
	}
	
	public synchronized void popupError(String text)
	{
		setErrorText(text);
		JOptionPane.showMessageDialog(null, text, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	public synchronized void popupInfo(String text)
	{
		setStatusText(text);
		JOptionPane.showMessageDialog(null, text, "Info", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public synchronized int getColumnCount()
	{
		return getSedpane().getMatrixWidth();
	}
	
	public synchronized int getRowCount()
	{
		return getSedpane().getMatrixHeight();
	}
	
	public synchronized int getFrameCount()
	{
		return getSedpane().getImageCount();
	}
	
	public synchronized boolean ask(String question)
	{
		return JOptionPane.showConfirmDialog(null, question, "?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
	}
	
	
	
	
	
	protected void resetClearTask()
	{
		if (getStatusClearDelay() > 0)
		{
			statusClearer.unregisterAll();
			
			statusClearer.register
			(
				new Runnable()
				{
					public void run()
					{
						clearStatusText();
					}
				},
				System.currentTimeMillis()+getStatusClearDelay()
				);
		}
	}
	//GUI Controllers>
	//GUI>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<Core Logic
	//Todo Swap out all the raw JOptionPane calls with GUI controllers
	
	/**
	 * This handles the general request to make use of a given file.<br>
	 * It could be a sprite file, or a image file; this method will check which and delegate it to the appropriate handler.<br>
	 */
	public void load(File f)
	{
		String ln = f.getName().toLowerCase();
		try
		{
			if (ln.endsWith(".c16") || ln.endsWith(".s16") || ln.endsWith(".blk"))
				open(f);
			else
				importImage(f, getSedpane().getImageCount());
		}
		catch (FileNotFoundException exc)
		{
			JOptionPane.showMessageDialog(null, "Could not find "+f.getName(), "Load error", JOptionPane.ERROR_MESSAGE);
		}
		catch (IOException exc)
		{
			JOptionPane.showMessageDialog(null, "Error occurred loading "+f.getName()+" : \""+exc.getMessage()+"\"", "Load error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	
	
	
	
	/**
	 * Determines which format and delegates to the appropriate {@link #writeBLK(String, BufferedImage) writeFoo} method.<br>
	 */
	public void save(File f) throws IOException
	{
		//Todo make this not based on the file suffix, but user-controllable  (suffix is better than contents, so ambiguous files can be manually differentiated, but user control in the GUI is even better)
		int dot = f.getName().lastIndexOf(".");
		String ext = ".c16";
		if (dot != -1)
			ext = f.getName().substring(dot);
		
		if (ext.equalsIgnoreCase(".blk"))
		{
			BufferedImage sel = getSelectedImage();
			if (sel != null)
				writeBLK(f.getAbsolutePath(), sel);
			else if (getSedpane().getImageCount() == 1)
				writeBLK(f.getAbsolutePath(), getSedpane().getImageStorage().load(0));
			else
				popupInfo("You must select which image to export.");
		}
		else
		{
			BufferedImage[] frames = getSedpane().getImageStorage().loadAll();
			if (ext.equalsIgnoreCase(".s16"))
			{
				writeS16(f.getAbsolutePath(), frames);
			}
			else if (ext.equalsIgnoreCase(".spr"))
			{
				writeSPR(f.getAbsolutePath(), frames);
			}
			else //Default is c16
			{
				writeC16(f.getAbsolutePath(), frames);
			}
		}
	}
	
	public void writeBLK(String dest, final BufferedImage image)
	{
		final ToBLKConverter toconv = new ToBLKConverter();
		toconv.setBits565(is565());
		toconv.setBLKFile(dest);
		
		Thread writer = new Thread() //So as not to tie up the awt thread
		{
			@Override
			public void run()
			{
				setStatusText("BLK Conversion has initiated...");
				
				toconv.setBackground(image);
				
				try
				{
					toconv.write();
					
					setStatusText("BLK Writing has finished.");
				}
				catch (IOException exc)
				{
					setErrorText("An I/O error occurred during BLK writing: "+exc.getMessage());
				}
				catch (FormatMismatchException exc)
				{
					setErrorText("A format error occurred during BLK writing: "+exc.getMessage());
				}
			}
		};
		writer.start();
	}
	
	public void writeC16(String dest, final BufferedImage[] images)
	{
		final ToC16Converter toconv = new ToC16Converter();
		toconv.setBits565(is565());
		toconv.setTransparencyEmulation(isTransparencyEmulationPureBlackSet());
		toconv.setC16File(dest);
		
		Thread writer = new Thread() //So as not to tie up the awt thread
		{
			@Override
			public void run()
			{
				setStatusText("C16 Conversion has initiated...");
				
				toconv.setFrames(images);
				try
				{
					toconv.write();
					
					
					setStatusText("C16 Writing has finished.");
				}
				catch (IOException exc)
				{
					setErrorText("An I/O error occurred during C16 writing: "+exc.getMessage());
				}
				catch (FormatMismatchException exc)
				{
					setErrorText("A format error occurred during C16 writing: "+exc.getMessage());
				}
			}
		};
		writer.start();
	}
	
	public void writeS16(String dest, final BufferedImage[] images)
	{
		final ToS16Converter toconv = new ToS16Converter();
		toconv.setBits565(is565());
		toconv.setS16File(dest);
		
		Thread writer = new Thread() //So as not to tie up the awt thread
		{
			@Override
			public void run()
			{
				setStatusText("S16 Writing has initiated...");
				
				toconv.setFrames(images);
				
				try
				{
					toconv.write();
					
					
					setStatusText("S16 Writing has finished.");
				}
				catch (IOException exc)
				{
					setErrorText("An I/O error occurred during S16 writing: "+exc.getMessage());
				}
				catch (FormatMismatchException exc)
				{
					setErrorText("A format error occurred during S16 writing: "+exc.getMessage());
				}
			}
		};
		writer.start();
	}
	
	public void writeSPR(String dest, final BufferedImage[] images)
	{
		popupInfo("Creatures 1 .spr files are not yet supported, email rprogrammer@gmail.com if you want them.");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	public void open(File f) throws FileNotFoundException, IOException
	{
		int dot = f.getName().lastIndexOf(".");
		String ext = ".c16";
		if (dot != -1)
			ext = f.getName().substring(dot);
		
		if (ext.equalsIgnoreCase(".blk"))
		{
			readBLK(f.getAbsolutePath());
		}
		else if (ext.equalsIgnoreCase(".s16"))
		{
			readS16(f.getAbsolutePath());
		}
		else if (ext.equalsIgnoreCase(".spr"))
		{
			readSPR(f.getAbsolutePath());
		}
		else //Default is c16
		{
			readC16(f.getAbsolutePath());
		}
	}
	
	public void readBLK(String src)
	{
		final FromBLKConverter fromconv = new FromBLKConverter();
		fromconv.setBLKFile(src);
		
		Thread writer = new Thread() //So as not to tie up the awt thread
		{
			@Override
			public void run()
			{
				setStatusText("BLK Reading has initiated...");				
				try
				{
					fromconv.read();
					
					setStatusText("BLK Reading has finished.");
					
					getSedpane().clear();
					getSedpane().add(fromconv.getBackground());
				}
				catch (IOException exc)
				{
					setErrorText("An i/o error occurred during BLK reading: "+exc.getMessage());
				}
				catch (FormatMismatchException exc)
				{
					setErrorText("A blk validity error occurred during BLK reading: "+exc.getMessage());
				}
			}
		};
		writer.start();
	}
	
	public void readC16(String src)
	{
		final FromC16Converter fromconv = new FromC16Converter();
		fromconv.setC16File(src);
		
		Thread writer = new Thread() //So as not to tie up the awt thread
		{
			@Override
			public void run()
			{
				setStatusText("C16 Reading has initiated...");				
				try
				{
					fromconv.read();
					
					setStatusText("C16 Reading has finished.");
					
					getSedpane().setImages(fromconv.getFrames());
				}
				catch (IOException exc)
				{
					setErrorText("An i/o error occurred during C16 reading: "+exc.getMessage());
				}
				catch (FormatMismatchException exc)
				{
					setErrorText("The file doesn't appear to be a valid C16 file: "+exc.getMessage());
				}
			}
		};
		writer.start();
	}
	
	public void readS16(String src)
	{
		final FromS16Converter fromconv = new FromS16Converter();
		fromconv.setS16File(src);
		
		Thread writer = new Thread() //So as not to tie up the awt thread
		{
			@Override
			public void run()
			{
				setStatusText("S16 Reading has initiated...");				
				try
				{
					fromconv.read();
					
					setStatusText("S16 Reading has finished.");
					
					getSedpane().setImages(fromconv.getFrames());
				}
				catch (IOException exc)
				{
					setErrorText("An i/o error occurred during S16 reading: "+exc.getMessage());
				}
				catch (FormatMismatchException exc)
				{
					setErrorText("The file doesn't appear to be a valid S16 file: "+exc.getMessage());
				}
			}
		};
		writer.start();
	}
	
	public void readSPR(String dest)
	{
		popupInfo("SPR Reading is not supported yet.");
	}
	
	
	
	
	
	
	
	
	public void importImage(BufferedImage img, int index)
	{
		getSedpane().add(img, index);
	}
	
	public void importImage(File file, int index) throws IOException
	{
		if (!file.exists())
		{
			setErrorText(file.getName()+" does not exist.");
			return;
		}
		else if (!file.isFile())
		{
			setErrorText(file.getName()+" is not a file.");
			return;
		}
		
		setStatusText("Importing "+file.getName());
		BufferedImage image = ImageIO.read(file);
		if (image == null)
		{
			setErrorText("Could not import "+file.getName());
		}
		else
		{
			importImage(image, index);
			setStatusText("Done.");
		}
	}
	
	public void importImages(File[] files, int index) throws IOException
	{
		int currentIndex = index;
		for (File f : files)
		{
			importImage(f, currentIndex);
			currentIndex++;
		}
	}
	
	public void importImages(List<File> files, int index) throws IOException
	{
		int currentIndex = index;
		for (File f : files)
		{
			importImage(f, currentIndex);
			currentIndex++;
		}
	}
	
	
	
	public void exportImage(File file, BufferedImage image) throws IOException
	{
		//Todo make this not based on the file suffix, but user-controllable  (suffix is better than contents, so ambiguous files can be manually differentiated, but user control in the GUI is even better)
		ImageWriterSpi format = null;
		{
			String lowercaseName = file.getName().toLowerCase();
			Iterator<ImageWriterSpi> spis = IIORegistry.getDefaultInstance().getServiceProviders(ImageWriterSpi.class, true);
			WriterLoop: while (spis.hasNext())
			{
				ImageWriterSpi spi = spis.next();
				
				SuffixLoop: for (String suffix : spi.getFileSuffixes())
				{
					if (lowercaseName.endsWith(suffix.toLowerCase()))
					{
						format = spi;
						break WriterLoop;
					}
				}
			}
		}
		
		if (format == null)
		{
			popupError("I don't know how to make that type of file.");
		}
		else
		{
			exportImage(file, image, format);
		}
	}
	
	public void exportImage(File file, BufferedImage image, ImageWriterSpi format) throws IOException
	{
		if (!prepareFileForExport(file))
			return;
		
		setStatusText("Exporting "+file.getName());
		ImageWriter writer = format.createWriterInstance();
		FileImageOutputStream out = new FileImageOutputStream(file);
		writer.setOutput(out);
		writer.write(image);
		out.close();
		writer.dispose();
		setStatusText("Done.");
	}
	
	
	
	
	public void importSet(File representative, int imageInsertionIndex) throws IOException
	{
		if (!representative.isFile())
		{
			popupError("Could not import "+representative+" - it is not a file.");
			return;
		}
		
		
		//Learn the pattern based on this one file.
		final String setPrefix;
		final String setSuffix;
		{
			String name = representative.getName();
			int lastDotPos = name.lastIndexOf('.');
			
			int start = 0;
			
			if (lastDotPos == -1)
				start = name.length();
			else
				start = lastDotPos;
			
			int index = start;
			
			while (true)
			{
				index--;
				
				if (index < 0)
				{
					break;
				}
				
				char c = name.charAt(index);
				
				if (Character.digit(c, 10) == -1)
				{
					break;
				}
			}
			
			
			setPrefix = name.substring(0, index+1);
			setSuffix = name.substring(start);
		}
		
		
		
		
		//Find the files in the set
		final File[] setMembers;
		{
			File dir = representative.getParentFile();
			
			final String lowerPrefix = setPrefix.toLowerCase();
			final String lowerSuffix = setSuffix.toLowerCase();
			
			setMembers = dir.listFiles
				(
					new java.io.FileFilter()
					{
						public boolean accept(File file)
						{
							if (!file.isFile())
								return false;
							
							String lowerName = file.getName().toLowerCase();
							
							return lowerName.startsWith(lowerPrefix) && lowerName.endsWith(lowerSuffix);
						}
					}
					);
			
			
			//Sort set members by their index (which is not a simple as it sounds since 2 comes before 13 in natural string order)
			Arrays.sort
			(
				setMembers,
				new Comparator<File>()
				{
					public int compare(File a, File b)
					{
						String aname = a.getName();
						String bname = b.getName();
						int prefixLength = setPrefix.length();
						int suffixLength = setSuffix.length();
						String aIndexPart = aname.substring(prefixLength, aname.length()-suffixLength);
						String bIndexPart = bname.substring(prefixLength, bname.length()-suffixLength);
						
						int aIndex = StringUtilities.parseIntegerLeniently(aIndexPart, 10, 0);
						int bIndex = StringUtilities.parseIntegerLeniently(bIndexPart, 10, 0);
						
						if (aIndex > bIndex)
							return 1;
						else if (aIndex < bIndex)
							return -1;
						else
							return 0;
					}
				}
				);
		}
		
		
		
		
		//Now actually import them
		importImages(setMembers, imageInsertionIndex);
	}
	
	
	
	
	
	
	/**
	 * @param firstSetIndex The first number to start naming files with; not the which image to start with, all images owned by this window are exported.
	 */
	public void exportSet(File dir, String base, int firstSetIndex, ImageWriterSpi format) throws IOException
	{
		if (!dir.isDirectory())
		{
			popupError(dir.getAbsolutePath() + " is not a valid directory.");
			return;
		}
		
		if (base == null)
			base = "";
		
		
		//<Naming
		File[] setFiles = null;
		{
			setFiles = new File[getSedpane().getImageCount()];
			
			int baseDotPos = base.lastIndexOf('.');
			String basePrefix = null, baseSuffix = null;
			if (baseDotPos == -1)
			{
				basePrefix = base;
				baseSuffix = "";
			}
			else
			{
				basePrefix = base.substring(0, baseDotPos);
				baseSuffix = base.substring(baseDotPos);
			}
			
			boolean endsWithSupportedSuffix = false;
			{
				String baseSuffixLowerCase = baseSuffix.toLowerCase();
				for (String ext : format.getFileSuffixes())
				{
					if (baseSuffixLowerCase.endsWith(ext.toLowerCase()))
					{
						endsWithSupportedSuffix = true;
						break;
					}
				}
			}
			
			if (!endsWithSupportedSuffix && format.getFileSuffixes().length > 0)
				baseSuffix += "."+format.getFileSuffixes()[0];
			
			for (int i = 0; i < setFiles.length; i++)
			{
				setFiles[i] = new File(dir, basePrefix + (i+firstSetIndex) + baseSuffix);
			}
		}
		//Naming>
		
		
		//<Verify
		boolean atleastOneExists = false;
		{
			for (File file : setFiles)
			{
				if (file.exists())
				{
					atleastOneExists = true;
					break;
				}
			}
		}
		
		
		if (atleastOneExists)
		{
			boolean answer = ask("Some of these files exist, are you sure you want to overwrite?");
			
			if (!answer)
				return;
		}
		//Verify>
		
		
		//<IO
		ImageWriter writer = format.createWriterInstance();
		
		for (int i = 0; i < setFiles.length; i++)
		{
			File file = setFiles[i];
			
			if (!prepareFileForExport(file))
				continue;
			
			BufferedImage image = getSedpane().getImageStorage().load(i);
			
			FileImageOutputStream fios = new FileImageOutputStream(file);
			
			writer.reset();
			writer.setOutput(fios);
			writer.write(image);
			
			fios.close();
		}
		
		writer.dispose();
		//IO>
	}
	
	
	
	
	
	
	public void importContactSheet(File sheetFile, int insertionIndex, int tilewidth, int tileheight) throws IOException
	{
		if (tilewidth == 0 || tileheight == 0)
		{
			setErrorText("Dimensions "+tilewidth+"x"+tileheight+" are invalid.");
			return;
		}
		
		
		BufferedImage sheet = null;
		{
			setStatusText("Loading sheet file "+sheetFile.getName());
			
			sheet = ImageIO.read(sheetFile);
			
			if (sheet.getWidth() == 0 || sheet.getHeight() == 0)
			{
				popupError(sheetFile.getName()+" is an invalid image file.");
				return;
			}
		}
		
		
		int cols = 0;
		int rows = 0;
		{
			if (sheet.getWidth() % tilewidth != 0 || sheet.getHeight() % tileheight != 0)
			{
				popupError(tilewidth+"x"+tileheight+" tiles would not fit evenly into a "+sheet.getWidth()+"x"+sheet.getHeight()+" sheet.");
				return;
			}
			
			if (tilewidth > sheet.getWidth() || tileheight > sheet.getHeight())
			{
				popupError("Tiles cannot be larger than the sheet!");
				return;
			}
			
			cols = sheet.getWidth() / tilewidth;
			rows = sheet.getHeight() / tileheight;
		}
		
		
		
		//Actually import them
		{
			WritableRaster sheetRaster = sheet.getRaster();
			int currentIndex = insertionIndex;
			for (int i = 0; i < cols*rows; i++)
			{
				int col = i % cols;
				int row = i / cols;
				
				setStatusText("Sequestering tile "+(i+1)+" of "+(cols*rows)+" ("+(col+1)+","+(row+1)+")");
				
				WritableRaster child = sheetRaster.createWritableChild
					(
						//X,Y
						col*tilewidth, row*tileheight,
						
						//Width, Height
						tilewidth, tileheight,
						
						//Others
						0,0, //The child is a stand-alone image
						null
						);
				
				BufferedImage tile = new BufferedImage(sheet.getColorModel(), child, sheet.isAlphaPremultiplied(), null);
				
				importImage(tile, currentIndex);
				
				currentIndex++;
			}
		}
		
		
		setStatusText("Done.");
	}
	
	
	
	
	
	public void exportContactSheet(File file, int cols, int rows, ImageWriterSpi format) throws IOException
	{
		if (!prepareFileForExport(file))
			return;
		
		if (getSedpane().getImageCount() == 0)
		{
			setErrorText("No images to export.");
			return;
		}
		
		if (getSedpane().getImageCount() == 1)
		{
			setStatusText("Exporting "+file.getName());
			exportImage(file, getSedpane().getImageStorage().load(0), format);
			setStatusText("Done.");
			return;
		}
		
		
		//This indirectly asserts neither cols nor rows is 0
		if (cols * rows != getSedpane().getImageCount())
		{
			setErrorText(getSedpane().getImageCount()+" images doesn't make a "+cols+"x"+rows+" grid.");
			return;
		}
		
		
		int tilewidth = 0;
		int tileheight = 0;
		{
			BufferedImage representative = getSedpane().getImageStorage().load(0);
			tilewidth = representative.getWidth();
			tileheight = representative.getHeight();
			
			for (int i = 1; i < getSedpane().getImageCount(); i++)
			{
				BufferedImage image = getSedpane().getImageStorage().load(i);
				
				if (image.getWidth() != tilewidth || image.getHeight() != tileheight)
				{
					setErrorText("Not all images are the same size.");
					return;
				}
			}
		}
		
		
		setStatusText("Generating "+cols+"x"+rows+" contact sheet...");
		
		//Make the contact sheet
		BufferedImage sheet = null;
		{
			sheet = new BufferedImage(tilewidth*cols, tileheight*rows, BufferedImage.TYPE_INT_ARGB);
			ColorConvertOp converter = new ColorConvertOp(null);
			
			for (int i = 0; i < getSedpane().getImageCount(); i++)
			{
				int col = i % cols;
				int row = i / cols;
				
				BufferedImage tile = getSedpane().getImageStorage().load(i);
				BufferedImage compatibleTile = null;
				{
					if (tile.getColorModel().equals(sheet.getColorModel()) && tile.getSampleModel().equals(sheet.getSampleModel()))
					{
						compatibleTile = tile;
					}
					else
					{
						compatibleTile = new BufferedImage(tile.getWidth(), tile.getHeight(), BufferedImage.TYPE_INT_ARGB);
						converter.filter(tile, compatibleTile);
					}
				}
				
				sheet.getRaster().setDataElements(col*tilewidth, row*tileheight, compatibleTile.getRaster());
			}
		}
		
		
		setStatusText("Exporting "+file.getName());
		
		//Now export it
		{
			ImageWriter writer = format.createWriterInstance();
			FileImageOutputStream out = new FileImageOutputStream(file);
			writer.setOutput(out);
			writer.write(sheet);
			out.close();
			writer.dispose();
		}
		
		setStatusText("Done.");
	}
	
	
	
	protected boolean prepareFileForExport(File file)
	{
		if (file.exists())
		{
			if (!file.isFile())
			{
				setErrorText(file.getName()+" is not a file.");
				return false;
			}
			else
			{
				file.delete();
			}
		}
		
		if (file.exists())
		{
			setErrorText(file.getName()+" still exists.");
			return false;
		}
		return true;
	}
	//Core Logic>
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//<Accessors
	public EdosApplicationCoordinator getApplicationCoordinator()
	{
		return this.edosApplicationCoordinator;
	}
	
	public void setApplicationCoordinator(EdosApplicationCoordinator edosApplicationCoordinator)
	{
		this.edosApplicationCoordinator = edosApplicationCoordinator;
	}
	//Accessors>
}
