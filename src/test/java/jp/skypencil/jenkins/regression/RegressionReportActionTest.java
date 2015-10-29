package jp.skypencil.jenkins.regression;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleProject;

import jp.skypencil.jenkins.regression.RegressionReportAction.BuildInfo;

public class RegressionReportActionTest {
	@Rule
	public JenkinsRule j = new JenkinsRule();
	
	private FreeStyleProject project;
	private RegressionReportAction regressionReportAction;
	
	@Before
	public void init() throws IOException {
		project = j.createFreeStyleProject("project1");
		regressionReportAction = new RegressionReportAction(project);
	}

	private void initBuilds(int numberOfBuilds) throws Exception {
		for (int i = 0; i < numberOfBuilds; i++) {
			j.buildAndAssertSuccess(project);
		}
	}
	
	@Test
	public void testGetUrl1() {
		assertEquals("job/project1/failedTest", regressionReportAction.getUrl());
	}

	@Test
	public void testGetUrl2() throws IOException {
		project.renameTo("project2");
		assertEquals("job/project2/failedTest", regressionReportAction.getUrl());
	}

	@Test
	public void testGetBuilds1() throws Exception {
		List<BuildInfo> builds = regressionReportAction.getBuilds();
		assertEquals(0, builds.size());
	}
	
	@Test
	public void testGetBuilds2() throws Exception {
		initBuilds(3);
		List<BuildInfo> builds = regressionReportAction.getBuilds();
		assertEquals(3, builds.size());
		assertEquals(3, builds.get(0).getNumber());
		assertEquals(2, builds.get(1).getNumber());
		assertEquals(1, builds.get(2).getNumber());
	}
	
	@Test
	public void testGetBuilds3() throws Exception {
		initBuilds(5);
		project.removeRun(project.getBuildByNumber(4));
		List<BuildInfo> builds = regressionReportAction.getBuilds();
		assertEquals(4, builds.size());
		assertEquals(5, builds.get(0).getNumber());
		assertEquals(3, builds.get(1).getNumber());
		assertEquals(2, builds.get(2).getNumber());
		assertEquals(1, builds.get(3).getNumber());
	}

	@Test
	public void testGetBuildInfo1() throws Exception {
		initBuilds(3);
		assertEquals(1, regressionReportAction.getBuildInfo("1").getNumber());
		assertEquals(2, regressionReportAction.getBuildInfo("2").getNumber());
		assertEquals(3, regressionReportAction.getBuildInfo("3").getNumber());
	}
	
	@Test
	public void testGetBuildInfo2() throws Exception {
		initBuilds(5);
		project.removeRun(project.getBuildByNumber(4));
		assertEquals(1, regressionReportAction.getBuildInfo("1").getNumber());
		assertEquals(2, regressionReportAction.getBuildInfo("2").getNumber());
		assertEquals(3, regressionReportAction.getBuildInfo("3").getNumber());
		assertEquals(5, regressionReportAction.getBuildInfo("5").getNumber());
	}
	
}
