/*
 * Created on Jun 15, 2008
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.gui.edos;

import rebound.hci.graphics2d.gui.components.ImageFormatDropDown;
import rebound.hci.graphics2d.gui.components.JFileChooserWrapper;
import rebound.hci.graphics2d.gui.components.LatentFileBox;
import rebound.hci.graphics2d.gui.components.ImageFormatDropDown.Parity;
import rebound.hci.graphics2d.gui.components.JFileChooserWrapper.JFileChooserDisplayType;

import java.awt.Color;
import javax.imageio.spi.ImageWriterSpi;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ExportSetDialog
extends JFrame
{
	protected EdosWindow callback;
	
	
	protected JPanel pane;
	protected ImageFormatDropDown formatBox;
	protected LatentFileBox directoryField;
	protected JTextField baseNameField;
	protected JLabel statusLabel;
	protected JButton submitButton;
	protected JButton cancelButton;
	
	protected Color infoStatusColor, errorStatusColor;
	
	
	public ExportSetDialog()
	{
		super("Export Set");
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
					 * dir h28
					 * name h20
					 * submit+ cancel h28
					 * status h20
					 */
					
					int emptySpace = height - (20+28+20+28+20);
					
					int cancelWidth = width / 2;
					int submitWidth = width - cancelWidth;
					
					getFormatBox().setLocation(0, 0);
					getFormatBox().setSize(width, 20);
					getDirectoryField().setLocation(0, 20);
					getDirectoryField().setSize(width, 28);
					getBaseNameField().setLocation(0, 20+28);
					getBaseNameField().setSize(width, 20);
					getSubmitButton().setLocation(0, 20+28+20);
					getSubmitButton().setSize(submitWidth, 28);
					getCancelButton().setLocation(submitWidth, 20+28+20);
					getCancelButton().setSize(cancelWidth, 28);
					getStatusLabel().setLocation(0, 20+28+20+28+emptySpace);
					getStatusLabel().setSize(width, 20);
				}
			};
			
			pane.add(getFormatBox());
			pane.add(getDirectoryField());
			pane.add(getBaseNameField());
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
	
	
	
	
	public LatentFileBox getDirectoryField()
	{
		if (directoryField == null)
		{
			JFileChooser jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			JFileChooserWrapper wrapper = new JFileChooserWrapper(jfc);
			wrapper.setDisplayType(JFileChooserDisplayType.CUSTOM);
			wrapper.setCustomApproveButtonText("Choose folder");
			
			directoryField = new LatentFileBox();
			directoryField.setDefaultText("Choose a containing folder.");
			directoryField.setFileChooser(wrapper);
		}
		return this.directoryField;
	}
	
	
	
	
	public JTextField getBaseNameField()
	{
		if (baseNameField == null)
		{
			baseNameField = new JTextField();
		}
		return this.baseNameField;
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
			submitButton.addActionListener(e -> submitClicked());
		}
		return this.submitButton;
	}
	
	
	
	public JButton getCancelButton()
	{
		if (cancelButton == null)
		{
			cancelButton = new JButton();
			cancelButton.setText("Cancel");
			cancelButton.addActionListener(e -> cancelClicked());
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
		getDirectoryField().setSelectedFile(null);
		getBaseNameField().setText("File name template");
		
		setVisible(true);
		
		getBaseNameField().requestFocusInWindow();
		getBaseNameField().selectAll();
	}
	
	
	
	
	public void cancelClicked()
	{
		setVisible(false);
	}
	
	
	
	
	
	public void submitClicked()
	{
		if (validateFields())
			doExport();
	}
	
	
	
	
	
	
	
	public boolean validateFields()
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
					return false;
				}
				else
				{
					//It's ok now.
					return false;
				}
			}
			else
			{
				setErrorText("You need to choose an image format.");
				return false;
			}
		}
		
		
		
		//<Basename
		String basename = getBaseNameField().getText();
		
		//<Empty
		if (basename.length() == 0)
		{
			setStatusText("The number will be the entire filename.");
		}
		//Empty>
		
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
						if (basename.toLowerCase().endsWith(ext.toLowerCase()))
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
		
		
		
		//<Directory
		if (getDirectoryField().getSelectedFile() == null || !getDirectoryField().getSelectedFile().isDirectory())
		{
			setErrorText("You must choose a folder to save files in.");
			return false;
		}
		//Directory>
		
		
		return true;
	}
	
	
	
	
	
	public void doExport()
	{
		setVisible(false);
		getCallback().exportSetCallback(getDirectoryField().getSelectedFile(), getBaseNameField().getText(), 1, getFormatBox().getSelectedWriterSpi());
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
