package jp.skypencil.jenkins.regression;

import static java.lang.System.out;
import static com.google.common.collect.Iterables.transform;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.Mailer;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
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
    private final boolean failedTestExtra1Option1;
    private final boolean failedTestExtra1Option2;
    private final String failedTestExtra2;
    private final String failedTestExtra3;
    private MailSender mailSender = new RegressionReportNotifier.MailSender() {
        @Override
        public void send(MimeMessage message) throws MessagingException {
            Transport.send(message);
        }
    };

    @DataBoundConstructor
    public RegressionReportNotifier(String recipients, boolean sendToCulprits, boolean attachLogs, boolean failedTestExtra1Option1, boolean failedTestExtra1Option2, String failedTestExtra2, String failedTestExtra3) {
        this.recipients = recipients;
        this.sendToCulprits = sendToCulprits;
        this.failedTestExtra1Option1 = failedTestExtra1Option1;
        this.failedTestExtra1Option2 = failedTestExtra1Option2;
        this.failedTestExtra2 = failedTestExtra2;
        this.failedTestExtra3 = failedTestExtra3;
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

    public boolean getFailedTestExtra1Option1() {
        return failedTestExtra1Option1;
    }

    public boolean getFailedTestExtra1Option2() {
        return failedTestExtra1Option2;
    }
    
    public String getFailedTestExtra2() {
        return failedTestExtra2;
    }

    public String getFailedTestExtra3() {
        return failedTestExtra3;
    }
    public boolean getAttachLogs(){
    	return attachLogs;
    }
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException {
        PrintStream logger = listener.getLogger();

        String extraSettings1String;
        if (failedTestExtra1Option1) {
        	extraSettings1String = "Selection 1 checked, ";
        }
        else {
        	extraSettings1String = "Selection 1 unchecked, ";
        }
        if (failedTestExtra1Option2) {
        	extraSettings1String = extraSettings1String + "Selection 2 checked";
        }
        else {
        	extraSettings1String = extraSettings1String + "Selection 2 unchecked";
        }
        logger.println("FailedTest Extra Settings 1 value: " + extraSettings1String);
        System.out.println("FailedTest Extra Settings 1 value: " + extraSettings1String);
        logger.println("FailedTest Extra Settings 2 value: " + failedTestExtra2);
        System.out.println("FailedTest Extra Settings 2 value: " + failedTestExtra2);
        logger.println("FailedTest Extra Settings 3 value: " + failedTestExtra3);
        System.out.println("FailedTest Extra Settings 3 value: " + failedTestExtra3);
        
        
        if (build.getResult() == Result.SUCCESS) {
            logger.println("regression reporter doesn't run because build is success.");
            return true;
        }

        AbstractTestResultAction<?> testResultAction = build
                .getAction(AbstractTestResultAction.class);
        if (testResultAction == null) {
            // maybe compile error occurred
            logger.println("regression reporter doesn't run because test doesn\'t run.");
            return true;
        }

        logger.println("regression reporter starts now...");
        List<CaseResult> regressionedTests = listRegressions(testResultAction);

        writeToConsole(regressionedTests, listener);
        try {
            mailReport(regressionedTests, recipients, listener, build);
        } catch (MessagingException e) {
            e.printStackTrace(listener.error("failed to send mails."));
        }

        logger.println("regression reporter ends.");
	logger.println("We understand this code");
	System.out.println("We understand this code");
        return true;
    }

    private List<CaseResult> listRegressions(
            AbstractTestResultAction<?> testResultAction) {
        List<? extends TestResult> failedTest = testResultAction.getFailedTests();
        Iterable<? extends TestResult> filtered = Iterables.filter(failedTest, new RegressionPredicate());
        List<CaseResult> regressionedTests =
                Lists.newArrayList(Iterables.transform(filtered, new TestResultToCaseResult()));
        return regressionedTests;
    }

    private void writeToConsole(List<CaseResult> regressions,
            BuildListener listener) {
        if (regressions.isEmpty()) {
            return;
        }

        PrintStream oStream = listener.getLogger();
        // TODO link to test result page
        for (CaseResult result : regressions) {
            // listener.hyperlink(url, text)
            oStream.printf("[REGRESSION]%s - description: %s%n",
                    result.getFullName(), result.getErrorDetails());
        }
    }

    private void mailReport(List<CaseResult> regressions, String recipients,
            BuildListener listener, AbstractBuild<?, ?> build)
            throws MessagingException {
        if (regressions.isEmpty()) {
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
            adminAddress = new InternetAddress(
                    JenkinsLocationConfiguration.get().getAdminAddress());
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
        List<Address> recipentList = parse(recipients, listener);
        if (sendToCulprits) {
            recipentList.addAll(loadAddrOfCulprits(build, listener));
        }

        MimeMessage message = new MimeMessage(session);
                
        message.setSubject(Messages.RegressionReportNotifier_MailSubject());
        message.setRecipients(RecipientType.TO,
                recipentList.toArray(new Address[recipentList.size()]));
        
        //If user has checked the attachment box the build logs will be attached to email
	if (attachLogs){
            //compare(build);
            System.out.println("Logs are attached to email");
	    BodyPart emailAttachment = new MimeBodyPart();
	    Multipart multipart = new MimeMultipart();
                
	    //adding email body text
	    BodyPart bodyText = new MimeBodyPart();
	    bodyText.setText(builder.toString());
	    multipart.addBodyPart(bodyText);
                
	    //adding email attachment
	    int len = build.getLogFile().getPath().length();
	    String file = build.getLogFile().getPath().substring(0, (len-3));
	    String fileName = "log";
	    File buildLog = build.getLogFile();
	    if(buildLog == null){
                System.out.println("Logs is Null");
	    }
	    DataSource source = new FileDataSource(file);
	    emailAttachment.setDataHandler(new DataHandler(source));
	    emailAttachment.setFileName(fileName);
	    multipart.addBodyPart(emailAttachment);

	    //setting message properties to multipart
	    message.setContent(multipart);  
	    message.setFrom(adminAddress);
	    message.setSentDate(new Date());

	    //equavalent to Transport.send()
	    mailSender.send(message);
        }
        else{
	    message.setContent("", "text/plain");
            message.setFrom(adminAddress);
            message.setText(builder.toString());
            message.setSentDate(new Date());

            mailSender.send(message);
        }
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

    private void compare(AbstractBuild<?, ?> build){
	String line = null;
	FileReader fileReader;
	BufferedReader bufferedReader;
	try {
	    fileReader = new FileReader(build.getLogFile());
	    bufferedReader = new BufferedReader(fileReader);
	    try {
		while((line = bufferedReader.readLine()) != null) {
		    if(line.startsWith("Results :")){
			System.out.println(line);
			for(int i = 0; i < 5; i++){
			    System.out.println(bufferedReader.readLine());
			}
		    }
		    bufferedReader.close();
		}
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
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
}
