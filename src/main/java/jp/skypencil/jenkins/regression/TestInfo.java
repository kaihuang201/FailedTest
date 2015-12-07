package jp.skypencil.jenkins.regression;

import java.text.DecimalFormat;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.ExtensionPoint;

/**
 * 
 * @author Team FailedTest This class implements the structure that describes
 *         test information.
 *
 */
public class TestInfo implements ExtensionPoint {
	private String fullName;
	private String name;
	private String className;
	private String packageName;
	private String status;
	private String otherStatus;
	private int passedCount = 0;
	private int failedCount = 0;
	private int skippedCount = 0;
	private int buildNumber;

	/**
	 * Creates a new instance of TestInfo.
	 * 
	 * @param fullName
	 *            test full name as String.
	 * @param status
	 *            test status as String.
	 * @param buildNumber
	 *            build number as int.
	 */
	@DataBoundConstructor
	public TestInfo(String fullName, String status, int buildNumber) {
		this.fullName = fullName;
		this.status = status;
		this.buildNumber = buildNumber;
		if (status.equals("Passed"))
			this.otherStatus = "Failed";
		else if (status.equals("Failed"))
			this.otherStatus = "Passed";
		parseNames();
	}

	/**
	 * Parses test name, class name, and package name from test full name.
	 */
	private void parseNames() {
		String[] fullNameArray = fullName.split("\\.");
		name = fullNameArray[fullNameArray.length - 1];
		className = fullNameArray[fullNameArray.length - 2];

		if (fullName.length() > (name.length() + className.length() + 1)) {
			packageName = fullName.substring(0, fullName.length() - name.length() - className.length() - 2);
		}
	}

	/**
	 * Returns test full name.
	 * 
	 * @return test full name as String
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * Returns test name without package and class names.
	 * 
	 * @return test name as String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns class name.
	 * 
	 * @return class name as String
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Returns package name.
	 * 
	 * @return package name as String
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * Returns test status.
	 * 
	 * @return test status as String
	 */
	public String getStatus() {
		return status;
	}

	public String getOtherStatus() {
		return otherStatus;
	}

	/**
	 * Returns the number of builds the test passed.
	 * 
	 * @return passed count as int
	 */
	public int getPassedCount() {
		return passedCount;

	}

	/**
	 * Returns the number of builds the test failed.
	 * 
	 * @return failed count as int
	 */
	public int getFailedCount() {
		return failedCount;

	}

	/**
	 * Returns the number of builds the test was skipped.
	 * 
	 * @return skipped count as int
	 */
	public int getSkippedCount() {
		return skippedCount;

	}

	/**
	 * Increment passed count, failed count, or skipped count by 1.
	 * 
	 * @param statusToIncrement
	 *            a String to determine which status count to increment. Choose
	 *            among Passed, Failed, or Skipped.
	 */
	public void incrementCount(String statusToIncrement) {
		if (statusToIncrement == "Passed") {
			passedCount++;
		} else if (statusToIncrement == "Failed") {
			failedCount++;
		} else if (statusToIncrement == "Skipped") {
			skippedCount++;
		}
	}

	public int getBuildNumber() {
		return buildNumber;
	}

	/**
	 * Calculates the failing rate of the test.
	 * 
	 * @return failing rate as double
	 */
	public double getFailingRate() {
		double total = failedCount + passedCount;
		double failingRate = 0;

		if (total > 0) {
			failingRate = failedCount / total;
			DecimalFormat df = new DecimalFormat("#.##");
			failingRate = Double.valueOf(df.format(failingRate));
		}

		return failingRate;
	}
}
