/*
 * Created on Jun 18, 2008
 * 	by the great Eclipse(c)
 */
package rebound.jagent.ui.gui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import rebound.jagent.ResourceHog;

public class AboutDialog
extends JFrame
{
	public static final String DEFAULT_TITLE = "About";
	public static final int DEFAULT_WIDTH = 800;
	public static final int DEFAULT_HEIGHT = 495;
	
	
	protected String resource;
	
	protected JPanel pane;
	protected JScrollPane scrollPane;
	protected JEditorPane mediaPane;
	protected JButton closeButton;
	
	
	public AboutDialog(String resource)
	{
		super();
		setResource(resource);
		setSize(800, 495);
		setContentPane(getPane());
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setResizable(true);
		//		loadWindowProperties(resource);
	}
	
	
	/* Todo Scan the about.html resource for window properties (title, width, height)
	public void loadWindowProperties(String resource)
	{
		//Extract the properties
		int preferredWidth = 0;
		int preferredHeight = 0;
		String preferredTitle = null;
		{
			try
			{
				InputStream in = ResourceHog.getResourceAsStream(resource);
				XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
				fac.setValidating(false);
				XmlPullParser parser = fac.newPullParser();
				parser.setInput(in, "UTF-8");
				
				//Find the html tag
				while (true)
				{
					parser.next();
					
					if (parser.getEventType() == XmlPullParser.START_TAG)
					{
						if (parser.getName().equalsIgnoreCase("html"))
							break;
					}
					else if (parser.getEventType() == XmlPullParser.END_DOCUMENT)
					{
						throw new IOException();
					}
				}
				
				//TODO Find head tag
				
				//TODO Scan head children looking for meta and title
				
				//TODO Make about-edos.html into xhtml
			}
			catch (IOException exc)
			{
				//Just fall back to defaults
			}
			catch (XmlPullParserException exc)
			{
			}
		}
		
		
		
		//Apply the properties
		if (preferredTitle == null)
			preferredTitle = DEFAULT_TITLE;
		if (preferredWidth == 0)
			preferredWidth = DEFAULT_WIDTH;
		if (preferredHeight == 0)
			preferredHeight = DEFAULT_HEIGHT;
		
		setTitle(preferredTitle);
		setSize(preferredWidth, preferredHeight);
	}
	 */
	
	
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
					
					float buttonWidthRatio = 1f/3f;
					int buttonHeight = 28;
					
					
					
					int width = getWidth();
					int height = getHeight();
					
					int buttonWidth = (int)(width * buttonWidthRatio);
					int buttonPadding = (width - buttonWidth) / 2;
					
					getScrollPane().setLocation(0, 0);
					getScrollPane().setSize(width, height - buttonHeight);
					
					getCloseButton().setLocation(buttonPadding, height-buttonHeight);
					getCloseButton().setSize(buttonWidth, buttonHeight);
				}
			};
			
			pane.add(getScrollPane());
			pane.add(getCloseButton());
		}
		return this.pane;
	}
	
	public JScrollPane getScrollPane()
	{
		if (scrollPane == null)
		{
			scrollPane = new JScrollPane(getMediaPane());
		}
		return this.scrollPane;
	}
	
	public JEditorPane getMediaPane()
	{
		if (mediaPane == null)
		{
			mediaPane = new JEditorPane();
			mediaPane.setEditable(false);
			
			//Hyperlinks
			{
				final JEditorPane _mp = mediaPane; //We're not going to change it
				mediaPane.addHyperlinkListener(e -> {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                    {
                        if ("mailto".equalsIgnoreCase(e.getURL().getProtocol()))
                        {
                            if (Desktop.isDesktopSupported())
                            {
                                try
                                {
                                    Desktop.getDesktop().mail(e.getURL().toURI());
                                }
                                catch (IOException | URISyntaxException ignored)
                                {
                                }
                            }
                        }
                        else
                        {
                            //Load the page in the about browser
                            try
                            {
                                _mp.setPage(e.getURL());
                            }
                            catch (IOException exc)
                            {
                                setPageToIOError(exc);
                            }
                        }
                    }
                });
			}
			
			
			//Load initial content
			URL contentURL = getContentURL();
			
			if (contentURL == null)
			{
				setPageTo404(getResource());
			}
			else
			{
				mediaPane.setContentType("text/html");
				
				try
				{
					mediaPane.setPage(contentURL);
				}
				catch (IOException exc)
				{
					setPageToIOError(exc);
				}
			}
		}
		return this.mediaPane;
	}
	
	public void setPageTo404(String charge)
	{
		mediaPane.setContentType("text/html");
		mediaPane.setText
		(
				"<html>" +
						"	<body bgcolor=\"#800000\">" +
						"		<center>" +
						"			<h1>" +
						"				<font color=\"#FFFF00\">" +
						"					Could not locate required resource <tt>"+charge+"</tt>" +
						"				</font>" +
						"			</h1>" +
						"		</center>" +
						"	</body>" +
						"</html>"
				);
	}
	
	public void setPageToIOError(IOException exc)
	{
		mediaPane.setContentType("text/html");
		mediaPane.setText
		(
				"<html>" +
						"	<body bgcolor=\"#800000\">" +
						"		<center>" +
						"			<h1>" +
						"				<font color=\"#FFFF00\">" +
						"					IO Error loading required resource <tt>"+getResource()+"</tt>: "+exc.getMessage()+
						"				</font>" +
						"			</h1>" +
						"		</center>" +
						"	</body>" +
						"</html>"
				);
	}
	
	
	public JButton getCloseButton()
	{
		if (closeButton == null)
		{
			closeButton = new JButton();
			closeButton.setText("Close");
			closeButton.addActionListener(e -> closeButtonClicked());
		}
		return closeButton;
	}
	
	
	
	
	
	public URL getContentURL()
	{
		return ResourceHog.getResource(getResource());
	}
	
	
	
	
	
	
	
	public void closeButtonClicked()
	{
		setVisible(false);
	}
	
	
	public void display()
	{
		setVisible(true);
	}
	
	
	
	
	
	public String getResource()
	{
		return this.resource;
	}
	public void setResource(String resource)
	{
		this.resource = resource;
	}
}
