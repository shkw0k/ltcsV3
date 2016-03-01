package config;

public class Global {
	// The variable names are in lower case. They correspond to the variables (in upper case) in the configuration file. 
	public int alarm_port = 43868;
	public int collector_client_port = 43869;
	public String http_docroot = ".";
	public int http_port = 50080;
	public double latitude = 0;

	public double latitude_degs = 0;
	public double latitude_mins = 0;
	public double latitude_secs = 0;
	public double longitude = 0;
	public double longitude_degs = 0;
	public double longitude_mins = 0;
	public double longitude_secs = 0;
	public double max_rayleigh_altitude = 15000.0;
	public double max_sodium_altitude = 100000.0;
	public double min_sodium_altitude = 80000.0;
	public double new_target_min_dist = 60;
	public double prediction_hours = 3;
	public int priority_rule = 4;
	public String ltcsws_url = "";
	public int status_port = 43870;
	public double ut_hours_offset = -10;
	public boolean use_legacy = false;
	public boolean do_probequery = false;
	public String simdatadir = ".";
	
	public String logfile = "ltcsServer.log";
	public String logmode = "STDERR";
}