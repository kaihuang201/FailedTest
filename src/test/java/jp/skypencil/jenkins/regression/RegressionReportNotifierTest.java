package jp.skypencil.jenkins.regression;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;

import hudson.Launcher;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.User;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.CaseResult.Status;
import hudson.tasks.test.AbstractTestResultAction;

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
	public void testCompileErrorOccured() throws InterruptedException, IOException {
		doReturn(null).when(build).getAction(AbstractTestResultAction.class);
		RegressionReportNotifier notifier = new RegressionReportNotifier("", false, false, true, true, false, false);

		assertTrue(notifier.perform(build, launcher, listener));
	}

	@Test
	public void testSend() throws InterruptedException, MessagingException {
		makeRegression();

		RegressionReportNotifier notifier = new RegressionReportNotifier("author@mail.com", false, false, true, true,
				false, false);
		MockedMailSender mailSender = new MockedMailSender();
		notifier.setMailSender(mailSender);

		assertTrue(notifier.perform(build, launcher, listener));
		assertNotNull(mailSender.getSentMessage());
		Address[] to = mailSender.getSentMessage().getRecipients(RecipientType.TO);
		assertEquals(1, to.length);
		assertEquals("author@mail.com", to[0].toString());
	}

	@Test
	public void testSendToCulprits() throws InterruptedException, MessagingException {
		makeRegression();

		RegressionReportNotifier notifier = new RegressionReportNotifier("author@mail.com", true, false, true, true,
				false, false);
		MockedMailSender mailSender = new MockedMailSender();
		notifier.setMailSender(mailSender);

		assertTrue(notifier.perform(build, launcher, listener));
		assertNotNull(mailSender.getSentMessage());
		Address[] to = mailSender.getSentMessage().getRecipients(RecipientType.TO);
		assertThat(to.length, is(2));
		assertEquals("author@mail.com", to[0].toString());
		assertEquals("culprit@mail.com", to[1].toString());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testAttachLogFile() throws InterruptedException, MessagingException, IOException {
		makeRegression();

		File f = new File(getClass().getResource("/log").getPath());
		AnnotatedLargeText text = new AnnotatedLargeText(f, Charset.defaultCharset(), false, build);
		doReturn(text).when(build).getLogText();
		doReturn(f.getAbsoluteFile().getParentFile()).when(build).getRootDir();

		RegressionReportNotifier notifier = new RegressionReportNotifier("author@mail.com", false, true, true, true,
				false, false);
		MockedMailSender mailSender = new MockedMailSender();
		notifier.setMailSender(mailSender);

		assertNotNull(build.getLogText());
		assertTrue(notifier.perform(build, launcher, listener));
		assertNotNull(mailSender.getSentMessage());

		Address[] to = mailSender.getSentMessage().getRecipients(RecipientType.TO);
		assertEquals(1, to.length);
		assertEquals("author@mail.com", to[0].toString());

		assertTrue(notifier.getAttachLog());
		assertTrue(mailSender.getSentMessage().getContent() instanceof Multipart);

		Multipart multipartContent = (Multipart) mailSender.getSentMessage().getContent();
		assertEquals(2, multipartContent.getCount());
		assertEquals(Part.ATTACHMENT, ((MimeBodyPart) multipartContent.getBodyPart(1)).getDisposition());
		assertNull(((MimeBodyPart) multipartContent.getBodyPart(0)).getDisposition());
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

	private static final class MockedMailSender implements RegressionReportNotifier.MailSender {
		private MimeMessage sentMessage = null;

		@Override
		public void send(MimeMessage message) throws MessagingException {
			sentMessage = message;
		}

		public MimeMessage getSentMessage() {
			return sentMessage;
		}
	}
}
