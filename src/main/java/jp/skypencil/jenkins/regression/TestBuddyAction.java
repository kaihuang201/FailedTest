package jp.skypencil.jenkins.regression;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.util.RunList;

public class TestBuddyAction extends Actionable implements Action {
	@SuppressWarnings("rawtypes")
	AbstractProject project;

	public static TreeMap<Integer, BuildInfo> all_builds = new TreeMap<Integer, BuildInfo>(Collections.reverseOrder());

	/**
	 * Creates a new instance of TestBuddyAction.
	 * 
	 * @param project
	 *            an AbstractProject.
	 */
	public TestBuddyAction(@SuppressWarnings("rawtypes") AbstractProject project) {
		this.project = project;
	}

	/**
	 * Returns the display name of the action page.
	 * 
	 * @return the display name as String
	 */
	@Override
	public final String getDisplayName() {
		return "Test Buddy";
	}

	/**
	 * Returns the file path of the action page icon.
	 * 
	 * @return the icon file path as String
	 */
	@Override
	public final String getIconFileName() {
		return "/images/jenkins.png";
	}

	/**
	 * Returns the url name of the action page.
	 * 
	 * @return the url name as String
	 */
	@Override
	public String getUrlName() {
		return "test_buddy";
	}

	/**
	 * Returns the search url of the action page.
	 * 
	 * @return the search url as String
	 */
	@Override
	public String getSearchUrl() {
		return "test_buddy";
	}

	/**
	 * Returns the project associated with the action page.
	 * 
	 * @return an AbstractProject
	 */
	@SuppressWarnings("rawtypes")
	public AbstractProject getProject() {
		return this.project;
	}

	/**
	 * Returns the combination of project url and the action page url name.
	 * 
	 * @return the project url appended with the action page url name as String
	 */
	public String getUrl() {
		return this.project.getUrl() + this.getUrlName();
	}

	/**
	 * Returns the value of an url query string.
	 * 
	 * @param parameterName
	 *            query string parameter name.
	 * @return query string value as String
	 */
	public String getUrlParam(String parameterName) {
		return Stapler.getCurrentRequest().getParameter(parameterName);
	}

	/**
	 * This method loop through all the builds in the project, all the tests in
	 * each builds, and store them in local all_builds TreeMap.
	 * 
	 * @return a List of BuildInfo which contains every build in the project.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<BuildInfo> getBuilds() {
		Set<Integer> missingBuildNumbers = new HashSet<Integer>(all_builds.keySet());
		RunList<Run> runs = project.getBuilds();
		Iterator<Run> runIterator = runs.iterator();

		while (runIterator.hasNext()) {
			Run run = runIterator.next();
			int num = run.getNumber();
			missingBuildNumbers.remove(num);

			if (!all_builds.containsKey(num)) {
				List<String> authors = TestBuddyHelper.getAuthors((AbstractBuild) run);
				double rates[] = TestBuddyHelper.getRatesforBuild((AbstractBuild) run);
				BuildInfo build = new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2(),
						run.getResult().toString(), authors, rates[0], rates[1]);
				build.add_tests(get_ini_Tests(String.valueOf(build.getNumber())));
				all_builds.put(num, build);
			}
		}

		for (int missingBuildNumber : missingBuildNumbers) {
			all_builds.remove(missingBuildNumber);
		}

		return new ArrayList<BuildInfo>(all_builds.values());
	}

	/**
	 * This method takes a string representation of build number and returns the
	 * corresponding BuildInfo. If there is no local copy of that build,
	 * getBuildInfo_backup will be called.
	 * 
	 * @param number
	 *            is a String representing the build number.
	 * @return BuildInfo which contains a build's information.
	 */
	public BuildInfo getBuildInfo(String number) {
		if (all_builds.containsKey(Integer.valueOf(number))) {
			// System.out.println("getting local copy");
			return all_builds.get(Integer.valueOf(number));
		}

		// System.out.println("NOT nice, calling back up function");
		return getBuildInfo_backup(number);
	}

	/**
	 * This method takes a string representation of build number and returns the
	 * corresponding BuildInfo. It will loop through the project and try to find
	 * the corresponding build.
	 * 
	 * @param number
	 *            is a String representing the build number.
	 * @return BuildInfo which contains a build's information.
	 */
	@SuppressWarnings("rawtypes")
	public BuildInfo getBuildInfo_backup(String number) {
		int buildNumber = Integer.parseInt(number);
		Run run = project.getBuildByNumber(buildNumber);
		List<String> authors = TestBuddyHelper.getAuthors((AbstractBuild) run);
		double rates[] = TestBuddyHelper.getRatesforBuild((AbstractBuild) run);
		return new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2(), run.getResult().toString(),
				authors, rates[0], rates[1]);
	}

	/**
	 * Searches tests where the full name (package, class, and test name)
	 * contains the search text. The search is case insensitive.
	 * 
	 * @param searchText
	 *            text to search.
	 * @return a List of TestInfo.
	 */
	@JavaScriptMethod
	public List<TestInfo> searchTests(String searchText) {
		TreeMap<String, TestInfo> testMap = new TreeMap<String, TestInfo>();

		for (BuildInfo build : getBuilds()) {
			for (TestInfo test : build.getTests()) {
				if (test.getFullName().toLowerCase().contains(searchText.toLowerCase())) {
					TestInfo testInfo;
					if (!testMap.containsKey(test.getFullName())) {
						testInfo = new TestInfo(test.getFullName(), test.getStatus(), build.getNumber());
						testMap.put(test.getFullName(), testInfo);
					} else {
						testInfo = testMap.get(test.getFullName());
					}

					testInfo.incrementCount(test.getStatus());
				}
			}
		}

		return new ArrayList<TestInfo>(testMap.values());
	}

	/**
	 * Returns a summary of all tests from all builds in the project.
	 * 
	 * @return a List of TestInfo
	 */
	public List<TestInfo> listTests() {
		return searchTests("");
	}

	/**
	 * This method takes a test name and returns a list of TestInfos
	 * representing all the tests across all builds with the given test name
	 * 
	 * @param testName
	 *            is a String containing the name of test to search for
	 * @return a List of TestInfos with the given testName, empty if no tests
	 *         with the given testName was found.
	 */
	public List<TestInfo> getAllTestInfosForTestName(String testName) {
		ArrayList<TestInfo> ret = new ArrayList<TestInfo>();
		for (BuildInfo build : getBuilds()) {
			TestInfo testInfo = build.getTest(testName);
			if (testInfo != null) {
				ret.add(testInfo);
			}

		}
		return ret;
	}

	/**
	 * This method takes a test name and looks through all builds to find the
	 * test with the given name and returns a String concatenating its package
	 * name, class name, and test name.
	 * 
	 * @param testName
	 *            is a String containing the name of test to search for
	 * @return a String containing the package name, the class name and the test
	 *         name. An empty string if no test with the testName testName was
	 *         found.
	 */
	public String getTestName(String testName) {
		String ret = "";
		for (BuildInfo build : getBuilds()) {
			TestInfo testInfo = build.getTest(testName);
			if (testInfo != null) {
				return testInfo.getPackageName() + " " + testInfo.getClassName() + " " + testInfo.getName();
			}
		}
		return ret;
	}

	/**
	 * This method takes a test name, looks through all builds to compute the
	 * number of passing tests, failing tests, skipped tests, and passing rate,
	 * of the test with the given test name.
	 * 
	 * @param testName
	 *            is a String containing the name of test to search for
	 * @return an array of Strings containing the passing rate, the number of
	 *         passing tests, the number of failing tests, and the number of
	 *         skipped tests, in that order.
	 */
	public String[] getTestRates(String testName) {
		List<TestInfo> testInfos = getAllTestInfosForTestName(testName);
		String[] ret = new String[4];
		double totalNum = testInfos.size();
		// System.out.println(totalNum);
		int passedNum = 0;
		int failed = 0;
		int skipped = 0;
		if (totalNum == 0)
			return ret;
		for (TestInfo test : testInfos) {
			if (test.getStatus().toLowerCase().equals("passed")) {
				passedNum++;
			} else if (test.getStatus().toLowerCase().equals("failed")) {
				failed++;
			} else if (test.getStatus().toLowerCase().equals("skipped")) {
				skipped++;
			}
		}
		double passingRate = passedNum / totalNum;
		DecimalFormat df = new DecimalFormat("#.##");
		passingRate = Double.valueOf(df.format(passingRate));
		ret[0] = String.valueOf(passingRate);
		ret[1] = String.valueOf(passedNum);
		ret[2] = String.valueOf(failed);
		ret[3] = String.valueOf(skipped);
		System.out.println("jelly called this java function");
		return ret;
	}

	/**
	 * This method loop through all the local builds and return all authors
	 * 
	 * @return A set of String which representing the name of author.
	 */
	public Set<String> getAllAuthors() {
		Set<String> authors = new HashSet<String>();
		for (BuildInfo build : getBuilds()) {
			authors.addAll(build.getAuthors());
		}
		return authors;
	}

	/**
	 * This method takes a string representation of build number and returns all
	 * the tests in that build. If no local copy of that build is find,
	 * get_ini_Tests will be called
	 * 
	 * @param number
	 *            is a String representing the build number.
	 * @return A List of TestInfo from corresponding build.
	 */
	public List<TestInfo> getTests(String number) {
		if (all_builds.containsKey(Integer.valueOf(number))) {

			return all_builds.get(Integer.valueOf(number)).getTests();
		}

		// System.out.println("NOT nice, calling back up function");

		return get_ini_Tests(number);
	}

	/**
	 * This method takes a string representation of build number, loop through
	 * the project and returns all the tests in that build.
	 * 
	 * @param number
	 *            is a String representing the build number.
	 * @return A List of TestInfo from corresponding build.
	 */
	@SuppressWarnings("rawtypes")
	public List<TestInfo> get_ini_Tests(String number) {
		int buildNumber = Integer.parseInt(number);
		AbstractBuild build = project.getBuildByNumber(buildNumber);
		ArrayList<CaseResult> caseResults = TestBuddyHelper.getAllCaseResultsForBuild(build);

		return convertCaseResultsToTestInfos(caseResults, "Passed", "Failed", Integer.parseInt(number));
	}

	@SuppressWarnings("rawtypes")
	public List<TestInfo> getNewPassFail() {
		AbstractBuild build = project.getLastBuild();
		return getChangedTests(build, build.getPreviousBuild(), "Newly Passed", "Newly Failed");
	}

	// compare two builds
	@SuppressWarnings("rawtypes")
	@JavaScriptMethod
	public List<TestInfo> getBuildCompare(String buildNumber1, String buildNumber2) {
		int build1 = Integer.parseInt(buildNumber1);
		int build2 = Integer.parseInt(buildNumber2);
		AbstractBuild buildOne = project.getBuildByNumber(build1);
		AbstractBuild buildTwo = project.getBuildByNumber(build2);

		return getChangedTests(buildOne, buildTwo, "Passed", "Failed");
	}

	@SuppressWarnings("rawtypes")
	@JavaScriptMethod
	public ArrayList<Tuple<TestInfo, TestInfo>> getDetailedBuildComparison(String buildNumber1, String buildNumber2) {
		int build1 = Integer.parseInt(buildNumber1);
		int build2 = Integer.parseInt(buildNumber2);
		AbstractBuild buildOne = project.getBuildByNumber(build1);
		AbstractBuild buildTwo = project.getBuildByNumber(build2);

		ArrayList<Tuple<CaseResult, CaseResult>> myDifferences = TestBuddyHelper.matchTestsBetweenBuilds(buildOne,
				buildTwo);
		ArrayList<Tuple<TestInfo, TestInfo>> allTestInfos = (ArrayList<Tuple<TestInfo, TestInfo>>) convertCaseResultsToTestInfosTwo(
				myDifferences, build1, build2);
		return allTestInfos;
	}

	/**
	 * Returns a list of test differences between two builds.
	 * 
	 * @param build1
	 *            an AbstractBuild.
	 * @param build2
	 *            another AbstractBuild to compare with build1.
	 * @param passedStatus
	 *            a status String to be displayed when the CaseResult status is
	 *            passing.
	 * @param failedStatus
	 *            a status String to be displayed when the CaseResult status is
	 *            failing.
	 * @return a List of TestInfo.
	 */
	@SuppressWarnings("rawtypes")
	public List<TestInfo> getChangedTests(AbstractBuild build1, AbstractBuild build2, String passedStatus,
			String failedStatus) {
		ArrayList<CaseResult> changedTests = TestBuddyHelper.getChangedTestsBetweenBuilds(build1, build2);
		return convertCaseResultsToTestInfos(changedTests, passedStatus, failedStatus, 0);
	}

	/**
	 * Converts a list of CaseResult to a list of TestInfo.
	 * 
	 * @param caseResults
	 *            a List of CaseResult.
	 * @param passedStatus
	 *            a status String to be displayed when the CaseResult status is
	 *            passing.
	 * @param failedStatus
	 *            a status String to be displayed when the CaseResult status is
	 *            failing.
	 * @param build_no
	 *            build number in int.
	 * @return a List of TestInfo.
	 */
	public List<TestInfo> convertCaseResultsToTestInfos(List<CaseResult> caseResults, String passedStatus,
			String failedStatus, int build_no) {
		List<TestInfo> tests = new ArrayList<TestInfo>();

		for (CaseResult caseResult : caseResults) {
			if (caseResult != null) {
				if (caseResult.isFailed()) {
					tests.add(new TestInfo(caseResult.getFullName(), failedStatus, build_no));
				} else if (caseResult.isPassed()) {
					tests.add(new TestInfo(caseResult.getFullName(), passedStatus, build_no));
				} else if (caseResult.isSkipped()) {
					tests.add(new TestInfo(caseResult.getFullName(), "Skipped", build_no));
				}
			}
		}

		return tests;
	}

	public List<Tuple<TestInfo, TestInfo>> convertCaseResultsToTestInfosTwo(
			List<Tuple<CaseResult, CaseResult>> caseResults, int build1, int build2) {
		List<Tuple<TestInfo, TestInfo>> tests = new ArrayList<Tuple<TestInfo, TestInfo>>();

		TestInfo tmp1;
		TestInfo tmp2;

		for (Tuple<CaseResult, CaseResult> t : caseResults) {
			CaseResult first = t.first;
			CaseResult second = t.second;

			if (first != null) {
				if (first.isFailed())
					tmp1 = new TestInfo(first.getFullName(), "Failed", build1);
				else if (first.isPassed())
					tmp1 = new TestInfo(first.getFullName(), "Passed", build1);
				else if (first.isSkipped())
					tmp1 = new TestInfo(first.getFullName(), "Skipped", build1);
				else
					tmp1 = new TestInfo(first.getFullName(), "Unknown", build1);
			} else
				tmp1 = new TestInfo(second.getFullName(), "Did not exist", build1);

			if (second != null) {
				if (second.isFailed())
					tmp2 = new TestInfo(second.getFullName(), "Failed", build2);
				else if (second.isPassed())
					tmp2 = new TestInfo(second.getFullName(), "Passed", build2);
				else if (second.isSkipped())
					tmp2 = new TestInfo(second.getFullName(), "Skipped", build2);
				else
					tmp2 = new TestInfo(second.getFullName(), "Unknown", build2);
			} else
				tmp2 = new TestInfo(first.getFullName(), "Did not exist", build2);

			tests.add(new Tuple<TestInfo, TestInfo>(tmp1, tmp2));
		}

		return tests;
	}
}
