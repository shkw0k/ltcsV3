package utils;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Logger {

	public enum LoggingMode {
		BOTH(3), LOGFILE(2), STDERR(1), NONE(0);

		int value = 0;

		LoggingMode(int n) {
			value = n;
		}
	}
	
	static public LoggingMode LogMode = LoggingMode.NONE;
	static public LogStream LogStreamOut = null; 

	static public void error(String msg) {
		Logger.log("Error", msg);
	}

	static public String getTimeStamp() {
		Date date = Calendar.getInstance().getTime();
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss.SSS");
		return formatter.format(date);
	}

	static public void info(String msg) {
		Logger.log("Info", msg);
	}

	static public void log(String level, String msg) {
		switch (LogMode) {
		case NONE:
			return;
		case STDERR:
			break;
		case LOGFILE:
			break;
		case BOTH:
			LogStreamOut.println(Logger.getTimeStamp() + " [" + level + "] " + msg);
			break;
		}
		System.err.println(Logger.getTimeStamp() + " [" + level + "] " + msg);
	}

	static public void useFile(String fn, LoggingMode mode)
			throws FileNotFoundException, IOException {
		LogStream logStream = null;
		FileOutputStream os = null;
		LogMode = mode;
		switch (mode) {
		case LOGFILE: // log file
			if (fn != null) {
				os = new FileOutputStream(fn, true);
				logStream = new LogStream(os, null);				
				System.setErr(logStream);
			}
			break;
		case BOTH: // both
			if (fn != null) {
				os = new FileOutputStream(fn, true);
				logStream = new LogStream(os, System.err);
				LogStreamOut = logStream;
			}
			break;
		case NONE: // none
			;
		case STDERR: // stderr
		default:
			return;
		}
	} // Logger

	static public void warn(String msg) {
		Logger.log("Warning", msg);
	}
} // Logger

class LogStream extends PrintStream {
	PrintStream err = null;

	public LogStream(FileOutputStream fs, PrintStream ps) {
		super(fs);
		err = ps;
	} // constructor

	public void write(byte[] a) throws IOException {
		if (err != null) 
			err.write(a);
		super.write(a);
		super.flush();
	} // write

	public void write(byte[] a, int s, int l) {
		if (err != null)
			err.write(a, s, l);
		super.write(a, s, l);
		super.flush();
	} // write

	public void write(int b) {
		if (err != null)
			err.write(b);
		super.write(b);
		super.flush();
	} // write

} // LogStream
