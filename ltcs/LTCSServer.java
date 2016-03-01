package ltcs;

import java.io.FileNotFoundException;
import java.io.IOException;

import models.ChangedEvents;
import config.Global;
import config.LtcsConfig;
import utils.HttpService;
import utils.Logger;
import utils.Logger.LoggingMode;

/**
 * LTCS Server class
 * 
 * @author skwok
 * 
 */
public class LTCSServer {

	static public ChangedEvents changedEvents = new ChangedEvents();

	/**
	 * This is the main entry point for LTCS V2. Logs go to ltcsServer.log
	 * unless overridden in config file. 
	 * 
	 * Parameter 1: configuration file (xml)
	 * 
	 * @param args
	 */
	static public void main(String args[]) {
		try {
			String configFile = args[0];
			LTCSServer server = new LTCSServer(configFile);
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Usage: ltcs confgfile");
			System.exit(0);
		}
	}
	
	
	private Collector col = null;
	private GeometryAnalyzer ga = null;
	private Global global = null;
	private LtcsConfig ltcsConfig = null;

	/**
	 * 1. First load configuration.
	 * 
	 * 2. Init Collector.
	 * 
	 * 3. Init Geometry Analyzer.
	 * 
	 * @param configFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public LTCSServer(String configFile) throws FileNotFoundException, IOException {

		Logger.useFile("ltcsServer.log", LoggingMode.STDERR);

		ltcsConfig = new LtcsConfig(configFile);
		global = ltcsConfig.getGlobal();

		Logger.useFile(global.logfile,
				LoggingMode.valueOf(global.logmode.toUpperCase()));
		col = new Collector(ltcsConfig);

		ga = new GeometryAnalyzer(col.pReaders);

		showInfo();
	}

	private void showInfo() {
		if (global.use_legacy)
			Logger.info("Use legacy override (DB)");
		else
			Logger.info("Use override with files");
	}

	/**
	 * Start geometry analyzer and HTTP server
	 */
	private void start() {
		ga.start();

		try {
			HttpService httpService = new HttpService(
					ltcsConfig.getGlobal().http_port, new HttqHandlerFactory(
							col, ga, ltcsConfig));
			httpService.start();
		} catch (IOException e) {
			Logger.error("Error while starting HTTP service");
		}
	}

}
