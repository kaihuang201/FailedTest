package jp.skypencil.jenkins.regression;

import static java.lang.System.out;

import static com.google.common.collect.Iterables.transform;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Project;
import hudson.model.Result;

import hudson.model.Run;

import hudson.model.TransientProjectActionFactory;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Mailer;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.junit.TestResultAction;

//import hudson.tasks.junit.TestResult;
import hudson.tasks.test.TestResult;

import hudson.util.RunList;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Address;
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

import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.ClassResult;

import jp.skypencil.jenkins.regression.TestBuddyHelper;

/**
 * @version 1.0
 * @author eller86 (Kengo TODA)
 */
public final class RegressionReportNotifier extends Notifier {
    static interface MailSender {
        void send(MimeMessage message) throws MessagingException;
    }

    private static final int MAX_RESULTS_PER_MAIL = 20;
    private final String recipients;
    private final boolean sendToCulprits;
    private final boolean attachLogs;
    private MailSender mailSender = new RegressionReportNotifier.MailSender() {
        @Override
        public void send(MimeMessage message) throws MessagingException {
            Transport.send(message);
        }
    };

    @DataBoundConstructor
    public RegressionReportNotifier(String recipients, boolean sendToCulprits, boolean attachLogs) {
        this.recipients = recipients;
        this.sendToCulprits = sendToCulprits;
        this.attachLogs = attachLogs;
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

    public boolean getAttachLogs(){
    	return attachLogs;
    }
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        PrintStream logger = listener.getLogger();
        
        AbstractTestResultAction<?> testResultAction = build.getAction(AbstractTestResultAction.class);
        if (testResultAction == null) {
            // maybe compile error occurred
            logger.println("TestBuddy reporter doesn't run because test doesn\'t run.");
            return true;
        }

        /* S01: Obtain list of newly passed tests and write to console */
        logger.println("TestBuddy reporter starts now......");
        //List<CaseResult> tests = listAllTests(build, build.getId(), logger);
        List<CaseResult> tests = TestBuddyHelper.getAllCaseResultsForBuild(build);
        //for(CaseResult cr : tests) logger.println(cr.getFullName() + "is passing: " + cr.isPassed());
        List<CaseResult> newlyPassedTests = listNewlyPassed(testResultAction, tests);
		writeToConsolePassed(newlyPassedTests, listener);
        List<CaseResult> regressionedTests = listRegressions(testResultAction);
        writeToConsole(regressionedTests, listener);


        try {
            mailReport(regressionedTests, newlyPassedTests, recipients, listener, build);
        } catch (MessagingException e) {
            e.printStackTrace(listener.error("failed to send mails."));
        }

        logger.println("TestBuddy reporter ends.");
        return true;
    }

    public List<CaseResult> listRegressions(AbstractTestResultAction<?> testResultAction) {
        List<? extends TestResult> failedTest = testResultAction.getFailedTests();
        Iterable<? extends TestResult> filtered = Iterables.filter(failedTest, new RegressionPredicate());
        List<CaseResult> regressionedTests =
                Lists.newArrayList(Iterables.transform(filtered, new TestResultToCaseResult()));
        return regressionedTests;
	}

    /* S01: Return a list of all newly passed tests (inverse of regression) */
	public List<CaseResult> listNewlyPassed(AbstractTestResultAction<?> testResultAction, List<CaseResult> allTests) {
		List<CaseResult> newlyPassedTests = new ArrayList<CaseResult>();
		if(testResultAction.getPreviousResult() != null) {
		    List<TestResult> prevFailedTests = testResultAction.getPreviousResult().getFailedTests();
            List<TestResult> currPassedTests = new ArrayList<TestResult>();
            for(TestResult c : allTests) {
                if(c.isPassed()) currPassedTests.add(c);
            }
		    for(TestResult prev : prevFailedTests) {
                String prevTestName = prev.getFullName();
                for(TestResult cur : currPassedTests) {
                    if(cur.getFullName().equals(prevTestName)) {
                        if(prev instanceof CaseResult) {
                            newlyPassedTests.add((CaseResult)prev);
                        }
                    }
                }
		    }
		}
        return newlyPassedTests;
    }

    private void writeToConsole(List<CaseResult> regressions, BuildListener listener) {
        if (regressions.isEmpty()) {
            return;
        }

        PrintStream oStream = listener.getLogger();
        // TODO link to test result page
        for (CaseResult result : regressions) {
            // listener.hyperlink(url, text)
            oStream.printf("[REGRESSION]%s - description: %s%n", result.getFullName(), result.getErrorDetails());
        }
    }

    /* S01: Write newly passed tests to console */
    private void writeToConsolePassed(List<CaseResult> regressions, BuildListener listener) {
        if (regressions.isEmpty()) {
            return;
        }

        PrintStream oStream = listener.getLogger();
        // TODO link to test result page
        for (CaseResult result : regressions) {
            // listener.hyperlink(url, text)
            oStream.printf("[PROGRESSION]%s - description: %s%n", result.getFullName(), result.getErrorDetails());
        }
	}

    private void mailReport(List<CaseResult> regressions, List<CaseResult> newlyPassed, String recipients,
            BuildListener listener, AbstractBuild<?, ?> build)
            throws MessagingException {
        if (regressions.isEmpty() && newlyPassed.isEmpty()) {
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
        builder.append(regressions.size() + " regressions found.");
        builder.append("\n");
        for (int i = 0, max = Math
                .min(regressions.size(), MAX_RESULTS_PER_MAIL); i < max; ++i) { // to
                                                                                // save
                                                                                // heap
                                                                                // to
                                                                                // avoid
                                                                                // OOME.
            CaseResult result = regressions.get(i);
            builder.append("  ");
            builder.append(result.getFullName());
            builder.append("\n");
        }
        if (regressions.size() > MAX_RESULTS_PER_MAIL) {
            builder.append("  ...");
            builder.append("\n");
        }

        /* S08: Append newly passed tests */
        builder.append(newlyPassed.size() + " newly passed tests found.");
        builder.append("\n");
        for (int i = 0, max = Math.min(newlyPassed.size(), MAX_RESULTS_PER_MAIL); i < max; ++i) { 
            CaseResult result = newlyPassed.get(i);
            builder.append("  ");
            builder.append(result.getFullName());
            builder.append("\n");
        }

    	if (newlyPassed.size() > MAX_RESULTS_PER_MAIL) {
    	    builder.append("  ...");
    	    builder.append("\n");
    	}


        List<Address> recipentList = parse(recipients, listener);
        if (sendToCulprits) {
            recipentList.addAll(loadAddrOfCulprits(build, listener));
        }

        MimeMessage message = new MimeMessage(session);
                
        message.setSubject(Messages.RegressionReportNotifier_MailSubject());
        message.setRecipients(RecipientType.TO, recipentList.toArray(new Address[recipentList.size()]));
        
        
        /* S08: If user has checked the attachment box the build logs will be attached to email */
    	if (attachLogs){
    	    attachLogFile(build, message, builder.toString(), listener.getLogger());
        }
    	else{
    	    message.setContent("", "text/plain");
    	    message.setText(builder.toString());
    	}
    	
    	message.setFrom(adminAddress);
        message.setSentDate(new Date());

        mailSender.send(message);
    }

    private Set<Address> loadAddrOfCulprits(AbstractBuild<?, ?> build,
            BuildListener listener) {
        Set<User> authorSet = Sets.newHashSet(transform(build.getChangeSet(),
                new ChangeSetToAuthor()));
        Set<Address> addressSet = Sets.newHashSet(transform(authorSet,
                new UserToAddr(listener.getLogger())));
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

    /* S08: Attach build log file to email, called from mailReport() */
    private void attachLogFile(AbstractBuild<?, ?> build, MimeMessage message, String content, PrintStream logger) 
            throws MessagingException {
    	BodyPart emailAttachment = new MimeBodyPart();
    	Multipart multipart = new MimeMultipart();
                    
    	//adding email body text
    	BodyPart bodyText = new MimeBodyPart();
    	bodyText.setText(content);
    	multipart.addBodyPart(bodyText);
                    
    	//adding email attachment
    	String file = build.getLogFile().getPath();
    	String fileName = "log";
    	DataSource source = new FileDataSource(file);
    	emailAttachment.setDataHandler(new DataHandler(source));
    	emailAttachment.setFileName(fileName);
    	multipart.addBodyPart(emailAttachment);
    	logger.println("Build log file " + file + " is attached to the email");
    	
    	message.setContent(multipart);  
    }

    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.RegressionReportNotifier_DisplayName();
        }
    }
    
    @Extension
    public static final class TestBuddyExtension extends TransientProjectActionFactory{

    	@Override
    	public Collection<? extends Action> createFor(@SuppressWarnings("rawtypes") AbstractProject target) {
    		
    		final List<TestBuddyAction> projectActions = target
                    .getActions(TestBuddyAction.class);
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
