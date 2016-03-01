package utils;

import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class StarUtils {
	/**
	 * @param azDeg
	 * @param elDeg
	 * @param latitude
	 * @param longitude
	 * @return {raDeg, decDeg}
	 */
	static public double[] azEl2raDec(double azDeg, double elDeg,
			double latitude, double longitude) {
		double tan1, tan2;
		double sinDec;
		double azRad = Math.toRadians(azDeg-180);
		double elRad = Math.toRadians(elDeg);
		double sinAz = Math.sin(azRad);
		double cosAz = Math.cos(azRad);
		double tanElev = Math.tan(elRad);
		double sinElev = Math.sin(elRad);
		double cosElev = Math.cos(elRad);
		double latRad = Math.toRadians(latitude);
		double sinLatitude = Math.sin(latRad);
		double cosLatitude = Math.cos(latRad);
		tan1 = sinAz;
		tan2 = (cosAz * sinLatitude + tanElev * cosLatitude);
		sinDec = sinLatitude * sinElev - cosLatitude * cosElev * cosAz;

		double raDeg = normAngle(getSiderealTime() + longitude
				- Math.toDegrees(Math.atan2(tan1, tan2)));
		double decDeg = Math.toDegrees(Math.asin(sinDec));
		return new double[] { raDeg, decDeg };
	}

	static public String d2digits(int d) {
		if (d < 10)
			return "0" + d;
		return String.valueOf(d);
	} // d2digits

	static public double[] dec2Degree(double decimal) {
		// return deg, min, seconds in an array of doubles
		double res[] = new double[4];
		if (decimal < 0) {
			res[0] = -1.0;
			decimal = -decimal - 0.0001 / 3600.0;
		} else {
			res[0] = 1.0;
			decimal += 0.0001 / 3600.0;
		}

		res[1] = (int) decimal;
		decimal -= res[1];
		res[2] = (int) (decimal * 60);
		decimal -= res[2] / 60.0;
		res[3] = decimal * 3600.0;
		return res;
	} // dec2Degree

	static public String deg2String(double deg[]) {
		String sign = deg[0] < 0 ? "-" : "";
		int d = (int) deg[1];
		int m = (int) deg[2];
		int s = (int) deg[3];
		int s100 = (int) ((deg[3] - s) * 100.0 + 0.5);
		if (s100 >= 100) {
			s += 1;
			s100 -= 100;
		}
		return sign + d2digits(d) + "d" + d2digits(m) + "m" + d2digits(s) + "."
				+ d2digits(s100) + "s";
	} // deg2String

	static public String deg2String(double deg) {
		return deg2String(dec2Degree(deg));
	} // deg2String

	static public double[] degree2Hour(double deg) {
		// return hour, min, seconds in an array of doubles
		return dec2Degree(deg / 15);
	} // degree2Hour

	static public double getSiderealTime() {
		int y, m, d, h, min, s, msec;
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
		y = now.get(Calendar.YEAR);
		m = now.get(Calendar.MONTH) + 1;
		d = now.get(Calendar.DAY_OF_MONTH);
		h = now.get(Calendar.HOUR_OF_DAY);
		min = now.get(Calendar.MINUTE);
		s = now.get(Calendar.SECOND);
		msec = now.get(Calendar.MILLISECOND);
		return siderealTime(y, m, d, h, min, (double) s + msec / 1000.0);
	}

	static public double hour2Degree(int h, int min, double s) {
		double deg = ((double) h + (double) min / 60.0 + s / 3600.0) * 15.0;
		return deg;
	} // hour2Degree

	static public String hour2String(double hour[]) {
		String sign = hour[0] < 0 ? "-" : "";
		int h = (int) hour[1];
		int m = (int) hour[2];
		int s = (int) hour[3];
		int s100 = (int) ((hour[3] - s) * 100.0 + 0.5);
		return sign + d2digits(h) + "h" + d2digits(m) + "m" + d2digits(s) + "."
				+ d2digits(s100) + "s";
	} // hour2String

	static public String hour2String(double hour) {
		return hour2String(degree2Hour(hour));
	} // hour2String

	static public double hourAngle(double stime0, double ra) {
		return normAngle(stime0 - ra);
	} // HourAngle

	static public double hourAngle(double stime0, double longitude, double ra) {
		return normAngle(stime0 + longitude - ra);
	} // HourAngle

	static public double hourStr2Degree(String hstr) {
		int h = 0, m = 0;
		double s = 0.0;

		try {
			hstr = hstr.replace('h', ' ').replace('m', ' ').replace('s', ' ');
			hstr = hstr.replace(':', ' ');
			StringTokenizer st = new StringTokenizer(hstr);
			h = Integer.parseInt(st.nextToken());
			m = Integer.parseInt(st.nextToken());
			s = Double.parseDouble(st.nextToken());
		} catch (Exception e) {
		}
		return hour2Degree(h, m, s);
	} // hourStr2Degree

	static public double julianDay(int y, int m, double d) {
		int A, B;
		if (m <= 2) {
			m += 12;
			--y;
		}
		A = y / 100;
		B = 2 - A + A / 4;

		return (int) (365.25 * (y + 4716)) + (int) (30.6001 * (m + 1)) + d + B
				- 1524.5;
	} // JulianDay

	static public void main (String args[]) {
		double ra = 22.1;
		double dec = 40;
		double latitude = 19.825947;
		double longitude = -155.474710; // keck 1
		
		double res[] = StarUtils.raDec2AzEl(ra*15, dec, latitude, longitude);
		System.out.println (String.format("az=%.4f el=%.4f", res[0], res[1]));
		
		res = StarUtils.azEl2raDec(res[0], res[1], latitude, longitude);
		System.out.println ("ra=" + res[0]/15.0 + " dec=" + res[1]);
	}

	static public double normAngle(double ang) {
		while (ang < 0)
			ang += 360.0;
		while (ang >= 360.0)
			ang -= 360.0;
		return ang;
	} // normAngle

	/**
	 * @param raDeg
	 * @param decDeg
	 * @param latitude
	 * @param longitude
	 * @returnn {az, el} in degree
	 */
	static public double[] raDec2AzEl(double raDeg, double decDeg,
			double latitude, double longitude) {
		double az, el;
		double stime0 = getSiderealTime();
		double ha = hourAngle(stime0, longitude, raDeg);
		// System.out.println ("hour angle " + hour2String(ha));
		double azel[] = tanAzimuthSinElev(ha, latitude, decDeg);
		az = normAngle(Math.toDegrees(Math.atan2(-azel[0], -azel[1])));
		el = Math.toDegrees(Math.asin(azel[2]));
		return new double[] { az, el };
	}

	static public double siderealTime(int y, int m, int d) {
		// in degeree
		double T = (julianDay(y, m, d) - 2451545.0) / 36525;
		double stime0 = 100.46061837 + T
				* (36000.770053608 + T * (0.0003879333 - T / 38710000));
		return normAngle(stime0);
	} // SiderealTime

	static public double siderealTime(int y, int m, int d, int h, int min,
			double s) {
		// in degeree
		double p = hour2Degree(h, min, s) * 1.000273790935;
		return normAngle(siderealTime(y, m, d) + p);
	} // SiderealTime

	static public String sinElev2String(double sinElev) {
		double el = Math.asin(sinElev);
		el = normAngle(Math.toDegrees(el));
		return deg2String(dec2Degree(el));
	} // sinElev2String

	static public double str2Degree(String str) {
		int d = 0, m = 0;
		double s = 0.0;
		boolean neg = false;

		try {
			str = str.replace('d', ' ').replace('m', ' ').replace('s', ' ');
			str = str.replace(':', ' ').trim();
			neg = str.startsWith("-");
			StringTokenizer st = new StringTokenizer(str);
			d = (int) Double.parseDouble(st.nextToken());
			if (d < 0)
				d = -d;
			m = Integer.parseInt(st.nextToken());
			s = Double.parseDouble(st.nextToken());
		} catch (Exception e) {
			e.printStackTrace();
		}
		double res = (double) d + (double) m / 60.0 + s / 3600.0;
		if (neg)
			res = -res;
		return res;
	} // str2Degree

	static public String tanAz2String(double sa, double ca) {
		double az = Math.atan2(sa, ca);
		az = normAngle(Math.toDegrees(az));
		return deg2String(dec2Degree(az));
	} // atanAz2String

	static public double[] tanAzimuthSinElev(double hangle, double latitude,
			double declination) {
		latitude = Math.toRadians(latitude);
		declination = Math.toRadians(declination);

		double sinLatitude = Math.sin(latitude);
		double cosLatitude = Math.cos(latitude);
		double sinDeclination = Math.sin(declination);
		double cosDeclination = Math.cos(declination);
		return tanAzimuthSinElevFast(hangle, sinLatitude, cosLatitude,
				sinDeclination, cosDeclination);
	} // TanAzimuthSinElev
	

	static public double[] tanAzimuthSinElevFast(double hangle,
			double sinLatitude, double cosLatitude, double sinDeclination,
			double cosDeclination) {
		double res[] = new double[3];
		hangle = Math.toRadians(hangle);

		double cosHangle = Math.cos(hangle);
		double sinHangle = Math.sin(hangle);
		double tanDeclination = 0;

		if (cosDeclination != 0.0)
			tanDeclination = sinDeclination / cosDeclination;

		// tang Az = res[0] / res[1]
		res[0] = sinHangle;
		res[1] = (cosHangle * sinLatitude - tanDeclination * cosLatitude);

		// sin el = res[2]
		res[2] = sinLatitude * sinDeclination + cosLatitude * cosDeclination
				* cosHangle;

		return res;
	} // TanAzimuthSinElev
} // StarUtils