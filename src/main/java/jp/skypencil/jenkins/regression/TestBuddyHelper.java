package jp.skypencil.jenkins.regression;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;

/**
 * 
 * @author Team FailedTest This class contains all the helper functions that are
 *         used by TestBuddyAction.java
 *
 */
public class TestBuddyHelper {

	/**
	 * Returns a list of CaseResults that are contained in a build. Currently
	 * this function only handles builds whose getResult return an object of
	 * type TestResultAction or AggregatedTestResultAction
	 * 
	 * @param build
	 *            an AbstractBuild object from which the caller wants to get the
	 *            case results.
	 * @return an ArrayList of CaseResult.
	 */
	@SuppressWarnings("rawtypes")
	public static ArrayList<CaseResult> getAllCaseResultsForBuild(AbstractBuild build) {
		ArrayList<CaseResult> ret = new ArrayList<CaseResult>();
		List<AbstractTestResultAction> testActions = build.getActions(AbstractTestResultAction.class);

		for (AbstractTestResultAction testAction : testActions) {
			if (testAction instanceof TestResultAction) {
				TestResult testResult = (TestResult) testAction.getResult();
				ret.addAll(getTestsFromTestResult(testResult));
			} else if (testAction instanceof AggregatedTestResultAction) {
				List<AggregatedTestResultAction.ChildReport> child_reports = ((AggregatedTestResultAction) testAction)
						.getChildReports();
				for (AggregatedTestResultAction.ChildReport child_report : child_reports) {
					TestResult testResult = (TestResult) child_report.result;
					ret.addAll(getTestsFromTestResult(testResult));
				}
			}
		}
		return ret;
	}

	/**
	 * Returns a list of authors that make the change to the build
	 * 
	 * @param build
	 *            an AbstractBuild object from which the caller wants to get the
	 *            case results.
	 * @return an List of String.
	 **/
	@SuppressWarnings("rawtypes")
	public static List<String> getAuthors(AbstractBuild build) {
		List<String> ret = new ArrayList<String>();
		ChangeLogSet change = build.getChangeSet();
		if (!change.isEmptySet()) {
			for (Object entry : change.getItems()) {
				hudson.scm.ChangeLogSet.Entry e = (hudson.scm.ChangeLogSet.Entry) entry;
				if (!ret.contains(e.getAuthor().getDisplayName())) {
					ret.add(e.getAuthor().getDisplayName());
				}
			}
		}
		return ret;
	}

	/**
	 * Returns a double array containing passed tests number, and passing rate
	 * for a build
	 * 
	 * @param build
	 *            an AbstractBuild object from which the caller wants to get the
	 *            case results.
	 * @return an Array of double.
	 **/
	@SuppressWarnings("rawtypes")
	public static double[] getRatesforBuild(AbstractBuild build) {
		double[] ret = new double[2];
		ArrayList<CaseResult> caseResults = TestBuddyHelper.getAllCaseResultsForBuild(build);
		double total_tests = caseResults.size();
		double passed_tests = 0;
		double passing_rate = 0;
		for (CaseResult caseResult : caseResults) {
			if (caseResult.isPassed()) {
				passed_tests++;
			}
		}
		if (total_tests != 0) {
			passing_rate = passed_tests / total_tests;
		}
		DecimalFormat df = new DecimalFormat("#.##");
		passing_rate = Double.valueOf(df.format(passing_rate));
		ret[0] = passed_tests;
		ret[1] = passing_rate;
		return ret;
	}

	/**
	 * A helper function that returns a of CaseResult from a TestReult object.
	 * 
	 * @param testResult
	 *            a TestResult object that contains PackageResult as its
	 *            children
	 * @return An ArrayList of CaseResult.
	 */
	private static ArrayList<CaseResult> getTestsFromTestResult(TestResult testResult) {
		ArrayList<CaseResult> tests = new ArrayList<CaseResult>();
		Collection<PackageResult> packageResults = testResult.getChildren();
		for (PackageResult packageResult : packageResults) {
			Collection<ClassResult> class_results = packageResult.getChildren();
			for (ClassResult class_result : class_results) {
				Collection<CaseResult> case_results = class_result.getChildren();
				tests.addAll(case_results);
			}
		}

		return tests;
	}

	/**
	 * Given two builds thisBuild and otherBuild, returns the a list of Tuples
	 * of matching CaseResult. Each pair is of form (CaseResultFromThisBuild,
	 * CaseResultFromThatBuild)
	 *
	 * @param thisBuild
	 *            an AbstractBuild.
	 * @param otherBuild
	 *            another AbstractBuild, which is compared against thisBuild
	 * @return an ArrayList of Tuples of CaseResults.Each pair is of form
	 *         (CaseResultFromThisBuild, CaseResultFromThatBuild) if a matching
	 *         case result is not found in the other build, a null is used
	 *         instead.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ArrayList<Pair<CaseResult, CaseResult>> matchTestsBetweenBuilds(AbstractBuild thisBuild,
			AbstractBuild otherBuild) {
		ArrayList<CaseResult> thisResults = getAllCaseResultsForBuild(thisBuild);
		HashMap<String, CaseResult> hmap = hashAid(otherBuild);

		ArrayList<Pair<CaseResult, CaseResult>> returnValue = new ArrayList<Pair<CaseResult, CaseResult>>();
		for (CaseResult thisCaseResult : thisResults) {
			String currTestName = thisCaseResult.getFullName();
			CaseResult otherCaseResult = null;
			if (hmap.containsKey(currTestName)) {
				otherCaseResult = hmap.get(currTestName);
				hmap.remove(currTestName);
			}
			Pair tuple = new Pair<CaseResult, CaseResult>(thisCaseResult, otherCaseResult);
			returnValue.add(tuple);
		}

		for (CaseResult otherCaseResultInMap : hmap.values()) {
			Pair tuple = new Pair<CaseResult, CaseResult>(null, otherCaseResultInMap);
			returnValue.add(tuple);
		}

		return returnValue;
	}

	private static HashMap<String, CaseResult> hashAid(AbstractBuild otherBuild) {
		ArrayList<CaseResult> otherResults = getAllCaseResultsForBuild(otherBuild);

		HashMap<String, CaseResult> hmap = new HashMap<String, CaseResult>();
		for (CaseResult otherCaseResult : otherResults) {
			hmap.put(otherCaseResult.getFullName(), otherCaseResult);
		}
		return hmap;
	}

	/**
	 * Given two builds thisBuild and otherBuild, returns the a list of
	 * CaseResult that have different fail/pass results.
	 *
	 * @param thisBuild
	 *            an AbstractBuild.
	 * @param otherBuild
	 *            another AbstractBuild, which is compared against thisBuild
	 * @return an ArrayList of CaseResult in thisBuild that satisfies any of the
	 *         following conditions: - fails in thisBuild, but passes in
	 *         otherBuild - passes in thisBuild, but fails in otherBuild
	 */
	@SuppressWarnings("rawtypes")
	public static ArrayList<CaseResult> getChangedTestsBetweenBuilds(AbstractBuild thisBuild,
			AbstractBuild otherBuild) {
		ArrayList<CaseResult> thisResults = getAllCaseResultsForBuild(thisBuild);
		HashMap<String, CaseResult> hmap = hashAid(otherBuild);

		ArrayList<CaseResult> returnValue = new ArrayList<CaseResult>();
		for (CaseResult thisCaseResult : thisResults) {
			String currTestName = thisCaseResult.getFullName();
			if (hmap.containsKey(currTestName)) {
				CaseResult otherCaseResult = hmap.get(currTestName);
				if ((thisCaseResult.isPassed() && otherCaseResult.isFailed())
						|| (thisCaseResult.isFailed() && otherCaseResult.isPassed())) {
					returnValue.add(thisCaseResult);
				}
			}
		}
		return returnValue;
	}

}