package jp.skypencil.jenkins.regression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestResult;

import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.junit.TestResultAction;

public class TestBuddyHelper {


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
