package ltcs;

import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.TreeMap;

import models.PointingInfo;
import models.SiteInfo;
import utils.Logger;
import config.LtcsConfig;

public class Collector {
	private long lastTime = 0;
	private LtcsConfig ltcsConfig;
	private LtcsLegacy ltcsLegacy;

	private int nrReqs = 0;

	TreeMap<String, PntInfoReader> pReaders;

	/**
	 * The collector builds a list of PntInfoReaders. 
	 * 
	 * 
	 * @param ltcscfg
	 */
	public Collector(LtcsConfig ltcscfg) {
		ltcsConfig = ltcscfg;
		buildPntInfoReaders();
	}

	/**
	 * Reads in LTCS config. Creates and starts PntInfoReaders for each
	 * telescope. Returns a list of PntInfoReaders.
	 * 
	 * @param fname
	 */

	private void buildPntInfoReaders() {
		TreeMap<String, SiteInfo> telescopes = getLtcsConfig().getTelescopes();
		TreeMap<String, PntInfoReader> hashtable = new TreeMap<String, PntInfoReader>();

		for (SiteInfo site : telescopes.values()) {
			if (site.url == null || site.url.length() < 4)
				continue;
			try {
				PntInfoReader pt = new PntInfoReader(site.telescope, site);
				if (!site.enabled)
					continue;
				pt.setUpdateRateMs((int) (site.period * 1000));
				hashtable.put(site.telescope, pt);
			} catch (MalformedURLException e) {
				Logger.error("Failed to create URL for " + site.telescope + " "
						+ site.url);
			}
		}

		pReaders = hashtable;
	}

	public void clearPinfoReader(String tel) {
		pReaders.get(tel).clear();
	}

	synchronized void count() {
		++nrReqs;
	}

	public LtcsConfig getLtcsConfig() {
		return ltcsConfig;
	}

	synchronized public double getReqPerSec() {
		long now = Calendar.getInstance().getTimeInMillis();
		long dt = now - lastTime;
		if (dt > 0) {
			double rps = nrReqs * 1000.0 / dt;
			lastTime = now;
			nrReqs = 0;
			return rps;
		}
		return 0;
	}

	public void setLtcsConfig(LtcsConfig ltcsConfig) {
		this.ltcsConfig = ltcsConfig;
	}

	/**
	 * Checks the override status for given telescope using the legacy system,
	 * where the information is stored in a database rather than in a file.
	 * pInfo is updated with override info.
	 * 
	 * @param pInfo
	 * @return
	 */
	@Deprecated
	public PointingInfo XXcheckLegacy(PointingInfo pInfo) {
		if (ltcsLegacy == null)
			return pInfo;
		LtcsLegacy lly = ltcsLegacy;
		String tel = pInfo.site;
		String ovrState = lly.getField(tel, "ovr_state");
		if (ovrState.equalsIgnoreCase("YES")) {
			double fov = lly.getFieldDouble(tel, "fov");
			String impacted = lly.getField(tel, "laser_impacted");
			String lsstate = lly.getField(tel, "state");
			pInfo.fov = fov;
			pInfo.laserImpacted = impacted.equalsIgnoreCase("YES");
			pInfo.laserState = lsstate;
			pInfo.overrideFields = true;
		}
		return pInfo;
	}

}
