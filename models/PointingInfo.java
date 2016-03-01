package models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import utils.Angle;

/**
 * Data structure representing the telescope pointing information, which is
 * stored in LTCS pointing format as name value pairs.
 * 
 * @author skwok
 * 
 */
public class PointingInfo implements ChangedEvent {

	static public PointingInfo createFrom(String src) throws IOException {
		BufferedReader rd;
		try {
			URL url = new URL(src);
			rd = new BufferedReader(new InputStreamReader(url.openStream()));
		} catch (MalformedURLException e) {
			rd = new BufferedReader(new FileReader(src));
		}
		return PointingInfo.createFromReader(rd);
	}

	/**
	 * Reads pointing information from URL. URL can have file:// or http://
	 * protocol.
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	static public PointingInfo createFromReader(BufferedReader rd) throws IOException {
		String line;
		String parts[];
		String name, value;
		PointingInfo pinfo = new PointingInfo();
		while (rd.ready()) {
			line = rd.readLine();
			if (line == null) {
				break;
			}
			parts = line.trim().split("=");
			if (parts.length < 2)
				continue;
			name = parts[0].trim().toUpperCase();
			value = parts[1].trim();
			pinfo.set(name, value);
		}
		rd.close();
		return pinfo;
	}

	public Angle azimuth;

	public Vector<CollisionResult> collisions = null;
	public Angle dec;
	public double ditherRadius;
	public double dra, ddec;
	public Angle elevation;
	public double equinox;
	public double fov;
	public boolean hasServer = false;
	public boolean laserImpacted = false;
	public String laserState = "OFF";
	public boolean logData = false;
	public String ltcsUrl = "";
	public Vector<CollisionResult> newCollisions = null;

	public String observation_id = "";
	public boolean overrideFields = false;
	public double ovrfov;
	public boolean ovrlaserImpacted;
	public String ovrlaserState = "";
	public boolean ovrlogData = false;
	public boolean queryInProgress = false;
	public boolean queryServerUp = false;
	public Angle ra;
	public String site = "Undefined";
	public double staleDataFactor = 100;
	public long timeStamp1, timeStamp2;
	public String tracking_mode = "";
	public double updateRate;

	public String version = "";

	public PointingInfo() {
		ra = new Angle(0);
		dec = new Angle(0);
		azimuth = new Angle(0);
		elevation = new Angle(0);
		timeStamp1 = 0L;
		timeStamp2 = 1L;
		updateRate = 2;
		dra = 0;
		ddec = 0;
	}

	public boolean isStale() {
		long currTime = now();

		long thold = (long) (updateRate * staleDataFactor);
		thold = Math.max(1, thold);

		long diff1 = currTime - timeStamp1;
		long diff2 = currTime - timeStamp2;

		// System.out.println("isstale " + diff1 + " " + diff2 + " " + thold);
		return (diff1 > thold) || (diff2 > thold);
	}

	public boolean isTrue(String value) {
		value = value.toUpperCase();
		return value.equals("ON") || value.equals("YES") || value.equals("TRUE");
	}

	private void labelStr(StringBuilder sb, String sep, String name, String value) {
		sb.append(String.format("%s'%s':'%s'", sep, name, value));
	}

	public long now() {
		Calendar cal = Calendar.getInstance();
		return (long) (cal.getTimeInMillis() / 1000);
	}

	@Deprecated
	public void replaceRaDec(URL src, double raDeg, double decDeg) throws IOException {

		StringBuilder sb = new StringBuilder();
		long tstamp = Calendar.getInstance().getTimeInMillis() / 1000;
		sb.append("TIMESTAMP1=" + tstamp);
		sb.append("\nSITE=" + site);
		sb.append("\nTRACKING_MODE=" + tracking_mode);
		if (tracking_mode.equals("SIDEREAL")) {
			sb.append("\nRA=" + raDeg / 15);
			sb.append("\nDEC=" + decDeg);
			sb.append("\nEQUINOX=2000");
			sb.append("\nDRA=0.000000");
			sb.append("\nDDEC=0.000000");
		} else if (tracking_mode.equals("NON_SIDEREAL")) {
			sb.append("\nRA=" + raDeg / 15);
			sb.append("\nDEC=" + decDeg);
			sb.append("\nEQUINOX=2000");
			sb.append("\nDRA=" + dra);
			sb.append("\nDDEC=" + ddec);
		} else {
			sb.append("\nAZ=" + azimuth.getDegree());
			sb.append("\nEL" + elevation.getDegree());
		}

		sb.append("\nFOV=0.189000");
		sb.append("\nLASER_IMPACTED=NO");
		sb.append("\nLASER_STATE=OFF");
		sb.append("\nLOG_DATA=ON");
		sb.append("\nUPDATE_RATE=3");
		sb.append("\nDITHER_RADIUS=0.000000");
		sb.append("\nTIMESTAMP2=" + tstamp);
		BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(src.openConnection().getOutputStream()));
		wr.write(sb.toString());
		wr.close();
	}

	public void set(String name, String value) {
		name = name.toUpperCase();
		if (name.equals("NAME") || name.equals("SITE")) {
			site = value;
			return;
		}
		if (name.equals("TELESCOPE")) {
			site = value;
			return;
		}
		if (name.equals("RA")) {
			ra = Angle.asDegree(Double.parseDouble(value)); // in hours
			return;
		}
		if (name.equals("DEC")) {
			dec = Angle.asDegree(Double.parseDouble(value));
			return;
		}
		if (name.equals("EQUINOX")) {
			equinox = Double.parseDouble(value);
			return;
		}
		if (name.equals("DRA")) {
			dra = Double.parseDouble(value);
			return;
		}
		if (name.equals("DDEC")) {
			ddec = Double.parseDouble(value);
			return;
		}
		if (name.equals("DITHER_RADIUS")) {
			ditherRadius = Double.parseDouble(value);
			return;
		}
		if (name.equals("UPDATE_RATE")) {
			updateRate = Double.parseDouble(value);
			return;
		}
		if (name.equals("FOV")) {
			fov = Double.parseDouble(value);
			return;
		}
		if (name.equals("LASER_IMPACTED")) {
			laserImpacted = isTrue(value);
			return;
		}
		if (name.equals("LASER_STATE")) {
			laserState = value;
			return;
		}
		if (name.equals("LOG_DATA")) {
			logData = isTrue(value);
			return;
		}
		if (name.equals("TIMESTAMP1")) {
			timeStamp1 = Long.parseLong(value);
			return;
		}
		if (name.equals("TIMESTAMP2")) {
			timeStamp2 = Long.parseLong(value);
			return;
		}
		if (name.equals("STALE_DATA_FACTOR")) {
			staleDataFactor = Double.parseDouble(value);
			return;
		}
		if (name.equals("VERSION")) {
			version = value;
			return;
		}
		if (name.equals("OBSERVATION_ID")) {
			observation_id = value;
			return;
		}
		if (name.equals("TRACKING_MODE")) {
			tracking_mode = value;
			return;
		}
		if (name.equals("AZ")) {
			azimuth = Angle.asSexagecimal(value);
			return;
		}
		if (name.equals("EL")) {
			elevation = Angle.asSexagecimal(value);
			return;
		}

		System.out.println("unknown " + name);
	}

	public void setOverride(String name, String value) {
		name = name.toUpperCase();
		if (name.equals("FOV")) {
			ovrfov = Double.parseDouble(value);
			return;
		}
		if (name.equals("LASER_IMPACTED")) {
			ovrlaserImpacted = isTrue(value);
			return;
		}
		if (name.equals("LASER_STATE")) {
			ovrlaserState = value;
			return;
		}
		if (name.equals("LOG_DATA")) {
			ovrlogData = isTrue(value);
			return;
		}
		if (name.equals("OVERRIDE_URL_FIELDS")) {
			overrideFields = isTrue(value);
			return;
		}
		if (name.equals("QUERY_IN_PROGRESS")) {
			queryInProgress = isTrue(value);
			return;
		}
	}

	public String toJson() {
		String sep = "";
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		labelStr(sb, sep, "Telescope", site);
		sep = ",";
		labelStr(sb, sep, "RA", "" + ra.getDegree()); // hours
		labelStr(sb, sep, "DEC", "" + dec.getDegree());
		labelStr(sb, sep, "FOV", "" + fov);
		labelStr(sb, sep, "LASER_IMPACTED", laserImpacted ? "YES" : "NO");
		labelStr(sb, sep, "LASER_STATE", laserState);
		labelStr(sb, sep, "TIMESTAMP", "" + timeStamp1);
		labelStr(sb, sep, "isStale", isStale() ? "STALE" : "");

		labelStr(sb, sep, "OVRLASER_IMPACTED", ovrlaserImpacted ? "YES" : "NO");
		labelStr(sb, sep, "OVRLASER_STATE", ovrlaserState);
		labelStr(sb, sep, "OVRFOV", "" + ovrfov);
		labelStr(sb, sep, "OVERRIDE_FIELDS", overrideFields ? "YES" : "NO");
		labelStr(sb, sep, "OVRLOG_DATA", ovrlogData ? "ON" : "OFF");
		labelStr(sb, sep, "LTCSURL", ltcsUrl);

		StringBuilder colSb = new StringBuilder();
		colSb.append("{");
		if (collisions != null) {
			String sep2 = "";
			for (int i = 0; i < collisions.size(); ++i) {
				CollisionResult cr = collisions.get(i);
				colSb.append(sep2);
				colSb.append("'" + cr.telescope.site + "':");
				colSb.append(cr.collisionDist);
				sep2 = ",";
			}
		}
		colSb.append("}");

		sb.append(sep + "'COLLISIONS':" + colSb.toString());

		String str = "No server";
		if (hasServer) {
			str = queryServerUp ? "UP" : "DOWN";
		}
		labelStr(sb, sep, "LTCSSRV_STATE", str);
		sb.append("}");
		return sb.toString();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String sep = ", ";
		sb.append(site);
		sb.append(sep + (queryServerUp ? "UP" : "DOWN"));
		sb.append(sep + (laserImpacted ? "YES" : "NO"));
		sb.append(sep + laserState);
		sb.append(sep + toTime(timeStamp1));
		sb.append(sep + (isStale() ? " STALE" : "OK"));

		return sb.toString();
	}

	public String toTime(long ts) {
		SimpleDateFormat dt = new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss");
		return dt.format(new Date(ts * 1000L));
	}
}
