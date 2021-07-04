//-----------------------------------------------------------------------------
// Colorize MultimediaLib
// Copyright 2009-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import nl.colorize.util.Platform;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Can be used to define and parse arguments for a command line interface. This
 * class only supports named arguments and flags, arguments without an explicit
 * name are not supported.
 * <p>
 * This class exists because Args4j has not been updated since 2016. However,
 * it provides an API that is quite different from Args4j's annotation-based
 * approach. Instead, it is more similar to command line arguments support in
 * other languages, such as Python's argparse.
 * <p>
 * The following example shows how to define a simple command line interface:
 * <p>
 * <pre>
 *     public static void main(String[] args) {
 *         CommandLineArgumentParser argParser = new CommandLineArgumentParser("MyApp");
 *         argParser.add("--input", "Input directory");
 *         argParser.addOptional("--overwrite", false, "Overwrites existing values");
 *         argParser.parseArgs(args)
 *
 *         File inputDir = argParser.getFile("input");
 *         boolean overwrite = argParser.getBool("overwrite");
 *     }
 * </pre>
 */
public class CommandLineArgumentParser {

    private String applicationName;
    private Map<String, CommandLineArg> definedArgs;
    private Map<String, String> parsedArgs;
    private PrintWriter out;

    private static final CharMatcher LIST_SEP = CharMatcher.anyOf(",;:");
    private static final Splitter LIST_SPLITTER = Splitter.on(LIST_SEP).trimResults().omitEmptyStrings();

    public CommandLineArgumentParser(String applicationName, PrintWriter out) {
        this.applicationName = applicationName;
        this.out = out;
        this.definedArgs = new LinkedHashMap<>();
        this.parsedArgs = new HashMap<>();
    }

    public CommandLineArgumentParser(String applicationName) {
        this(applicationName, new PrintWriter(System.out));
    }

    /**
     * Adds a mandatory command line argument with the specified name.
     *
     * @throws IllegalStateException if another argument with the same name has
     *         already been defined.
     */
    public void add(String name, String usage) {
        Preconditions.checkState(!definedArgs.containsKey(name),
            "Argument '" + name + "' has already been defined");

        CommandLineArg arg = new CommandLineArg(name, true, null, usage);
        definedArgs.put(name, arg);
    }

    /**
     * Adds an optional command line argument with the specified name and
     * default value.
     *
     * @throws IllegalStateException if another argument with the same name
     *         has already been defined.
     */
    public void addOptional(String name, String defaultValue, String usage) {
        Preconditions.checkState(!definedArgs.containsKey(name),
            "Argument '" + name + "' has already been defined");

        CommandLineArg arg = new CommandLineArg(name, false, defaultValue, usage);
        definedArgs.put(name, arg);
    }

    /**
     * Adds an optional command line flag with the specified name and default
     * value.
     *
     * @throws IllegalStateException if another argument with the same name
     *         has already been defined.
     */
    public void addOptional(String name, boolean defaultValue, String usage) {
        addOptional(name, String.valueOf(defaultValue), usage);
    }

    /**
     * Prints the usage information message for the command line interface. This
     * is done automatically if the provides arguments are incomplete.
     */
    public void printUsage() {
        int nameColumnWidth = definedArgs.keySet().stream()
            .mapToInt(String::length)
            .max()
            .orElse(0);

        out.println("Usage: " + applicationName);

        for (CommandLineArg arg : definedArgs.values()) {
            String name = arg.required ? "<" + arg.name + ">" : "[" + arg.name + "]";
            out.println("       " + Strings.padEnd(name, nameColumnWidth + 4, ' ') + arg.usage);
        }

        out.flush();
    }

    private void printUsage(CommandLineInterfaceException cause) {
        printUsage();
        out.println();
        out.println(cause.getMessage());
        out.println();
        out.flush();
    }

    /**
     * Attempts to parse all arguments using the provided values. If the provided
     * arguments are missing or incomplete, the usage information will be
     * displayed and the application will terminate.
     */
    public void parseArgs(String... args) {
        try {
            tryParseArgs(args);
        } catch (CommandLineInterfaceException e) {
            printUsage(e);
            System.exit(1);
        }
    }

    /**
     * Attempts to parse all arguments using the provided values. In most cases
     * applications should prefer {@link #parseArgs(String[])}, which will
     * automatically show the usage information if the provided values are
     * incomplete. In contrast, this message will simply throw an exception and
     * will require the caller to handle the exception.
     */
    public void tryParseArgs(String... args) throws CommandLineInterfaceException {
        int index = 0;

        while (index < args.length) {
            CommandLineArg arg = findArgument(args[index]);

            if (arg.isFlag()) {
                parseArgument(arg, "true");
                index += 1;
            } else {
                parseArgument(arg, args[index + 1]);
                index += 2;
            }
        }

        for (CommandLineArg arg : definedArgs.values()) {
            if (!parsedArgs.containsKey(normalizeName(arg.name))) {
                parseArgument(arg, null);
            }
        }
    }

    private CommandLineArg findArgument(String value) {
        if (definedArgs.containsKey(value)) {
            return definedArgs.get(value);
        } else {
            throw new CommandLineInterfaceException("Unknown argument: " + value);
        }
    }

    private void parseArgument(CommandLineArg arg, String value) {
        if (value != null && !value.isEmpty()) {
            parsedArgs.put(normalizeName(arg.name), value);
        } else {
            if (arg.defaultValue == null) {
                throw new CommandLineInterfaceException("Missing required argument: " + arg.name);
            }

            parsedArgs.put(normalizeName(arg.name), arg.defaultValue);
        }
    }

    private String normalizeName(String name) {
        return name.replace("-", "").toLowerCase();
    }

    public String get(String name) throws CommandLineInterfaceException {
        Preconditions.checkState(!parsedArgs.isEmpty(),
            "Command line arguments have not been parsed yet");

        String value = parsedArgs.get(normalizeName(name));

        if (value != null) {
            return value;
        } else {
            throw new CommandLineInterfaceException("Unknown argument: " + name);
        }
    }

    public List<String> getList(String name) throws CommandLineInterfaceException {
        return LIST_SPLITTER.splitToList(get(name));
    }

    public int getInt(String name) throws CommandLineInterfaceException {
        return Integer.parseInt(get(name));
    }

    public float getFloat(String name) throws CommandLineInterfaceException {
        return Float.parseFloat(get(name));
    }

    public double getDouble(String name) throws CommandLineInterfaceException {
        return Double.parseDouble(get(name));
    }

    public boolean getBool(String name) throws CommandLineInterfaceException {
        return get(name).equals("true");
    }

    public File getFile(String name) throws CommandLineInterfaceException {
        return parseFilePath(get(name));
    }

    public List<File> getFileList(String name) throws CommandLineInterfaceException {
        return LIST_SPLITTER.splitToStream(get(name))
            .map(this::parseFilePath)
            .collect(Collectors.toList());
    }

    private File parseFilePath(String path) {
        if (path.isEmpty()) {
            throw new CommandLineInterfaceException("Empty file path: " + path);
        }

        if (path.startsWith("~/")) {
            try {
                File homeDir = Platform.getUserHomeDir();
                path = homeDir.getAbsolutePath() + "/" + path.substring(2);
            } catch (UnsupportedOperationException e) {
                // Ignore, platform does not support user home directory,
                // so relative paths starting with ~ are not supported.
            }
        }

        return new File(path);
    }

    /**
     * Represents one of the arguments in the command line interface.
     */
    private static class CommandLineArg {

        private String name;
        private boolean required;
        private String defaultValue;
        private String usage;

        public CommandLineArg(String name, boolean required, String defaultValue, String usage) {
            this.name = name;
            this.required = required;
            this.defaultValue = defaultValue;
            this.usage = usage;
        }

        public boolean isFlag() {
            return "true".equals(defaultValue) || "false".equals(defaultValue);
        }
    }
}
