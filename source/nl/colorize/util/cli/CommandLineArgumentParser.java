//-----------------------------------------------------------------------------
// Colorize MultimediaLib
// Copyright 2009-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import nl.colorize.util.AppProperties;
import nl.colorize.util.LoadUtils;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
 *     public static void main(String[] argv) {
 *         AppProperties args = new CommandLineArgumentParser("MyApp")
 *             .addRequired("--input", "Input directory");
 *             .addOptional("--output", "Output directory");
 *             .addFlag("--overwrite", "Overwrites existing values");
 *             .parseArgs(argv)
 *
 *         File inputDir = args.getFile("input");
 *         File outputDir = args.getFile("input", new File("/tmp"));
 *         boolean overwrite = args.getBool("overwrite");
 *     }
 * </pre>
 * <p>
 * When providing argument values on the command line, argument names are
 * considered case-insensitive and normalized. For example, using "input",
 * "-input", "--input", or "--input=value" all map to the same argument. When
 * *retrieving* the argument, the canonical argument name is "input", i.e.
 * lowercase and without hyphens or the equals symbol.
 */
public class CommandLineArgumentParser {

    private String applicationName;
    private Map<String, Argument> definedArgs;
    private PrintWriter out;

    protected CommandLineArgumentParser(String applicationName, PrintWriter out) {
        this.applicationName = applicationName;
        this.out = out;
        this.definedArgs = new LinkedHashMap<>();
    }

    public CommandLineArgumentParser(String applicationName) {
        this(applicationName, new PrintWriter(System.out));
    }

    public CommandLineArgumentParser(Class<?> applicationName) {
        this(applicationName.getSimpleName());
    }

    private void addArgument(String name, boolean required, boolean flag, String usage) {
        Preconditions.checkArgument(name.startsWith("-"), "Invalid argument name: " + name);
        Preconditions.checkArgument(!(flag && required), "Flag cannot be required argument");

        Argument arg = new Argument(name, required, flag, usage);
        String normalizedName = name.replace("-", "").toLowerCase();

        Preconditions.checkState(!definedArgs.containsKey(normalizedName),
            "Argument '" + name + "' has already been defined");

        definedArgs.put(normalizedName, arg);
    }

    /**
     * Adds a required/mandatory command line argument with the specified name.
     * Returns {@code this} for method chaining.
     *
     * @throws IllegalStateException if another argument with the same name has
     *         already been defined.
     */
    public CommandLineArgumentParser addRequired(String name, String usage) {
        addArgument(name, true, false, usage);
        return this;
    }

    /**
     * Adds an optional command line argument with the specified name and
     * default value. Returns {@code this} for method chaining.
     *
     * @throws IllegalStateException if another argument with the same name
     *         has already been defined.
     */
    public CommandLineArgumentParser addOptional(String name, String usage) {
        addArgument(name, false, false, usage);
        return this;
    }

    /**
     * Adds an optional command line flag with the specified name. Flags are
     * always optional, and always have a default value of false (i.e. when
     * the flag is not set). Returns {@code this} for method chaining.
     *
     * @throws IllegalStateException if another argument with the same name
     *         has already been defined.
     */
    public CommandLineArgumentParser addFlag(String name, String usage) {
        addArgument(name, false, true, usage);
        return this;
    }

    /**
     * Prints the usage information message for the command line interface. This
     * is done automatically if the provides arguments are incomplete.
     */
    public void printUsage() {
        int nameColumnWidth = definedArgs.values().stream()
            .mapToInt(arg -> arg.name.length())
            .max()
            .orElse(0);

        out.println("Usage: " + applicationName);

        for (Argument arg : definedArgs.values()) {
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
     * Parses the provided command line arguments. If required arguments are
     * missing or invalid, the usage information will be displayed and the
     * application will terminate.
     */
    public AppProperties parseArgs(String... args) {
        try {
            return tryParseArgs(args);
        } catch (CommandLineInterfaceException e) {
            printUsage(e);
            System.exit(1);
            throw new AssertionError("System exit");
        }
    }

    /**
     * Parses the provided command line arguments, and throws an exception if
     * ant required arguments are missing or invalid. This method is similar
     * to {@link #parseArgs(String...)}, but does not print the usage
     * information or exit the application.
     *
     * @throws CommandLineInterfaceException if the requirements for mandatory
     *         command line arguments are not met.
     */
    public AppProperties tryParseArgs(String... argv) throws CommandLineInterfaceException {
        List<String> providedArgs = processArgsList(argv);
        Map<Argument, String> parsedArgs = new HashMap<>();
        int index = 0;

        while (index < providedArgs.size()) {
            Argument arg = lookupDefinedArgument(providedArgs.get(index));

            if (arg.flag) {
                register(arg, "true", parsedArgs);
                index += 1;
            } else {
                register(arg, providedArgs.get(index + 1), parsedArgs);
                index += 2;
            }
        }

        checkRequiredArguments(parsedArgs.keySet());

        Map<String, String> values = finalizeArgumentValues(parsedArgs);
        return new ArgumentValues(values);
    }

    /**
     * Normalizes the list of provided arguments so that the notations
     * {@code --a b} and {@code --a=b} both produce {@code [--a, b]}.
     */
    private List<String> processArgsList(String[] argv) {
        Splitter argSplitter = Splitter.on("=").trimResults();

        return Arrays.stream(argv)
            .flatMap(argSplitter::splitToStream)
            .toList();
    }

    private String getNormalizedArgumentName(String name) {
        return name.replace("-", "").toLowerCase();
    }

    private Argument lookupDefinedArgument(String name) throws CommandLineInterfaceException {
        String normalizedName = getNormalizedArgumentName(name);
        Argument arg = definedArgs.get(normalizedName);
        if (arg == null) {
            throw new CommandLineInterfaceException("Unknown argument '" + name + "'");
        }
        return arg;
    }

    private void register(Argument arg, String value, Map<Argument, String> parsedArgs) {
        if (!parsedArgs.containsKey(arg)) {
            parsedArgs.put(arg, value);
        }
    }

    private void checkRequiredArguments(Set<Argument> parsed) throws CommandLineInterfaceException {
        Set<String> parsedNames = parsed.stream()
            .map(Argument::name)
            .collect(Collectors.toSet());

        for (Argument arg : definedArgs.values()) {
            if (arg.required && !parsedNames.contains(arg.name)) {
                throw new CommandLineInterfaceException("Missing required argument '" + arg + "'");
            }
        }
    }

    private Map<String, String> finalizeArgumentValues(Map<Argument, String> parsedArgs) {
        Map<String, String> values = new HashMap<>();

        // All flags have an implicit default value of false, i.e. when
        // they are not specified.
        for (Argument arg : definedArgs.values()) {
            if (arg.flag) {
                values.put(getNormalizedArgumentName(arg.name), "false");
            }
        }

        for (Argument arg : parsedArgs.keySet()) {
            values.put(getNormalizedArgumentName(arg.name), parsedArgs.get(arg));
        }

        return values;
    }

    /**
     * Represents one of the arguments in the command line interface. This
     * referred to the *defined* argument, not to the *parsed* argument.
     */
    private record Argument(String name, boolean required, boolean flag, String usage) {

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Extends the {@link AppProperties} interface to perform the argument name
     * normalization by this class. This allows arguments to be accessed by both
     * identifier name (e.g. "output") and their actual command line name (e.g.
     * "-o" or "--output").
     */
    private class ArgumentValues implements AppProperties {

        private Properties properties;

        public ArgumentValues(Map<String, String> values) {
            this.properties = LoadUtils.toProperties(values);
        }

        @Override
        public Properties getProperties() {
            return properties;
        }

        @Override
        public String get(String name, String defaultValue) {
            return properties.getProperty(getNormalizedArgumentName(name), defaultValue);
        }
    }
}
