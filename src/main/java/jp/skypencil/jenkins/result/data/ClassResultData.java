package jp.skypencil.jenkins.result.data;

import hudson.tasks.junit.ClassResult;


public class ClassResultData extends ResultData {

	public ClassResultData(ClassResult classResult) {

		setName(classResult.getName());
		setPassed(classResult.getFailCount()==0);
		setSkipped(classResult.getSkipCount() == classResult.getTotalCount());
		setTotalTests(classResult.getTotalCount());
		setTotalFailed(classResult.getFailCount());
		setTotalPassed(classResult.getPassCount());
		setTotalSkipped(classResult.getSkipCount());
		setTotalTimeTaken(classResult.getDuration());
		evaluateStatus();
	}

}