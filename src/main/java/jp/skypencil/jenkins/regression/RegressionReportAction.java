package jp.skypencil.jenkins.regression;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.RunList;

public class RegressionReportAction extends Actionable implements Action {
	@SuppressWarnings("rawtypes")
	AbstractProject project;
	
	public RegressionReportAction(@SuppressWarnings("rawtypes") AbstractProject project){
		this.project = project;
	}

	/**
     * The display name for the action.
     * 
     * @return the name as String
     */
    public final String getDisplayName() {
        return "FailedTest";
    }

    /**
     * The icon for this action.
     * 
     * @return the icon file as String
     */
    public final String getIconFileName() {
    	return "/images/jenkins.png";
    }

    /**
     * The url for this action.
     * 
     * @return the url as String
     */
    public String getUrlName() {
        return "failedTest";
    }

    /**
     * Search url for this action.
     * 
     * @return the url as String
     */
	public String getSearchUrl() {
		return "failedTest";
	}

    @SuppressWarnings("rawtypes")
	public AbstractProject getProject(){
    	return this.project;
    }

	public String getUrl() {
		return this.project.getUrl() + this.getUrlName();
	}
	
	public String getUrlParam(String parameterName) {
		return Stapler.getCurrentRequest().getParameter(parameterName);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public List<BuildInfo> getBuilds() {
		List<BuildInfo> builds = new ArrayList<BuildInfo>();
		RunList<Run> runs = project.getBuilds();
		Iterator<Run> runIterator = runs.iterator();
		while (runIterator.hasNext()) {
			Run run = runIterator.next();
			BuildInfo build = new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2());
			builds.add(build);
		}
		
		return builds;
	}
	
	@SuppressWarnings("rawtypes")
	public BuildInfo getBuildInfo(String number) {
		int buildNumber = Integer.parseInt(number);
		Run run = project.getBuildByNumber(buildNumber);
		return new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2());
	}
	
	@SuppressWarnings("rawtypes")
	public List<TestInfo> getTests(String number) {
		int buildNumber = Integer.parseInt(number);
		Run run = project.getBuildByNumber(buildNumber);
		List<AbstractTestResultAction> testActions = run.getActions(hudson.tasks.test.AbstractTestResultAction.class);
		for (hudson.tasks.test.AbstractTestResultAction testAction : testActions) {
			/*TestResult testResult = (TestResult) testAction.getResult();
			System.out.println(testResult.getDisplayName());
			Collection<PackageResult> packageResults = testResult.getChildren();
			for (PackageResult packageResult : packageResults) {
				System.out.println(packageResult.getDisplayName());
				resultInfo.addPackage(buildNumber, packageResult);					
			}*/
		}
		return new ArrayList<TestInfo>();
	}
	
	public static class BuildInfo implements ExtensionPoint {
		private int number;
		private Calendar timestamp;
		private String timestampString;

		@DataBoundConstructor
		public BuildInfo(int number, Calendar timestamp, String timestampString) {
			this.number = number;
			this.timestamp = timestamp;
			this.timestampString = timestampString;
		}

		public int getNumber() {
			return number;
		}

		public String getTimestampString() {
			return timestampString;
		}

		public String getReadableTimestamp() {
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy h:mm:ss a");
			return dateFormat.format(timestamp.getTime());
		}
	}
	
	public static class TestInfo implements ExtensionPoint {
		private String name;
		private String status;
		
		@DataBoundConstructor
		public TestInfo(String name, String status) {
			this.name = name;
			this.status = status;
		}
		
		public String getName() {
			return name;
		}
		
		public String getStatus() {
			return status;
		}
	}
}