package jp.skypencil.jenkins.regression;

import com.google.common.base.Function;

import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.TestResult;

class TestResultToCaseResult implements Function<TestResult, CaseResult> {
	@Override
	public CaseResult apply(TestResult input) {
		return (CaseResult) input;
	}
}
