package jp.skypencil.jenkins.regression;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.AbstractProject;
import jp.skypencil.jenkins.regression.TestBuddyAction.BuildInfo;
import jp.skypencil.jenkins.regression.TestBuddyAction.TestInfo;

public class TestBuddyActionTest {
	@Rule
	public JenkinsRule j = new JenkinsRule();
	
	@SuppressWarnings("rawtypes")
	private AbstractProject project;
	private TestBuddyAction testBuddyAction;
	
	private void initFreeStyleProject() throws IOException {
		project = j.createFreeStyleProject("project1");
		testBuddyAction = new TestBuddyAction(project);
	}
	
	private void initMavenProject() throws Exception {
		j.configureMaven3();
		project = j.createMavenProject("project1");
		testBuddyAction = new TestBuddyAction(project);
	}

	private void createBuild(String source) throws Exception {
		project.setScm(new ExtractResourceSCM(getClass().getResource(source + ".zip")));
		project.scheduleBuild2(0).get();
	}
	
	private void createBuilds(int count) throws Exception {
		for (int i = 0; i < count; i++) {
			project.scheduleBuild2(0).get();
		}
	}
	
	@Test
	public void testGetUrl1() throws IOException {
		initFreeStyleProject();
		assertEquals("job/project1/test_buddy", testBuddyAction.getUrl());
	}
	
	@Test
	public void testGetUrl2() throws IOException {
		initFreeStyleProject();
		project.renameTo("project2");

		assertEquals("job/project2/test_buddy", testBuddyAction.getUrl());
	}
	
	@Test
	public void testGetBuilds1() throws IOException {
		initFreeStyleProject();
		List<BuildInfo> builds = testBuddyAction.getBuilds();
		assertEquals(0, builds.size());
	}
	
	@Test
	public void testGetBuilds2() throws Exception {
		initFreeStyleProject();
		createBuilds(3);

		List<BuildInfo> builds = testBuddyAction.getBuilds();
		assertEquals(3, builds.size());
		assertEquals(3, builds.get(0).getNumber());
		assertEquals(2, builds.get(1).getNumber());
		assertEquals(1, builds.get(2).getNumber());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetBuilds3() throws Exception {
		initFreeStyleProject();
		createBuilds(5);

		project.removeRun(project.getBuildByNumber(4));

		List<BuildInfo> builds = testBuddyAction.getBuilds();
		assertEquals(4, builds.size());
		assertEquals(5, builds.get(0).getNumber());
		assertEquals(3, builds.get(1).getNumber());
		assertEquals(2, builds.get(2).getNumber());
		assertEquals(1, builds.get(3).getNumber());
	}

	@Test
	public void testGetBuildInfo1() throws Exception {
		initFreeStyleProject();
		createBuilds(3);

		assertEquals(1, testBuddyAction.getBuildInfo("1").getNumber());
		assertEquals(2, testBuddyAction.getBuildInfo("2").getNumber());
		assertEquals(3, testBuddyAction.getBuildInfo("3").getNumber());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetBuildInfo2() throws Exception {
		initFreeStyleProject();
		createBuilds(5);

		project.removeRun(project.getBuildByNumber(4));

		assertEquals(1, testBuddyAction.getBuildInfo("1").getNumber());
		assertEquals(2, testBuddyAction.getBuildInfo("2").getNumber());
		assertEquals(3, testBuddyAction.getBuildInfo("3").getNumber());
		assertEquals(5, testBuddyAction.getBuildInfo("5").getNumber());
	}
	
	@Test
	public void testGetTests1() throws Exception {
		initMavenProject();
		createBuild("Source_3");

		List<TestInfo> tests = testBuddyAction.getTests("1");
		assertEquals(3, tests.size());

		assertEquals("testApp1", tests.get(0).getName());
		assertEquals("AppTest", tests.get(0).getClassName());
		assertEquals("pkg", tests.get(0).getPackageName());
		assertEquals("Passed", tests.get(0).getStatus());

		assertEquals("testApp2", tests.get(1).getName());
		assertEquals("AppTest", tests.get(1).getClassName());
		assertEquals("pkg", tests.get(1).getPackageName());
		assertEquals("Failed", tests.get(1).getStatus());

		assertEquals("testApp3", tests.get(2).getName());
		assertEquals("AppTest", tests.get(2).getClassName());
		assertEquals("pkg", tests.get(2).getPackageName());
		assertEquals("Passed", tests.get(2).getStatus());
	}
	
	@Test
	public void testGetTests2() throws Exception {
		initMavenProject();
		createBuild("Source_6");

		List<TestInfo> tests = testBuddyAction.getTests("1");
		assertEquals(3, tests.size());

		assertEquals("testApp1", tests.get(0).getName());
		assertEquals("AppTest", tests.get(0).getClassName());
		assertEquals("pkg", tests.get(0).getPackageName());
		assertEquals("Passed", tests.get(0).getStatus());

		assertEquals("testApp2", tests.get(1).getName());
		assertEquals("AppTest", tests.get(1).getClassName());
		assertEquals("pkg", tests.get(1).getPackageName());
		assertEquals("Passed", tests.get(1).getStatus());

		assertEquals("testApp3", tests.get(2).getName());
		assertEquals("AppTest", tests.get(2).getClassName());
		assertEquals("pkg", tests.get(2).getPackageName());
		assertEquals("Skipped", tests.get(2).getStatus());
	}
}
