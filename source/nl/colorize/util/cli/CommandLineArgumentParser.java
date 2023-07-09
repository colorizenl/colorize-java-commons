//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import nl.colorize.util.PropertyDeserializer;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Define supported arguments for a command line interface, then parse the
 * provided arguments accordingly. This class only allows named arguments and
 * flags, arguments without an explicit name are not supported.
 * <p>
 * Refer to the library's README file for information and examples on how to
 * define a command line interface using this class.
 * <p>
 * When providing argument values on the command line, argument names are
 * considered case-insensitive and normalized. For example, using "input",
 * "-input", "--input", or "--input=value" all map to the same argument.
 * <p>
 * This class exists because Args4j has not been updated since 2016. Command
 * line arguments are defined using annotations, in a similar way to Args4j.
 * However, the annotations are added to record fields, which Args4j couldn't
 * use because it predates records being introduced in Java 17.
 */
public class CommandLineArgumentParser {

    private String applicationName;
    private PrintWriter out;
    private boolean exitOnFail;
    private List<String> descriptionLines;
    private boolean colors;
    private PropertyDeserializer propertyDeserializer;

    private static final String DEFAULT_VALUE_MARKER = "$$default";

    public CommandLineArgumentParser(String applicationName) {
        this(applicationName, new PrintWriter(System.out), true);
    }

    public CommandLineArgumentParser(Class<?> applicationName) {
        this(applicationName.getSimpleName());
    }

    protected CommandLineArgumentParser(String applicationName, PrintWriter out, boolean exitOnFail) {
        this.applicationName = applicationName;
        this.out = out;
        this.exitOnFail = exitOnFail;
        this.descriptionLines = new ArrayList<>();
        this.colors = true;
        this.propertyDeserializer = new PropertyDeserializer();
    }

    /**
     * Returns the {@link PropertyDeserializer} that is used to convert command
     * line arguments, which are always strings, to the correct type.
     */
    public PropertyDeserializer getPropertyDeserializer() {
        return propertyDeserializer;
    }

    /**
     * Extends the usage description with the specified line. This information
     * will be displayed when the command line interface's usage information
     * messsage is printed.
     */
    public void addDescription(String... lines) {
        descriptionLines.addAll(List.of(lines));
    }

    /**
     * By default, command line output uses ANSI color to improve readability.
     * Calling this method disables colors and reverts to plain text output.
     */
    public void disableColor() {
        this.colors = false;
    }

    /**
     * Prints the usage information message for the command line interface. This
     * is done automatically if the provides arguments are incomplete.
     */
    public void printUsage(Class<?> cli) {
        int nameColumnWidth = 2 + findAnnotatedFields(cli).stream()
            .mapToInt(field -> formatArgName(field).length())
            .max()
            .orElse(0);

        if (!descriptionLines.isEmpty()) {
            descriptionLines.forEach(out::println);
            out.println();
        }

        out.println(format("Usage: " + applicationName, AnsiColor.CYAN_BOLD));

        for (Field field : findAnnotatedFields(cli)) {
            String name = formatArgName(field);
            String usage = field.getAnnotation(Arg.class).usage();
            out.println("    " + Strings.padEnd(name, nameColumnWidth, ' ') + usage);
        }

        out.flush();
    }

    private void printUsage(Class<?> cli, CommandLineInterfaceException cause) {
        printUsage(cli);
        out.println();
        out.println(format(cause.getMessage(), AnsiColor.RED_BOLD));
        out.println();
        out.flush();
    }

    private String formatArgName(Field field) {
        String name = getArgName(field);
        if (!name.startsWith("-")) {
            name = "--" + name;
        }
        if (!isRequired(field)) {
            name = "[" + name + "]";
        }
        return format(name, AnsiColor.CYAN_BOLD);
    }

    private String format(String text, AnsiColor color) {
        if (colors) {
            return color.format(text);
        } else {
            return text;
        }
    }

    /**
     * Parses the command line arguments based on the {@link Arg} annotations
     * in the specified class. If the provided arguments are incomplete or
     * invalid, usage information will be displayed. If this class was
     * configured to exit on failure, {@code System.exit} will be invoked to
     * exit the application.
     *
     * @throws CommandLineInterfaceException If the provided arguments are
     *         incomplete or invalid.
     * @throws IllegalArgumentException if {@code cli} does not define any
     *         constructor arguments annotated with{@link Arg}.
     */
    public <T> T parse(String[] argv, Class<T> cli) throws CommandLineInterfaceException {
        try {
            Map<String, String> values = parseArgValues(argv);

            Constructor<?> constructor = findDefaultConstructor(cli);
            constructor.setAccessible(true);
            T instance = (T) constructor.newInstance();

            for (Field field : findAnnotatedFields(cli)) {
                Object value = convertArgValue(field, values);
                if (value != null) {
                    field.setAccessible(true);
                    field.set(instance, value);
                }
            }

            return instance;
        } catch (CommandLineInterfaceException e) {
            handleInvalidCommandLineInput(cli, e);
            throw e;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Error while initializing " + cli.getName(), e);
        }
    }

    private void handleInvalidCommandLineInput(Class<?> cli, CommandLineInterfaceException e) {
        printUsage(cli, e);
        if (exitOnFail) {
            System.exit(1);
        }
    }

    private Constructor<?> findDefaultConstructor(Class<?> cli) {
        try {
            return cli.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class missing default constructor: " + cli.getName());
        }
    }

    private List<Field> findAnnotatedFields(Class<?> cli) {
        return Arrays.stream(cli.getDeclaredFields())
            .filter(field -> field.getAnnotation(Arg.class) != null)
            .toList();
    }

    private Map<String, String> parseArgValues(String[] argv) {
        List<String> providedArgs = processProvidedArgs(argv);
        Map<String, String> values = new HashMap<>();
        int index = 0;

        while (index < providedArgs.size()) {
            String current = providedArgs.get(index);

            if (isNamedArgument(providedArgs, index)) {
                String name = normalizeArgumentName(current);
                values.put(name, providedArgs.get(index + 1));
                index += 2;
            } else if (isFlagArgument(providedArgs, index)) {
                String name = normalizeArgumentName(current);
                values.put(name, "true");
                index += 1;
            } else {
                throw new CommandLineInterfaceException("Unexpected argument '" + current + "'");
            }
        }

        return values;
    }

    private boolean isNamedArgument(List<String> providedArgs, int index) {
        return index <= providedArgs.size() - 2 &&
            providedArgs.get(index).startsWith("-") &&
            !providedArgs.get(index + 1).startsWith("-");
    }

    private boolean isFlagArgument(List<String> providedArgs, int index) {
        return providedArgs.get(index).startsWith("-");
    }

    private Object convertArgValue(Field field, Map<String, String> values) {
        Arg config = field.getAnnotation(Arg.class);
        Class<?> type = field.getType();
        String name = getArgName(field, config);
        String value = values.get(name);

        if (value == null) {
            if (type == boolean.class) {
                value = "false";
            } else if (isRequired(field)) {
                throw new CommandLineInterfaceException("Missing required argument '" + name + "'");
            } else if (!config.defaultValue().equals(DEFAULT_VALUE_MARKER)) {
                value = config.defaultValue();
            }
        }

        if (value == null) {
            return null;
        }

        try {
            return propertyDeserializer.parse(value, type);
        } catch (IllegalArgumentException e) {
            throw new CommandLineInterfaceException("Invalid value for '" + name + "': " + value);
        }
    }

    private boolean isRequired(Field field) {
        Arg config = field.getAnnotation(Arg.class);
        boolean defaultValue = !config.defaultValue().equals(DEFAULT_VALUE_MARKER);

        return !(field.getType().equals(boolean.class) || !config.required() || defaultValue);
    }

    /**
     * Normalizes the list of provided arguments so that the notations
     * {@code --a b} and {@code --a=b} both produce {@code [--a, b]}.
     */
    private List<String> processProvidedArgs(String[] argv) {
        Splitter argSplitter = Splitter.on("=").trimResults();

        return Arrays.stream(argv)
            .flatMap(argSplitter::splitToStream)
            .toList();
    }

    private String getArgName(Field field, Arg config) {
        if (config.name().equals(DEFAULT_VALUE_MARKER)) {
            return normalizeArgumentName(field.getName());
        } else {
            return normalizeArgumentName(config.name());
        }
    }

    private String getArgName(Field field) {
        return getArgName(field, field.getAnnotation(Arg.class));
    }

    private String normalizeArgumentName(String name) {
        return name.replace("-", "").toLowerCase();
    }
}
