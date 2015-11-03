package jp.skypencil.jenkins.regression;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.maven.MavenModuleSet;
import jp.skypencil.jenkins.regression.TestBuddyHelper;

import hudson.tasks.junit.CaseResult;
import hudson.model.AbstractBuild;

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
        assertEquals(3, case_results.size());

        List<String> fullTestNames = new ArrayList<String>();
        for (CaseResult caseResult : case_results) {
        	fullTestNames.add(caseResult.getFullName());
        }
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp1"));
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp2"));
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp3"));
	}

	@Test
	public void testGetAllCaseResultsForBuild2() throws Exception {
		createBuild("Source_4");
        AbstractBuild build = project.getBuildByNumber(1);
        List<CaseResult> case_results = TestBuddyHelper.getAllCaseResultsForBuild(build);
        assertEquals(4, case_results.size());
        
        List<String> fullTestNames = new ArrayList<String>();
        for (CaseResult caseResult : case_results) {
        	fullTestNames.add(caseResult.getFullName());
        }
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp1"));
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp2"));
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp3"));
        assertTrue(fullTestNames.contains("pkg.AppTest.testApp4"));
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testgetRatesforBuild() throws Exception {
		createBuild("Source_5");
        AbstractBuild build = project.getBuildByNumber(1);
        double[] rates = TestBuddyHelper.getRatesforBuild(build);
        assertEquals(3.0, rates[0], 0.001);
        assertEquals(0.75, rates[1], 0.001);
	}
	
    @Test
    public void testGetChangedTestsBetweenBuilds1() throws Exception {
		createBuild("Source_1");
		createBuild("Source_2");
		List<CaseResult> diffArray;	
	
		AbstractBuild b1 = project.getBuildByNumber(1);
		AbstractBuild b2 = project.getBuildByNumber(2);
		
		List<CaseResult> caseResultsB1 = TestBuddyHelper.getAllCaseResultsForBuild(b1);
		List<CaseResult> caseResultsB2 = TestBuddyHelper.getAllCaseResultsForBuild(b2);

		assertEquals(3, caseResultsB1.size());
		diffArray = TestBuddyHelper.getChangedTestsBetweenBuilds(b1, b2);
		assertEquals(1, diffArray.size());
    }
}
