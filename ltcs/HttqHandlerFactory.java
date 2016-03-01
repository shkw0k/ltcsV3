package ltcs;

import config.LtcsConfig;
import utils.HandlerFactory;
import utils.HttpHandler;

public class HttqHandlerFactory implements HandlerFactory {
	private Collector collector;
	private String docRoot;
	private LtcsConfig ltcsConfig;
	private GeometryAnalyzer geometryAnalyzer;
	
	public HttqHandlerFactory(Collector col, GeometryAnalyzer ga, LtcsConfig lcfg) {
		this.collector = col;
		this.ltcsConfig = lcfg;
		this.geometryAnalyzer = ga;
		this.docRoot = lcfg.getGlobal().http_docroot;
	}

	@Override
	public HttpHandler createHandler() {
		ReqProcessor colh = new ReqProcessor(ltcsConfig, collector, geometryAnalyzer);
		colh.setDocRoot(docRoot);
		return colh;		
	}		
}