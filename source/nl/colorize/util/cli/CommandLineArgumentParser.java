//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import nl.colorize.util.DateParser;
import nl.colorize.util.FileUtils;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
    private Map<Class, TypeMapper> typeMappers;

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
        this.typeMappers = new HashMap<>();

        registerStandardTypeMappers();
    }

    /**
     * Registers the specified function to convert command line arguments,
     * which are always strings, to the correct type. By default, type mappers
     * are available for all primitive types. This method can be used to add
     * additional type mappers for custom types.
     *
     * @throws IllegalArgumentException if a type mapper has already been
     *         registered for the specified type.
     */
    public <T> void registerTypeMapper(Class<T> type, Function<String, T> mapper) {
        Preconditions.checkArgument(!typeMappers.containsKey(type),
            "Type mapper already registered for " + type.getName());

        TypeMapper<T> typeMapper = value -> mapper.apply(value);
        typeMappers.put(type, typeMapper);
    }

    private void registerStandardTypeMappers() {
        registerTypeMapper(String.class, value -> value);
        registerTypeMapper(boolean.class, value -> value.equalsIgnoreCase("true"));
        registerTypeMapper(int.class, Integer::parseInt);
        registerTypeMapper(long.class, Long::parseLong);
        registerTypeMapper(float.class, Float::parseFloat);
        registerTypeMapper(double.class, Double::parseDouble);
        registerTypeMapper(File.class, FileUtils::expandUser);
        registerTypeMapper(Date.class, DateParser::parse);
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

        out.println("Usage: " + applicationName);

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
        out.println(cause.getMessage());
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
        return name;
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

        Preconditions.checkArgument(typeMappers.containsKey(type),
            "No type mapper registered for " + type.getName());

        if (value == null) {
            return null;
        }

        try {
            return typeMappers.get(type).convert(value);
        } catch (NumberFormatException e) {
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

    /**
     * Converts provided argument values, which are always strings, to the
     * correct type. Throws {@link CommandLineInterfaceException} to indicate
     * error messages for invalid values.
     */
    @FunctionalInterface
    private static interface TypeMapper<T> {

        public T convert(String value) throws CommandLineInterfaceException;
    }
}
