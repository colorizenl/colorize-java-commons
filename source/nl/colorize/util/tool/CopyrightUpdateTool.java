//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.tool;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import nl.colorize.util.FileUtils;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Relation;
import nl.colorize.util.Tuple;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Command line tool that updates the copyright statement at the top of the 
 * file, for all files in a directory. This tool assumes all files use the 
 * UTF-8 character encoding and the Unix line separator (\n). Different 
 * character encodings or line endings will be replaced.
 */
public class CopyrightUpdateTool {

    private String startCopyrightYear;
    private String newCopyrightYear;
    private String license;

    private static final Pattern COPYRIGHT_PATTERN = Pattern.compile(
        "Copyright\\s+(20\\d+)(\\s*[-,]\\s*)?(20\\d+)?");
    private static final List<String> SUPPORTED_FILE_EXTENSIONS = ImmutableList.of(
        ".java", ".js", ".ts", ".php", ".swift", ".gradle", ".md", ".properties", ".json", ".yaml");
    private static final List<String> EXCLUDE_DIRS = ImmutableList.of(
        "/build/", "/lib/", "/node_modules/", "/.git/", "/.gradle/", "/.idea/", "/out/");
    private static final Charset FILE_CHARSET = Charsets.UTF_8;
    private static final String LEAVE_MARKER = "leave";
    private static final Logger LOGGER = LogHelper.getLogger(CopyrightUpdateTool.class);

    public static void main(String[] args) {
        if (args.length != 4) {
            LOGGER.info("Usage: CopyrightUpdateTool <dir> <startYear> <copyrightYear> <license>");
            LOGGER.info("");
            LOGGER.info("Providing the value of 'leave' for any of the arguments");
            LOGGER.info("will not rewrite that value and retain the original.");
            System.exit(1);
        }
        
        File dir = new File(args[0]);
        String startCopyrightYear = args[1];
        String newCopyrightYear = args[2];
        String license = args[3];

        CopyrightUpdateTool tool = new CopyrightUpdateTool(startCopyrightYear, newCopyrightYear, license);
        tool.updateDirectory(dir);
    }
    
    public CopyrightUpdateTool(String startCopyrightYear, String newCopyrightYear, String license) {
        this.startCopyrightYear = startCopyrightYear;
        this.newCopyrightYear = newCopyrightYear;
        this.license = license;
    }

    private void updateDirectory(File dir) {
        List<File> files = findFiles(dir);
        LOGGER.info("Updating copyright statements");
        LOGGER.info("Found " + files.size() + " files");
        
        for (File file : files) {
            LOGGER.info("Updating " + FileUtils.getRelativePath(file, dir));
            try {
                updateCopyrightStatement(file);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Cannot update file " + file.getName(), e);
            }
        }
        
        LOGGER.info("Done, updated " + files.size() + " files");
    }

    private List<File> findFiles(File dir) {
        try {
            return Files.walk(dir.toPath())
                .map(path -> path.toFile())
                .filter(file -> !file.isDirectory() && shouldUpdateCopyright(file))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot walk directory " + dir, e);
        }
    }
    
    private boolean shouldUpdateCopyright(File file) {
        String ext = "." + FileUtils.getFileExtension(file);
        return SUPPORTED_FILE_EXTENSIONS.contains(ext) && !isExcludedDir(file);
    }
    
    private boolean isExcludedDir(File file) {
        for (String excludedDir : EXCLUDE_DIRS) {
            if (file.getAbsolutePath().contains(excludedDir)) {
                return true;
            }
        }
        return false;
    }

    private void updateCopyrightStatement(File file) throws IOException {
        List<String> originalLines = FileUtils.readLines(file, FILE_CHARSET);
        List<String> processedLines = new ArrayList<String>();
        
        for (String line : originalLines) {
            processedLines.add(processLine(line));
        }
        
        for (Tuple<String, String> diff : calculateDiff(originalLines, processedLines)) {
            LOGGER.info("    <<< " + diff.getLeft().trim());
            LOGGER.info("    >>> " + diff.getRight().trim());
        }
            
        PrintWriter writer = new PrintWriter(file, FILE_CHARSET.displayName());
        for (String line : processedLines) {
            writer.println(line);
        }
        writer.close();
    }

    protected String processLine(String line) {
        Matcher matcher = COPYRIGHT_PATTERN.matcher(line);

        if (!matcher.find()) {
            return processLineLicense(line);
        }

        if (matcher.group(3) == null) {
            return matcher.replaceFirst("Copyright " + newCopyrightYear);
        } else if (startCopyrightYear.equals(LEAVE_MARKER)) {
            return matcher.replaceFirst("Copyright $1$2" + newCopyrightYear);
        } else {
            return matcher.replaceFirst("Copyright " + startCopyrightYear + "$2" + newCopyrightYear);
        }
    }

    private String processLineLicense(String line) {
        if (license == null || LEAVE_MARKER.equals(license)) {
            return line;
        }

        if (!line.trim().toLowerCase().startsWith("// apache license")) {
            return line;
        }

        return "// " + license;
    }

    private Relation<String, String> calculateDiff(List<String> first, List<String> second) {
        if (first.size() != second.size()) {
            throw new IllegalArgumentException("File should have the same length before/after");
        }
        
        Relation<String, String> diff = Relation.of();
        for (int i = 0; i < first.size(); i++) {
            if (!first.get(i).equals(second.get(i))) {
                diff.add(Tuple.of(first.get(i), second.get(i)));
            }
        }
        return diff;
    }
}
