package jp.skypencil.jenkins.regression;

import com.google.common.base.Predicate;

import hudson.tasks.junit.CaseResult;

class NewTestPredicate implements Predicate<Tuple<CaseResult, CaseResult>> {
	@Override
	public boolean apply(Tuple<CaseResult, CaseResult> input) {
		return input.second == null;
	}
}
