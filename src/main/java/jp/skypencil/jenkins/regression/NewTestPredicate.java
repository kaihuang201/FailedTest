package jp.skypencil.jenkins.regression;

import com.google.common.base.Predicate;

import hudson.tasks.junit.CaseResult;

/**
 * Filter for filtering out Pairs that have a null second element.
 */
class NewTestPredicate implements Predicate<Pair<CaseResult, CaseResult>> {
	@Override
	public boolean apply(Pair<CaseResult, CaseResult> input) {
		return input.second == null;
	}
}
