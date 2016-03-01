package ltcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

import models.ChangedEvents;
import models.PointingInfo;
import models.SiteInfo;
import utils.Logger;

/*
 * This class is a thread that reads the LTCS pointing file and stores the
 * information in a PointingInfo object.
 * 
 * There is one thread per telescope.
 * 
 * @author skwok
 * 
 */
public class PntInfoReader {
	private long lastUpdated = 0;
	private String ltcsUrl = "";

	private String overrideURL;
	private PointingInfo cachedPntInfo = new PointingInfo();
	private String pntSource;
	private String queryServerURL;
	private SiteInfo siteInfo = null;
	private String telescopeName;

	private int updateRateMs = 15000; // 15s update rate

	/**
	 * Constructor
	 * 
	 * @param telescope
	 * @param site
	 * @throws MalformedURLException
	 */
	public PntInfoReader(String telescope, SiteInfo site)
			throws MalformedURLException {
		siteInfo = site;
		pntSource = site.url;
		queryServerURL = site.queryUrl;
		overrideURL = site.override_url;
		setTelescopeName(site.telescope);
		cachedPntInfo.site = site.telescope;
		ltcsUrl = site.mainUrl;
	}

	
	/**
	 * Forces update next time.
	 */
	synchronized public void clear() {
		lastUpdated = 0;
	}

	/**
	 * When pointing info is updated, a new event is added to the changedEvent
	 * queue. This means that if nobody is querying the server, no events are
	 * generated.
	 * 
	 * updateRateMs comes from configuration This method is called when the web
	 * page requests pointing info. The update rate of the web page is normally
	 * slower than updateRateMs, so in that case new pointing info is read from
	 * the pointing file. Caching the info this way prevents the server read
	 * from file unnecessarily.
	 * 
	 * @return
	 */
	synchronized public PointingInfo getPntInfo(boolean doQuery) {
		long now = Calendar.getInstance().getTime().getTime();
		if (now - lastUpdated > updateRateMs) {
			ChangedEvents cev = LTCSServer.changedEvents;
			boolean serverUp = doQuery && probeServer();
			updatePntInfo(serverUp);
			overrideInfo();
			cev.add(cachedPntInfo);
			lastUpdated = now;
		}
		return cachedPntInfo;
	}

	public SiteInfo getSiteInfo() {
		return siteInfo;
	}

	public String getTelescopeName() {
		return telescopeName;
	}

	private void overrideInfo() {
		URL ovrUrl;
		String path = "";
		try {
			InputStream ins;
			path = overrideURL;
			if (path.startsWith("http://")) {
				try {
					ovrUrl = new URL(overrideURL);
					ins = ovrUrl.openStream();
				} catch (Exception e) {
					System.out.println("bad URL " + overrideURL);
					return;
				}
			} else {
				if (path.startsWith("file://"))
					path = overrideURL.replace("file://", "");
				File ovrFile = new File(path);
				path = ovrFile.getAbsolutePath();
				if (ovrFile.exists() && ovrFile.canRead()) {
					ins = new FileInputStream(ovrFile);
				}
				return;
			}

			BufferedReader rd = new BufferedReader(new InputStreamReader(ins));
			String parts[];
			String name, value;
			while (rd.ready()) {
				String line = rd.readLine();
				if (line == null)
					break;
				parts = line.trim().split("=");
				name = parts[0];
				value = parts[1];
				cachedPntInfo.setOverride(name, value);
			}
			rd.close();
		} catch (FileNotFoundException fe) {
			Logger.error("File not found " + overrideURL + " "
					+ fe.getMessage());
			fe.printStackTrace();
		} catch (Exception e) {
			Logger.error("Error while reading override file " + telescopeName
					+ " " + overrideURL + " " + e.getMessage());
		}
	}

	/**
	 * Checks if the remove server is running by querying a dummy target.
	 * 
	 * This method is invoked if DO_PROBEQUERY = 'YES' in config file.
	 * 
	 * @return
	 */
	private boolean probeServer() {
		if (queryServerURL == null || queryServerURL.length() < 4) {
			return false;
		}
		String queryStr = queryServerURL + "?telescope=" + telescopeName
				+ "&ra=20&dec=20&equinox=2000&fov=0.5&laser_state=OFF";
		boolean sup = false;
		try {
			URL srvURL = new URL(queryStr);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					srvURL.openStream()));
			String line = rd.readLine();
			sup = (line != null) && (!line.toUpperCase().contains("DOWN"));
			rd.close();
		} catch (IOException e) {
			sup = false;
		}
		// Logger.info("Probing " + queryStr + "; " + (sup ? "UP" : "DONW"));

		return sup;
	}

	public void setTelescopeName(String telescopeName) {
		this.telescopeName = telescopeName;
	}

	public void setUpdateRateMs(int updateRateMs) {
		this.updateRateMs = updateRateMs;
	}

	/**
	 * Creates a new PntInfo object and caches it.
	 * 
	 * pntSource is URL of pointing information
	 * 
	 * ltcsURL is URL of LTCS web site of remote LTCS server hosting/running
	 * LTCS
	 * 
	 * Reads pointing info from given URL in config.
	 */
	private void updatePntInfo(boolean serverUp) {
		try {
			//Logger.info("Reading Pnt info from " + pntSource);
			PointingInfo pinfo = PointingInfo.createFrom(pntSource);
			pinfo.site = telescopeName;
			pinfo.hasServer = queryServerURL != null
					&& queryServerURL.length() > 0;
			pinfo.queryServerUp = serverUp;
			pinfo.collisions = cachedPntInfo.collisions;
			pinfo.ltcsUrl = ltcsUrl;
			cachedPntInfo = pinfo;
		} catch (Exception e) {
			return;
		}
	}
}
