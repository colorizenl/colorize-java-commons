//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import com.google.common.base.Strings;
import nl.colorize.util.Config;
import nl.colorize.util.Platform;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Define supported arguments for a command line interface, then parse the
 * provided arguments accordingly. This class only allows named arguments and
 * flags, arguments without an explicit name are not supported.
 * <p>
 * The following example shows how to define a simple command line interface:
 * <p>
 * <pre><code>
 * public class MyApp {
 *     {@literal @}Arg(name = "--input", usage = "Input directory")
 *     public File inputDir
 *
 *     {@literal @}Arg
 *     public boolean overwrite;
 *
 *     public static void main(String[] argv) {
 *         CommandLineArgumentParser argParser = new CommandLineArgumentParser(MyApp.class);
 *         MyApp app = argParser.parse(argv, MyApp.class);
 *     }
 * }
 * </code></pre>
 * <p>
 * When providing argument values on the command line, argument names are
 * considered case-insensitive and normalized. For example, using "input",
 * "-input", "--input", or "--input=value" all map to the same argument.
 * <p>
 * Command line arguments are parsed using {@link Config}. This comes with
 * support for all standard data types, as explained in the {@link Config}
 * class documentation. Support for additional data types can be added by
 * providing a custom {@link Config} in the constructor.
 * <p>
 * This class exists because Args4j has not (at the time of writing) been
 * updated in over five years. This class is similar to Args4j in that
 * command line arguments are defined using annotations. However, it also
 * supports records, a Java feature that is not supported by Args4j since
 * records didn't exist when it was last updated.
 */
public class CommandLineArgumentParser {

    private String applicationName;
    private PrintWriter out;
    private boolean exitOnFail;
    private List<String> descriptionLines;
    private boolean colors;
    private Config propertyDeserializer;

    private static final String DEFAULT_VALUE_MARKER = "$$default";

    public CommandLineArgumentParser(String applicationName) {
        this(applicationName, new PrintWriter(System.out, true, System.out.charset()), true);
    }

    public CommandLineArgumentParser(Class<?> applicationName) {
        this(applicationName.getSimpleName());
    }

    protected CommandLineArgumentParser(String applicationName, PrintWriter out, boolean exitOnFail) {
        this.applicationName = applicationName;
        this.out = out;
        this.exitOnFail = exitOnFail;
        this.descriptionLines = new ArrayList<>();
        this.colors = Platform.isMac() || Platform.isLinux();
        this.propertyDeserializer = Config.raw();
    }

    /**
     * Registers a type mapper function to parse command line arguments into
     * the specified type. The type needs to be an exact match, matching a
     * type hierarchy is not supported.
     *
     * @see Config#registerTypeMapper(Class, Function) (Class, Function)
     */
    public <T> void registerTypeMapper(Class<T> type, Function<String, T> typeMapper) {
        propertyDeserializer.registerTypeMapper(type, typeMapper);
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
        List<Field> fields = findAnnotatedFields(cli);

        int nameColumnWidth = 2 + fields.stream()
            .mapToInt(field -> formatArgName(field).length())
            .max()
            .orElse(0);

        if (!descriptionLines.isEmpty()) {
            descriptionLines.forEach(out::println);
            out.println();
        }

        out.println(format("Usage: " + applicationName, AnsiColor.CYAN_BOLD));

        for (Field field : fields) {
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
     *                                       incomplete or invalid.
     * @throws IllegalArgumentException      if {@code cli} does not define any
     *                                       constructor arguments annotated with{@link Arg}.
     */
    public <T> T parse(String[] argv, Class<T> cli) throws CommandLineInterfaceException {
        try {
            T instance = createInstance(cli);
            List<Field> fields = findAnnotatedFields(cli);
            setDefaultValues(instance, fields);
            parseArgValues(argv, instance, fields);
            checkMissingRequiredArguments(instance, fields);
            return instance;
        } catch (CommandLineInterfaceException e) {
            handleInvalidCommandLineInput(cli, e);
            throw e;
        }
    }

    private void handleInvalidCommandLineInput(Class<?> cli, CommandLineInterfaceException e) {
        printUsage(cli, e);
        if (exitOnFail) {
            System.exit(1);
        }
    }

    private <T> T createInstance(Class<T> cli) {
        try {
            Constructor<T> constructor = cli.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class missing default constructor: " + cli.getName());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to instantiate class: " + cli.getName(), e);
        }
    }

    private List<Field> findAnnotatedFields(Class<?> cli) {
        return Arrays.stream(cli.getDeclaredFields())
            .filter(field -> field.getAnnotation(Arg.class) != null)
            .toList();
    }

    private void parseArgValues(String[] argv, Object instance, List<Field> fields) {
        List<String> providedArgs = processProvidedArgs(argv);
        int index = 0;
        Set<Field> fulfilled = new HashSet<>();

        while (index < providedArgs.size()) {
            String current = providedArgs.get(index);
            String argName = normalizeArgumentName(current);
            Field field = findMatchingField(argName, fields).orElse(null);

            if (field == null) {
                throw new CommandLineInterfaceException("Unknown argument '" + current + "'");
            } else if (fulfilled.contains(field)) {
                throw new CommandLineInterfaceException("Duplicate argument '" + argName + "'");
            } else if (field.getType() == boolean.class) {
                setFieldValue(instance, field, "true");
                fulfilled.add(field);
                index += 1;
            } else if (index >= providedArgs.size() - 1) {
                throw new CommandLineInterfaceException("Missing value for argument '" + argName + "'");
            } else {
                setFieldValue(instance, field, providedArgs.get(index + 1));
                fulfilled.add(field);
                index += 2;
            }
        }
    }

    private Optional<Field> findMatchingField(String needle, List<Field> haystack) {
        return haystack.stream()
            .filter(field -> getPossibleFieldNames(field).contains(needle))
            .findFirst();
    }

    private Set<String> getPossibleFieldNames(Field field) {
        Set<String> possibleNames = new HashSet<>();
        possibleNames.add(getArgName(field));
        for (String alias : field.getAnnotation(Arg.class).aliases()) {
            possibleNames.add(normalizeArgumentName(alias));
        }
        return possibleNames;
    }

    private Object convertArgValue(Field field, String value) {
        try {
            Class<?> type = field.getType();
            return propertyDeserializer.get(value, type);
        } catch (IllegalArgumentException e) {
            String argName = getArgName(field);
            throw new CommandLineInterfaceException("Invalid value for '" + argName + "': " + value);
        }
    }

    private void setFieldValue(Object instance, Field field, String rawValue) {
        Object parsedValue = convertArgValue(field, rawValue);

        if (parsedValue != null) {
            try {
                field.setAccessible(true);
                field.set(instance, parsedValue);
            } catch (IllegalAccessException e) {
                throw new CommandLineInterfaceException("Cannot set field: " + field.getName(), e);
            }
        }
    }

    private Object getFieldValue(Object instance, Field field) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new CommandLineInterfaceException("Cannot get field: " + field.getName(), e);
        }
    }

    private void setDefaultValues(Object instance, List<Field> fields) {
        for (Field field : fields) {
            Arg config = field.getAnnotation(Arg.class);
            if (!config.defaultValue().equals(DEFAULT_VALUE_MARKER)) {
                setFieldValue(instance, field, config.defaultValue());
            }
        }
    }

    private void checkMissingRequiredArguments(Object instance, List<Field> fields) {
        for (Field field : fields) {
            if (isRequired(field) && getFieldValue(instance, field) == null) {
                String argName = getArgName(field);
                throw new CommandLineInterfaceException("Missing required argument '" + argName + "'");
            }
        }
    }

    private boolean isFlag(Field field) {
        Class<?> type = field.getType();
        return type.equals(boolean.class) || type.equals(Boolean.class);
    }

    private boolean isRequired(Field field) {
        Arg config = field.getAnnotation(Arg.class);
        boolean hasDefaultValue = !config.defaultValue().equals(DEFAULT_VALUE_MARKER);

        return !(isFlag(field) || !config.required() || hasDefaultValue);
    }

    /**
     * Normalizes the list of provided arguments so that the notations
     * {@code --a b} and {@code --a=b} both produce {@code [--a, b]}.
     */
    private List<String> processProvidedArgs(String[] argv) {
        return Arrays.stream(argv)
            .flatMap(this::splitProvidedArg)
            .toList();
    }

    private Stream<String> splitProvidedArg(String arg) {
        if (arg.startsWith("-") && arg.contains("=")) {
            String name = arg.substring(0, arg.indexOf("="));
            String value = arg.substring(name.length() + 1);
            return Stream.of(name, value);
        } else {
            return Stream.of(arg);
        }
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
