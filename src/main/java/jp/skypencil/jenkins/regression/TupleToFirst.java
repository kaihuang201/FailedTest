package jp.skypencil.jenkins.regression;

import com.google.common.base.Function;

import hudson.tasks.junit.CaseResult;

class TupleToFirst implements Function<Pair<CaseResult, CaseResult>, CaseResult> {
	@Override
	public CaseResult apply(Pair<CaseResult, CaseResult> input) {
		return input.first;
	}
}
