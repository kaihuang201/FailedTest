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

import hudson.tasks.junit.CaseResult;
import hudson.model.AbstractBuild;

public class TestBuddyHelperTest {
	@Rule
	public JenkinsRule j = new JenkinsRule();
	private MavenModuleSet project;
	private TestBuddyAction testBuddyAction;
	
	@Before
	public void init() throws Exception {
		j.configureMaven3();
		project = j.createMavenProject("project1");
		testBuddyAction = new TestBuddyAction(project);

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
	public void testGetBuilds1() {
		List<BuildInfo> myBuilds = testBuddyAction.getBuilds();
		assertTrue(myBuilds.size() == 6);		
	}

	@Test
	public void testGetAllCaseResultForBuild1() {
		assertTrue(true);
	}

	@Test
	public void testGetAllCaseResultForBuild2() {
		assertTrue(true);
	}

    @Test
    public void testGetTestFromTestResult1() {
		assertTrue(true);
    }

    @Test
    public void testGetChangedTestsBetweenBuilds1() {
		List<CaseResult> diffArray;	
	
		AbstractBuild b1 = project.getBuildByNumber(1);
		AbstractBuild b2 = project.getBuildByNumber(2);
		
		List<CaseResult> caseResultsB1 = TestBuddyHelper.getAllCaseResultsForBuild(b1);
		List<CaseResult> caseResultsB2 = TestBuddyHelper.getAllCaseResultsForBuild(b2);

		assertEquals(3, caseResultsB1.size());
		// diffArray = TestBuddyHelper.getChangedTestsBetweenBuilds(b1, b2);
		assertTrue(true);
    }
}
