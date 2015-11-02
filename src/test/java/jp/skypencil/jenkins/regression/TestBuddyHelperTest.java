package jp.skypencil.jenkins.regression;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.maven.MavenModuleSet;
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
	}

	private void createBuild(String source) throws Exception {
		project.setScm(new ExtractResourceSCM(getClass().getResource(source + ".zip")));
		project.scheduleBuild2(0).get();
	}
	
	@Test
	public void testGetAllCaseResultsForBuild1() throws Exception {
		createBuild("Source_1");
		AbstractBuild build = project.getBuildByNumber(1);
        List<CaseResult> case_results = TestBuddyHelper.getAllCaseResultsForBuild(build);
        assertEquals(case_results.size(), 3);
	}

	@Test
	public void testGetAllCaseResultsForBuild2() throws Exception {
		createBuild("Source_4");
        AbstractBuild build = project.getBuildByNumber(1);
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
