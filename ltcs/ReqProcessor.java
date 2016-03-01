package ltcs;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Iterator;

import models.PointingInfo;
import models.SiteInfo;
import config.Global;
import config.LtcsConfig;
import utils.HttpHandler;
import utils.Logger;

public class ReqProcessor extends HttpHandler {

	private final String APPJSON = "application/json";
	private final Collector collector;
	private GeometryAnalyzer geometryAnalyzer;
	private Global global;
	private LtcsConfig ltcsConfig;

	// private String LTCSLegacyURL =
	// "http://kapoli.keck.hawaii.edu/ltcsTools/ltcsws.php?Cmd=getTable&table=mergedPnt";

	/**
	 * This class handles the HTTP requests.
	 * 
	 * The methods that handle the requests have the form methodName(ArgList
	 * args, StringBuffer sb) and they return a String containing the mime-type.
	 * The method handleRequest() determines how to handle the requests.
	 * 
	 * @param collector
	 * @param geometryAnalyzer
	 */
	ReqProcessor(LtcsConfig lcfg, Collector collector, GeometryAnalyzer ga) {
		this.collector = collector;
		ltcsConfig = lcfg;
		geometryAnalyzer = ga;
		global = ltcsConfig.getGlobal();

		if (global.use_legacy) {
			Logger.error("Cannot use legacy override anymore.");
		}
	}

	public String findCollision(ArgList args, StringBuffer sb) throws IOException {
		Logger.info("pnt=" + args.get("pointingTel") + " targ=" + args.get("targetTel") + " dist="
				+ Double.parseDouble(args.get("distance")));
		double res[] = geometryAnalyzer.findCollision(args.get("pointingTel"), args.get("targetTel"),
				Double.parseDouble(args.get("distance")));
		sb.append(String.format("{az:%f, el:%f}", res[0], res[1]));
		Logger.info("Find collision " + res[0] + " " + res[1]);
		return APPJSON;
	}

	public String getReqPerSec(ArgList args, StringBuffer sb) throws IOException {
		double rps = collector.getReqPerSec();
		sb.append(String.format("{rps:%.2f}", rps));
		return APPJSON;
	}

	/**
	 * Gets telescope schedule from URL specified in config file. The format of
	 * the schedule is in json, which is then evaluated by the browser. Here,
	 * the schedule info is read from the web site and forwarded to the browser.
	 * 
	 * @param args
	 *            , not used
	 * @throws IOException
	 */
	public String getSchedule(ArgList args, StringBuffer sb) throws IOException {
		return APPJSON;
	}

	/**
	 * Getstatus and getPntInfos return the same information.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public String getStatus(ArgList args, StringBuffer sb) throws IOException {
		try {
			return pntInfosAsJson(args, sb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return APPJSON;
	}

	/**
	 * Processes the request. First, it checks the request (or command) and
	 * tried to invoke the command. If the command fails, tries to serve the
	 * request as a regular page (or mime media).
	 * 
	 * @see utils.HttpHandler#handleRequest(java.lang.String,
	 *      utils.HttpHandler.ArgList)
	 */
	@Override
	public void handleRequest(String reqStr, ArgList args) {
		Class<?> tc = getClass();
		Class<?> argClasses[] = new Class<?>[] { ArgList.class, StringBuffer.class };
		String command = reqStr;
		try {
			command = reqStr.replaceAll("/", "");
			if (command.equals(""))
				command = "ltcs";
			Method meth = tc.getMethod(command, argClasses);
			StringBuffer sb = new StringBuffer();
			String contType = (String) meth.invoke(this, args, sb);

			if (contType != null) {
				outputHeaderType(contType);
				outputContent(sb.toString());
			}
			Logger.info("OK, req=" + command + ", " + args.toString());
		} catch (Exception e) {
			if (!servePage(reqStr)) {
				output404(command, reqStr);
				Logger.error("Error, req=" + command + ", " + args.toString());
				e.printStackTrace();
			}
		}
		collector.count();
	}

	/**
	 * This is meant to be the entry point for the web site. For example:
	 * http://ltcsServer/ltcs
	 * 
	 * @param args
	 * @throws IOException
	 */
	public String ltcs(ArgList args, StringBuffer sb) throws IOException {
		servePage("/index.html");
		return null;
	}

	public String ltcsWS(ArgList args, StringBuffer sb) throws IOException {
		String ltcsWS = collector.getLtcsConfig().getGlobal().ltcsws_url;

		StringBuffer sbuf = new StringBuffer();
		String sep = ltcsWS.contains("?") ? "&" : "?";
		Enumeration<String> keys = args.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			String value = args.get(key);
			sbuf.append(sep);
			sbuf.append(key);
			sbuf.append("=");
			sbuf.append(value);
			sep = "&";
		}

		ltcsWS += sbuf.toString();
		URL url = new URL(ltcsWS);
		URLConnection ucon = url.openConnection();
		InputStream ins = ucon.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(ins);

		byte buf[] = new byte[5000];
		while (true) {
			int res = bis.read(buf);
			if (res <= 0)
				break;
			String s = new String(buf, 0, res);
			sb.append(s);
		}

		return APPJSON;
	}

	private void output404(String command, String reqStr) {
		write("HTTP/1.0 404 File not found\n\n");
		write(String.format("File not found: %s\n", command));
		Logger.error("Error while processing " + reqStr);
	}

	private void outputContent(String cont) {
		write(cont);
	}

	/**
	 * Outputs the PntInfos as JSON. Parameter tel can be 'all' or a site, ie.
	 * tel=KECK1
	 * 
	 * @param args
	 * @throws IOException
	 */
	private String pntInfosAsJson(ArgList args, StringBuffer sb) throws IOException {
		PntInfoReader pReader;
		PointingInfo pInfo;
		String tel = args.get("tel");
		tel = tel == null ? "all" : tel;
		boolean doProbeQuery = global.do_probequery;
		sb.append("{");
		if (!tel.equalsIgnoreCase("all")) {
			// Get PntInfo for one site=tel
			pReader = collector.pReaders.get(tel);
			if (pReader != null) {
				pInfo = pReader.getPntInfo(doProbeQuery);
				sb.append("'" + pInfo.site + "':");
				sb.append(pInfo.toJson());
			}
		} else {
			// Get PntInfo for all sites
			Iterator<PntInfoReader> en = collector.pReaders.values().iterator();
			String sep = "";
			while (en.hasNext()) {
				pReader = en.next();
				pInfo = pReader.getPntInfo(doProbeQuery);
				// System.out.println ("tel " + pInfo.telescope + " " +
				// pInfo.toJson());
				sb.append(sep);
				sb.append("'" + pInfo.site + "':");
				sb.append(pInfo.toJson());
				sep = ",\n";
			}
		}
		sb.append("}");

		return APPJSON;
	}

	/**
	 * Override info can be edited in the browser. When ready, that info is
	 * saved to a file via this method. If successful, the updated statuses for
	 * all telescopes are returned.
	 * 
	 * @param args
	 *            , not used, post values are retrieved via readPostValues()
	 * @throws IOException
	 */
	public String saveOvrValues(ArgList args, StringBuffer sb) throws IOException {
		ArgList args1;
		String ovrFile = "undef";
		try {
			args1 = readPostValues();
			String list[] = new String[] { "FOV", "LASER_IMPACTED", "LASER_STATE", "LOG_DATA", "OVERRIDE_URL_FIELDS" };
			String tel = args1.get("tel");
			LtcsConfig ltcsConfig = collector.getLtcsConfig();
			SiteInfo siteInfo = ltcsConfig.getTelescopes().get(tel);
			ovrFile = siteInfo.override_url;
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ovrFile)));
			for (String s : list) {
				out.println(s + "=" + args1.get(s));
			}
			out.flush();
			out.close();
			collector.clearPinfoReader(tel);
		} catch (Exception e) {
			Logger.error("Could not save override file " + ovrFile);
			sb.append(String.format("HTTP/1.0 500 %s\n\n", e.getMessage()));
			sb.append(String.format("%s\n", e.getMessage()));
			return "text/html";
		}
		args.put("tel", "all");
		return getStatus(args, sb);
	}

	/**
	 * Set Simulated data.
	 * 
	 * This is used only for testing. The PntInfoSim.html is the correcpsonding
	 * web page that shows the telescopes to be simulated. Each telescope can
	 * call setSimData, which includes a pnt information that need to be save to
	 * the simulated data directory, global.simdatadir.
	 * 
	 * @param args
	 * @param sb
	 * @return getStatus(args, sb);
	 * 
	 * @throws IOException
	 */
	public String setSimData(ArgList args, StringBuffer sb) throws IOException {
		String simDataDir = ltcsConfig.getGlobal().simdatadir;

		ArgList args1 = readPostValues();
		args.putAll(args1);

		String tel = args.get("tel");
		String fname = simDataDir + File.separator + tel + "_ltcs.dat";
		StringBuffer sbuf = new StringBuffer();
		
		long timeStamp = System.currentTimeMillis()/1000;
		
		try {
			Logger.info("Saving sim data to " + fname);
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fname)));
			
			sbuf.append("TIMESTAMP1="+timeStamp+"\n");
			sbuf.append("VERSION=VERSION3\n");
			sbuf.append("OBSERVATION_ID=" + tel + "-" + timeStamp + "\n");
			
			storeValue (args, tel, "tel", "SITE", sbuf);			
			storeValue (args, tel, "trackingMode", "TRACKING_MODE", sbuf);
			storeValue (args, tel, "az", "AZ", sbuf);
			storeValue (args, tel, "el", "EL", sbuf);
			storeValue (args, tel, "fov", "FOV", sbuf);
			storeValue (args, tel, "laserImpacted", "LASER_IMPACTED", sbuf);
			storeValue (args, tel, "laserState", "LASER_STATE", sbuf);
			storeValue (args, tel, "ditherRadius", "DITHER_RADIUS", sbuf);

			sbuf.append("TIMESTAMP2="+timeStamp+"\n");
		
			out.println(sbuf.toString());
			out.flush();
			out.close();
			Logger.info("Sim data saved " + args.toString());
		} catch (Exception e) {
			Logger.error("Could not save simulated data to " + fname);
			sb.append(String.format("HTTP/1.0 500 %s\n\n", e.getMessage()));
			sb.append(String.format("%s\n", e.getMessage()));
			return "text/html";
		}
		args.put("tel", "all");
		return getStatus(args, sb);
	}

	private void storeValue (ArgList args, String tel, String key, String label, StringBuffer sb) {
		String res = args.get(tel+key);
		if (res == null) return;
		sb.append(label + "=" + res + "\n");
	}
	
	/*
	 * For testing only. Gets the viewer position from a file. The viewer
	 * position is translated to the camera position for the 3D scene. File is
	 * in JSON, ie {zoom:z, az:az, el:el}, one single line.
	 */
	@Deprecated
	public String XgetViewerPosition(ArgList args, StringBuffer sb) throws IOException {
		String viewPosFile = global.http_docroot + File.separator + "viewpos.dat";
		File file = new File(viewPosFile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			char cbuf[] = new char[5000];
			int res;
			while (br.ready()) {
				res = br.read(cbuf);
				if (res <= 0)
					break;
				sb.append(cbuf, 0, res);
			}
			br.close();
		} catch (Exception e) {
			sb.append("{}");
		}
		return APPJSON;
	}
}