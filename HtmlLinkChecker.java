/**
 * Program Name: HtmlLinkChecker.java
 * Purpose: SwingWorker for checking links in html text input
 * Coder: Jason Benoit
 * Date: Nov 30, 2023
 */

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class HtmlLinkChecker extends SwingWorker<Report, String[]>
{

	String pageText;
	HashSet<String> checkedOK;
	PressbooksLinkChecker frame;
	Report report;
	int linksChecked;
	
	public HtmlLinkChecker(String pageText, PressbooksLinkChecker frame)
	{
		super();
		this.pageText = pageText;
		this.frame = frame;
	}

	@Override
	protected Report doInBackground() throws Exception
	{
		checkedOK = new HashSet<String>();
		report = new Report(frame.httpResponses);
		linksChecked = 0;
		ArrayList<String> links = new ArrayList<String>();
		
		int pos = pageText.indexOf("href=");;
		
		while (pos >= 0 && pos < pageText.length())
		{
			if (isCancelled())
			{
				frame.report = report;
				return null;
			}
			pos += 6;
			int aposIdx = pageText.indexOf('\'', pos);
			int quoteIdx = pageText.indexOf('\"', pos);
			int linkEndIdx = aposIdx < 0 ? quoteIdx < 0 ? pos : quoteIdx : 
				quoteIdx < 0 ? aposIdx : Math.min(aposIdx, pos);
			links.add(pageText.substring(pos, linkEndIdx));
			pageText = pageText.substring(linkEndIdx);
			pos = pageText.indexOf("href=");
		}
		
		checkLinks(links);

		return report;
	}
	
	protected void process(List<String[]> chunks)
	{
		frame.numLinksCheckedLbl.setText("Links checked: " + linksChecked);
		frame.tableLbl.setText("Problems Detected (" + String.valueOf(report.entries.size()) +")");
		for (String[] chunk : chunks)
		{
			if (isCancelled())
			{
				frame.report = report;
				return;
			}
			if (!chunk[2].equals("200"))
			{
				frame.tableModel.addEntry(chunk);
			}		
		}
	}
	
	protected void done()
	{
		try
		{
			frame.report = get();
			frame.scanHTMLBtn.setText("Check Links");
			frame.scanURLBtn.setText("Check Links");
			frame.numLinksCheckedLbl.setText(frame.numLinksCheckedLbl.getText() + " - Finished!");
			frame.exportBtn.setEnabled(true);
			frame.imageLbl.setVisible(false);
		} 
		catch (InterruptedException | ExecutionException e)
		{		
			JOptionPane.showMessageDialog(frame, "Something went wrong ... Please try again.");
			e.printStackTrace();
		}
		catch (CancellationException e)
		{
			System.out.println("HtmlLinkChecker stopped!");
			e.printStackTrace();
		}
	}
	
	private void checkLinks(ArrayList<String> links)
	{
		
		for (String link : links)
		{
			if (isCancelled())
			{
				frame.report = report;
				return;
			}
						
			// No need to check Table of Content links over and over again
			if (checkedOK.contains(link) || link.indexOf("http") != 0)
			{
				continue;
			}
			
			try
			{
				Connection connection = Jsoup.connect(link)
						.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
						.timeout(10000)
						.ignoreHttpErrors(true)
						.ignoreContentType(true);
				
				Connection.Response response = null;
				response = connection.execute();
				int statusCode = response.statusCode();
				String msg = response.statusMessage();
				
				linksChecked++;
				
				String[] line = {"Text Input", link, String.valueOf(statusCode), msg};			
				publish(line);
				
				if (statusCode == 200)
				{
					checkedOK.add(link);
				}
				else
				{
					report.addEntry("Text Input", link, statusCode, msg);
				}	
			}
			catch (UnknownHostException | IllegalArgumentException ex)
			{	
				//add line to the report showing invalid url
				report.addEntry("Text Input", link, -1, "Invalid URL");
				String[] line = {"Text Input", link, "-1", "Invalid URL"};			
				publish(line);				
			}
			catch (IOException ex)
			{
				report.addEntry("Text Input", link, 0, "Error reading response - possible redirect");
				String[] line = {"Text Input", link, "0", "Error reading response - possible redirect"};			
				publish(line);
			}			
		}
	}
}
//end class