package jp.skypencil.jenkins.regression;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.PrintStream;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.google.common.base.Function;

import hudson.tasks.Mailer;
import jenkins.model.Jenkins;

/**
 * Convert user id to email address. Returned value can be null.
 */
final class UserIdToAddr implements Function<String, Address> {
	private final String defaultSuffix;
	private final PrintStream logger;

	UserIdToAddr(PrintStream logger) {
		this.logger = checkNotNull(logger);
		if (Jenkins.getInstance() != null) {
			defaultSuffix = Mailer.descriptor().getDefaultSuffix();
		} else {
			defaultSuffix = "@mail.com";
		}
	}

	/**
	 * Convert user id to email address. Returned value can be null.
	 * 
	 * @param userId
	 *            user Id in string format
	 * 
	 * @return email address
	 */
	@Override
	public Address apply(String userId) {
		if (defaultSuffix != null && defaultSuffix.contains("@")) {
			try {
				return new InternetAddress(userId + defaultSuffix);
			} catch (AddressException e) {
				e.printStackTrace(logger);
			}
		}

		return null;
	}
}
