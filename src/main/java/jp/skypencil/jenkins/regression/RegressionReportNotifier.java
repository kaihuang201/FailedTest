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

import jp.skypencil.jenkins.regression.TestResultsAnalyzerAction;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.ClassResult;

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
        logger.println("TestBuddy reporter starts now...");
        List<CaseResult> tests = listAllTests(build, build.getId(), logger);
        //for(CaseResult cr : tests) logger.println(cr.getFullName() + "is passing: " + cr.isPassed());
		List<CaseResult> newlyPassedTests = listNewlyPassed(testResultAction, tests, logger);
		writeToConsolePassed(newlyPassedTests, listener);
        List<CaseResult> regressionedTests = listRegressions(testResultAction);
        writeToConsole(regressionedTests, listener);

        

        try {
            mailReport(regressionedTests, newlyPassedTests, recipients, listener, build);
        } catch (MessagingException e) {
            e.printStackTrace(listener.error("failed to send mails."));
        }

        logger.println("TestBuddy reporter ends.");

        //logger.println("The number of test results we got back are " + getSimpleFunc(build, logger));
        return true;
    }

//	@SuppressWarnings("unchecked")
//	public ArrayList<hudson.tasks.junit.TestResult> getSimpleFunc(AbstractBuild<?, ?> build){
//		ArrayList<hudson.tasks.junit.TestResult> myArrayList = new ArrayList<hudson.tasks.junit.TestResult>();
//		AbstractProject project = build.getProject();
//		if (project == null) {
//			return myArrayList;
//		}
//		else {
//			RunList<Run> runs = project.getBuilds();
//			Iterator<Run> runIterator = runs.iterator();
//			while (runIterator.hasNext()) {
//				Run run = runIterator.next();
//				List<hudson.tasks.junit.TestResultAction> testActions = run.getActions(hudson.tasks.junit.TestResultAction.class);
//				for (TestResultAction testAction : testActions) {
//					hudson.tasks.junit.TestResult testResult = testAction.getResult();
//					myArrayList.add(testResult);
//				}
//			}
//			return myArrayList;
//		}
//	}
	
	// @SuppressWarnings("unchecked")
	// public int getSimpleFunc(AbstractBuild<?, ?> build, PrintStream logger){
	// 	ArrayList<TestResult> myArrayList = new ArrayList<TestResult>();
	// 	AbstractProject project = build.getProject();
	// 	if (project == null) {
	// 		return 9;
	// 	}
	// 	else {
	// 		RunList<Run> runs = project.getBuilds();
	// 		String lastBuild = project.getBuilds().getFirstBuild().getDisplayName();
	// 		Iterator<Run> runIterator = runs.iterator();
	// 		//int size = runs.size();
	// 		ArrayList<String> buildNames = new ArrayList<String>();
	// 		while (runIterator.hasNext()) {
	// 			Run run = runIterator.next();
	// 			buildNames.add(run.getDisplayName());
	// 			List<hudson.tasks.test.AggregatedTestResultAction> testActions = run.getActions(hudson.tasks.test.AggregatedTestResultAction.class);
	// 			if(testActions.isEmpty()){
	// 				break;
	// 			}
	// 			for (hudson.tasks.test.AggregatedTestResultAction testAction : testActions) {
	// 				System.out.println("type of testAction: " + testAction.getResult().getClass());
 //                    List<AggregatedTestResultAction.ChildReport> list = testAction.getResult();
 //                    //logger.println(list.size());
 //                    logger.println(list.get(0).child);

 //                    hudson.tasks.junit.TestResult t = (hudson.tasks.junit.TestResult)list.get(0).result;
 //                    logger.print(t + ": ");
 //                    logger.println(t.getFullName() + " is passing " + t.isPassed());
 //                    Collection<PackageResult> pkgCol = t.getChildren();
 //                    //logger.println(pkgCol.size()); //1
 //                    //logger.println(pkgCol.iterator().next().getClass()); //expect PackageResult
 //                    Collection<ClassResult> cCol = (pkgCol.iterator().next()).getChildren();
 //                    //logger.println(cCol.size()); //3
 //                    //logger.println(cCol.iterator().next().getClass()); //expect ClassResult
 //                    Iterator<ClassResult> classIter = cCol.iterator();
 //                    while(classIter.hasNext()) {
 //                        ClassResult cl = classIter.next();
 //                        Collection<CaseResult> caseCol = cl.getChildren();
 //                        //logger.println(caseCol.size()); //3,5,2
 //                        //logger.println(caseCol.iterator().next().getClass()); //expect CaseResult
 //                        Iterator<CaseResult> caseIter = caseCol.iterator();
 //                        while(caseIter.hasNext()) {
 //                            CaseResult c = caseIter.next();
 //                            logger.println(c.getFullName() + " is passing " + c.isPassed());
 //                        }
 //                    }
                    

	// 				//Class<? extends Object> x = testAction.getResult().getClass();
	// 				//TestResult testResult = testAction.getResult();
	// 				myArrayList.add(t);
	// 			}
	// 		}
	// 		return myArrayList.size();
	// 	}
	// }




    @SuppressWarnings("unchecked")
    public List<CaseResult> listAllTests(AbstractBuild<?, ?> build, String buildId, PrintStream logger){
        List<CaseResult> testsList = new ArrayList<CaseResult>();

        AbstractProject project = build.getProject();
        if (project == null) {
            return testsList;
        }
        RunList<Run> runs = project.getBuilds();
        String lastBuild = project.getBuilds().getFirstBuild().getDisplayName();
        Iterator<Run> runIterator = runs.iterator();
        while (runIterator.hasNext()) {
            Run run = runIterator.next();
            if(run.getId().equals(buildId)) {
                List<hudson.tasks.test.AggregatedTestResultAction> testActions = run.getActions(hudson.tasks.test.AggregatedTestResultAction.class);
                for (hudson.tasks.test.AggregatedTestResultAction testAction : testActions) {
                    List<AggregatedTestResultAction.ChildReport> childReport = testAction.getResult();
                    logger.println(childReport.get(0).child);

                    hudson.tasks.junit.TestResult testResult = (hudson.tasks.junit.TestResult)childReport.get(0).result;
                    Collection<PackageResult> pkgCol = testResult.getChildren();
                    Collection<ClassResult> cCol = (pkgCol.iterator().next()).getChildren();
                    Iterator<ClassResult> classIter = cCol.iterator();
                    while(classIter.hasNext()) {
                        ClassResult cl = classIter.next();
                        Collection<CaseResult> caseCol = cl.getChildren();
                        Iterator<CaseResult> caseIter = caseCol.iterator();
                        while(caseIter.hasNext()) {
                            CaseResult c = caseIter.next();
                            testsList.add(c);
                        }
                    }
                }
            }
            
        }
        return testsList;
    }








//	
//	@SuppressWarnings("unchecked")
//	public int getSimpleFunc(){
//		ArrayList<hudson.tasks.junit.TestResult> myArrayList = new ArrayList<hudson.tasks.junit.TestResult>();
//		AbstractProject project = TestResultsAnalyzerAction.getProject();
//		if (project == null) {
//			return 9;
//		}
//		else {
//			RunList<Run> runs = project.getBuilds();
//			int size = runs.size();
//			Iterator<Run> runIterator = runs.iterator();
//			Run run = runIterator.next();
//			while (runIterator.hasNext()) {
//				List<AggregatedTestResultAction> testActions = run.getActions(hudson.tasks.test.AggregatedTestResultAction.class);
//				if(testActions.isEmpty()){
//					return 88680;
//				}
//				for (hudson.tasks.test.AbstractTestResultAction testAction : testActions) {
//					hudson.tasks.junit.TestResult testResult = (hudson.tasks.junit.TestResult) testAction.getResult();
//					myArrayList.add(testResult);
//				}
//			}
//			return myArrayList.size();
//		}
//	}
    
    private List<CaseResult> listRegressions(
            AbstractTestResultAction<?> testResultAction) {
        List<? extends TestResult> failedTest = testResultAction.getFailedTests();
        Iterable<? extends TestResult> filtered = Iterables.filter(failedTest, new RegressionPredicate());
        List<CaseResult> regressionedTests =
                Lists.newArrayList(Iterables.transform(filtered, new TestResultToCaseResult()));
        return regressionedTests;
	}

    /* S01: Return a list of all newly passed tests (inverse of regression) */
	private List<CaseResult> listNewlyPassed(AbstractTestResultAction<?> testResultAction, List<CaseResult> allTests, PrintStream logger) {
		List<CaseResult> newlyPassedTests = new ArrayList<CaseResult>();
		if(testResultAction.getPreviousResult() != null) {
		    List<? extends TestResult> prevFailedTests = testResultAction.getPreviousResult().getFailedTests();
		    //logger.println(prevFailedTests.size()); logger.println(prevFailedTests.get(0).getFullName());
            //List<? extends TestResult> currFailedTests = testResultAction.getFailedTests();
		    //logger.println(currFailedTests.size()); logger.println(currFailedTests.get(0).getFullName());
            List<TestResult> currPassedTests = new ArrayList<TestResult>();
            for(TestResult c : allTests) {
                if(c.isPassed()) currPassedTests.add(c);
            }
            logger.println(prevFailedTests.size());
            logger.println(currPassedTests.size());
            

		    for(TestResult prev : prevFailedTests) {
                String prevTestName = prev.getFullName();
                for(TestResult cur : currPassedTests) {
                    if(cur.getFullName().equals(prevTestName)) {
                        if(prev instanceof CaseResult) {
                            newlyPassedTests.add((CaseResult)prev);
                        }
                    }
                }
               
                //CaseResult prevCase = (CaseResult)prev;
                // if(currPassedTests.contains(prev)) {
                //      if(prev instanceof CaseResult) {
                //         newlyPassedTests.add((CaseResult)prev);
                //     }
                // }
                
                // String prevTestName = prev.getFullName();
                // boolean stillFailing = false;
                // for(TestResult cur : currFailedTests) {
                //     if(cur.getFullName().equals(prevTestName)) {
                //         stillFailing = true;
                //         logger.println(prevTestName + " is still failing.");
                //     }
                // }
                // if(!stillFailing) {
                //     if(prev instanceof CaseResult) {
                //         CaseResult c = (CaseResult)prev;
                //         //logger.println(c.getPassedTests().size());
                //         //logger.println(c.isPassed());
                //         newlyPassedTests.add(c);
                //     }
                // }
		    }
            logger.println(newlyPassedTests.size());
            
		}
		else {
		    logger.println("testResultAction.getPreviousResult() returns null");
		}
        return newlyPassedTests;

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

    /* S01: Write newly passed tests to console */
    private void writeToConsolePassed(List<CaseResult> regressions,
            BuildListener listener) {
        if (regressions.isEmpty()) {
            return;
        }

        PrintStream oStream = listener.getLogger();
        // TODO link to test result page
        for (CaseResult result : regressions) {
            // listener.hyperlink(url, text)
            oStream.printf("[NEWLYPASSED]%s - description: %s%n",
                    result.getFullName(), result.getErrorDetails());
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
