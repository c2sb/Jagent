/*
 * Created on Jun 17, 2008
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.gui.edos;

import rebound.graphics2d.gui.components.ImageFormatDropDown;
import rebound.graphics2d.gui.components.JFileChooserWrapper;
import rebound.graphics2d.gui.components.LatentFileBox;
import rebound.graphics2d.gui.components.ImageFormatDropDown.Parity;
import rebound.graphics2d.gui.components.JFileChooserWrapper.JFileChooserDisplayType;

import static rebound.text.StringUtilities.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.imageio.spi.ImageWriterSpi;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import rebound.text.IndependentCursor;
import rebound.text.StringUtilities;

public class ExportContactSheetDialog
extends JFrame
{
	protected EdosWindow callback;
	
	
	protected JPanel pane;
	protected ImageFormatDropDown formatBox;
	protected LatentFileBox fileBox;
	protected JTextField dimensionsField;
	protected JLabel statusLabel;
	protected JButton submitButton;
	protected JButton cancelButton;
	
	protected Color infoStatusColor, errorStatusColor;
	
	
	public ExportContactSheetDialog()
	{
		super("Export Contact Sheet");
		setSize(300, 23+116);
		setContentPane(getPane());
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setResizable(true);
	}
	
	
	
	
	public JPanel getPane()
	{
		if (pane == null)
		{
			pane = new JPanel()
			{
				@Override
				public void doLayout()
				{
					super.doLayout();
					
					int width = getWidth();
					int height = getHeight();
					
					/*
					 * format h20
					 * file h28
					 * dimensions h20
					 * submit+ cancel h28
					 * status h20
					 */
					
					
					int emptySpace = height - (20+28+20+28+20);
					
					int cancelWidth = width / 2;
					int submitWidth = width - cancelWidth;
					
					getFormatBox().setLocation(0, 0);
					getFormatBox().setSize(width, 20);
					getFileBox().setLocation(0, 20);
					getFileBox().setSize(width, 28);
					getDimensionsField().setLocation(0, 20+28);
					getDimensionsField().setSize(width, 20);
					getSubmitButton().setLocation(0, 20+28+20);
					getSubmitButton().setSize(submitWidth, 28);
					getCancelButton().setLocation(submitWidth, 20+28+20);
					getCancelButton().setSize(cancelWidth, 28);
					getStatusLabel().setLocation(0, 20+28+20+28+emptySpace);
					getStatusLabel().setSize(width, 20);
				}
			};
			
			pane.add(getFormatBox());
			pane.add(getFileBox());
			pane.add(getDimensionsField());
			pane.add(getSubmitButton());
			pane.add(getCancelButton());
			pane.add(getStatusLabel());
		}
		return this.pane;
	}
	
	
	
	
	public ImageFormatDropDown getFormatBox()
	{
		if (formatBox == null)
		{
			formatBox = new ImageFormatDropDown();
			formatBox.populateProviders(Parity.WRITER);
		}
		return this.formatBox;
	}
	
	
	
	
	public LatentFileBox getFileBox()
	{
		if (fileBox == null)
		{
			JFileChooser jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			JFileChooserWrapper wrapper = new JFileChooserWrapper(jfc);
			wrapper.setDisplayType(JFileChooserDisplayType.CUSTOM);
			wrapper.setCustomApproveButtonText("Choose");
			
			fileBox = new LatentFileBox();
			fileBox.setDefaultText("Choose a destination file.");
			fileBox.setFileChooser(wrapper);
		}
		return this.fileBox;
	}
	
	
	
	
	public JTextField getDimensionsField()
	{
		if (dimensionsField == null)
		{
			dimensionsField = new JTextField();
		}
		return this.dimensionsField;
	}
	
	
	
	public JLabel getStatusLabel()
	{
		if (statusLabel == null)
		{
			statusLabel = new JLabel();
			statusLabel.setText("Pick a format, folder, and filename.");
			
			infoStatusColor = statusLabel.getForeground();
			errorStatusColor = Color.red.darker();
		}
		return this.statusLabel;
	}
	
	
	
	public JButton getSubmitButton()
	{
		if (submitButton == null)
		{
			submitButton = new JButton();
			submitButton.setText("Save Set");
			submitButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					submitButtonClicked();
				}
			});
		}
		return this.submitButton;
	}
	
	
	
	public JButton getCancelButton()
	{
		if (cancelButton == null)
		{
			cancelButton = new JButton();
			cancelButton.setText("Cancel");
			cancelButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					cancelClicked();
				}
			});
		}
		return this.cancelButton;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public void setStatusText(String text)
	{
		getStatusLabel().setForeground(infoStatusColor);
		getStatusLabel().setText(text);
		getCallback().setStatusText(text);
	}
	
	public void setErrorText(String text)
	{
		getStatusLabel().setForeground(errorStatusColor);
		getStatusLabel().setText(text);
	}
	
	
	
	
	
	
	
	
	public void display()
	{
		getFileBox().setSelectedFile(null);
		getDimensionsField().setText(getCallback().getColumnCount() + "x" + getCallback().getRowCount());
		
		setVisible(true);
		
		getDimensionsField().requestFocusInWindow();
		getDimensionsField().selectAll();
	}
	
	
	
	
	public void cancelClicked()
	{
		setVisible(false);
	}
	
	
	
	
	
	public void submitButtonClicked()
	{
		submit();
	}
	
	
	
	
	
	
	
	public void submit()
	{
		ImageWriterSpi spi = getFormatBox().getSelectedWriterSpi();
		
		if (spi == null)
		{
			if (getFormatBox().getSelectedObjects().length == 0)
			{
				getFormatBox().populateProviders(Parity.WRITER);
				
				if (getFormatBox().getSelectedObjects().length == 0)
				{
					//Oh NO! ImageIO doesn't have any registered ImageWriters! (I smell a poor runtime implementation)
					setErrorText("(ErrCode: XIW) Uh oh. There are no ImageWriters installed on your system.");
					return;
				}
				else
				{
					//It's ok now.
					return;
				}
			}
			else
			{
				setErrorText("You need to choose an image format.");
				return;
			}
		}
		
		
		
		//<Basename
		File file = getFileBox().getSelectedFile();
		
		//<Empty
		if (file == null)
		{
			setErrorText("You must choose a file to save the sheet in.");
			return;
		}
		//Empty>
		
		//<Overwrite
		else if (file.exists())
		{
			int choice = JOptionPane.showConfirmDialog(null, file.getName()+" already exists, do you want to replace it?", "Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (choice != JOptionPane.YES_OPTION)
				return;
		}
		//Overwrite>
		
		//<Extension
		else
		{
			boolean badExt = false;
			{
				String[] exts = spi.getFileSuffixes(); //These include the '.'
				if (exts.length > 0)
				{
					badExt = true;
					for (String ext : exts)
					{
						if (file.getName().toLowerCase().endsWith(ext.toLowerCase()))
						{
							badExt = false;
							break;
						}
					}
				}
			}
			
			if (badExt)
			{
				setStatusText(spi.getFileSuffixes()[0] + " is a conventional filename suffix.");
			}
		}
		//Extension>
		//Basename>
		
		
		
		
		
		//Parse dimensions and potentially set error
		int cols = 0, rows = 0;
		{
			String raw = getDimensionsField().getText();
			char[] data = raw.toCharArray();
			
			boolean wildcardCols = false;
			boolean wildcardRows = false;
			
			IndependentCursor cursor = new IndependentCursor();
			
			while (cursor.getCursor() < data.length && data[cursor.getCursor()] != '*' && !isDigit(data[cursor.getCursor()], 10))
				cursor.setCursor(cursor.getCursor()+1); //Skip to the first number
			
			if (cursor.getCursor() < data.length)
			{
				if (data[cursor.getCursor()] == '*')
				{
					wildcardCols = true;
					cursor.setCursor(1);
				}
				else
				{
					cols = (int)StringUtilities.parseBasicNumber(data, 0, data.length, cursor, 10);
				}
			}
			
			while (cursor.getCursor() < data.length && data[cursor.getCursor()] != '*' && !isDigit(data[cursor.getCursor()], 10))
				cursor.setCursor(cursor.getCursor()+1); //Skip to the first number
			
			if (cursor.getCursor() < data.length)
			{
				if (data[cursor.getCursor()] == '*')
					wildcardRows = true;
				else
					rows = (int)StringUtilities.parseBasicNumber(data, 0, data.length, cursor, 10);
			}
			
			
			int total = getCallback().getFrameCount();
			
			if (wildcardCols && wildcardRows)
			{
				setErrorText("You can't set both to *");
				return;
			}
			else if (wildcardCols || wildcardRows)
			{
				if (wildcardCols)
				{
					if (rows != 0)
						cols = total / rows;
				}
				else //wildcardRows
				{
					if (cols != 0)
						rows = total / cols;
				}
			}
			
			
			if (cols == 0 || rows == 0)
			{
				setErrorText("You must specify the dimensions in the format WxH");
				return;
			}
			else if (cols * rows != total)
			{
				setErrorText(cols+"x"+rows+" doesn't make a grid of "+total+" frames.");
				return;
			}
		}
		
		
		setVisible(false);
		getCallback().exportContactSheetCallback(file, cols, rows, spi);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	public EdosWindow getCallback()
	{
		return this.callback;
	}
	
	public void setCallback(EdosWindow callback)
	{
		this.callback = callback;
	}
}
