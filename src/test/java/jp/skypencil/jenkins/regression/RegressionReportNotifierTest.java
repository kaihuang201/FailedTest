package jp.skypencil.jenkins.regression;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.CaseResult.Status;
import hudson.tasks.test.AbstractTestResultAction;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;

import hudson.FilePath;
import java.io.File;
import java.net.URL;
import java.io.IOException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CaseResult.class)
public class RegressionReportNotifierTest {
    private BuildListener listener;
    private Launcher launcher;
    private AbstractBuild<?, ?> build;

    @Before
    public void setUp() throws Exception {
        listener = mock(BuildListener.class);
        launcher = mock(Launcher.class);
        build = mock(AbstractBuild.class);
        PrintStream logger = mock(PrintStream.class);
        doReturn("").when(build).getUrl();
        doReturn(logger).when(listener).getLogger();
    }

    @Test
    public void testCompileErrorOccured() throws InterruptedException,
            IOException {
        doReturn(null).when(build).getAction(AbstractTestResultAction.class);
        RegressionReportNotifier notifier = new RegressionReportNotifier("", false, false);

        assertThat(notifier.perform(build, launcher, listener), is(true));
    }

    @Test
    public void testSend() throws InterruptedException, MessagingException {
        makeRegression();

        RegressionReportNotifier notifier = new RegressionReportNotifier("author@mail.com", false, false);
        MockedMailSender mailSender = new MockedMailSender();
        notifier.setMailSender(mailSender);

        assertThat(notifier.perform(build, launcher, listener), is(true));
        assertThat(mailSender.getSentMessage(), is(notNullValue()));
        Address[] to = mailSender.getSentMessage().getRecipients(RecipientType.TO);
        assertThat(to.length, is(1));
        assertThat(to[0].toString(), is(equalTo("author@mail.com")));
    }

    @Test
    public void testSendToCulprits() throws InterruptedException, MessagingException {
        makeRegression();

        RegressionReportNotifier notifier = new RegressionReportNotifier("author@mail.com", true, false);
        MockedMailSender mailSender = new MockedMailSender();
        notifier.setMailSender(mailSender);

        assertThat(notifier.perform(build, launcher, listener), is(true));
        assertThat(mailSender.getSentMessage(), is(notNullValue()));
        Address[] to = mailSender.getSentMessage().getRecipients(RecipientType.TO);
        assertThat(to.length, is(2));
        assertThat(to[0].toString(), is(equalTo("author@mail.com")));
        assertThat(to[1].toString(), is(equalTo("culprit@mail.com")));
    }

    @Test
    public void testAttachLogFile() throws InterruptedException, MessagingException, IOException {
        
        makeRegression();
        doReturn(this.getClass().getResource("")).when(build).getWorkspace();

        URL url = this.getClass().getResource("log"); //"file:/home/yjong2/cs427/project/FailedTest/target/test-classes/jp/skypencil/jenkins/regression/"
        final File attachment = new File(url.getFile());
        assertThat(attachment.toString(), is(equalTo("/home/yjong2/cs427/project/FailedTest/target/test-classes/jp/skypencil/jenkins/regression/log")));

        assertThat(build.getWorkspace(), is(notNullValue()));
        //im thinking that setting the workspace directory to where our fake log file is, will let getLogFile() find our file
       

        RegressionReportNotifier notifier = new RegressionReportNotifier("author@mail.com", false, true);
        MockedMailSender mailSender = new MockedMailSender();
        notifier.setMailSender(mailSender);

        assertThat(build.getLogFile(), is(notNullValue())); //we need a log file:(
        assertThat(notifier.perform(build, launcher, listener), is(true)); 
        assertThat(mailSender.getSentMessage(), is(notNullValue()));
        Address[] to = mailSender.getSentMessage().getRecipients(RecipientType.TO);
        assertThat(to.length, is(1));
        assertThat(to[0].toString(), is(equalTo("author@mail.com")));

        assertThat(notifier.getAttachLogs(), is(true));
        //assertThat(mailSender.getSentMessage().getContentType(), is())
        

    }


    private void makeRegression() {
        AbstractTestResultAction<?> result = mock(AbstractTestResultAction.class);
        doReturn(result).when(build).getAction(AbstractTestResultAction.class);
        doReturn(Result.FAILURE).when(build).getResult();
        User culprit = mock(User.class);
        doReturn("culprit").when(culprit).getId();
        doReturn(new ChangeLogSetMock(build).withChangeBy(culprit)).when(build).getChangeSet();

        CaseResult failedTest = mock(CaseResult.class);
        doReturn(Status.REGRESSION).when(failedTest).getStatus();
        List<CaseResult> failedTests = Lists.newArrayList(failedTest);
        doReturn(failedTests).when(result).getFailedTests();
    }

    private void makeNewlyPassing() {
        AbstractTestResultAction<?> result = mock(AbstractTestResultAction.class);
        doReturn(result).when(build).getAction(AbstractTestResultAction.class);
        doReturn(Result.SUCCESS).when(build).getResult();
        User culprit = mock(User.class);
        doReturn("culprit").when(culprit).getId();
        doReturn(new ChangeLogSetMock(build).withChangeBy(culprit)).when(build).getChangeSet();

        CaseResult passedTest = mock(CaseResult.class);
        doReturn(Status.PASSED).when(passedTest).getStatus();
        List<CaseResult> passedTests = Lists.newArrayList(passedTest);
        doReturn(passedTests).when(result).getFailedTests();
    }

    private static final class MockedMailSender implements
            RegressionReportNotifier.MailSender {
        private MimeMessage sentMessage;

        @Override
        public void send(MimeMessage message) throws MessagingException {
            sentMessage = message;
        }

        public MimeMessage getSentMessage() {
            return sentMessage;
        }
    }
}
