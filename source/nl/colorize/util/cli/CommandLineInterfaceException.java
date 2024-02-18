//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

/**
 * Exception that indicates invalid or missing input was provided to an
 * application's command line interface. Used in combination with
 * {@link CommandLineArgumentParser}.
 */
public class CommandLineInterfaceException extends RuntimeException {

    public CommandLineInterfaceException(String message) {
        super(message);
    }

    public CommandLineInterfaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
