//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import nl.colorize.util.Property;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Define supported arguments for a command line interface, then parse the
 * provided arguments accordingly. This class only allows named arguments and
 * flags, arguments without an explicit name are not supported.
 * <p>
 * This class exists because Args4j has not been updated since 2016. However,
 * it provides an API that is quite different from Args4j's annotation-based
 * approach. Instead, it is more similar to defining command line arguments
 * using Python's {@code argparse}.
 * <p>
 * The following example shows how to define a simple command line interface:
 * <p>
 * <pre>
 *     public static void main(String[] argv) {
 *         CommandLineArgumentParser args = new CommandLineArgumentParser("MyApp")
 *             .addRequired("--input", "Input directory");
 *             .addOptional("--output", "Output directory");
 *             .addFlag("--overwrite", "Overwrites existing values");
 *             .parseArgs(argv)
 *
 *         File inputDir = args.get("input").getFile();
 *         File outputDir = args.get("input").getFile(new File("/tmp"));
 *         boolean overwrite = args.get("overwrite").getBool();
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
    private Map<Argument, String> parsedArgs;
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
    public CommandLineArgumentParser parseArgs(String... args) {
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
    public CommandLineArgumentParser tryParseArgs(String... argv) throws CommandLineInterfaceException {
        List<String> providedArgs = processArgsList(argv);
        parsedArgs = new HashMap<>();
        int index = 0;

        while (index < providedArgs.size()) {
            Argument arg = lookupDefinedArgument(providedArgs.get(index));

            if (arg.flag) {
                register(arg, "true");
                index += 1;
            } else {
                register(arg, providedArgs.get(index + 1));
                index += 2;
            }
        }

        checkRequiredArguments();

        return this;
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

    private void register(Argument arg, String value) {
        if (!parsedArgs.containsKey(arg)) {
            parsedArgs.put(arg, value);
        }
    }

    private void checkRequiredArguments() throws CommandLineInterfaceException {
        Set<String> parsedNames = parsedArgs.keySet().stream()
            .map(Argument::name)
            .collect(Collectors.toSet());

        for (Argument arg : definedArgs.values()) {
            if (arg.required && !parsedNames.contains(arg.name)) {
                throw new CommandLineInterfaceException("Missing required argument '" + arg + "'");
            }
        }
    }

    /**
     * Returns the value of the command line argument with the specified name,
     * or the default value if the argument was defined as optional.
     *
     * @throws IllegalStateException if this method is called before any
     *         command line arguments have been parsed.
     */
    public Property get(String name) {
        Preconditions.checkState(parsedArgs != null,
            "Command line arguments have not been parsed yet");

        String normalizedName = getNormalizedArgumentName(name);
        Argument definedArg = definedArgs.get(normalizedName);
        String value = parsedArgs.get(definedArg);

        if (value != null && !value.isEmpty()) {
            return Property.of(value);
        } else if (definedArg.required) {
            // This should never happen since we have already checked all required
            // arguments at this point.
            throw new AssertionError("Missing required argument '" + definedArg + "'");
        } else if (definedArg.flag) {
            return Property.of("false");
        } else {
            return Property.allowNull();
        }
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
}
