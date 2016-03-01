package config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.TreeMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import models.SiteInfo;


/**
 * LTCS Configuration Class.
 * 
 * The configuration file is a xml file containing a global section and a list of telescope sections.
 * See config.xml.
 * 
 * @author skwok
 *
 */
public class LtcsConfig {
	private Global global;
	private TreeMap<String, SiteInfo> telescopes;

	public LtcsConfig(String fname) {
		telescopes = new TreeMap<String, SiteInfo>();
		readConfigFile(fname);
	}

	public Global getGlobal() {
		return global;
	}

	public SiteInfo getSiteInfo(String telName) {
		return telescopes.get(telName);
	}
	
	public TreeMap<String, SiteInfo> getTelescopes() {
		return telescopes;
	}

	/**
	 * Reads the configuration file fname.
	 * This is a typical event driven XML parser.
	 * 
	 * @param fname
	 */
	public void readConfigFile(String fname) {
		String currTag = "";
		
		global = new Global();
		SiteInfo currSiteInfo = null;

		XMLInputFactory fy = XMLInputFactory.newInstance();
		try {
			String tag;
			XMLStreamReader rd = fy.createXMLStreamReader(fname,
					new FileReader(fname));
			while (rd.hasNext()) {
				int eventType = rd.next();
				switch (eventType) {
				case XMLEvent.CHARACTERS:
					String cont = rd.getText().trim();
					if (cont.length() > 0)
						System.out.println("chars " + rd.getText().trim());
					break;
				case XMLEvent.START_ELEMENT:
					tag = rd.getName().getLocalPart();
					if (tag.equalsIgnoreCase("Telescope")) {
						currTag = rd.getAttributeValue(null, "name");
						currSiteInfo = new SiteInfo(currTag);
						String enStr = rd.getAttributeValue(null, "enabled");
						currSiteInfo.queryUrl = rd.getAttributeValue(null,
								"queryUrl");
						currSiteInfo.mainUrl = rd.getAttributeValue(null,
								"mainUrl");
						currSiteInfo.enabled = enStr != null
								&& (enStr.equalsIgnoreCase("YES"));
						break;
					}
					if (tag.equalsIgnoreCase("Global")) {
						currTag = "Global";
						setFields(rd, global);
						break;
					}
					if (tag.equalsIgnoreCase("collector")) {
						if (currTag.equalsIgnoreCase("Global"))
							setFields(rd, global);
						else
							setFields(rd, currSiteInfo);
						break;
					}
					if (tag.equalsIgnoreCase("geomAnalyzer")) {
						if (currTag.equalsIgnoreCase("Global"))
							setFields(rd, global);
						else
							setFields(rd, currSiteInfo);
						break;
					}
					break;
				case XMLEvent.END_ELEMENT:
					tag = rd.getName().getLocalPart();
					if (tag.equalsIgnoreCase("Telescope")) {
						if (currTag != null) {
							telescopes.put(currTag, currSiteInfo);
							currTag = "";
						}
					}
				default:
					;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	/**
	 * When the XML parser identifies a tag, this method is called.
	 * The parameter obj can be of any class.
	 * For each attribute, its name and values are used to set the variables
	 * in the obj. The variables must be declared in the class.
	 * Depending on the type of the variable, the corresponding conversion routine is called.
	 * 
	 * @param rd
	 * @param obj
	 */
	private void setFields(XMLStreamReader rd, Object obj) {
		int len = rd.getAttributeCount();
		int i;
		Class<?> gClass = obj.getClass();
		for (i = 0; i < len; ++i) {
			try {
				String name = rd.getAttributeName(i).getLocalPart();
				String value = rd.getAttributeValue(i);
				Field fd = null;
				try {
					fd = gClass.getDeclaredField(name.toLowerCase());
				} catch (NoSuchFieldException ef) {
					System.out.println("Unknown field " + name + " " + obj.toString());
					continue;
				}
				Class<?> type = fd.getType();

				if (type.equals(int.class)) {
					fd.setInt(obj, Integer.parseInt(value));
				} else if (type.equals(double.class)) {
					fd.setDouble(obj, Double.parseDouble(value));
				} else if (type.equals(String.class)) {
					fd.set(obj, value);
				} else if (type.equals(boolean.class)) {
					value = value.toLowerCase();
					boolean bval = value.equals("yes") || 
						value.equals("true");
					fd.set(obj,  bval);
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void showAll() {
		System.out.println("global lat " + global.latitude_degs + " "
				+ global.latitude_mins + " " + global.latitude_secs);
		System.out.println ("doProbeQuery " + global.do_probequery);
		for (SiteInfo ga : telescopes.values()) {
			System.out.println("ga " + ga.telescope + " " + ga.url);
		}
	}
}
