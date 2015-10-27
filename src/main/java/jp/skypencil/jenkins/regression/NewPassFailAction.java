package jp.skypencil.jenkins.regression;

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

public class NewPassFailAction extends Actionable implements Action {
	@SuppressWarnings("rawtypes")
	AbstractProject project;

	public NewPassFailAction(@SuppressWarnings("rawtypes") AbstractProject project){
		this.project = project;
	}

	/**
	 * The display name for the action.
	 * 
	 * @return the name as String
	 */
	public final String getDisplayName() {
		return "NewlyPassFailTestResults";
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
		return "newlyPassFailTestResults";
	}

	/**
	 * Search url for this action.
	 * 
	 * @return the url as String
	 */
	public String getSearchUrl() {
		return "newlyPassFailTestResults";
	}

	@SuppressWarnings("rawtypes")
	public AbstractProject getProject() {
		return this.project;
	}

}
