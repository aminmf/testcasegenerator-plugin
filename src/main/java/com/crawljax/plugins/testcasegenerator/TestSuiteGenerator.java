/**
 * Created Apr 17, 2008
 */
package com.crawljax.plugins.testcasegenerator;

import java.io.IOException;
import java.util.List;

import com.crawljax.core.CrawlSession;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.util.Helper;

/**
 * Test suite generator for crawljax. IMPORTANT: only works with CrawljaxConfiguration TODO: Danny,
 * also make sure package name is correct
 * 
 * @author danny
 * @version $Id: TestSuiteGenerator.java 6276 2009-12-23 15:37:09Z frank $
 */
public class TestSuiteGenerator implements PostCrawlingPlugin {

	private final String TEST_SUITE_PATH = "src/test/java/generated";
	private final String CLASS_NAME = "GeneratedTestCases";
	private final String FILE_NAME_TEMPLATE = "TestCase.vm";

	private final String XML_STATES = TEST_SUITE_PATH + "/states.xml";
	private final String XML_EVENTABLES = TEST_SUITE_PATH + "/eventables.xml";

	private CrawlSession session;

	// private static final Logger LOGGER = Logger.getLogger(TestSuiteGenerator.class.getName());

	@Override
	public void postCrawling(CrawlSession session) {
		this.session = session;
		try {
			Helper.directoryCheck(TEST_SUITE_PATH);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String fileName = generateTestCases();
		System.out.println("Tests generated in " + fileName);

	}

	/**
	 * @return the filename of the generated java test class, null otherwise
	 */
	public String generateTestCases() {
		TestSuiteGeneratorHelper testSuiteGeneratorHelper = new TestSuiteGeneratorHelper(session);
		List<TestMethod> testMethods = testSuiteGeneratorHelper.getTestMethods();

		try {
			JavaTestGenerator generator =
			        new JavaTestGenerator(CLASS_NAME, session.getInitialState().getUrl(),
			                testMethods, session.getCrawljaxConfiguration());

			testSuiteGeneratorHelper.writeStateVertexTestDataToXML(XML_STATES);
			testSuiteGeneratorHelper.writeEventableTestDataToXML(XML_EVENTABLES);
			generator.useXmlInSteadOfDB(XML_STATES, XML_EVENTABLES);

			return generator.generate(Helper.addFolderSlashIfNeeded(TEST_SUITE_PATH),
			        FILE_NAME_TEMPLATE);

		} catch (Exception e) {
			System.out.println("Error generating testsuite: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
