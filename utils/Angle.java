package utils;

import java.io.Serializable;

public class Angle implements Serializable {

	private static final long serialVersionUID = -3706883898558891093L;

	// Static method, takes angle in degree and returns a new instance of
	// Angle.
	static public Angle asDegree(double angDegree) {
		return new Angle(Math.toRadians(angDegree));
	}

	// Static method, takes angle in radians and returns a new instance of
	// Angle.
	static public Angle asRadian(double angRadian) {
		return new Angle(angRadian);
	}
	
	static public Angle asHour (double hour) {
		return new Angle (Math.toRadians(hour * 15));
	}

	// Static method, takes angle in sexagecimal and returns a new instance
	// of
	// Angle. The input string must be in the form NDDdMMdSS.ssss, where N
	// is
	// minus sign, plus sign or empty, DD is the integer degrees part, MM
	// integer minutes part, and SS.ssss seconds part with zero or more
	// decimal
	// digits, d, the delimiter, can be a space or ‘:’ or ‘dms’ for degrees,
	// minutes and seconds. Alternatively, the input string can be in
	// decimal
	// degrees.
	public static Angle asSexagecimal(String angSexa) {
		angSexa = angSexa.trim().replace(':', ' ');
		String inStrs[] = angSexa.split(" ");

		double deg = 0, min = 0, sec = 0;
		int len = inStrs.length;
		double sign = (angSexa.startsWith("-") ? -1.0 : 1.0);

		if (len >= 1)
			deg = Math.abs(Double.valueOf(inStrs[0]));
		if (len >= 2)
			min = Double.valueOf(inStrs[1]);
		if (len >= 3)
			sec = Double.valueOf(inStrs[2]);

		return Angle.asDegree(sign * (deg + min / 60.0 + sec / 3600.0));
	}

	public static String getSexagecimal(double deg) {
		return Angle.getSexagecimal(deg, 3, ' ');
	}

	public static String getSexagecimal(double deg, char delimiter) {
		return Angle.getSexagecimal(deg, 3, delimiter);
	}

	public static String getSexagecimal(double deg, int ndigits, char delimiter) {
		Angle ang = Angle.asDegree(deg);
		return ang.getSexagecimal(delimiter, ndigits);
	}

	public static double radian2Arcsec(double radians) {
		return Math.toDegrees(radians) * 3600.0;
	}

	public static double arcsec2Radian(double as) {
		return Math.toRadians(as / 3600.0);
	}

	private double radValue = 0.0; // value in radians

	// Default constructor. Default angle = 0.0
	public Angle() {
		radValue = 0.0;
	}

	public Angle(double angRad) {
		radValue = angRad;
	}

	// Returns angle in degrees
	public double getDegree() {
		return Math.toDegrees(radValue);
	}

	// Returns angle in radians
	public double getRadian() {
		return radValue;
	}

	// Returns angle in sexagecimal notation with the given number of digits
	// for
	// seconds.
	public String getSexagecimal() {
		return getSexagecimal(3);
	}

	public String getSexagecimal(char delimiter) {
		return getSexagecimal(delimiter, 3);
	}

	public String getSexagecimal(int ndigits) {
		return getSexagecimal(' ', ndigits);
	}

	public String getSexagecimal(char demlimiter, int ndigits) {
		String dstr, mstr, sstr;
		String sign;
		double value;
		double degValue = getDegree();
		int offset = 3;
		if (degValue < 0.0) {
			sign = "-";
			value = -degValue;
		} else {
			sign = " ";
			value = degValue;
		}

		value += 1.0 / Math.pow(10, ndigits + 6);

		int dd = (int) value;
		value = (value - dd) * 60.0;

		int mm = (int) value;
		value = (value - mm) * 60.0;

		double ss = value;

		if (dd < 10)
			dstr = "0" + dd;
		else
			dstr = "" + dd;

		if (mm < 10)
			mstr = "0" + mm;
		else
			mstr = "" + mm;

		if (ss < 10)
			sstr = String.format("0%f", ss);
		else
			sstr = String.format("%f", ss);

		if (ndigits == 0)
			offset = 2;

		sstr += "000000000";

		return sign + dstr + demlimiter + mstr + demlimiter
				+ sstr.substring(0, ndigits + offset);
	}

	// Normalize the angle to the range [0,360).
	public double normal360() {
		double degValue = getDegree();
		while (degValue < 0) {
			degValue += 360.0;
		}
		while (degValue >= 360) {
			degValue -= 360;
		}
		return degValue;
	}

	public String toString() {
		return getSexagecimal();
	}
}
