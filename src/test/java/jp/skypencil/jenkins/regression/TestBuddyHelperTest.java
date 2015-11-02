package jp.skypencil.jenkins.regression;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.maven.MavenModuleSet;
import jp.skypencil.jenkins.regression.TestBuddyAction.BuildInfo;
import jp.skypencil.jenkins.regression.TestBuddyAction.TestInfo;
import jp.skypencil.jenkins.regression.TestBuddyHelper;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;

public class TestBuddyHelperTest {
	@Rule
	public JenkinsRule j = new JenkinsRule();
	private MavenModuleSet project;
	
	@Before
	public void init() throws Exception {
		j.configureMaven3();
		project = j.createMavenProject("project1");

		createBuild("Source_1");
		createBuild("Source_2");
		createBuild("Source_3");
		createBuild("Source_4");
		createBuild("Source_5");
		createBuild("Source_6");
	}

	private void createBuild(String source) throws Exception {
		project.setScm(new ExtractResourceSCM(getClass().getResource(source + ".zip")));
		project.scheduleBuild2(0);

		// This will ensure the build is completed before continuing the process
		j.waitUntilNoActivity();
	}
	
	@Test
	public void testGetAllCaseResultsForBuild1() {
        AbstractBuild build = project.getBuildByNumber(1);
        List<CaseResult> case_results = TestBuddyHelper.getAllCaseResultsForBuild(build);
        assertEquals(case_results.size(), 3);
	}

	@Test
	public void testGetAllCaseResultsForBuild2() {
        AbstractBuild build = project.getBuildByNumber(4);
        List<CaseResult> case_results = TestBuddyHelper.getAllCaseResultsForBuild(build);
        assertEquals(case_results.size(), 4);
	}

    @Test
    public void testGetTestFromTestResult1() {
    }

    @Test
    public void testGetChangedTestsBetweenBuilds1() {
    }
}
