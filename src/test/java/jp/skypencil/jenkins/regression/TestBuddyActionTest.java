package jp.skypencil.jenkins.regression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.junit.CaseResult;

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

		assertEquals("Failed", testDifferences.get(0).getStatus());
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

	@LocalData
	@Test
	public void testListTests() {
		List<TestInfo> testList = testBuddyAction.listTests();
		assertEquals(5, testList.size());

		assertEquals("testApp1", testList.get(0).getName());
		assertEquals("AppTest", testList.get(0).getClassName());
		assertEquals("pkg", testList.get(0).getPackageName());
		assertEquals(5, testList.get(0).getPassedCount());
		assertEquals(3, testList.get(2).getPassedCount());
		assertEquals(1, testList.get(2).getFailedCount());
		assertEquals(1, testList.get(2).getSkippedCount());
	}

	@LocalData
	@Test
	public void testGetAllTestInfosForTestName() {
		List<TestInfo> allTests = testBuddyAction.getAllTestInfosForTestName("pkg.AppTest.testApp2");
		assertEquals(5, allTests.size());
		assertEquals("testApp2", allTests.get(4).getName());

		allTests = testBuddyAction.getAllTestInfosForTestName("pkg.AppTest.testApp4");
		assertEquals(2, allTests.size());
		assertEquals("testApp4", allTests.get(1).getName());

		allTests = testBuddyAction.getAllTestInfosForTestName("pkg.AppTest.testApp5");
		assertEquals(2, allTests.size());
		assertEquals("testApp5", allTests.get(1).getName());
	}

	@LocalData
	@Test
	public void testGetTestRates() {
		String[] rates = testBuddyAction.getTestRates("pkg.AppTest.testApp2");
		assertEquals("4", rates[1]); // passedNum
		assertEquals("1", rates[2]); // failedNum
		assertEquals("0", rates[3]); // skippedNum
		assertEquals("0.8", rates[0]);
		rates = testBuddyAction.getTestRates("pkg.AppTest.testApp3");
		assertEquals("3", rates[1]); // passedNum
		assertEquals("1", rates[2]); // failedNum
		assertEquals("1", rates[3]); // skippedNum
		assertEquals("0.6", rates[0]);
	}

	@LocalData
	@Test
	public void testGetTestName() {
		String name = testBuddyAction.getTestName("pkg.AppTest.testApp1");
		assertEquals("pkg AppTest testApp1", name);
		name = testBuddyAction.getTestName("pkg.AppTest.testApp4");
		assertEquals("pkg AppTest testApp4", name);
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testConvertCaseResultsToTestInfosTwo1() {
		AbstractBuild b3 = project.getBuildByNumber(3);
		AbstractBuild b4 = project.getBuildByNumber(4);

		ArrayList<Pair<CaseResult, CaseResult>> myTuples = TestBuddyHelper.matchTestsBetweenBuilds(b3, b4);
		assertEquals(5, myTuples.size());

		ArrayList<Pair<TestInfo, TestInfo>> arr = (ArrayList<Pair<TestInfo, TestInfo>>) testBuddyAction
				.convertCaseResultsToTestInfos(myTuples, 3, 4);
		assertEquals(5, arr.size());

		assertEquals("pkg.AppTest.testApp1", arr.get(0).first.getFullName());
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testConvertAid1() {
		AbstractBuild b2 = project.getBuildByNumber(2);
		AbstractBuild b4 = project.getBuildByNumber(4);

		ArrayList<Pair<CaseResult, CaseResult>> myTuples = TestBuddyHelper.matchTestsBetweenBuilds(b2, b4);
		assertEquals(5, myTuples.size());

		Pair<CaseResult, CaseResult> lastPair = myTuples.get(4);
		TestInfo myTestInfo1 = testBuddyAction.convertAid(lastPair.first, lastPair.second, "Passed", "Failed", 2);
		TestInfo myTestInfo2 = testBuddyAction.convertAid(lastPair.second, lastPair.first, "Passed", "Passed", 4);
		assertTrue(myTestInfo1.getFullName().equals(myTestInfo2.getFullName()));
		assertTrue(myTestInfo1.getStatus().equals(new String("Did not exist")));
	}

	@LocalData
	@Test
	public void testGetDetailedDifferentBuildComparison1() {
		String b2 = new String("2");
		String b4 = new String("4");

		List<Pair<TestInfo, TestInfo>> myTestInfos = testBuddyAction.getDetailedDifferentBuildComparison(b2, b4);
		assertEquals(2, myTestInfos.size());
		Pair<TestInfo, TestInfo> lastTuple = myTestInfos.get(myTestInfos.size() - 1);
		assertTrue(!lastTuple.first.getStatus().equals(lastTuple.second.getStatus()));
	}

	@LocalData
	@Test
	public void testGetDetailedDifferentBuildComparison2() {
		String b3 = new String("3");
		String b5 = new String("5");

		List<Pair<TestInfo, TestInfo>> myTestInfos = testBuddyAction.getDetailedDifferentBuildComparison(b3, b5);
		assertEquals(4, myTestInfos.size());
		Pair<TestInfo, TestInfo> firstTuple = myTestInfos.get(0);
		assertTrue(firstTuple.first.getStatus().equals(new String("Passed")));
		assertTrue(firstTuple.second.getStatus().equals(new String("Failed")));
		assertTrue(firstTuple.first.getFullName().equals(new String("pkg.AppTest.testApp2")));
	}
}
