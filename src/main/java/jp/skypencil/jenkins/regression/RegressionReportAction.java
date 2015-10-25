package jp.skypencil.jenkins.regression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.RunList;
import net.sf.json.JSONObject;

public class RegressionReportAction extends Actionable implements Action {
@SuppressWarnings("rawtypes") AbstractProject project;
	
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
    
    public List<JSONObject> getBuilds() {
    	List<JSONObject> builds = new ArrayList<JSONObject>();
		RunList<Run> runs = project.getBuilds();
		Iterator<Run> runIterator = runs.iterator();
		while (runIterator.hasNext()) {
			Run run = runIterator.next();
			JSONObject build = new JSONObject();
			build.put("buildNumber", run.getNumber());
			build.put("timestamp", run.getTimestampString2());
			builds.add(build);
		}
		
		return builds;
    }
}