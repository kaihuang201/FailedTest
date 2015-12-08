package jp.skypencil.jenkins.regression;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;

import hudson.model.User;
import hudson.scm.ChangeLogSet.Entry;

/**
 * This class has a method that retries Author from Entry
 * 
 * @author Team FailedTest
 *
 */
final class ChangeSetToAuthor implements Function<Entry, User> {

	/**
	 * This method retries Author (User) from Entry
	 * 
	 * @param from
	 *            Entry
	 * 
	 * @return The author of the from parameter
	 */
	@Override
	public User apply(Entry from) {
		checkNotNull(from);
		return from.getAuthor();
	}

}
