//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.common.io.Closeables;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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
 * <p>
 * Running shell commands exposes both the application and the underlying system
 * to considerable security risks, such as code injection. Do not perform shell 
 * commands that contain user-supplied input, or that access or use user-supplied 
 * files. Escaping user input does not properly address the security risks, due
 * to the number of possible edge cases (e.g. command substitution, variable 
 * evaluation, redirection). Because of the potential risks, {@code CommandRunner}
 * is automatically disabled in sandboxed environments, to protect those 
 * environments from attackers that want to break out of the sandbox. Attempting
 * to run shell commands in those environments will result in a 
 * {@code SecurityException}.
 *
 * @deprecated It is no longer needed to create {@code Process} instances using this
 *             class. Use {@link java.lang.ProcessBuilder} instead, which supports
 *             common operations such as setting environment variables and capturing
 *             process output. For executing SSH command, use a library such as
 *             <a href="http://www.jcraft.com/jsch/index.html">JSch</a>.
 */
@Deprecated
public class CommandRunner {
    
    private List<String> command;
    private boolean shellMode;
    private String remoteHost;
    private String remoteUser;
    private File workingDir;
    private long timeout = 0;
    private boolean loggingEnabled;
    
    private AtomicBoolean done;
    private int exitCode;
    private StringWriter output;
    
    private static final Escaper BASH_ESCAPER = Escapers.builder()
        .addEscape(' ', "\\ ")
        .addEscape('$', "\\$")
        .addEscape('`', "\\`")
        .addEscape('\\', "\\\\")
        .addEscape('!', "\\!")
        .build();
    
    private static final Joiner COMMAND_JOINER = Joiner.on(' ').skipNulls();
    private static final char SSH_COMMAND_WRAP_CHAR = '"';
    private static final Logger LOGGER = LogHelper.getLogger(CommandRunner.class);
    
    /**
     * Creates a {@code CommandRunner} that will execute the specified command.
     * The process will not start until {@link #execute()} is called.
     * @throws SecurityException when attempting to use {@code CommandRunner}
     *         in a sandboxed environment, due to the security risks. Refer to
     *         the class documentation for more information. 
     * @throws IllegalArgumentException if the command is empty.
     */
    public CommandRunner(List<String> cmd) {
        if (isSandboxedEnvironment()) {
            throw new SecurityException("CommandRunner not allowed in sandboxed environments");
        }
        
        if (cmd.isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }
        
        command = ImmutableList.copyOf(cmd);
        shellMode = false;
        timeout = 0L;
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
     * @throws TimeoutException if the external process's running time exceeds
     *         the maximum allowed time (as indicated by {@link #getTimeout()}).
     * @throws SecurityException if the JVM's security settings do not allow
     *         the external process to be started. 
     * @throws UnsupportedOperationException if the platform does not support
     *         starting an external process.
     */
    public void execute() throws IOException, TimeoutException {
        if (!isExecuteSupported()) {
            throw new UnsupportedOperationException("Running an external process is " +
                    "not supported on the current platform");
        }
        
        setDone(false);
        exitCode = -1;
        output = new StringWriter();
        
        try {
            if (timeout > 0) {
                runProcessInBackground();
            } else {
                runProcess();
            }
        } catch (InterruptedException e) {
            throw new IOException("External process execution interrupted", e);
        }
        
        setDone(true);
    }
    
    private void runProcessInBackground() throws IOException, TimeoutException {
        FutureTask<String> task = wrapInBackgroundTask();
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
            // be obtained, even though the command resulted in a timeout.
            setDone(true);
            throw e;
        }
    }
    
    private FutureTask<String> wrapInBackgroundTask() {
        return new FutureTask<String>(new Callable<String>() {
            public String call() throws Exception {
                runProcess();
                return "";
            }
        });
    }
    
    private void runProcess() throws IOException, InterruptedException {
        Process process = startProcess();
        BufferedReader outputReader = null;
        
        try {
            // Read the process output using the platform's default 
            // character encoding.
            outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            captureProcessOutput(outputReader);
            
            exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Command returned exit code " + exitCode);
            }
        } finally {
            Closeables.close(outputReader, true);
            process.destroy();
        }
    }

    private Process startProcess() throws IOException {
        if (loggingEnabled) {
            LOGGER.info(getCommandString());
        }
        
        ProcessBuilder processBuilder = new ProcessBuilder(getCommand());
        processBuilder.directory(workingDir);
        processBuilder.redirectErrorStream(true);
        
        return processBuilder.start();
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
    
    private List<String> getCommand() {
        if (shellMode) {
            List<String> shellCommandArgs = escapeArguments(command);
            String shellCommand = COMMAND_JOINER.join(shellCommandArgs);
            if (remoteHost != null) {
                shellCommand = "ssh " + getRemoteDestination() + " " + 
                        escapeCommand(shellCommand, SSH_COMMAND_WRAP_CHAR); 
            }
            return ImmutableList.of("sh", "-c", shellCommand);
        } else {
            return command;
        }
    }
    
    public String getCommandString() {
        return COMMAND_JOINER.join(getCommand());
    }
    
    private List<String> escapeArguments(List<String> args) {
        List<String> escaped = new ArrayList<>();
        for (String arg : args) {
            escaped.add(BASH_ESCAPER.escape(arg));
        }
        return escaped;
    }
    
    private String escapeCommand(String command, char escapeChar) {
        Escaper escaper = Escapers.builder()
                .addEscape(escapeChar, "\\" + escapeChar)
                .build();
        
        StringBuilder buffer = new StringBuilder();
        buffer.append(escapeChar);
        buffer.append(escaper.escape(command));
        buffer.append(escapeChar);
        return buffer.toString();
    }

    private String getRemoteDestination() {
        if (remoteUser == null) {
            return remoteHost;
        } else {
            return remoteUser + "@" + remoteHost;
        }
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
        if (remoteHost != null && !isRemoteHostSupported()) {
            throw new UnsupportedOperationException("Executing commands on a remote host " + 
                    "is not supported on the current platform");
        }
        
        this.remoteHost = remoteHost;
        this.remoteUser = remoteUser;
        if (remoteHost != null) {
            shellMode = true;
        }
    }
    
    public void setRemoteHost(String remoteHost) {
        setRemoteHost(remoteHost, null);
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
    
    /**
     * Configure the maximum time (in milliseconds) a command is allowed to take.
     * Exceeding this time limit will result in a {@code TimeoutException}. 
     * Setting a value of 0 indicates that commands will run without a timeout.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public long getTimeout() {
        return timeout;
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
        return !Platform.isGoogleCloud();
    }
    
    public boolean isShellModeSupported() {
        return isUnixLikePlatform();
    }
    
    public boolean isRemoteHostSupported() {
        return isUnixLikePlatform();
    }
    
    private boolean isSandboxedEnvironment() {
        return Platform.isGoogleCloud() ||
                Platform.isMacAppSandboxEnabled() ||
                Platform.isAndroid();
    }
    
    private boolean isUnixLikePlatform() {
        return Platform.isMac() || Platform.isLinux();
    }
    
    @Override
    public String toString() {
        return getCommandString();
    }
}
