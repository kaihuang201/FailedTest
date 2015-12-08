package jp.skypencil.jenkins.regression;

import com.google.common.base.Predicate;

import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.TestResult;

/**
 * Filter to filter out regressed tests
 */
class RegressionPredicate implements Predicate<TestResult> {
	@Override
	public boolean apply(TestResult input) {
		if (input instanceof CaseResult) {
			CaseResult caseResult = (CaseResult) input;
			return caseResult.getStatus().isRegression();
		}
		return false;
	}
}
