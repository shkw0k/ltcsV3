package utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import utils.HttpHandler.ArgList;

/**
 * Abstract class to handle HTTP request.
 * 
 * @author skwok
 * 
 */
public abstract class HttpHandler {

	public class ArgList extends Hashtable<String, String> {
		private static final long serialVersionUID = 2808756263554271353L;

		public double getDouble(String s) {
			return Double.parseDouble(s);
		}

		public int getInt(String s) {
			return Integer.parseInt(s);
		}
		
		public String toString() {
			Enumeration<String> k = keys();
			StringBuffer sb = new StringBuffer();
			String sep = "";
			while (k.hasMoreElements()) {
				String key = k.nextElement();
				String value = get(key);
				sb.append(sep + key + "=" + value);
				sep = ", ";
			}
			return sb.toString();
		}
	};

	protected String docRoot = ".";
	protected OutputStream oStream = null;
	protected BufferedReader reader = null;

	public HttpHandler() {
		// dummy constructor
	}

	public String getDocRoot() {
		return docRoot;
	}

	abstract public void handleRequest(String command, ArgList args);

	protected void outputHeaderType(String type) {
		write("HTTP/1.0 200 OK\n");
		if (type != null && type.length() > 0)
			write("Content-type: " + type + "\n");
		write("\n");
	}

	protected ArgList query2Hash(String args) {
		ArgList ht = new ArgList();
		if (args == null || args.length() == 0)
			return ht;
		String parts[] = args.split("&");
		int i, len = parts.length;
		for (i = 0; i < len; ++i) {
			String p2[] = parts[i].split("=");
			if (p2.length == 1) {
				ht.put(p2[0], "");
			} else if (p2.length == 2) {
				ht.put(p2[0], p2[1]);
			}
		}
		return ht;
	}

	private Vector<String> readHeader() throws IOException {
		Vector<String> headers = new Vector<String>(10);
		while (true) {
			String str = reader.readLine();
			if (str == null)
				return null;
			if (str.length() == 0) {
				break;
			}
			headers.add(str);
		}
		return headers;
	}

	protected ArgList readPostValues() throws IOException {
		StringBuilder sb = new StringBuilder();
		char cbuf[] = new char[10000];
		while (reader.ready()) {
			int res = reader.read(cbuf);
			if (res == 0)
				break;
			String line = new String(cbuf);
			if (line.length() == 0)
				break;
			sb.append(line);
		}
		return query2Hash(sb.toString());
	}

	public void processRequest() {
		try {
			Vector<String> headers = readHeader();
			if (headers == null)
				return;
			String strs[] = ((String) headers.get(0)).split(" ");
			URL action = new URL(new URL("http://localhost/"), strs[1]);
			String command = action.getPath();
			String args = action.getQuery();
			ArgList htable = query2Hash(args);
			handleRequest(command, htable);
		} catch (IOException e) {
			Logger.error(e.getMessage());
			outputHeaderType("text/plain");
		} finally {
			closeSockets();
		}
	}

	protected void closeSockets() {
		try {
			oStream.close();
		} catch (Exception e) {
			;
		}
		try {
			reader.close();
		} catch (Exception e) {
			;
		}
	}

	protected String getMimeType(String name) {
		String mt = URLConnection.guessContentTypeFromName(name);
		if (mt != null)
			return mt;
		name = name.toLowerCase();
		if (name.endsWith(".js"))
			return "text/javascript";
		if (name.endsWith(".css"))
			return "text/css";
		if (name.endsWith(".xml"))
			return "application/xml";
		return "text/plain";
	}

	/**
	 * Serves a web page, image or any other type of files.
	 * 
	 * @param request
	 * @return
	 */
	protected boolean servePage(String request) {
		File ff = new File(docRoot + request);
		if (ff.canRead()) {
			String mimetype = getMimeType(ff.getName());
			try {
				BufferedInputStream rd = new BufferedInputStream(
						new FileInputStream(ff));
				byte buf[] = new byte[1024 * 10];
				outputHeaderType(mimetype);
				while (rd.available() > 0) {
					int res = rd.read(buf);
					if (res < 0)
						break;
					oStream.write(buf, 0, res);
				}
				rd.close();
				oStream.flush();
				return true;
			} catch (IOException e) {
				Logger.error("Failed to process " + request + " " + e.getMessage());
			}
		}
		return false;
	}

	public void setDocRoot(String docRoot) {
		this.docRoot = docRoot;
	}

	public void setSocket(Socket ss) throws IOException {
		reader = new BufferedReader(new InputStreamReader(ss.getInputStream()));
		oStream = ss.getOutputStream();
	} // setSocket

	protected void write(String out) {
		try {
			oStream.write(out.getBytes());
		} catch (IOException e) {
			;
		}
	}

	protected void write(String out, int off, int len) {
		try {
			oStream.write(out.getBytes(), off, len);
		} catch (IOException e) {
			;
		}
	}
} // Class HttpHandler
