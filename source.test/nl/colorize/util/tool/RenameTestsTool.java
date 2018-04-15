//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.tool;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import nl.colorize.util.LogHelper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Finds all Java, JavaScript, and TypeScript unit test files that use the
 * prefix naming convention (i.e. "TestSomething") and renames both the
 * file and classes to the suffix naming convention (i.e. "SomethingTest").
 */
public class RenameTestsTool {

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("class (Test\\w+)");
    private static final Logger LOGGER = LogHelper.getLogger(RenameTestsTool.class);

    public static void main(String[] args) {
        if (args.length != 1) {
            LOGGER.info("Usage: RenameTestsTool <sourceDir>");
            System.exit(1);
        }

        File sourceDir = new File(args[0]);
        Preconditions.checkArgument(sourceDir.isDirectory() && sourceDir.exists(),
                "Invalid source directory: " + sourceDir.getAbsolutePath());

        RenameTestsTool tool = new RenameTestsTool();
        try {
            tool.renameTestFilesInDirectory(sourceDir);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot rename test files", e);
        }
    }

    private void renameTestFilesInDirectory(File sourceDir) throws IOException {
        List<File> testFiles = findTestFiles(sourceDir);
        LOGGER.info("Renaming " + testFiles.size() + " test files");

        for (File testFile : testFiles) {
            File replacementFile = getReplacementFile(testFile);
            LOGGER.info("Renaming " + testFile.getName() + " to " + replacementFile.getName());
            renameTestFile(testFile, replacementFile);
        }

        LOGGER.info("Done");
    }

    private List<File> findTestFiles(File sourceDir) throws IOException {
        return Files.walk(sourceDir.toPath())
            .map(path -> path.toFile())
            .filter(file -> file.isFile() && isPrefixNamingConventionTestFile(file))
            .collect(Collectors.toList());
    }

    private boolean isPrefixNamingConventionTestFile(File file) {
        String name = file.getName();

        if (name.endsWith(".java") || name.endsWith(".js") || name.endsWith(".ts")) {
            return name.startsWith("Test");
        } else {
            return false;
        }
    }

    private File getReplacementFile(File testFile) {
        String originalName = testFile.getName();
        int dotIndex = originalName.lastIndexOf('.');
        String replacementName = originalName.substring(4, dotIndex) + "Test" +
                originalName.substring(dotIndex);
        return new File(testFile.getParentFile(), replacementName);
    }

    private void renameTestFile(File original, File replacement) throws IOException {
        List<String> lines = Files.readAllLines(original.toPath(), Charsets.UTF_8);
        List<String> rewrittenLines = new ArrayList<>();

        for (String line : lines) {
            Matcher matcher = CLASS_NAME_PATTERN.matcher(line);
            String replacementLine = matcher.replaceFirst("class " + replacement.getName().substring(
                    0, replacement.getName().lastIndexOf('.')));
            if (!line.equals(replacementLine)) {
                LOGGER.info("    > " + replacementLine.trim());
            }
            rewrittenLines.add(replacementLine);
        }

        PrintWriter writer = new PrintWriter(replacement, Charsets.UTF_8.displayName());
        rewrittenLines.forEach(line -> writer.println(line));
        writer.close();

        original.delete();
    }
}
