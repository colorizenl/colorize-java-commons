//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Charsets;

import org.junit.Test;

/**
 * Unit test for the {@code CommandRunner} class.
 */
public class CommandRunnerTest {
    
    @Test
    public void testCaptureOutput() throws Exception {
        CommandRunner commandRunner = new CommandRunner("pwd");
        commandRunner.execute();
        assertEquals(0, commandRunner.getExitCode());
        assertEquals(Platform.getUserWorkingDirectory().getAbsolutePath(), commandRunner.getOutput());
    }
    
    @Test
    public void testWorkingDirectory() throws Exception {
        CommandRunner commandRunner = new CommandRunner("pwd");
        commandRunner.setWorkingDirectory(new File("source.test"));
        commandRunner.execute();
        assertEquals(new File("source.test").getAbsolutePath(), commandRunner.getOutput());
    }
    
    @Test
    public void testShellMode() throws Exception {
        File tempFile = LoadUtils.createTempFile("test 1", Charsets.UTF_8);
        assertEquals("test 1", FileUtils.read(tempFile, Charsets.UTF_8));
        
        CommandRunner commandRunner = new CommandRunner("echo", "test", ">", tempFile.getAbsolutePath());
        commandRunner.setShellMode(true);
        commandRunner.execute();
        assertEquals(0, commandRunner.getExitCode());
        assertEquals("test\n", FileUtils.read(tempFile, Charsets.UTF_8));
    }
    
    @Test(expected=TimeoutException.class)
    public void testTimeout() throws Exception {
        CommandRunner commandRunner = new CommandRunner("sleep", "3");
        commandRunner.setShellMode(true);
        commandRunner.setTimeout(500);
        commandRunner.execute();
    }
    
    @Test
    public void testDoubleEscapeRemoteCommand() {
        CommandRunner commandRunner = new CommandRunner("cat", "first second.txt");
        commandRunner.setRemoteHost("test.colorize.nl");
        
        assertEquals("sh -c ssh test.colorize.nl \"cat first\\ second.txt\"", commandRunner.toString());
    }
    
    @Test
    public void testQuoteArguments() throws Exception {
        File tempFile = LoadUtils.createTempFile(Platform.getTempDir(), "test file.txt", 
                "test", Charsets.UTF_8);
        
        CommandRunner commandRunner = new CommandRunner("cat", tempFile.getAbsolutePath());
        commandRunner.setShellMode(true);
        commandRunner.setLoggingEnabled(true);
        commandRunner.execute();
        
        assertEquals("sh -c cat " + tempFile.getAbsolutePath().replace(" ", "\\ "), commandRunner.toString());
        assertEquals(0, commandRunner.getExitCode());
        assertEquals("test", commandRunner.getOutput());
    }
}
