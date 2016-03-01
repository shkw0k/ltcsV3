package ltcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;

import utils.Logger;

/**
 * This a thread that reads the WS URL once per second for override info.
 * The WS IRL can be web page or a local file.
 * 
 * This is should not be used in LTCS2.
 * 
 * @author skwok
 *
 */
@Deprecated
public class LtcsLegacy extends Thread {
	
	private Hashtable <String, Integer> fields = null;
	private Hashtable<String, String[]> table;
	private URL wsURL;
	
	public LtcsLegacy (String urlStr) throws IOException {
		table = new Hashtable<String, String[]> (6);
		wsURL = new URL (urlStr);

		getLtcsStatus ();
		start();
	}

	public String getField (String tel, String field) {
		int idx = fields.get(field);
		return table.get(tel)[idx];
	}
	
	public double getFieldDouble (String tel, String field) {
		return Double.parseDouble(getField (tel, field));
	}

	public int getFieldInt (String tel, String field) {
		return Integer.parseInt(getField (tel, field));
	}
	
	private void getLtcsStatus() throws IOException {
		InputStream ins = wsURL.openStream();
		BufferedReader rd = new BufferedReader (new InputStreamReader(ins));
		String head = rd.readLine();
		if (fields == null) 
			setFields(head);
		while (true) {
			String line = rd.readLine();
			if (line == null) break;
			String parts[] = line.split(",");
			table.put(parts[0], parts);
		}
		rd.close();
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				getLtcsStatus ();	
				sleep(1000);
			}
			catch (Exception e) {
				Logger.error(e.getMessage());
			}
		}
	}
	
	private void setFields (String line) {
		if (line == null) return;
		String parts[] = line.trim().split(",");
		
		int i, len = parts.length;
		fields = new Hashtable<String, Integer> (len);
		
		for (i = 0; i < len; ++i) {
			fields.put(parts[i], i);
		}
	}	
}
