/**
 * Class Name: LinkChecker.java
 * Purpose: SwingWorker class for link checking operations
 * Coder: Jason Benoit
 * Date: Nov 29, 2023
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LinkChecker extends SwingWorker<Report, String[]> 
{	
	String url;
	HashSet<String> checkedOK;
	ArrayList<String>tocLinks;
	PressbooksLinkChecker frame;
	Report report;
	int linksChecked;
	
	public LinkChecker(String url, PressbooksLinkChecker frame)
	{
		super();
		this.url = url;
		this.frame = frame;
	}

	@Override
	protected Report doInBackground() throws Exception
	{
		checkedOK = new HashSet<String>();
		tocLinks = new ArrayList<String>();
		report = new Report(frame.httpResponses);
		linksChecked = 0;
		
		Document doc = loadPage(url);
		
		if (doc != null)
		{			
			// Get table of contents links
			Elements el = doc.select("p.toc__title > a");
			
			// If this is the cover page, use "Read Book" button URL instead
			if (el.size() == 0)
			{
				Elements readBtn = doc.select("div.book-header__cta a.call-to-action");
				Element btnURL = readBtn.first();
				url = btnURL.attr("href");
				report = doInBackground();
				return report;
			}
			
			for (Element element : el)
			{
				if (isCancelled())
				{
					frame.report = report;
					return null;
				}
				String src = element.attr("href");
				tocLinks.add(src);
				checkedOK.add(src);			
			}
		}
	
		for (String pageURL : tocLinks)
		{
			
			if (isCancelled())
			{
				frame.report = report;
				return null;
			}
			Document page = loadPage(pageURL);
			
			// Prevent re-checking table of contents and other PB components
			for (Element pbDiv : page.select(".header, .footer"))
			{
				pbDiv.remove();
			}
			
			linksChecked++;
			
			checkLinks(page, pageURL);
		}	

		return report;
	}
	
	protected void process(List<String[]> chunks)
	{
		frame.updateLinkStats(linksChecked, report.entries.size());
		for (String[] chunk : chunks)
		{
			if (!chunk[2].equals("200"))
			{
				frame.tableModel.addEntry(chunk);
			}		
		}
	}
	
	private void resetFrame()
	{
		frame.numLinksCheckedLbl.setText(frame.numLinksCheckedLbl.getText() + " - Finished!");
		frame.scanHTMLBtn.setText("Check Links");
		frame.scanURLBtn.setText("Check Links");
		frame.exportBtn.setEnabled(true);
		frame.imageLbl.setVisible(false);
	}
	
	protected void done()
	{
		try
		{
			
			frame.report = get();
			resetFrame();
			
		} 
		catch (InterruptedException | ExecutionException e)
		{		
			JOptionPane.showMessageDialog(frame, "Something went wrong ... Please try again.");
		}
		catch (CancellationException e)
		{
			System.out.println("LinkChecker cancelled!\n");
			e.printStackTrace();
		}
	}
	
	private Document loadPage(String pageURL)
	{
		if (isCancelled())
		{
			frame.report = report;
			return null;
		}
		try
		{			
			Connection connection = Jsoup.connect(pageURL)
					.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
					.timeout(10000)
					.ignoreContentType(true);
			
			Connection.Response response = null;
			response = connection.execute();
			int statusCode = response.statusCode();
			
			if (statusCode == 200)
			{
				return connection.get();
			}
			else
			{
				return null;
			}
		}
		catch (UnknownHostException | IllegalArgumentException ex)
		{	
			report.addEntry(pageURL, "N/A", -2, "Invalid Page URL");
			String[] line = {pageURL, "N/A", "-2", "Invalid Page URL"};			
			publish(line);			
		}
		catch (IOException ex)
		{
			report.addEntry(pageURL, "N/A", 0, "Error reading response");
			String[] line = {pageURL, "N/A", "0", "Error reading response"};			
			publish(line);
		}
		return null;
	}
	
	private void checkLinks(Document page, String pageURL)
	{
		if (page == null)
		{
			return;
		}
				
		Elements links = page.select("a:not(.twitter, .footer__pressbooks__icon)");
		
		for (Element link : links)
		{
			if (isCancelled())
			{
				frame.report = report;
				return;
			}
			
			String linkURL = link.attr("href");
						
			// No need to check Table of Content links over and over again
			if (checkedOK.contains(linkURL) || linkURL.indexOf("http") != 0)
			{
				continue;
			}
			
			try
			{
				Connection connection = Jsoup.connect(linkURL)
						.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
						.timeout(10000)
						.ignoreHttpErrors(true)
						.ignoreContentType(true);
				
				Connection.Response response = null;
				response = connection.execute();
				int statusCode = response.statusCode();
				String msg = response.statusMessage();
				
				linksChecked++;
				
				String[] line = {pageURL, linkURL, String.valueOf(statusCode), msg};			
				publish(line);
				
				if (statusCode == 200)
				{
					checkedOK.add(linkURL);
				}
				else
				{
					report.addEntry(pageURL, linkURL, statusCode, msg);
				}	
			}
			catch (UnknownHostException | IllegalArgumentException ex)
			{	
				//add line to the report showing invalid url
				report.addEntry(pageURL, linkURL, -1, "Invalid URL");
				String[] line = {pageURL, linkURL, "-1", "Invalid URL"};			
				publish(line);				
			}
			catch (IOException ex)
			{
				report.addEntry(pageURL, linkURL, 0, "Error reading response - possible redirect");
				String[] line = {pageURL, linkURL, "0", "Error reading response - possible redirect"};			
				publish(line);
			}			
		}
	}
}
//end class