package jp.skypencil.jenkins.regression;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.util.RunList;
import jp.skypencil.jenkins.regression.TestBuddyHelper;

public class TestBuddyAction extends Actionable implements Action {
	@SuppressWarnings("rawtypes")
	AbstractProject project;
	private static final String[] BUILD_STATUSES = {"SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"};
	
	public TestBuddyAction(@SuppressWarnings("rawtypes") AbstractProject project){
		this.project = project;
	}

	/**
     * The display name for the action.
     * 
     * @return the name as String
     */
    public final String getDisplayName() {
        return "Test Buddy";
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
        return "test_buddy";
    }

    /**
     * Search url for this action.
     * 
     * @return the url as String
     */
	public String getSearchUrl() {
		return "test_buddy";
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
			List<String> authors = TestBuddyHelper.getChangeLogForBuild((AbstractBuild) run);
			double rates[] = TestBuddyHelper.getRatesforBuild((AbstractBuild) run);
			BuildInfo build = new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2(), BUILD_STATUSES[run.getResult().ordinal], authors, rates[0], rates[1]);
			builds.add(build);
		}
		
		return builds;
	}
	
	@SuppressWarnings("rawtypes")
	public BuildInfo getBuildInfo(String number) {
		int buildNumber = Integer.parseInt(number);
		Run run = project.getBuildByNumber(buildNumber);
		List<String> authors = TestBuddyHelper.getChangeLogForBuild((AbstractBuild) run);
		double rates[] = TestBuddyHelper.getRatesforBuild((AbstractBuild) run);
		return new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2(), run.getBuildStatusSummary().message, authors, rates[0], rates[1]);
	}
	
	public List<String> getAllBuildStatuses() {
		List<String> buildStatuses = Arrays.asList(BUILD_STATUSES.clone());
		Collections.sort(buildStatuses);
		return buildStatuses;
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Set<String> getAllAuthors() {
		Set<String> authors = new HashSet<String>();
		RunList<Run> runs = project.getBuilds();
		Iterator<Run> runIterator = runs.iterator();

		while (runIterator.hasNext()) {
			Run run = runIterator.next();
			authors.addAll(TestBuddyHelper.getChangeLogForBuild((AbstractBuild) run));
		}
		
		return authors;
	}
	
	@SuppressWarnings("rawtypes")
	public List<TestInfo> getTests(String number) {
		List<TestInfo> tests = new ArrayList<TestInfo>();
		
		int buildNumber = Integer.parseInt(number);
		AbstractBuild build = project.getBuildByNumber(buildNumber);
		
		ArrayList<CaseResult> caseResults = TestBuddyHelper.getAllCaseResultsForBuild(build);
		for (CaseResult caseResult : caseResults) {
			String className = "";
			String[] fullClassName = caseResult.getClassName().split("\\.");
			if (fullClassName.length > 0) {
				className = fullClassName[fullClassName.length - 1];
			}  
			String name[] = caseResult.getDisplayName().split("\\.");
			String short_name = name[name.length-1];
			if(caseResult.isFailed()){
				tests.add(new TestInfo(short_name, className, caseResult.getPackageName(), "Failed"));
			}
			else if(caseResult.isPassed()){
				tests.add(new TestInfo(short_name, className, caseResult.getPackageName(), "Passed"));
			}
			else if(caseResult.isSkipped()){
				tests.add(new TestInfo(short_name, className, caseResult.getPackageName(), "Skipped"));
			}			
		}
		
		return tests;
	}
	
	public List<TestInfo> getNewPassFail() {
		AbstractBuild build = project.getLastBuild();
		List<TestInfo> failPassTests = new ArrayList<TestInfo>();
		ArrayList<CaseResult> newlyFailPass = TestBuddyHelper.getChangedTestsBetweenBuilds(build, build.getPreviousBuild());
		for (CaseResult caseResult : newlyFailPass) {
			if(caseResult.isFailed()){
				failPassTests.add(new TestInfo(caseResult.getDisplayName(), null, caseResult.getPackageName(), "Newly Failed"));
			}
			else{
				failPassTests.add(new TestInfo(caseResult.getDisplayName(), null, caseResult.getPackageName(), "Newly Passed"));
			}		
		}
		//System.out.println("size of newPassFail array is " + newlyFailPass.size());
		return failPassTests;
	}

	//compare two builds
	@JavaScriptMethod
	public List<TestInfo> getBuildCompare(String buildNumber1, String buildNumber2){
		int build1 = Integer.parseInt(buildNumber1);
		int build2 = Integer.parseInt(buildNumber2);
		  AbstractBuild buildOne = project.getBuildByNumber(build1);
		  AbstractBuild buildTwo = project.getBuildByNumber(build2);
		  List<TestInfo> compareList = new ArrayList<TestInfo>();
		  ArrayList<CaseResult> buildCompare = TestBuddyHelper.getChangedTestsBetweenBuilds(buildOne, buildTwo);
		  for (CaseResult caseResult : buildCompare) {
		    compareList.add(new TestInfo(caseResult.getDisplayName(), null, caseResult.getPackageName(), "Status Changed"));
		  }
		  return compareList;  
		 }
	
	public static class BuildInfo implements ExtensionPoint {
		private int number;
		private Calendar timestamp;
		private String timestampString;
		private String status;
		private List<String> authors;
		private int passed_tests;
		private double passing_rate;
		@DataBoundConstructor
		public BuildInfo(int number, Calendar timestamp, String timestampString, String s, List<String> a, double pt, double pr) {
			this.number = number;
			this.timestamp = timestamp;
			this.timestampString = timestampString;
			this.status = s;
			this.authors = new ArrayList<String>(a);
			this.passed_tests = (int) pt;
			this.passing_rate = pr;
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
		
		public String getStatus(){
			return status;
		}
		
		public List<String> getAuthors(){
			return authors;
		}
		
		public String getAuthorsString() {
			return String.join(", ", authors);
		}
		
		public int getPassedTests(){
			return passed_tests;
		}
		
		public double getPassingRate(){
			return passing_rate;
		}
	}
	
	public static class TestInfo implements ExtensionPoint {
		private String name;
		private String className;
		private String packageName;
		private String status;
		
		@DataBoundConstructor
		public TestInfo(String name, String className, String packageName, String status) {
			this.name = name;
			this.className = className;
			this.packageName = packageName;
			this.status = status;
		}
		
		public String getName() {
			return name;
		}

		public String getClassName() {
			return className;
		}
		
		public String getPackageName() {
			return packageName;
		}
		
		public String getStatus() {
			return status;
		}
	}
	
}