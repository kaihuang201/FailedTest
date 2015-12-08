package jp.skypencil.jenkins.regression;

import com.google.common.base.Function;

import hudson.tasks.junit.CaseResult;

/**
 * This class implements has a method that takes in a tuple and returns the
 * first element.
 * 
 * @author Team FailedTest
 *
 */
class TupleToFirst implements Function<Pair<CaseResult, CaseResult>, CaseResult> {
	@Override
	public CaseResult apply(Pair<CaseResult, CaseResult> input) {
		return input.first;
	}
}
