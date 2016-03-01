package TestCases;

import config.LtcsConfig;

public class LtcsConfigTest {
	static public void main(String args[]) {
		LtcsConfig cc = new LtcsConfig(args[0]);
		cc.showAll();
	}
}
