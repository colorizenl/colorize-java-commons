//-----------------------------------------------------------------------------
// Colorize MultimediaLib
// Copyright 2009-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

/**
 * Exception that indicates invalid or missing input was provided to an
 * application's command line interface. Using this exception will *not* lead
 * to a stack trace, as it will be used to communicate the error to the caller
 * via the application's usage message.
 */
public class CommandLineInterfaceException extends RuntimeException {

    public CommandLineInterfaceException(String message) {
        super(message, null);
    }
}
