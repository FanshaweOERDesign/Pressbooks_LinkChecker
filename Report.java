/**
 * Program Name: Report.java
 * Purpose: TODO
 * Coder: Jason Benoit 0885941
 * Date: Nov 29, 2023
 */
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.swing.JOptionPane;

public class Report
{
	public ArrayList<Entry> entries;
	String[][] httpResponseCodes;
	
	public Report(String [][] httpResponseCodes)
	{
		this.entries = new ArrayList<Entry>();
		this.httpResponseCodes = httpResponseCodes;
	}
	
	public void addEntry(String pageURL, String linkURL, int status, String message)
	{
		Entry entry = new Entry();
		entry.pageURL = pageURL;
		entry.linkURL = linkURL;
		entry.status = status;
		entry.message = message;
		entries.add(entry);
	}
	
	private String responseDescription(int responseCode)
	{
		for (String[] response : httpResponseCodes)
		{
			if (String.valueOf(responseCode).equals(response[0]))
			{
				return response[2];
			}
		}
		
		return "Description not found";
	}
	
	public void saveAsCSV(String path, PressbooksLinkChecker frame)
	{
		try
		{
			FileWriter fw = new FileWriter(path);
			PrintWriter pw = new PrintWriter(fw);
			
			pw.print("PAGE, LINK, STATUS, MESSAGE, DESCRIPTION\n");
			
			for (Entry entry : entries)
			{
				pw.print("=HYPERLINK(\"" + entry.pageURL + "\"),");
				pw.print("=HYPERLINK(\"" + entry.linkURL + "\"),");
				pw.print(entry.status  + ",");
				pw.print(entry.message + ",");
				pw.print(responseDescription(entry.status));
				pw.print('\n');
			}
			
			pw.close();
			fw.close();
		}
		catch (IOException ex)
		{
			JOptionPane.showMessageDialog(frame, "Error: could not open file.");
		}		
	}
	
	class Entry
	{
		public String pageURL;
		public String linkURL;
		public int status;
		public String message;
	}
}
//end class