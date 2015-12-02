package jp.skypencil.jenkins.regression;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;

import hudson.model.User;
import hudson.scm.ChangeLogSet.Entry;

final class ChangeSetToAuthor implements Function<Entry, User> {

	@Override
	public User apply(Entry from) {
		checkNotNull(from);
		return from.getAuthor();
	}

}
