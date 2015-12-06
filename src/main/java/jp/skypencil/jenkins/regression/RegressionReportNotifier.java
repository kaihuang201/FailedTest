package jp.skypencil.jenkins.regression;

import static com.google.common.collect.Iterables.transform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.TransientProjectActionFactory;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Mailer;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;

/**
 * @version 1.0
 * @author eller86 (Kengo TODA)
 */

@SuppressWarnings("unchecked")
public final class RegressionReportNotifier extends Notifier {
	static interface MailSender {
		void send(MimeMessage message) throws MessagingException;
	}

	private static final int MAX_RESULTS_PER_MAIL = 20;
	private final String recipients;
	private final boolean sendToCulprits;
	private final boolean attachLog;
	private final boolean whenRegression;
	private final boolean whenProgression;
	private final boolean whenNewFailed;
	private final boolean whenNewPassed;

	private MailSender mailSender = new RegressionReportNotifier.MailSender() {
		@Override
		public void send(MimeMessage message) throws MessagingException {
			Transport.send(message);
		}
	};

	@DataBoundConstructor
	public RegressionReportNotifier(String recipients, boolean sendToCulprits, boolean attachLogs,
			boolean whenRegression, boolean whenProgression, boolean whenNewFailed, boolean whenNewPassed) {
		this.recipients = recipients;
		this.sendToCulprits = sendToCulprits;
		this.attachLog = attachLogs;
		this.whenRegression = whenRegression;
		this.whenProgression = whenProgression;
		this.whenNewFailed = whenNewFailed;
		this.whenNewPassed = whenNewPassed;
	}

	@VisibleForTesting
	void setMailSender(MailSender mailSender) {
		this.mailSender = mailSender;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public String getRecipients() {
		return recipients;
	}

	public boolean getSendToCulprits() {
		return sendToCulprits;
	}

	/**
	 * 
	 * @return true if user has checked Attach Logs option else it will return
	 *         false
	 */
	public boolean getAttachLog() {
		return attachLog;
	}

	public boolean getWhenRegression() {
		return whenRegression;
	}

	public boolean getWhenProgression() {
		return whenProgression;
	}

	public boolean getWhenNewFailed() {
		return whenNewFailed;
	}

	public boolean getWhenNewPassed() {
		return whenNewPassed;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException {
		PrintStream logger = listener.getLogger();

		AbstractTestResultAction<?> testResultAction = build.getAction(AbstractTestResultAction.class);
		if (testResultAction == null) {
			// maybe compile error occurred
			logger.println("TestBuddy reporter doesn't run because test doesn\'t run.");
			return true;
		}

		logger.println("TestBuddy reporter starts now......");

		List<Pair<CaseResult, CaseResult>> testTuples = new ArrayList<Pair<CaseResult, CaseResult>>();

		AbstractBuild<?, ?> prevBuild = build.getPreviousBuild();
		if (prevBuild != null) {
			testTuples = TestBuddyHelper.matchTestsBetweenBuilds(build, prevBuild);
		}

		// TODO maybe don't getTestResults for prevBuild twice?
		List<CaseResult> newlyPassedTests = listNewlyPassed(build);
		List<CaseResult> regressionedTests = listRegressions(testResultAction);

		List<Pair<CaseResult, CaseResult>> newTestTuples = Lists
				.newArrayList(Iterables.filter(testTuples, new NewTestPredicate()));
		List<CaseResult> newTests = Lists.newArrayList(Iterables.transform(newTestTuples, new TupleToFirst()));

		List<CaseResult> newTestsPassed = Lists.newArrayList(Iterables.filter(newTests, new PassedPredicate()));
		List<CaseResult> newTestsFailed = Lists.newArrayList(Iterables.filter(newTests, new FailedPredicate()));

		writeToConsole(regressionedTests, newlyPassedTests, newTestsFailed, newTestsPassed, listener);

		try {
			mailReport(regressionedTests, newlyPassedTests, newTestsFailed, newTestsPassed, recipients, listener,
					build);
		} catch (MessagingException e) {
			e.printStackTrace(listener.error("failed to send mails."));
		}

		logger.println("TestBuddy reporter ends.");
		return true;
	}

	/**
	 * This method takes a test result action, obtains the list of failed tests,
	 * and filters that list of tests to keep only those that are newly failing
	 * (CaseResult.Status == REGRESSION).
	 * 
	 * @param testResultAction
	 *            an AbstractTestResultAction from which to find the
	 *            regressioned tests
	 * @return a List of CaseResults representing the regression tests
	 */
	public List<CaseResult> listRegressions(AbstractTestResultAction<?> testResultAction) {
		List<? extends TestResult> failedTest = testResultAction.getFailedTests();
		Iterable<? extends TestResult> filtered = Iterables.filter(failedTest, new RegressionPredicate());
		List<CaseResult> regressionedTests = Lists
				.newArrayList(Iterables.transform(filtered, new TestResultToCaseResult()));
		return regressionedTests;
	}

	/**
	 * This method takes a build, compares it with the previous build, and
	 * returns a list of tests that are passing in this build but were failing
	 * in the previous build (progressions).
	 * 
	 * @param build
	 *            an AbstractBuild from which to find the progressioned tests
	 * @return a List of CaseResults representing the progression tests
	 */
	public List<CaseResult> listNewlyPassed(AbstractBuild<?, ?> build) {
		List<CaseResult> newlyPassedTests = new ArrayList<CaseResult>();
		if (build.getPreviousBuild() != null) {
			ArrayList<CaseResult> diffResults = TestBuddyHelper.getChangedTestsBetweenBuilds(build,
					build.getPreviousBuild());
			for (CaseResult res : diffResults) {
				if (res.isPassed())
					newlyPassedTests.add(res);
			}
		}
		return newlyPassedTests;
	}

	private void writeToConsole(List<CaseResult> regressions, List<CaseResult> progressions,
			List<CaseResult> newTestsFailed, List<CaseResult> newTestsPassed, BuildListener listener) {
		if (regressions.isEmpty() && progressions.isEmpty() && newTestsPassed.isEmpty() && newTestsFailed.isEmpty()) {
			return;
		}

		PrintStream oStream = listener.getLogger();

		// TODO link to test result page
		for (CaseResult result : regressions) {
			oStream.printf("[REGRESSION]%s - description: %s%n", result.getFullName(), result.getErrorDetails());
		}

		for (CaseResult result : progressions) {
			// listener.hyperlink(url, text)
			oStream.printf("[PROGRESSION]%s - description: %s%n", result.getFullName(), result.getErrorDetails());
		}

		for (CaseResult result : newTestsPassed) {
			oStream.printf("[NEW TEST PASSED]%s - description: %s%n", result.getFullName(), result.getErrorDetails());
		}

		for (CaseResult result : newTestsFailed) {
			oStream.printf("[NEW TEST FAILED]%s - description: %s%n", result.getFullName(), result.getErrorDetails());
		}
	}

	private void appendTests(List<CaseResult> tests, StringBuilder builder) {
		builder.append("\n");
		for (int i = 0, max = Math.min(tests.size(), MAX_RESULTS_PER_MAIL); i < max; ++i) {
			// to save heap to avoid OOME.
			CaseResult result = tests.get(i);
			builder.append("  ");
			builder.append(result.getFullName());
			builder.append("\n");
		}
		if (tests.size() > MAX_RESULTS_PER_MAIL) {
			builder.append("  ...");
			builder.append("\n");
		}
	}

	/**
	 * This method constructs the report and sends to the specified recipients
	 * 
	 * @param regressions
	 *            is the list of CaseResults representing the regression tests
	 * @param progressions
	 *            is the list of CaseResults representing the progression tests
	 * @param recipients
	 *            is a String containing the list of addresses to mail the
	 *            report to
	 * @param listener
	 *            is the BuildListener of this build
	 * @param build
	 *            is the AbstractBuild object
	 * @throws MessagingException
	 */
	private void mailReport(List<CaseResult> regressions, List<CaseResult> newlyPassed, List<CaseResult> newTestsFailed,
			List<CaseResult> newTestsPassed, String recipients, BuildListener listener, AbstractBuild<?, ?> build)
					throws MessagingException {

		if ((regressions.isEmpty() || !whenRegression) && (newlyPassed.isEmpty() || !whenProgression)
				&& (newTestsFailed.isEmpty() || !whenNewFailed) && (newTestsPassed.isEmpty() || !whenNewPassed)) {
			return;
		}

		// TODO link to test result page
		StringBuilder builder = new StringBuilder();
		String rootUrl = "";
		Session session = null;
		InternetAddress adminAddress = null;
		if (Jenkins.getInstance() != null) {
			rootUrl = Jenkins.getInstance().getRootUrl();
			session = Mailer.descriptor().createSession();
			adminAddress = new InternetAddress(JenkinsLocationConfiguration.get().getAdminAddress());
		}
		builder.append(Util.encode(rootUrl));
		builder.append(Util.encode(build.getUrl()));
		builder.append("\n\n");

		if (whenRegression) {
			builder.append(regressions.size() + " regressions found.");
			appendTests(regressions, builder);
		}

		if (whenProgression) {
			/* S08: Append newly passed tests */
			builder.append(newlyPassed.size() + " newly passed tests found.");
			appendTests(newlyPassed, builder);
		}

		if (whenNewPassed) {
			builder.append(newTestsPassed.size() + " tests newly added and passing.");
			appendTests(newTestsPassed, builder);
		}

		if (whenNewFailed) {
			builder.append(newTestsFailed.size() + " tests newly added and failing.");
			appendTests(newTestsFailed, builder);
		}

		List<Address> recipentList = parse(recipients, listener);
		if (sendToCulprits) {
			recipentList.addAll(loadAddrOfCulprits(build, listener));
		}

		MimeMessage message = new MimeMessage(session);
		message.setSubject(Messages.RegressionReportNotifier_MailSubject());
		message.setRecipients(RecipientType.TO, recipentList.toArray(new Address[recipentList.size()]));
		message.setContent("", "text/plain");
		message.setFrom(adminAddress);
		message.setText(builder.toString());
		message.setSentDate(new Date());

		if (attachLog) {
			try {
				attachLogFile(build, message, builder.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		mailSender.send(message);
	}

	private Set<Address> loadAddrOfCulprits(AbstractBuild<?, ?> build, BuildListener listener) {
		Set<User> authorSet = Sets.newHashSet(transform(build.getChangeSet(), new ChangeSetToAuthor()));
		Set<Address> addressSet = Sets.newHashSet(transform(authorSet, new UserToAddr(listener.getLogger())));
		return addressSet;
	}

	private List<Address> parse(String recipients, BuildListener listener) {
		List<Address> list = Lists.newArrayList();
		StringTokenizer tokens = new StringTokenizer(recipients);
		while (tokens.hasMoreTokens()) {
			String address = tokens.nextToken();
			try {
				list.add(new InternetAddress(address));
			} catch (AddressException e) {
				e.printStackTrace(listener.error(e.getMessage()));
			}
		}

		return list;
	}

	/**
	 * US08: Attach build log file to email, called from mailReport()
	 * 
	 * @param build
	 *            is an AbstractBuild object from which the log file is obtained
	 * @param message
	 *            is a MimeMessage for which to set the content to, provided by
	 *            mailReport()
	 * @param content
	 *            is a String containing the email's body text, provided by
	 *            mailReport()
	 * @param logger
	 *            allows the method to print messages to console log
	 * @throws MessagingException
	 */
	private void attachLogFile(AbstractBuild<?, ?> build, MimeMessage message, String content)
			throws MessagingException, IOException {

		Multipart multipart = new MimeMultipart();

		BodyPart bodyText = new MimeBodyPart();
		bodyText.setText(content);
		multipart.addBodyPart(bodyText);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		build.getLogText().writeLogTo(0, out);

		BodyPart emailAttachment = new MimeBodyPart();
		DataSource source = new ByteArrayDataSource(out.toByteArray(), "text/plain");
		emailAttachment.setDataHandler(new DataHandler(source));
		emailAttachment.setFileName("buildLog.txt");
		multipart.addBodyPart(emailAttachment);

		message.setContent(multipart);
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.RegressionReportNotifier_DisplayName();
		}
	}

	@Extension
	public static final class TestBuddyExtension extends TransientProjectActionFactory {

		@Override
		public Collection<? extends Action> createFor(@SuppressWarnings("rawtypes") AbstractProject target) {

			final List<TestBuddyAction> projectActions = target.getActions(TestBuddyAction.class);
			final ArrayList<Action> actions = new ArrayList<Action>();
			if (projectActions.isEmpty()) {
				final TestBuddyAction newAction = new TestBuddyAction(target);
				actions.add(newAction);
				return actions;
			} else {
				return projectActions;
			}
		}
	}
}
