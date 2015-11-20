package jp.skypencil.jenkins.regression;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestResult;

import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.junit.TestResultAction;

public class TestBuddyHelper {

    /** 
     * Returns a list of CaseResults that are contained in a build. Currently this
     * function only handles builds whose getResult return 
     * an object of type TestResultAction or AggregatedTestResultAction
     * @param build an AbstractBuild object from which the caller wants to get
     *      the case results.
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
            }
            else if (testAction instanceof AggregatedTestResultAction){
                List<AggregatedTestResultAction.ChildReport> child_reports = ((AggregatedTestResultAction)testAction).getChildReports();
                for(AggregatedTestResultAction.ChildReport child_report: child_reports){
                    TestResult testResult = (TestResult) child_report.result;
                    ret.addAll(getTestsFromTestResult(testResult));
                }
            } else {
                //TODO Unexpected?
            }
        }
        return ret;
    }
    
    /**
     * Returns a list of authors that make the change to the build
     * @param build an AbstractBuild object from which the caller wants to get
     *      the case results.
     * @return an List of String.
     * **/
    @SuppressWarnings("rawtypes")
    public static List<String> getAuthors(AbstractBuild build) {
    	List<String> ret = new ArrayList<String>();
    	ChangeLogSet change = build.getChangeSet();
    	if(!change.isEmptySet()){
    		for(Object entry:change.getItems()){
    			hudson.scm.ChangeLogSet.Entry e = (hudson.scm.ChangeLogSet.Entry)entry;
    			if(!ret.contains(e.getAuthor().getDisplayName())){
    				ret.add(e.getAuthor().getDisplayName());
    			}
    			
    			//System.out.println(e.getAuthor().getDisplayName());
    		}
    	}
    	
    	return ret;
      // System.out.println(build.getChangeSet().getItems()[0].toString());
    }
    
	/**
	 * Returns a double array containing passed tests number, and passing rate for a build
     * @param build an AbstractBuild object from which the caller wants to get
     *      the case results.
     * @return an Array of double.
	 * **/
	@SuppressWarnings("rawtypes")
	public static double[] getRatesforBuild(AbstractBuild build){
		double[] ret = new double[2];
		ArrayList<CaseResult> caseResults = TestBuddyHelper.getAllCaseResultsForBuild(build);
		double total_tests = caseResults.size();
		double passed_tests = 0;
		double passing_rate = 0;
		for (CaseResult caseResult : caseResults){
			if(caseResult.isPassed()){
				passed_tests++;
			}
		}
		if(total_tests != 0){
			passing_rate = passed_tests/total_tests;
		}
		DecimalFormat df = new DecimalFormat("#.##");      
		passing_rate = Double.valueOf(df.format(passing_rate));
		ret[0] = passed_tests;
		ret[1] = passing_rate;
		return ret;
	}


    	/** New Function **/
	private int reverseInteger (int n) {
		while(n!=0){
			n /= 10;
		}
		return n;
	}    

    /**
     * A helper fuction that returns a of CaseResult from a TestReult
     * object.
     * @param testResult a TestResult object that contains PackageResult as its
     *      children
     * @return An ArrayList of CaseResult.
     */
    private static ArrayList<CaseResult> getTestsFromTestResult(TestResult testResult) {
        ArrayList<CaseResult> tests = new ArrayList<CaseResult>();
        Collection<PackageResult> packageResults = testResult.getChildren();
        for (PackageResult packageResult : packageResults) {
            Collection<ClassResult> class_results = packageResult.getChildren();
            for(ClassResult class_result : class_results){
                Collection<CaseResult> case_results = class_result.getChildren();
                tests.addAll(case_results);
            }
        }

        return tests;
    }


    /**
     * Given two builds thisBuild and otherBuild, returns the a list of Tuples
     * of matching CaseResult. Each pair is of form 
     * (CaseResultFromThisBuild, CaseResultFromThatBuild)
     *
     * @param thisBuild an AbstractBuild.
     * @param otherBuild another AbstractBuild, which is compared against thisBuild
     * @return an ArrayList of Tuples of CaseResults.Each pair is of form 
     * (CaseResultFromThisBuild, CaseResultFromThatBuild)
     * if a matching case result is not found in the other build, a null is used
     * instead.
     */
    public static ArrayList<Tuple<CaseResult, CaseResult>> matchTestsBetweenBuilds(AbstractBuild thisBuild, AbstractBuild otherBuild) {
        ArrayList<CaseResult> thisResults = getAllCaseResultsForBuild(thisBuild);
        ArrayList<CaseResult> otherResults = getAllCaseResultsForBuild(otherBuild);

        HashMap<String, CaseResult> hmap = new HashMap<String, CaseResult>();
        for (CaseResult otherCaseResult : otherResults) {
            hmap.put(otherCaseResult.getFullName(), otherCaseResult); // add (test_name, CaseResult) to hmap
        }

        ArrayList<Tuple<CaseResult, CaseResult>> returnValue = new ArrayList<Tuple<CaseResult, CaseResult>>();
        for (CaseResult thisCaseResult : thisResults) {
            String currTestName = thisCaseResult.getFullName();
            CaseResult otherCaseResult = null;
            if (hmap.containsKey(currTestName)) {
                otherCaseResult = hmap.get(currTestName);
                hmap.remove(currTestName);
            }
            Tuple tuple = new Tuple<CaseResult, CaseResult>(thisCaseResult, otherCaseResult);
            returnValue.add(tuple);
        }

        for (CaseResult otherCaseResultInMap : hmap.values()) {
            Tuple tuple = new Tuple<CaseResult, CaseResult>(null, otherCaseResultInMap);
            returnValue.add(tuple);
        }

        return returnValue;
    }


    /**
     * Given two builds thisBuild and otherBuild, returns the a list of
     * CaseResult that have different fail/pass results.
     *
     * @param thisBuild an AbstractBuild.
     * @param otherBuild another AbstractBuild, which is compared againt thisBuild
     * @return an ArrayList of CaseResult in thisBuild that satisfies any of
     *  the folloing conditions:
     *      - fails in thisBuild, but passes in otherBuild
     *      - passes in thisBuild, but failes in otherBuild
     */
    public static ArrayList<CaseResult> getChangedTestsBetweenBuilds(AbstractBuild thisBuild, AbstractBuild otherBuild) {
        ArrayList<CaseResult> thisResults = getAllCaseResultsForBuild(thisBuild);
        ArrayList<CaseResult> otherResults = getAllCaseResultsForBuild(otherBuild);

        HashMap<String, CaseResult> hmap = new HashMap<String, CaseResult>();
        for (CaseResult otherCaseResult : otherResults) {
            hmap.put(otherCaseResult.getFullName(), otherCaseResult); // add (test_name, CaseResult) to hmap
        }

        ArrayList<CaseResult> returnValue = new ArrayList<CaseResult>();
        for (CaseResult thisCaseResult : thisResults) {
            String currTestName = thisCaseResult.getFullName();
            if (hmap.containsKey(currTestName)) {
                CaseResult otherCaseResult = hmap.get(currTestName);
                if (
                    (thisCaseResult.isPassed() && otherCaseResult.isFailed()) ||
                    (thisCaseResult.isFailed() && otherCaseResult.isPassed())
                    ) {
                    returnValue.add(thisCaseResult);
                }
                //TODO Handle other possible cases here.
            }
        }
        return returnValue;
    }
    
}
