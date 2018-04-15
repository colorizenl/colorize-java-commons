//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

import nl.colorize.util.FileUtils;
import nl.colorize.util.LogHelper;

/**
 * Converts files to the standard formatting of using 4 spaces as indent, and
 * using the \n line separator.
 */
public class ConversionTool {
    
    private File dir;
    private boolean dryRun;
    
    private static final List<String> SUPPORTED_FILE_EXTENSIONS = ImmutableList.of(
            ".as", ".bat", ".css", ".gradle", ".html", ".java", ".js", ".json", ".jsp", 
            ".md", ".properties", ".py", ".r", ".sh", ".sql", ".swift", ".ts", ".txt", 
            ".xml", ".yaml"); 
    private static final Charset CHARSET = Charsets.UTF_8;
    private static final Logger LOGGER = LogHelper.getLogger(ConversionTool.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            LOGGER.info("Usage: ConversionTool <dir> [-dryRun]");
            System.exit(1);
        }
        
        ConversionTool tool = new ConversionTool(new File(args[0]), 
                Arrays.asList(args).contains("-dryRun"));
        tool.run();
    }
    
    public ConversionTool(File dir, boolean dryRun) {
        this.dir = dir;
        this.dryRun = dryRun;
    }

    public void run() {
        for (File file : findFiles()) {
            LOGGER.info("Converting file " + file.getAbsolutePath());
            try {
                convertFile(file);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Cannot convert file " + file.getAbsolutePath(), e);
            }
        }
    }

    private List<File> findFiles() {
        try {
            return Files.walk(dir.toPath())
                .map(path -> path.toFile())
                .filter(file -> !file.isDirectory() && isSupportedFile(file))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot walk directory " + dir.getAbsolutePath(), e);
        }
    }

    private boolean isSupportedFile(File file) {
        return SUPPORTED_FILE_EXTENSIONS.stream() //
            .anyMatch(ext -> file.getName().endsWith(ext));
    }
    
    private void convertFile(File file) throws IOException {
        List<String> originalLines = FileUtils.readLines(file, CHARSET);
        List<String> convertedLines = originalLines.stream() //
            .map(line -> convertLine(line)) //
            .collect(Collectors.toList());
        
        if (!dryRun) {
            try (PrintWriter writer = new PrintWriter(file, CHARSET.displayName())) {
                for (String line : convertedLines) {
                    writer.println(line);
                }
            }
        }
    }

    private String convertLine(String line) {
        int leadingTabs = countLeadingTabs(line);
        if (leadingTabs == 0) {
            return line;
        } else {
            return line.replace("\t", "    ");
        }
    }

    private int countLeadingTabs(String line) {
        int leadingTabs = 0;
        
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\t') {
                leadingTabs++;
            } else {
                break;
            }
        }
        
        return leadingTabs;
    }
}
