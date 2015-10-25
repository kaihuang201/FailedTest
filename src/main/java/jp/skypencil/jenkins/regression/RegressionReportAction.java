package jp.skypencil.jenkins.regression;

import java.util.ArrayList;
import java.util.List;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Actionable;

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
    
}