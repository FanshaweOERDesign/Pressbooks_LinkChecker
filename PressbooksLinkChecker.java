/**!
Copyright (c) 2023 Jason Benoit

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

**This text is from: http://opensource.org/licenses/MIT**
!**/

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CancellationException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;

public class PressbooksLinkChecker extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	JButton scanURLBtn, scanHTMLBtn, exportBtn;
	JPanel inputPanel, resultsPanel;
	JTable problemTable;
	JTextField inputURLTxt;
	JTextArea inputHTMLTxt;
	JLabel numLinksCheckedLbl, inputLbl, titleLbl, imageLbl, tableLbl, inputHTMLLbl;
	String[] columnNames = {"Page", "Link", "Status", "Message"};
	Report report;
	int linksChecked;
	ProblemTableModel tableModel;
	JScrollPane scroll;
	String tableLblText = "Possible Problems Detected (0)";
	String linksCheckedText = "Links Checked: 0";
	String[][] httpResponses = {
			{"-2", "Invalid Page URL", "The Page link does not correspond to a page in this Pressbooks resource."},
			{"-1", "Invalid URL", "There is a syntax error in the supplied URL or else it is invalid for some other reason."},
			{"0", "Read Error", "Often received when address redirects to a new URL. Advisable to replace with new URL in these cases."},
      {"200", "OK", "The request was successful."},
      {"201", "Created", "The request was successful, and a resource was created."},
      {"204", "No Content", "The server successfully processed the request, but there is no content to send back."},
      {"400", "Bad Request", "The server could not understand the request."},
      {"401", "Unauthorized", "The request requires user authentication."},
      {"403", "Forbidden", "The server understood the request but refuses to authorize it. Received from sites that prompt user to sign in."},
      {"404", "Not Found", "The requested resource could not be found on the server. Received when there is no file/page at the provided URL."},
      {"500", "Internal Server Error", "A generic error message returned when an unexpected condition was encountered."},
      {"503", "Service Unavailable", "The server is not ready to handle the request. Common causes are a server that is down for maintenance or is overloaded."}
  };
	
	public PressbooksLinkChecker()
	{
		// Set up window
		super("Pressbooks Link Checker");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);//destroy this object when close button is pressed
		this.setSize(500, 500); //width and height in pixels
		this.setLocationRelativeTo(null);//centers the JFrame on the screen.
		Image icon = new ImageIcon(getClass().getResource("images/oer_logo.jpg")).getImage();
		this.setIconImage(icon);
		this.setLayout(new GridLayout(2, 1));
		
		// Initialize program variables
		report = new Report(httpResponses);
		linksChecked = 0;
		inputPanel = new JPanel();
		tableModel = new ProblemTableModel();
		
		/*Build GUI*/
		
		// Input field
		inputLbl = new JLabel("Enter URL to Pressbooks page below");
		JLabel inputLbl2 = new JLabel("(scans whole book from any page)");
		JPanel inputLblPanel = new JPanel();
		inputLblPanel.setLayout(new BoxLayout(inputLblPanel, BoxLayout.Y_AXIS));
		JPanel lblPanel1 = new JPanel();
		lblPanel1.add(inputLbl);
		JPanel lblPanel2 = new JPanel();
		lblPanel2.add(inputLbl2);
		inputLblPanel.add(lblPanel1);
		inputLblPanel.add(lblPanel2);
		inputURLTxt = new JTextField();
		inputURLTxt.setColumns(30);
		inputURLTxt.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e)
			{
				((JTextComponent) e.getSource()).selectAll();			
			}
			@Override
			public void focusLost(FocusEvent e){}			
		});
		JPanel inputTxtPanel = new JPanel();
		inputTxtPanel.add(inputURLTxt);	
		JTabbedPane tabbedPane = new JTabbedPane();
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
		inputPanel.add(inputLblPanel);
		inputPanel.add(inputTxtPanel);
		JPanel checkLinksBtnPanel = new JPanel();
		scanURLBtn = new JButton("Check Links");	
		checkLinksBtnPanel.add(scanURLBtn);
		inputPanel.add(checkLinksBtnPanel);
		tabbedPane.addTab("Scan Book from URL", inputPanel);
		
		// HTML Input
		inputHTMLTxt = new JTextArea(10, 50);
		inputHTMLTxt.setText("Paste HTML here");
		inputHTMLTxt.setLineWrap(true);
		inputHTMLTxt.setEditable(true);
		inputHTMLTxt.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e)
			{
				((JTextComponent) e.getSource()).selectAll();			
			}
			@Override
			public void focusLost(FocusEvent e){}			
		});

		JScrollPane htmlTextareaScroll = new JScrollPane(inputHTMLTxt);
		
		scanHTMLBtn = new JButton("Check Links");
		JPanel scanHTMLBtnPanel = new JPanel();
		scanHTMLBtnPanel.add(scanHTMLBtn);
		
		JPanel htmlInputPanel = new JPanel();
		htmlInputPanel.setLayout(new BoxLayout(htmlInputPanel, BoxLayout.Y_AXIS));
		htmlInputPanel.add(htmlTextareaScroll);
		htmlInputPanel.add(scanHTMLBtnPanel);		
		tabbedPane.addTab("Scan from HTML", htmlInputPanel);
		
		// Layout top panel
		JPanel sourcePanel = new JPanel(new GridLayout(1, 1));
		JPanel bottomSpacer = new JPanel();
		bottomSpacer.setPreferredSize(new Dimension(1, 20));
		sourcePanel.add(tabbedPane);
		this.add(sourcePanel);
		
		// Problem Table
		tableLbl = new JLabel(tableLblText);
		JPanel tblLblPanel = new JPanel();
		tblLblPanel.add(tableLbl);
		problemTable = new JTable(tableModel);
		problemTable.addMouseListener(new MouseAdapter() {
		  public void mouseClicked(MouseEvent e) {
		    if (e.getClickCount() == 1) {
		      JTable target = (JTable)e.getSource();
		      int row = target.getSelectedRow();
		      String code = String.valueOf(target.getModel().getValueAt(row, 2));
		      int httpResponsesIdx = 0;
		      for (;httpResponsesIdx < httpResponses.length; httpResponsesIdx++)
		      {
		      	if (httpResponses[httpResponsesIdx][0].equals(code))
		      	{
		      		break;
		      	}
		      }
		      
		      String description;
		      String message;
		      String formattedDescription = "";
		      
		      if (httpResponsesIdx >= httpResponses.length)
		      {
		      	message = String.valueOf(target.getModel().getValueAt(row, 1));
		      	formattedDescription += message;
		      }
		      else
		      {
		      	description = httpResponses[httpResponsesIdx][2];
			      message = httpResponses[httpResponsesIdx][1];
			      
			      String currLine = "";
			      // add line breaks to manage pop-up width
			      for (int i = 0; i < description.length(); i++)
			      {
			      	currLine += description.charAt(i);
			      	
			      	if (currLine.length() > 50)
			      	{
			      		int lastSpace = currLine.lastIndexOf(' ');
			      		formattedDescription += currLine.substring(0, lastSpace) + '\n';
			      		currLine = currLine.substring(lastSpace + 1);
			      	}
			      }
			      
			      if (currLine.length() > 0)
			      {
			      	formattedDescription += currLine;
			      }
		      }
		      
		     JOptionPane.showMessageDialog( PressbooksLinkChecker.this,
             "Status: " + code + "\nMessage: " + message 
            		 + "\n\n" + formattedDescription,
             "Info",
             JOptionPane.INFORMATION_MESSAGE);
		    }
		  }
		});
		
		scroll = new JScrollPane(problemTable);	
		tableLbl.setVisible(false);
		scroll.setVisible(false);
		
		// Bottom Panel (Links checked, Export Button, etc)
		numLinksCheckedLbl = new JLabel(linksCheckedText);
		JPanel bottomLblPanel = new JPanel();
		bottomLblPanel.add(numLinksCheckedLbl);
		numLinksCheckedLbl.setVisible(false);
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(bottomLblPanel);		
		ImageIcon myImgIcon = new ImageIcon(getClass().getResource("images/working.gif"));
		imageLbl = new JLabel(myImgIcon);
		JPanel gifPanel = new JPanel();
		gifPanel.add(imageLbl);
		imageLbl.setVisible(false);
		bottomPanel.add(gifPanel);		
		exportBtn = new JButton("Export results to .csv");
		JPanel bottomBtnPanel = new JPanel();
		bottomBtnPanel.add(exportBtn);
		bottomPanel.add(bottomBtnPanel);
		exportBtn.setVisible(false);
			
		// Results Panel Layout
		resultsPanel = new JPanel();
		resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));		
		resultsPanel.add(tblLblPanel);
		resultsPanel.add(scroll);
		resultsPanel.add(bottomPanel);
		this.add(resultsPanel);
		
		// Add listener
		Listener listener = new Listener(this);
		scanURLBtn.addActionListener(listener);
		scanHTMLBtn.addActionListener(listener);
		exportBtn.addActionListener(listener);
		
		// Licence Panel
		JPanel licencePanel = new JPanel();
		licencePanel.setLayout(new BoxLayout(licencePanel, BoxLayout.Y_AXIS));
		licencePanel.setPreferredSize(new Dimension(300, 200));
		licencePanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
		JLabel licTitle = new JLabel("<html><h1>Pressbooks Link Checker</h1>");
		licTitle.setHorizontalAlignment(JLabel.CENTER);
		JLabel licTxt1 = new JLabel("<html>Created by</html>");
		licTxt1.setHorizontalAlignment(JLabel.CENTER);
		JLabel licTxt2 = new JLabel("<html>Jason Benoit</html>");
		licTxt2.setHorizontalAlignment(JLabel.CENTER);
		JLabel licTxt3 = new JLabel("<html>at the</html>");
		licTxt3.setHorizontalAlignment(JLabel.CENTER);
		JLabel studioLink = new JLabel("<html><a href='https://www.google.com'>Fanshawe OER Design Studio</a>.</html>");
		studioLink.addMouseListener(new MouseAdapter(){
			
			@Override
			public void mouseClicked(MouseEvent e) 
			{
				try {
          
          Desktop.getDesktop().browse(new URI("https://www.fanshawelibrary.com/oerdesignstudio/"));
           
	      } catch (IOException | URISyntaxException e1) {
	          JOptionPane.showMessageDialog( PressbooksLinkChecker.this,
	                  "Could not open the hyperlink. Error: " + e1.getMessage(),
	                  "Error",
	                  JOptionPane.ERROR_MESSAGE);
	      }     
			}
		});
		studioLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		studioLink.setHorizontalAlignment(JLabel.CENTER);
		JLabel licTxt4 = new JLabel("<html>Made available under the</html>");
		licTxt4.setHorizontalAlignment(JLabel.CENTER);
		JLabel licenceLink = new JLabel("<html><a href=''>MIT Licence</a>.");
		licenceLink.setHorizontalAlignment(JLabel.CENTER);
licenceLink.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {

			        String licenceStr = """
Copyright (c) 2023 Jason Benoit and Fanshawe OER Design Studio

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

**This text is from: http://opensource.org/licenses/MIT**
			        		""";
			        JOptionPane.showMessageDialog(e.getComponent(), licenceStr);		    
			}			
		});
		
		licenceLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		licencePanel.add(licTitle);
		//licencePanel.add(licIcon);
		licencePanel.add(licTxt1);
		licencePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		licencePanel.add(licTxt2);
		licencePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		licencePanel.add(licTxt3);
		licencePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		licencePanel.add(studioLink);
		licencePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		licencePanel.add(licTxt4);
		licencePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		licencePanel.add(licenceLink);
		
		//Make window visible
		this.setVisible(true);
		
		// Pop up licence panel
		JOptionPane.showMessageDialog(this.getContentPane(), licencePanel, "Welcome", JOptionPane.PLAIN_MESSAGE, null);
	}
	
	// Method to update labels showing number of links checked and problems detected
	public void updateLinkStats(int linksChecked, int problemsDetected)
	{
		numLinksCheckedLbl.setText(linksCheckedText.substring(0, linksCheckedText.lastIndexOf(' ') + 1) 
				+ String.valueOf(linksChecked));
		tableLbl.setText(tableLblText.substring(0, tableLblText.lastIndexOf(' ') + 1)
				+ '(' + String.valueOf(problemsDetected) + ')');
	}
	
	// Event handler
	private class Listener implements ActionListener
	{
		PressbooksLinkChecker frame;
		SwingWorker<Report, String[]> checker = null;
		
		public Listener(PressbooksLinkChecker frame)
		{
			this.frame = frame;
		}
		
		private void showResults()
		{
			frame.numLinksCheckedLbl.setText("Links Checked: 0");
			frame.numLinksCheckedLbl.setVisible(true);
			frame.exportBtn.setVisible(true);
			frame.exportBtn.setEnabled(false);
			frame.imageLbl.setVisible(true);
			frame.tableLbl.setText(tableLblText);
			frame.tableLbl.setVisible(true);
			frame.scroll.setVisible(true);
			frame.tableModel.clear();
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			// Whole book scan from URL
			if (e.getSource() == scanURLBtn)
			{	
				
				if (scanURLBtn.getText().equals("Check Links"))
				{
					scanURLBtn.setText("Stop Scan");
					scanHTMLBtn.setText("Stop Scan");
					String url = inputURLTxt.getText();
					showResults();
					checker = new LinkChecker(url, frame);
					checker.execute();	
				}
				else if (scanURLBtn.getText().equals("Stop Scan"))
				{
					scanURLBtn.setText("Check Links");
					scanHTMLBtn.setText("Check Links");
					
					if (checker != null)
					{
						try
						{
							checker.cancel(true);
						}
						catch (CancellationException ex)
						{
							ex.printStackTrace();
						}
						
						//frame.numLinksCheckedLbl.setText(frame.numLinksCheckedLbl.getText() + " - Finished!");
						
					}
					
					frame.exportBtn.setEnabled(true);
					frame.imageLbl.setVisible(false);
					
				}
				
				
			}
			// HTML text input scan
			else if (e.getSource() == scanHTMLBtn)
			{
				if (scanHTMLBtn.getText().equals("Check Links"))
				{
					scanURLBtn.setText("Stop Scan");
					scanHTMLBtn.setText("Stop Scan");
					String html = inputHTMLTxt.getText();
					showResults();
					checker = new HtmlLinkChecker(html, frame);
					checker.execute();
				}
				else if (scanHTMLBtn.getText().equals("Stop Scan"))
				{
					scanHTMLBtn.setText("Check Links");
					scanURLBtn.setText("Check Links");
					
					if (checker != null)
					{
						try
						{
							checker.cancel(true);
						}
						catch (CancellationException ex)
						{
							ex.printStackTrace();
						}			
					}
					
					frame.exportBtn.setEnabled(true);
					frame.imageLbl.setVisible(false);
				}
			}
			else if (e.getSource() == exportBtn)
			{
				JFileChooser chooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("Comma-Separated Values (.csv)",
		        "csv");
				chooser.setFileFilter(filter);
				int retVal = chooser.showSaveDialog(frame);
				if (retVal == JFileChooser.APPROVE_OPTION)
				{
					File path = chooser.getSelectedFile();
					String pathStr = path.toString();
					int extensionIdx = pathStr.lastIndexOf('.');
					if (extensionIdx < 0 || !pathStr.substring(extensionIdx).equals(".csv"))
					{
						pathStr += ".csv";
						path = new File(pathStr);
					}
					report.saveAsCSV(pathStr, frame);
				}
			}			
		}
	}
		
	
	public static void main(String[] args)
	{
		new PressbooksLinkChecker();
	}

}
//end class