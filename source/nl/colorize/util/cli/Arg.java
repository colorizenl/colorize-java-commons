//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a field to represent one of the arguments in a command line
 * interface. Used in conjunction with {@link CommandLineArgumentParser}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Arg {

    /**
     * The command line argument name. If not specified, the field name will
     * also act as the argument name. The argument name is normalized, so a
     * name like "outputDir" will match different variants like "--outputDir",
     * "--outputdir", "-outputdir", and "-output-dir".
     */
    public String name() default "$$default";

    /**
     * List of alternative names for this argument. While {@link #name()}
     * represents this argument's "primary" name, using aliases can be used
     * for things like shorthand versions, or backward compatibility when
     * renaming arguments.
     */
    public String[] aliases() default {};

    /**
     * Indicates whether this is a required command line argument. When false,
     * the argument is considered optional. Failing to provide the argument
     * will result in the default value returned by {@link #defaultValue()},
     * or {@code null} if no default value has been specified.
     */
    public boolean required() default true;

    /**
     * The command line argument's default value that is used when no value for
     * the argument has been provided. If a default value is specified, the
     * argument is considered optional. If not, the argument is considered
     * mandatory.
     * <p>
     * Boolean flags are always considered optional, with a default value of
     * false. Attempting to define a default value for a boolean argument will
     * therefore be ignored.
     */
    public String defaultValue() default "$$default";

    /**
     * Optionally returns usage information, which is printed when no value or
     * an invalid value for the argument is provided.
     */
    public String usage() default "";
}
