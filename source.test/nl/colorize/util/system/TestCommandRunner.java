//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.system;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import nl.colorize.util.LoadUtils;
import nl.colorize.util.Platform;
import nl.colorize.util.system.CommandRunner;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for the {@code CommandRunner} class.
 */
public class TestCommandRunner {
	
	@Test
	public void testCaptureOutput() throws Exception {
		CommandRunner commandRunner = new CommandRunner("pwd");
		commandRunner.execute();
		assertEquals(0, commandRunner.getExitCode());
		assertEquals(Platform.getWorkingDirectory().getAbsolutePath(), commandRunner.getOutput());
	}
	
	@Test
	public void testWorkingDirectory() throws Exception {
		CommandRunner commandRunner = new CommandRunner("pwd");
		commandRunner.setWorkingDirectory(new File("source.test"));
		commandRunner.execute();
		assertEquals(new File("source.test").getAbsolutePath(), commandRunner.getOutput());
	}
	
	@Test
	public void testShellMode() throws IOException {
		File tempFile = LoadUtils.createTempFile("test 1", Charsets.UTF_8);
		assertEquals("test 1", Files.toString(tempFile, Charsets.UTF_8));
		
		CommandRunner commandRunner = new CommandRunner("echo \"test 2\" > " + tempFile.getAbsolutePath());
		commandRunner.setShellMode(true);
		commandRunner.execute();
		assertEquals(0, commandRunner.getExitCode());
		assertEquals("test 2\n", Files.toString(tempFile, Charsets.UTF_8));
	}
	
	@Test(expected = TimeoutException.class)
	public void testTimeout() throws Exception {
		CommandRunner commandRunner = new CommandRunner("echo 'a'; sleep 3; echo 'b';");
		commandRunner.setShellMode(true);
		commandRunner.execute(500);
	}
}
