package jp.skypencil.jenkins.regression;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
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

	@SuppressWarnings({"rawtypes", "unchecked"})
	public List<Build> getBuilds() {
		List<Build> builds = new ArrayList<Build>();
		RunList<Run> runs = project.getBuilds();
		Iterator<Run> runIterator = runs.iterator();
		while (runIterator.hasNext()) {
			Run run = runIterator.next();
			Build build = new Build(run.getNumber(), run.getTimestamp(), run.getTimestampString2());
			builds.add(build);
		}
		
		return builds;
	}

	public static class Build implements ExtensionPoint {
		private int number;
		private Calendar timestamp;
		private String timestampString;

		@DataBoundConstructor
		public Build(int number, Calendar timestamp, String timestampString) {
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
}