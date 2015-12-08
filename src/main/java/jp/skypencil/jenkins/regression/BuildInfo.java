package jp.skypencil.jenkins.regression;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.ExtensionPoint;

/**
 * 
 * @author Team FailedTest This class implements the structure that describes
 *         build information.
 *
 */
public class BuildInfo implements ExtensionPoint {
	private int number;
	private Calendar timestamp;
	private String timestampString;
	private String status;
	private List<String> authors;
	private int passedTests;
	private double passingRate;
	private LinkedHashMap<String, TestInfo> tests;

	/**
	 * Creates a new instance of BuildInfo.
	 * 
	 * @param number
	 *            build number as int.
	 * @param timestamp
	 *            timestamp the build was created as Calendar.
	 * @param timestampString
	 *            timestamp the build was created as String.
	 * @param status
	 *            build status as String.
	 * @param authors
	 *            a List of authors who contribute to the code changes.
	 * @param passedTests
	 *            number of passed tests as double.
	 * @param passingRate
	 *            tests passing rate (total passed tests divided by total tests)
	 *            as double.
	 */
	@DataBoundConstructor
	public BuildInfo(int number, Calendar timestamp, String timestampString, String status, List<String> authors,
			double passedTests, double passingRate) {
		this.number = number;
		this.timestamp = timestamp;
		this.timestampString = timestampString;
		this.status = status;
		this.authors = authors;
		this.passedTests = (int) passedTests;
		this.passingRate = passingRate;
		this.tests = new LinkedHashMap<String, TestInfo>();
	}

	/**
	 * Add Tests into the build
	 * 
	 * @param t
	 *            a list of TestInfo
	 */
	public void addTests(List<TestInfo> t) {
		for (TestInfo a : t) {
			tests.put(a.getFullName(), a);
		}
	}

	/**
	 * Get all Tests from this build
	 * 
	 * @return A list of TestInfo
	 */
	public List<TestInfo> getTests() {
		List<TestInfo> ret = new ArrayList<TestInfo>();
		Iterator<TestInfo> it = tests.values().iterator();
		while (it.hasNext()) {
			TestInfo t = it.next();
			ret.add(t);
		}
		return ret;
	}

	/**
	 * Add a test by name
	 * 
	 * @param name
	 *            a test name
	 * @return A TestInfo corresponding to the input name, return null if the
	 *         name does not exist
	 */
	public TestInfo getTest(String name) {
		if (tests.containsKey(name)) {
			return tests.get(name);
		}
		return null;
	}

	/**
	 * Get the build number
	 * 
	 * @return A build number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * Returns the timestamp the build was created.
	 * 
	 * @return the timestamp as String
	 */
	public String getTimestampString() {
		return timestampString;
	}

	/**
	 * Returns the timestamp the build was created in the following format:
	 * [3-character month] [date], [year] [hour]:[minute]:[second] [AM/PM]
	 * 
	 * @return the timestamp as String
	 */
	public String getReadableTimestamp() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy h:mm:ss a");
		return dateFormat.format(timestamp.getTime());
	}

	/**
	 * Returns the test status.
	 * 
	 * @return the test status as String
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Returns a list of authors who contribute to the code changes.
	 * 
	 * @return a List of String
	 */
	public List<String> getAuthors() {
		return authors;
	}

	/**
	 * Returns a list of authors who contribute to the code changes as a comma
	 * separated string.
	 * 
	 * @return a comma separated list of authors
	 */
	public String getAuthorsString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < authors.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(authors.get(i));
		}

		return builder.toString();
	}

	/**
	 * Returns the number of passed tests in the build.
	 * 
	 * @return number of passed tests as int
	 */
	public int getPassedTests() {
		return passedTests;
	}

	/**
	 * Returns the test passing rate (total passed tests divided by total tests)
	 * in the build.
	 * 
	 * @return tests passing rate as double
	 */
	public double getPassingRate() {
		return passingRate;
	}
}
