package jp.skypencil.jenkins.regression;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;

public class TestBuddyHelper {


    public static ArrayList<CaseResult> getAllCaseResultsForBuild(AbstractBuild build) {
        //TODO is this correct? Use getActions instead? Use AggregatedTestResultAction instead?
        List<AbstractTestResultAction> testActions = build.getActions(AbstractTestResultAction.class);
        ArrayList<CaseResult> returnValue = new ArrayList<CaseResult>();
        //TODO use Sundie's method here?
        return returnValue;
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
