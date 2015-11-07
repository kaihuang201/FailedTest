package jp.skypencil.jenkins.regression;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

	public static TreeMap<Integer,BuildInfo> all_builds = new TreeMap<Integer,BuildInfo>(Collections.reverseOrder());
	
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

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<BuildInfo> getBuilds() {
		Set<Integer> missingBuildNumbers = new HashSet<Integer> (all_builds.keySet()); 
		RunList<Run> runs = project.getBuilds();
		Iterator<Run> runIterator = runs.iterator();

		while(runIterator.hasNext()){
			Run run = runIterator.next();
			int num = run.getNumber();
			missingBuildNumbers.remove(num);

			if(!all_builds.containsKey(num)){
				List<String> authors = TestBuddyHelper.getAuthors((AbstractBuild) run);
				double rates[] = TestBuddyHelper.getRatesforBuild((AbstractBuild) run);
				BuildInfo build = new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2(), run.getResult().toString(), authors, rates[0], rates[1]);
				build.add_tests(get_ini_Tests(String.valueOf(build.getNumber())));
				all_builds.put(num, build);
			}
		}
		
		for (int missingBuildNumber: missingBuildNumbers) {
			all_builds.remove(missingBuildNumber);
		}

		return new ArrayList<BuildInfo> (all_builds.values());
	}
	
	public BuildInfo getBuildInfo(String number) {
		if(all_builds.containsKey(Integer.valueOf(number))){
			//System.out.println("getting local copy");
			return all_builds.get(Integer.valueOf(number));
		}

		//System.out.println("NOT nice, calling back up function");
		return getBuildInfo_backup(number);
	}
	
	@SuppressWarnings("rawtypes")
	public BuildInfo getBuildInfo_backup(String number) {
		int buildNumber = Integer.parseInt(number);
		Run run = project.getBuildByNumber(buildNumber);
		List<String> authors = TestBuddyHelper.getAuthors((AbstractBuild) run);
		double rates[] = TestBuddyHelper.getRatesforBuild((AbstractBuild) run);
		return new BuildInfo(run.getNumber(), run.getTimestamp(), run.getTimestampString2(), run.getResult().toString(), authors, rates[0], rates[1]);
	}

	@JavaScriptMethod
	public List<TestInfo> searchTests(String searchText) {
		HashMap<String, TestInfo> testMap = new HashMap<String, TestInfo>();
		
		for (BuildInfo build: getBuilds()) {
			for (TestInfo test: build.getTests()) {
				if (test.getFullName().toLowerCase().contains(searchText.toLowerCase())) {
					TestInfo testInfo;
					if (!testMap.containsKey(test.getFullName())) {
						testInfo = new TestInfo(test.getFullName(), test.getStatus());
						testMap.put(test.getFullName(), testInfo);
					}
					else {
						testInfo = testMap.get(test.getFullName());
					}
					
					testInfo.incrementCount(test.getStatus());
				}
			}
		}
		
		return new ArrayList<TestInfo> (testMap.values());
	}

	public Set<String> getAllAuthors() {
		Set<String> authors = new HashSet<String>();
		for(BuildInfo build : getBuilds()) {
			authors.addAll(build.getAuthors());
		}
		
		return authors;
	}
	

	public List<TestInfo> getTests(String number) {
		if(all_builds.containsKey(Integer.valueOf(number))){
			//System.out.println("getting local copy");
			return all_builds.get(Integer.valueOf(number)).getTests();
		}
		

		//System.out.println("NOT nice, calling back up function");
		return get_ini_Tests(number);
	}
	
	@SuppressWarnings("rawtypes")
	public List<TestInfo> get_ini_Tests(String number) {
		int buildNumber = Integer.parseInt(number);
		AbstractBuild build = project.getBuildByNumber(buildNumber);
		ArrayList<CaseResult> caseResults = TestBuddyHelper.getAllCaseResultsForBuild(build);

		return convertCaseResultsToTestInfos(caseResults, "Passed", "Failed");
	}
	
	@SuppressWarnings("rawtypes")
	public List<TestInfo> getNewPassFail() {
		AbstractBuild build = project.getLastBuild();
		return getChangedTests(build, build.getPreviousBuild(), "Newly Passed", "Newly Failed");
	}

	//compare two builds
	@SuppressWarnings("rawtypes")
	@JavaScriptMethod
	public List<TestInfo> getBuildCompare(String buildNumber1, String buildNumber2){
		int build1 = Integer.parseInt(buildNumber1);
		int build2 = Integer.parseInt(buildNumber2);
		AbstractBuild buildOne = project.getBuildByNumber(build1);
		AbstractBuild buildTwo = project.getBuildByNumber(build2);

		return getChangedTests(buildOne, buildTwo, "Status Changed", "Status Changed");  
	}
	
	@SuppressWarnings("rawtypes")
	public List<TestInfo> getChangedTests(AbstractBuild build1, AbstractBuild build2, String passedStatus, String failedStatus) {
		ArrayList<CaseResult> changedTests = TestBuddyHelper.getChangedTestsBetweenBuilds(build1, build2);
		return convertCaseResultsToTestInfos(changedTests, passedStatus, failedStatus);
	}
	
	public List<TestInfo> convertCaseResultsToTestInfos(List<CaseResult> caseResults, String passedStatus, String failedStatus) {
		List<TestInfo> tests = new ArrayList<TestInfo>();

		for (CaseResult caseResult : caseResults) {
			if(caseResult.isFailed()){
				tests.add(new TestInfo(caseResult.getFullName(), failedStatus));
			}
			else if(caseResult.isPassed()){
				tests.add(new TestInfo(caseResult.getFullName(), passedStatus));
			}
			else if(caseResult.isSkipped()){
				tests.add(new TestInfo(caseResult.getFullName(), "Skipped"));
			}
		}
		
		return tests;
	}
	
	public static class BuildInfo implements ExtensionPoint {
		private int number;
		private Calendar timestamp;
		private String timestampString;
		private String status;
		private List<String> authors;
		private int passed_tests;
		private double passing_rate;
		private List<TestInfo> tests;
		@DataBoundConstructor
		public BuildInfo(int number, Calendar timestamp, String timestampString, String s, List<String> a, double pt, double pr) {
			this.number = number;
			this.timestamp = timestamp;
			this.timestampString = timestampString;
			this.status = s;
			this.authors = new ArrayList<String>(a);
			this.passed_tests = (int) pt;
			this.passing_rate = pr;
			this.tests = new ArrayList<TestInfo>();
		}

		public void add_tests(List<TestInfo> t){
			tests = t;
		}
		
		public List<TestInfo> getTests(){
			return tests;
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
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < authors.size(); i++) {
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(authors.get(i));
			}

			return builder.toString();
		}
		
		public int getPassedTests(){
			return passed_tests;
		}
		
		public double getPassingRate(){
			return passing_rate;
		}
	}
	
	public static class TestInfo implements ExtensionPoint {
		private String fullName;
		private String name;
		private String className;
		private String packageName;
		private String status;
		private int passedCount = 0;
		private int failedCount = 0;
		private int skippedCount = 0;
		
		@DataBoundConstructor
		public TestInfo(String fullName, String status) {
			this.fullName = fullName;
			this.status = status;
			
			parseNames();
		}
		
		/* Parse name, className, and packageName from fullName */
		private void parseNames() {
			String[] fullNameArray = fullName.split("\\.");
			name = fullNameArray[fullNameArray.length - 1];
			className = fullNameArray[fullNameArray.length - 2];
			
			if (fullName.length() > (name.length() + className.length() + 1)) {
				packageName = fullName.substring(0, fullName.length() - name.length() - className.length() - 2);
			}
		}
		
		public String getFullName() {
			return fullName;
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
		
		public int getPassedCount(){
			return passedCount;
			
		}
		
		public int getFailedCount(){
			return failedCount;
			
		}
		
		public int getSkippedCount(){
			return skippedCount;
			
		}
		
		public void incrementCount(String statusToIncrement) {
			if (statusToIncrement == "Passed") {
				passedCount++;
			}
			else if (statusToIncrement == "Failed") {
				failedCount++;
			}
			else if (statusToIncrement == "Skipped") {
				skippedCount++;
			}
		}
	}
	
}