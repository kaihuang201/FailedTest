package jp.skypencil.jenkins.regression;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.AbstractProject;
import jp.skypencil.jenkins.regression.TestBuddyAction.BuildInfo;
import jp.skypencil.jenkins.regression.TestBuddyAction.TestInfo;

public class TestBuddyActionTest {
	@Rule
	public JenkinsRule j = new JenkinsRule();
	
	@SuppressWarnings("rawtypes")
	private AbstractProject project;
	private TestBuddyAction testBuddyAction;
	
	@Before
	public void init() {
		project = j.jenkins.getItemByFullName("project1", AbstractProject.class);
		testBuddyAction = new TestBuddyAction(project);
	}

	@LocalData
	@Test
	public void testGetUrl1() {
		assertEquals("job/project1/test_buddy", testBuddyAction.getUrl());
	}

	@LocalData
	@Test
	public void testGetUrl2() throws IOException {
		project.renameTo("project2");

		assertEquals("job/project2/test_buddy", testBuddyAction.getUrl());
	}

	@LocalData
	@Test
	public void testGetBuilds1() {
		List<BuildInfo> builds = testBuddyAction.getBuilds();
		assertEquals(5, builds.size());
		assertEquals(5, builds.get(0).getNumber());
		assertEquals(4, builds.get(1).getNumber());
		assertEquals(3, builds.get(2).getNumber());
		assertEquals(2, builds.get(3).getNumber());
		assertEquals(1, builds.get(4).getNumber());
	}
	
	@SuppressWarnings("unchecked")
	@LocalData
	@Test
	public void testGetBuilds2() {
		project.removeRun(project.getBuildByNumber(4));

		List<BuildInfo> builds = testBuddyAction.getBuilds();
		assertEquals(4, builds.size());
		assertEquals(5, builds.get(0).getNumber());
		assertEquals(3, builds.get(1).getNumber());
		assertEquals(2, builds.get(2).getNumber());
		assertEquals(1, builds.get(3).getNumber());
	}

	@LocalData
	@Test
	public void testGetBuildInfo1() {
		BuildInfo buildInfo = testBuddyAction.getBuildInfo("1");
		assertEquals(1, buildInfo.getNumber());
		assertEquals("SUCCESS", buildInfo.getStatus());
		assertEquals(2, buildInfo.getPassedTests());
		assertEquals(0.67, buildInfo.getPassingRate(), 0.001);
		assertEquals(0, buildInfo.getAuthors().size());
	}

	@LocalData
	@Test
	public void testGetBuildInfo2() {
		BuildInfo buildInfo = testBuddyAction.getBuildInfo("2");
		assertEquals(2, buildInfo.getNumber());
		assertEquals("SUCCESS", buildInfo.getStatus());
		assertEquals(3, buildInfo.getPassedTests());
		assertEquals(1, buildInfo.getPassingRate(), 0.001);
		assertEquals(1, buildInfo.getAuthors().size());
		assertEquals("developer2", buildInfo.getAuthors().get(0));
	}
	
	@LocalData
	@Test
	public void testGetBuildInfo3() {
		BuildInfo buildInfo = testBuddyAction.getBuildInfo("3");
		assertEquals(3, buildInfo.getNumber());
		assertEquals("SUCCESS", buildInfo.getStatus());
		assertEquals(4, buildInfo.getPassedTests());
		assertEquals(1, buildInfo.getPassingRate(), 0.001);
		assertEquals(2, buildInfo.getAuthors().size());
		assertEquals("developer1", buildInfo.getAuthors().get(0));
		assertEquals("developer2", buildInfo.getAuthors().get(1));
	}
	
	@LocalData
	@Test
	public void testGetBuildInfo4() {
		BuildInfo buildInfo = testBuddyAction.getBuildInfo("4");
		assertEquals(4, buildInfo.getNumber());
		assertEquals("UNSTABLE", buildInfo.getStatus());
		assertEquals(4, buildInfo.getPassedTests());
		assertEquals(0.8, buildInfo.getPassingRate(), 0.001);
		assertEquals(1, buildInfo.getAuthors().size());
		assertEquals("developer2", buildInfo.getAuthors().get(0));
	}
	
	@LocalData
	@Test
	public void testGetBuildInfo5() {
		BuildInfo buildInfo = testBuddyAction.getBuildInfo("5");
		assertEquals(5, buildInfo.getNumber());
		assertEquals("UNSTABLE", buildInfo.getStatus());
		assertEquals(2, buildInfo.getPassedTests());
		assertEquals(0.5, buildInfo.getPassingRate(), 0.001);
		assertEquals(1, buildInfo.getAuthors().size());
		assertEquals("developer1", buildInfo.getAuthors().get(0));
	}

	@LocalData
	@Test
	public void testGetTests1() {
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
	
	@LocalData
	@Test
	public void testGetTests2() {
		List<TestInfo> tests = testBuddyAction.getTests("2");
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
		assertEquals("Passed", tests.get(2).getStatus());
	}

	@LocalData
	@Test
	public void testGetTests3() {
		List<TestInfo> tests = testBuddyAction.getTests("3");
		assertEquals(4, tests.size());

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
		assertEquals("Passed", tests.get(2).getStatus());

		assertEquals("testApp4", tests.get(3).getName());
		assertEquals("AppTest", tests.get(3).getClassName());
		assertEquals("pkg", tests.get(3).getPackageName());
		assertEquals("Passed", tests.get(3).getStatus());
	}

	@LocalData
	@Test
	public void testGetTests4() {
		List<TestInfo> tests = testBuddyAction.getTests("4");
		assertEquals(5, tests.size());

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
		assertEquals("Passed", tests.get(2).getStatus());

		assertEquals("testApp4", tests.get(3).getName());
		assertEquals("AppTest", tests.get(3).getClassName());
		assertEquals("pkg", tests.get(3).getPackageName());
		assertEquals("Passed", tests.get(3).getStatus());

		assertEquals("testApp5", tests.get(4).getName());
		assertEquals("AppTest", tests.get(4).getClassName());
		assertEquals("pkg", tests.get(4).getPackageName());
		assertEquals("Failed", tests.get(4).getStatus());
	}
	
	@LocalData
	@Test
	public void testGetTests5() {
		List<TestInfo> tests = testBuddyAction.getTests("5");
		assertEquals(4, tests.size());

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
		assertEquals("Failed", tests.get(2).getStatus());

		assertEquals("testApp5", tests.get(3).getName());
		assertEquals("AppTest", tests.get(3).getClassName());
		assertEquals("pkg", tests.get(3).getPackageName());
		assertEquals("Passed", tests.get(3).getStatus());
	}
	
	@LocalData
	@Test
	public void testGetNewPassFail() {
		List<TestInfo> newFailPass = testBuddyAction.getNewPassFail();
		
		assertEquals(3, newFailPass.size());

		assertEquals("Newly Failed", newFailPass.get(0).getStatus());
		assertEquals("Newly Failed", newFailPass.get(1).getStatus());
		assertEquals("Newly Passed", newFailPass.get(2).getStatus());
	}
	
	@LocalData
	@Test
	public void testGetBuildCompare1() {
		List<TestInfo> testDifferences = testBuddyAction.getBuildCompare("4", "1");
		
		assertEquals(0, testDifferences.size());
	}

	@LocalData
	@Test
	public void testGetBuildCompare2() {
		List<TestInfo> testDifferences = testBuddyAction.getBuildCompare("5", "3");
		
		assertEquals(2, testDifferences.size());

		assertEquals("Status Changed", testDifferences.get(0).getStatus());
		assertEquals("Status Changed", testDifferences.get(1).getStatus());
	}

	@LocalData
	@Test
	public void testSearchTests() {
		List<TestInfo> searchResults = testBuddyAction.searchTests("app3");
		assertEquals(1, searchResults.size());

		assertEquals("testApp3", searchResults.get(0).getName());
		assertEquals("AppTest", searchResults.get(0).getClassName());
		assertEquals("pkg", searchResults.get(0).getPackageName());
		assertEquals(3, searchResults.get(0).getPassedCount());
		assertEquals(1, searchResults.get(0).getFailedCount());
		assertEquals(1, searchResults.get(0).getSkippedCount());
	}
}
