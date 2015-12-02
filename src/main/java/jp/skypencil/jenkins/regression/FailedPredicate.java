package jp.skypencil.jenkins.regression;

import com.google.common.base.Predicate;

import hudson.tasks.junit.CaseResult;

class FailedPredicate implements Predicate<CaseResult> {
	@Override
	public boolean apply(CaseResult input) {
		return !input.isFailed();
	}
}
