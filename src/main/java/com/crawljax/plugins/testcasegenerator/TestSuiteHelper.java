package com.crawljax.plugins.testcasegenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.condition.browserwaiter.WaitConditionChecker;
import com.crawljax.condition.invariant.Invariant;
import com.crawljax.condition.invariant.InvariantChecker;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawljaxController;
import com.crawljax.core.configuration.CrawlSpecificationReader;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.CrawljaxConfigurationReader;
import com.crawljax.core.plugin.CrawljaxPluginsUtil;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.Identification.How;
import com.crawljax.core.state.StateVertex;
import com.crawljax.forms.FormHandler;
import com.crawljax.forms.FormInput;
import com.crawljax.oraclecomparator.StateComparator;
import com.crawljax.util.ElementResolver;
import com.crawljax.util.XMLObject;

/**
 * Helper for the test suites.
 */
public class TestSuiteHelper {
	private static final Logger LOGGER = Logger.getLogger(TestSuiteHelper.class.getName());

	private EmbeddedBrowser browser;
	private String url;

	private StateComparator oracleComparator;
	private InvariantChecker invariantChecker;
	private WaitConditionChecker waitConditionChecker;
	// private String currentTestMethod;
	private final ArrayList<Eventable> eventables = new ArrayList<Eventable>();
	private FormHandler formHandler;
	private CrawlSession crawlSession;

	private Map<Long, StateVertex> mapStateVertices;
	private Map<Long, Eventable> mapEventables;

	// private ReportBuilder reportBuilder = new ReportBuilder("TestReport");

	/**
	 * @param crawljaxConfiguration
	 *            Configuration to use.
	 * @param xmlStates
	 *            The xml states.
	 * @param xmlEventables
	 *            The xml eventables.
	 * @throws Exception
	 *             On error.
	 */
	public TestSuiteHelper(CrawljaxConfiguration crawljaxConfiguration, String xmlStates,
	        String xmlEventables, String url) throws Exception {
		LOGGER.info("Loading needed xml files for States and Eventables");
		mapStateVertices = (Map<Long, StateVertex>) XMLObject.xmlToObject(xmlStates);
		mapEventables = (Map<Long, Eventable>) XMLObject.xmlToObject(xmlEventables);

		this.url = url;
		CrawljaxController controller = new CrawljaxController(crawljaxConfiguration);

		this.browser = controller.getBrowserPool().requestBrowser();

		this.crawlSession = new CrawlSession(controller.getBrowserPool());

		CrawljaxConfigurationReader configReader =
		        new CrawljaxConfigurationReader(crawljaxConfiguration);
		CrawlSpecificationReader crawlerReader = configReader.getCrawlSpecificationReader();

		this.formHandler =
		        new FormHandler(browser, configReader.getInputSpecification(),
		                crawlerReader.getRandomInputInForms());

		oracleComparator = new StateComparator(crawlerReader.getOracleComparators());
		invariantChecker = new InvariantChecker(crawlerReader.getInvariants());
		waitConditionChecker = new WaitConditionChecker(crawlerReader.getWaitConditions());

		LOGGER.info("Loading plugins...");
		CrawljaxPluginsUtil.loadPlugins(configReader.getPlugins());
		CrawljaxPluginsUtil.runPreCrawlingPlugins(browser);

	}

	/**
	 * Loads start url and checks initialUrlConditions.
	 * 
	 * @throws Exception
	 *             On error.
	 */
	public void goToInitialUrl() throws Exception {
		browser.goToUrl(url);
		waitConditionChecker.wait(browser);
		CrawljaxPluginsUtil.runOnUrlLoadPlugins(browser);
		// Thread.sleep(5000);
		// // String dom = browser.getDriver().getPageSource();
		// String dom = browser.getDom();
		// Helper.saveFile(browser.getDom(), "uniformedDom.txt");
		// Helper.saveFile(browser.getDriver().getPageSource(),
		// "driverDom.txt");
		// System.out.println("saved");
		// System.out.println(Helper.getDocumentToString(Helper.getDocument(dom)));
		// browser.close();
		// System.exit(0);
	}

	/**
	 * Closes browser and writes report.
	 * 
	 * @throws Exception
	 *             On error.
	 */
	public void tearDown() throws Exception {
		Thread.sleep(400);
		browser.close();
		// reportBuilder.buildReport();
	}

	/**
	 * Fill in form inputs.
	 * 
	 * @param formInputs
	 *            The form inputs to handle.
	 * @throws Exception
	 *             On error.
	 */
	public void handleFormInputs(List<FormInput> formInputs) throws Exception {
		formHandler.handleFormElements(formInputs);
	}

	/**
	 * Run the InCrawling plugins.
	 */
	public void runInCrawlingPlugins() {
		CrawljaxPluginsUtil.runOnNewStatePlugins(crawlSession);
	}

	private Eventable getEventable(Long eventableId) {

		return mapEventables.get(eventableId);

	}

	/**
	 * @param eventableId
	 *            Id of the eventable.
	 * @return whether the event is fired
	 */
	public boolean fireEvent(long eventableId) {
		try {
			// browser.closeOtherWindows();
			Eventable eventable = getEventable(eventableId);
			eventables.add(eventable);
			String xpath = eventable.getIdentification().getValue();

			ElementResolver er = new ElementResolver(eventable, browser);
			String newXPath = er.resolve();
			boolean fired = false;
			if (newXPath != null) {
				if (!xpath.equals(newXPath)) {
					LOGGER.info("XPath of \"" + eventable.getElement().getText()
					        + "\" changed from " + xpath + " to " + newXPath);
				}
				eventable.setIdentification(new Identification(How.xpath, newXPath));
				LOGGER.info("Firing: " + eventable);
				fired = browser.fireEvent(eventable);
			}
			if (!fired) {
				// String orgDom = "";
				// try {
				// orgDom = eventable.getEdge().getFromStateVertex().getDom();
				// } catch (Exception e) {
				// // TODO: Danny fix
				// orgDom = "<html>todo: fix</html>";
				// // LOGGER.info("Warning, could not get original DOM");
				// }
				// reportBuilder.addFailure(new EventFailure(browser, currentTestMethod, eventables,
				// orgDom, browser.getDom()));
			}
			waitConditionChecker.wait(browser);
			return fired;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return false;
		}
	}

	/**
	 * @param StateVertexId
	 *            The id of the state vertix.
	 * @return the State with id StateVertex Id
	 */
	public StateVertex getStateVertex(Long StateVertexId) {

		return mapStateVertices.get(StateVertexId);

	}

	/**
	 * @param StateVertexId
	 *            The id of the state vertix.
	 * @return return where the current DOM in the browser is equivalent with the state with
	 *         StateVertexId
	 */
	public boolean compareCurrentDomWithState(long StateVertexId) {
		try {
			StateVertex StateVertex = getStateVertex(StateVertexId);
			String stateDom = StateVertex.getDom();
			String browserDom = browser.getDom();
			if (!oracleComparator.compare(stateDom, browserDom, browser)) {
				LOGGER.info("Not Equivalent with state" + StateVertexId + ": \n"
				        + oracleComparator.getStrippedOriginalDom() + "\n"
				        + oracleComparator.getStrippedNewDom() + "\n");
				/*
				 * TODO: Danny, temporarily disabled ReportBuilder.addFailure(new
				 * StateFailure(browser, currentTestMethod, eventables, stateDom, browserDom,
				 * oracleComparator.getStrippedOriginalDom(), oracleComparator.getStrippedNewDom(),
				 * StateVertexId));
				 */
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @return whether all the invariants are satisfied
	 */
	public boolean checkInvariants() {
		boolean passed = invariantChecker.check(browser);
		if (!passed) {
			List<Invariant> failedInvariants = invariantChecker.getFailedInvariants();
			try {
				for (Invariant failedInvariant : failedInvariants) {
					// reportBuilder.addFailure(new InvariantFailure(browser, currentTestMethod
					// + " - " + failedInvariant.getDescription(), eventables, browser
					// .getDom(), failedInvariant.getDescription(), failedInvariant
					// .getInvariantCondition().getAffectedNodes()));
					LOGGER.info("Invariant failed: " + failedInvariant.toString());
				}
			} catch (Exception e) {
				LOGGER.error("Error with adding failure: " + e.getMessage(), e);
			}
		}
		return passed;
	}

	/**
	 * @param currentTestMethod
	 *            The current method that is used for testing
	 */
	public void newCurrentTestMethod(String currentTestMethod) {
		LOGGER.info("");
		LOGGER.info("New test: " + currentTestMethod);
		eventables.clear();
		// this.currentTestMethod = currentTestMethod;
	}

	/**
	 * @return the browser
	 */
	public EmbeddedBrowser getBrowser() {
		return browser;
	}

}
