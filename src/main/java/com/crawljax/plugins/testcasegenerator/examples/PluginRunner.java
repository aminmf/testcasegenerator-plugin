package com.crawljax.plugins.testcasegenerator.examples;

import com.crawljax.core.CrawljaxController;
import com.crawljax.core.configuration.CrawlSpecification;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.plugins.testcasegenerator.TestSuiteGenerator;

public class PluginRunner {

	private static final String URL = "http://google.com";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CrawljaxConfiguration config = new CrawljaxConfiguration();
		CrawlSpecification crawler = new CrawlSpecification(URL);
		crawler.setMaximumStates(3);
		crawler.clickDefaultElements();
		config.setCrawlSpecification(crawler);
		config.addPlugin(new TestSuiteGenerator());

		try {
			CrawljaxController crawljax = new CrawljaxController(config);
			crawljax.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
