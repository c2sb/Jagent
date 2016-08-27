/*
 * Created on May 27, 2006
 * 	by the wonderful Eclipse(c)
 */
package rebound.jagent.ui.gui.monk;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import rebound.jagent.ResourceHog;
import rebound.jagent.ui.gui.DropData;
import rebound.jagent.ui.gui.common.GUI;
import rebound.jagent.ui.gui.monk.Monkifier.OutputType;

public class MonkWindow
extends JFrame
implements DropTargetListener, Notifee, ItemListener
{
	public static final boolean UseAboutMenuItem;
	public static final boolean UseQuitMenuItem;
	
	static
	{
		UseAboutMenuItem = !MonkApplicationCoordinator.AppleJavaExtentionsPresent;
		UseQuitMenuItem = !MonkApplicationCoordinator.AppleJavaExtentionsPresent;
	}
	
	
	protected MonkApplicationCoordinator coordinator;
	
	protected JLabel currAction, bg;
	protected JRadioButton outPray, outPraySource;
	protected JPanel body;
	
	protected DropTarget target;
	protected ButtonGroup outGroup;
	protected Monkifier chimp;
	
	protected JMenuBar mnu;
	protected JMenu mnuFile;
	protected JMenuItem mnuFileQuit;
	protected JMenu mnuEdit;
	protected JCheckBoxMenuItem mnuEditMergeScripts;
	protected JMenu mnuHelp;
	protected final JMenuItem mnuHelpAbout = GUI.createMenuItem("About", KeyEvent.VK_F1, e -> getApplicationCoordinator().aboutClicked());
	
	public MonkWindow()
	{
		super("Monk");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		chimp = new Monkifier(this);
		outGroup = new ButtonGroup();
		
		JPanel body = getBody();
		
		if (body != null) //Todo pattern this better
		{
			setContentPane(body);
			
			//if (UseAboutMenuItem || UseQuitMenuItem || true) //as of 2.0.5 (2012-10-06), the merge-scripts setting requires the menu bar to always be shown because it will never be empty.
			setJMenuBar(getMnu());
			
			Icon i = getBG().getIcon();
			if (i == null)
				setSize(250, 250);
			else
				setSize(i.getIconWidth(), i.getIconHeight());
		}
	}
	
	
	
	
	public JPanel getBody()
	{
		if (body == null)
		{
			body = new JPanel(null, false)
			{
				public void doLayout()
				{
					super.doLayout();
					
					int w = getWidth();
					int h = getHeight();
					int cw = w/2;
					
					getBG().setSize(w, h);
					
					getCurrAction().setSize(w, 20);
					getCurrAction().setLocation(0, h-getCurrAction().getHeight());
					
					getOutputPray().setSize(cw, 20);
					
					getOutputPraySource().setLocation(cw, 0);
					getOutputPraySource().setSize(w-cw, 20);
				}
			};
			
			
			JLabel bg = getBG();
			if (bg != null)
			{
				body.add(getOutputPray());
				body.add(getOutputPraySource());
				body.add(getCurrAction());
				body.add(bg);
			}
			else
			{
				JLabel errorLbl = new JLabel();
				errorLbl.setHorizontalAlignment(JLabel.CENTER);
				errorLbl.setBackground(new Color(128, 0, 0));
				errorLbl.setForeground(Color.YELLOW);
				errorLbl.setText("Face image could not be found.");
				
				getContentPane().add(errorLbl);
				setSize(250, 250);
				getContentPane().setBackground(Color.RED);
				
				return null;
			}
		}
		return body;
	}
	
	public JLabel getBG()
	{
		if (bg == null)
		{
			bg = new JLabel();
			
			URL u = ResourceHog.getResource("monk-face.png");
			if (u == null)
				return null;
			//else
			
			Icon i = new ImageIcon(u);
			bg.setIcon(i);
			bg.setSize(i.getIconWidth(), i.getIconHeight());
			
			target = new DropTarget(bg, this);
		}
		return bg;
	}
	
	public JLabel getCurrAction()
	{
		if (currAction == null)
		{
			currAction = new JLabel();
			currAction.setHorizontalAlignment(JLabel.CENTER);
			currAction.setText("Awaiting files");
			
			currAction.addMouseListener(new MouseListener()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					currAction.setText("Awaiting files");
				}

				@Override
				public void mouseExited(MouseEvent e) {}
				@Override
				public void mouseEntered(MouseEvent e) {}
				@Override
				public void mouseReleased(MouseEvent e) {}
				@Override
				public void mousePressed(MouseEvent e) {}
			});
		}
		return currAction;
	}
	
	public JRadioButton getOutputPray()
	{
		if (outPray == null)
		{
			outPray = new JRadioButton("PRAY Chunk", true);
			outPray.setOpaque(false);
			outPray.setBackground(new Color(0, 0, 0, 0));
			outPray.addItemListener(this);
			outGroup.add(outPray);
		}
		return outPray;
	}
	
	public JRadioButton getOutputPraySource()
	{
		if (outPraySource == null)
		{
			outPraySource = new JRadioButton("PRAY Source", false);
			outPraySource.setOpaque(false);
			outPraySource.setBackground(new Color(0, 0, 0, 0));
			outPraySource.addItemListener(this);
			outGroup.add(outPraySource);
		}
		return outPraySource;
	}
	
	
	public JMenuBar getMnu()
	{
		if (mnu == null)
		{
			mnu = new JMenuBar();
			
			if (UseQuitMenuItem) mnu.add(getMnuFile());
			mnu.add(getMnuEdit());
			if (UseAboutMenuItem) mnu.add(getMnuHelp());
		}
		return this.mnu;
	}
	
	public JMenu getMnuFile()
	{
		if (mnuFile == null)
		{
			mnuFile = new JMenu("File");
			if (UseQuitMenuItem) mnuFile.add(getMnuFileQuit());
		}
		return this.mnuFile;
	}
	
	public JMenuItem getMnuFileQuit()
	{
		if (mnuFileQuit == null)
		{
			mnuFileQuit = new JMenuItem("Quit");
			mnuFileQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuFileQuit.addActionListener(e -> getApplicationCoordinator().quitClicked());
		}
		return this.mnuFileQuit;
	}
	
	public JMenu getMnuEdit()
	{
		if (mnuEdit == null)
		{
			mnuEdit = new JMenu("Edit");
			mnuEdit.add(getMnuEditMergeScripts());
		}
		return this.mnuEdit;
	}
	
	public JCheckBoxMenuItem getMnuEditMergeScripts()
	{
		if (mnuEditMergeScripts == null)
		{
			mnuEditMergeScripts = new JCheckBoxMenuItem("Merge scripts", true);
			mnuEditMergeScripts.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			mnuEditMergeScripts.addItemListener(this);
			mnuEditMergeScripts.addActionListener(e -> {
                itemStateChanged(null); //refresh the setting on the model (monkifier)
            });
		}
		return this.mnuEditMergeScripts;
	}
	
	public JMenu getMnuHelp()
	{
		if (mnuHelp == null)
		{
			mnuHelp = new JMenu("Help");
			if (UseAboutMenuItem) mnuHelp.add(mnuHelpAbout);
		}
		return this.mnuHelp;
	}

	
	
	
	
	
	
	
	public OutputType getOutputType()
	{
		if (getOutputPray().isSelected())
			return OutputType.PRAY;
		if (getOutputPraySource().isSelected())
			return OutputType.STANDARD_PRAY_TEMPLATE_SOURCE;
		return null;
	}
	
	public OutputType getSafeOutputType()
	{
		OutputType o = getOutputType();
		if (o == null)
			return OutputType.PRAY;
		else
			return o;
	}
	
	
	@Override
	public void notify(String message)
	{
		System.out.println("info: "+message);
		setStatus(message);
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
		{
			setStatus("Error");
			JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	public void setStatus(String s)
	{
		getCurrAction().setText(s);
	}
	
	
	public MonkApplicationCoordinator getApplicationCoordinator()
	{
		return this.coordinator;
	}
	
	public void setApplicationCoordinator(MonkApplicationCoordinator coordinator)
	{
		this.coordinator = coordinator;
	}
	
	
	
	
	
	
	
	public void process(final Collection<File> files)
	{
		new Thread(() -> chimp.monkify(files)).start();
	}
	
	public void process(final File... files)
	{
		new Thread(() -> chimp.monkify(files)).start();
	}
	
	public void process(final File file)
	{
		new Thread(() -> chimp.monkify(file)).start();
	}
	
	
	
	
	
	
	
	//Events
	public void drop(DropTargetDropEvent dtde)
	{
		try
		{
			List<File> files = DropData.getFileList(dtde);
			
			if (files != null)
				process(files);
			else
			{
				dtde.rejectDrop();
				dtde.dropComplete(false);
			}
		}
		catch (IOException exc)
		{
			error("Error occurred dropping: \""+exc.getMessage()+"\"");
		}
	}
	
	public void itemStateChanged(ItemEvent e)
	{
		chimp.setDesiredOutput(getSafeOutputType());
		chimp.setMergeScripts(getMnuEditMergeScripts().isSelected());
	}
	
	
	
	
	
	//Unused
	public void dragEnter(DropTargetDragEvent dtde) {}
	public void dragOver(DropTargetDragEvent dtde) {}
	public void dropActionChanged(DropTargetDragEvent dtde) {}
	public void dragExit(DropTargetEvent dte) {}
}
