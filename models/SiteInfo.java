package models;

import utils.Vector;

public class SiteInfo {
	// Altitude of the site in meter and feet, both are defined in config
	public double altitude = 0;
	public double altitude_ft = 0;

	// East offset in meters relative to reference Site
	public double e_offset = 0;

	// Site is included or ignored.
	public boolean enabled = false;

	// Factor to apply o primary mirror diameter
	public double error_term = 0.10;

	// The geometry analyzer controls the laser.
	public String ga_controls_laser = "NO";

	// Laser beam size in diameter, if less than 0.05, then FOV is conical and in degree (0.05deg = 3arcmin)
	// If greater than 0.05, then it is cylindrical and in meters, (0.05 m = 50mm)
	public double laser_beam_size = 0;
	
	// Telescope has laser
	public String laser_configured = "NO";
	
	// Main priority scheme 
	public String laser_priority_scheme = "2";
	
	// AZ projection angle relative to telescope axis in degrees
	public double laser_proj_angle = 0.0;
	
	// Distance from telescope axis in meters
	public double laser_proj_radius = 0.0;
	
	// Geodetic site latitude in deg and sexagecimal
	public double latitude = 0;
	public String latitude_dms = "";
	
	// Location error in meters
	public double location_error = 0.0;
	
	// Geodetic site longitude in deg (negative = West of GW) and sexagecimal 
	public double longitude = 0;
	public String longitude_dms = "";
	
	// Site main LTCS url.
	public String mainUrl = "";
	
	// North offset relative to reference telescope in meters
	public double n_offset = 0;
	
	// URL of override information
	public String override_url = "";
	
	// Update rate in sec
	public double period = 3;
	
	// Size of primary mirror in meters
	public double primary_size = 0;
	
	// Additional priority scheme info for laser_priority_scheme = 4 
	public String priority_scheme = "LASERS-YIELD";
	
	// URL for collision info queries
	public String queryUrl = "";
	
	// Delay in sec before clearing the shutter
	public double shutter_clear_delay = 0;
	
	// For testing
	public String sim_url = "";
	
	// algo = DETECT, FILTER or OFF
	// If DETECT then telescope slew/track state is set
	// If FILTER then laser impacted flag is set in addtion to telescope track state
	public String slew_sense_algorithm = "DETECT";
	
	// When slew_sense_algo is DETECT or FILTER, this threshold is used. In arcsec
	public double slew_threshold = 120;
	
	// How many times the period after which site is considered stale.
	public int stale_data_factor = 40;
	
	// Name of site/telescope
	public String telescope = "";
	
	// URL to collect pointing info
	public String url = "";
	
	// Elevation offset relative to reference telescope
	public double z_offset = 0;

	public SiteInfo(String name) {
		telescope = name;
	}

	public boolean hasLaser() {
		return laser_configured.equalsIgnoreCase("YES");
	}
	
	public Vector getVector() {
		return new Vector (e_offset, n_offset, z_offset);
	}
}