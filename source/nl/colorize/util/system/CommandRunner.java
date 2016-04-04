//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;

import nl.colorize.util.LogHelper;
import nl.colorize.util.Platform;

/**
 * Runs an external process and captures its output. This can optionally be done
 * in a separate thread with a timeout. Commands can be executed either locally
 * or on a remote host through SSH. Some platforms make a distinction between 
 * external programs and internal shell commands. In these cases the shell 
 * commands cannot be accessed. Shell mode can be enabled with 
 * {@link #setShellMode(boolean)}, which will make it possible to use the
 * internal shell commands.
 * <p>
 * Note that not all features of this class are supported on all platforms.
 * Attempting to use a feature tha is not supported on the current platform will
 * result in an {@code UnsupportedOperationException}. Feature support can be
 * queried by calling the different {@code isXXXSupported} methods.
 */
public class CommandRunner {
	
	private List<String> command;
	private boolean shellMode;
	private String remoteHost;
	private String remoteUser;
	private File workingDir;
	private boolean loggingEnabled;
	
	private AtomicBoolean done;
	private int exitCode;
	private StringWriter output;
	
	private static final Joiner COMMAND_JOINER = Joiner.on(' ').skipNulls();
	private static final Logger LOGGER = LogHelper.getLogger(CommandRunner.class);
	
	/**
	 * Creates a {@code CommandRunner} that will execute the specified command.
	 * The process will not start until {@link #execute()} is called.
	 * @throws IllegalArgumentException if the command is empty.
	 */
	public CommandRunner(List<String> cmd) {
		if (cmd.isEmpty()) {
			throw new IllegalArgumentException("Empty command");
		}
		
		command = ImmutableList.copyOf(cmd);
		shellMode = false;
		remoteHost = null;
		remoteUser = null;
		workingDir = null;
		loggingEnabled = false;
		
		done = new AtomicBoolean(false);
		exitCode = -1;
		output = new StringWriter();
	}
	
	/**
	 * Creates a {@code CommandRunner} that will execute the specified command.
	 * The process will not start until {@link #execute()} is called.
	 * @throws IllegalArgumentException if the command is empty.
	 */
	public CommandRunner(String... cmd) {
		this(ImmutableList.copyOf(cmd));
	}
	
	/**
	 * Runs the external process by executing the command. This method will
	 * block until the external process is done. The output of the process
	 * can be obtained afterwards by calling {@link #getExitCode()} and
	 * {@link #getOutput()}.
	 * @throws IOException if an error occurs while reading the output from the
	 *         external process.
	 * @throws SecurityException if the JVM's security settings do not allow
	 *         the external process to be started.
	 * @throws UnsupportedOperationException if the platform does not support
	 *         starting an external process.
	 */
	public void execute() throws IOException {
		if (!isExecuteSupported()) {
			throw new UnsupportedOperationException("Running an external process is " +
					"not supported on the current platform");
		}
		
		setDone(false);
		exitCode = -1;
		output = new StringWriter();
		
		try {
			runProcess();
		} catch (InterruptedException e) {
			throw new IOException("External process execution interrupted", e);
		}
		
		setDone(true);
	}
	
	private void runProcess() throws IOException, InterruptedException {
		List<String> wrappedCommand = getWrappedCommand();
		if (loggingEnabled) {
			LOGGER.info(getWrappedCommandString());
		}
		
		ProcessBuilder processBuilder = new ProcessBuilder(wrappedCommand);
		processBuilder.directory(workingDir);
		processBuilder.redirectErrorStream(true);
		
		Process process = processBuilder.start();
		BufferedReader outputReader = null;
		
		try {
			// Read the process output using the platform's default 
			// character encoding.
			InputStream stdout = process.getInputStream();
			outputReader = new BufferedReader(new InputStreamReader(stdout));
			captureProcessOutput(outputReader);
			
			exitCode = process.waitFor();
		} finally {
			Closeables.close(outputReader, true);
			process.destroy();
		}
	}

	private void captureProcessOutput(BufferedReader outputReader) throws IOException {
		String line = null;
		String lineSeparator = Platform.getLineSeparator();
		
		while ((line = outputReader.readLine()) != null) {
			output.write(line);
			output.write(lineSeparator);
			
			if (loggingEnabled) {
				LOGGER.info(line);
			}
		}
	}

	/**
	 * Runs the external process by executing the command. This method is
	 * similar to {@link #execute()}, but runs the external process in a
	 * separate thread. If the running time of this thread exceeds the timeout
	 * value a {@code TimeoutException} will be thrown.
	 * @param timeout Maximum allowed running time, in milliseconds.
	 * @throws IOException if an error occurs while reading the output from the
	 *         external process.
	 * @throws SecurityException if the JVM's security settings do not allow
	 *         the external process to be started.
	 * @throws TimeoutException if the external process's running time exceeds
	 *         the timeout.
	 * @throws UnsupportedOperationException if the platform does not support
	 *         starting an external process.
	 */
	public void execute(long timeout) throws IOException, TimeoutException {
		FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
			public String call() throws Exception {
				execute();
				return "";
			}
		});
		
		Thread workerThread = new Thread(task);
		workerThread.start();
		
		try {
			task.get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new IOException("Waiting for external process interrupted", e);
		} catch (ExecutionException e) {
			throw new IOException("Error while executing external process", e);
		} catch (TimeoutException e) {
			// Set the status to done so that the partial output can
			// be obtained.
			setDone(true);
			throw e;
		}
	}
	
	public String getCommandString() {
		return COMMAND_JOINER.join(command);
	}
	
	private List<String> getWrappedCommand() {
		if (shellMode) {
			List<String> wrappedCommand = new ArrayList<String>();
			wrappedCommand.add("sh");
			wrappedCommand.add("-c");
			if (remoteHost == null) {
				wrappedCommand.add(getCommandString());
			} else {
				wrappedCommand.add(getSSHCommandString());
			}
			return wrappedCommand;
		} else {
			return command;
		}
	}
	
	private String getWrappedCommandString() {
		List<String> wrappedCommand = getWrappedCommand();
		return COMMAND_JOINER.join(wrappedCommand);
	}
	
	private String getSSHCommandString() {
		String host = (remoteHost != null) ? remoteHost : "localhost";
		String destination = host;
		if (remoteUser != null) {
			destination = remoteUser + "@" + host;
		}
		
		List<String> sshCommand = new ArrayList<String>();
		sshCommand.add("ssh");
		sshCommand.add(destination);
		sshCommand.add("\"" + getCommandString() + "\"");
		return COMMAND_JOINER.join(sshCommand);
	}
	
	public void setShellMode(boolean shellMode) {
		if (shellMode && !isShellModeSupported()) {
			throw new UnsupportedOperationException("Shell mode is not supported on " + 
					"the current platform");
		}
		this.shellMode = shellMode;
	}
	
	public boolean isShellMode() {
		return shellMode;
	}
	
	public void setRemoteHost(String remoteHost, String remoteUser) {
		if ((remoteHost != null) && !isRemoteHostSupported()) {
			throw new UnsupportedOperationException("Executing commands on a remote host " + 
					"is not supported on the current platform");
		}
		this.remoteHost = remoteHost;
		this.remoteUser = remoteUser;
		this.shellMode = true;
	}
	
	public String getRemoteHost() {
		return remoteHost;
	}
	
	public String getRemoteUser() {
		return remoteUser;
	}

	public void setWorkingDirectory(File workingDir) {
		if (!workingDir.exists() || !workingDir.isDirectory()) {
			throw new IllegalArgumentException("No such directory: " + workingDir);
		}
		this.workingDir = workingDir;
	}
	
	public File getWorkingDirectory() {
		return workingDir;
	}
	
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
	}
	
	public boolean isLoggingEnabled() {
		return loggingEnabled;
	}

	private void setDone(boolean done) {
		this.done.set(done);
	}
	
	private boolean isDone() {
		return done.get();
	}
	
	/**
	 * Returns the external process's exit code.
	 * @throws IllegalStateException if the external process has not been started
	 *         yet or is currently running.
	 */
	public int getExitCode() {
		if (!isDone()) {
			throw new IllegalStateException("Exit code not available yet");
		}
		return exitCode;
	}
	
	/**
	 * Returns the external process's output. This includes everything it wrote
	 * to {@code stdout} and {@code stderr}.
	 * @throws IllegalStateException if the external process has not been started
	 *         yet or is currently running.
	 */
	public String getOutput() {
		if (!isDone()) {
			throw new IllegalStateException("Output not available yet");
		}
		return output.toString().trim();
	}
	
	public boolean isExecuteSupported() {
		return !Platform.isGoogleAppEngine();
	}
	
	public boolean isShellModeSupported() {
		return isUnixLikePlatform();
	}
	
	public boolean isRemoteHostSupported() {
		return isUnixLikePlatform();
	}
	
	private boolean isUnixLikePlatform() {
		return Platform.isOSX() || Platform.isLinux();
	}
	
	@Override
	public String toString() {
		return getCommandString();
	}
}
