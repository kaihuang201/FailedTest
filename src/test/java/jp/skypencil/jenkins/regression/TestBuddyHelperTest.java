package jp.skypencil.jenkins.regression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

public class TestBuddyHelperTest {
	@Rule
	public JenkinsRule j = new JenkinsRule();

	@SuppressWarnings("rawtypes")
	private AbstractProject project;

	@Before
	public void init() {
		project = j.jenkins.getItemByFullName("project1", AbstractProject.class);
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testGetAllCaseResultsForBuild1() {
		AbstractBuild build = project.getBuildByNumber(1);
		List<CaseResult> case_results = TestBuddyHelper.getAllCaseResultsForBuild(build);
		assertEquals(3, case_results.size());

		List<String> fullTestNames = new ArrayList<String>();
		for (CaseResult caseResult : case_results) {
			fullTestNames.add(caseResult.getFullName());
		}
		assertTrue(fullTestNames.contains("pkg.AppTest.testApp1"));
		assertTrue(fullTestNames.contains("pkg.AppTest.testApp2"));
		assertTrue(fullTestNames.contains("pkg.AppTest.testApp3"));
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testGetAllCaseResultsForBuild2() {
		AbstractBuild build = project.getBuildByNumber(5);
		List<CaseResult> case_results = TestBuddyHelper.getAllCaseResultsForBuild(build);
		assertEquals(4, case_results.size());

		List<String> fullTestNames = new ArrayList<String>();
		for (CaseResult caseResult : case_results) {
			fullTestNames.add(caseResult.getFullName());
		}
		assertTrue(fullTestNames.contains("pkg.AppTest.testApp1"));
		assertTrue(fullTestNames.contains("pkg.AppTest.testApp2"));
		assertTrue(fullTestNames.contains("pkg.AppTest.testApp3"));
		assertTrue(fullTestNames.contains("pkg.AppTest.testApp5"));
	}

	@SuppressWarnings({ "rawtypes" })
	@LocalData
	@Test
	public void testGetRatesforBuild1() {
		AbstractBuild build = project.getBuildByNumber(1);
		double[] rates = TestBuddyHelper.getRatesforBuild(build);
		assertEquals(2, rates[0], 0.001);
		assertEquals(0.67, rates[1], 0.001);
	}

	@SuppressWarnings({ "rawtypes" })
	@LocalData
	@Test
	public void testGetRatesforBuild2() {
		AbstractBuild build = project.getBuildByNumber(3);
		double[] rates = TestBuddyHelper.getRatesforBuild(build);
		assertEquals(4, rates[0], 0.001);
		assertEquals(1, rates[1], 0.001);
	}

	@SuppressWarnings({ "rawtypes" })
	@LocalData
	@Test
	public void testGetRatesforBuild3() {
		AbstractBuild build = project.getBuildByNumber(4);
		double[] rates = TestBuddyHelper.getRatesforBuild(build);
		assertEquals(4, rates[0], 0.001);
		assertEquals(0.8, rates[1], 0.001);
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testGetChangedTestsBetweenBuilds1() {
		List<CaseResult> diffArray;

		AbstractBuild b1 = project.getBuildByNumber(1);
		AbstractBuild b2 = project.getBuildByNumber(2);

		diffArray = TestBuddyHelper.getChangedTestsBetweenBuilds(b1, b2);
		assertEquals(0, diffArray.size());
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testGetChangedTestsBetweenBuilds2() {
		List<CaseResult> diffArray;

		AbstractBuild b1 = project.getBuildByNumber(5);
		AbstractBuild b2 = project.getBuildByNumber(4);

		diffArray = TestBuddyHelper.getChangedTestsBetweenBuilds(b1, b2);
		assertEquals(3, diffArray.size());
	}

	@LocalData
	@Test
	public void testGetAuthors1() {
		List<String> authors = TestBuddyHelper.getAuthors(project.getBuildByNumber(1));
		assertEquals(0, authors.size());
	}

	@LocalData
	@Test
	public void testGetAuthors2() {
		List<String> authors = TestBuddyHelper.getAuthors(project.getBuildByNumber(5));
		assertEquals(1, authors.size());
		assertEquals("developer1", authors.get(0));
	}

	@LocalData
	@Test
	public void testGetAuthors3() {
		List<String> authors = TestBuddyHelper.getAuthors(project.getBuildByNumber(3));
		assertEquals(2, authors.size());
		assertEquals("developer1", authors.get(0));
		assertEquals("developer2", authors.get(1));
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testMatchTestsBetweenBuilds1() {
		AbstractBuild b1 = project.getBuildByNumber(1);
		AbstractBuild b2 = project.getBuildByNumber(2);

		ArrayList<Pair<CaseResult, CaseResult>> myTuples = TestBuddyHelper.matchTestsBetweenBuilds(b1, b2);
		assertEquals(3, myTuples.size());
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testMatchTestsBetweenBuilds2() {
		AbstractBuild b2 = project.getBuildByNumber(2);
		AbstractBuild b3 = project.getBuildByNumber(3);

		ArrayList<Pair<CaseResult, CaseResult>> myTuples = TestBuddyHelper.matchTestsBetweenBuilds(b2, b3);
		assertEquals(4, myTuples.size());
		Pair<CaseResult, CaseResult> lastTuple = myTuples.get(3);
		assertNull(lastTuple.first);
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testMatchTestsBetweenBuilds3() {
		AbstractBuild b3 = project.getBuildByNumber(3);
		AbstractBuild b4 = project.getBuildByNumber(4);

		ArrayList<Pair<CaseResult, CaseResult>> myTuples = TestBuddyHelper.matchTestsBetweenBuilds(b3, b4);
		assertEquals(5, myTuples.size());
		Pair<CaseResult, CaseResult> lastTuple = myTuples.get(4);
		assertNull(lastTuple.first);
		assertTrue(lastTuple.second.isFailed());
	}

	@SuppressWarnings("rawtypes")
	@LocalData
	@Test
	public void testMatchTestsBetweenBuilds4() {
		AbstractBuild b4 = project.getBuildByNumber(4);
		AbstractBuild b5 = project.getBuildByNumber(5);

		ArrayList<Pair<CaseResult, CaseResult>> myTuples = TestBuddyHelper.matchTestsBetweenBuilds(b4, b5);
		assertEquals(5, myTuples.size());
		Pair<CaseResult, CaseResult> fourthTuple = myTuples.get(3);
		assertTrue(fourthTuple.first.isPassed());
		assertNull(fourthTuple.second);

		Pair<CaseResult, CaseResult> lastTuple = myTuples.get(4);
		assertFalse(lastTuple.first.isPassed());
		assertTrue(lastTuple.second.isPassed());
	}
}
