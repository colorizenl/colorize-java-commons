//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.cli;

/**
 * <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape codes</a>
 * to format command line output in a more human-readable way. This class
 * intentionally does not support white or black, since both black-on-white
 * and white-on-black terminals are widely used, so this avoids formatting
 * that would make the text unreadable.
 */
public enum AnsiColor {

    RED("\u001B[31m", false),
    GREEN("\u001B[32m", false),
    YELLOW("\u001B[33m", false),
    BLUE("\u001B[34m", false),
    MAGENTA("\u001B[35m", false),
    CYAN("\u001B[36m", false),

    RED_BOLD(RED, true),
    GREEN_BOLD(GREEN, true),
    YELLOW_BOLD(YELLOW, true),
    BLUE_BOLD(BLUE, true),
    MAGENTA_BOLD(MAGENTA, true),
    CYAN_BOLD(CYAN, true);

    private String colorCode;
    private boolean bold;

    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_RESET = "\u001B[0m";

    private AnsiColor(String colorCode, boolean bold) {
        this.colorCode = colorCode;
        this.bold = bold;
    }

    private AnsiColor(AnsiColor original, boolean bold) {
        this(original.colorCode, bold);
    }

    /**
     * Formats the specified text using this color and styling. Returns the
     * formatting string including ANSI escape codes.
     */
    public String format(String text) {
        StringBuilder buffer = new StringBuilder();
        if (bold) {
            buffer.append(ANSI_BOLD);
        }
        buffer.append(colorCode);
        buffer.append(text);
        buffer.append(ANSI_RESET);
        return buffer.toString();
    }

    /**
     * Formats the specified text using this color and styling, then prints the
     * result to {@code stdout}. Using this method is a shorthand/convenience
     * version of {@code System.out.print(format(text))}.
     */
    public void print(String text) {
        System.out.print(format(text));
    }

    /**
     * Formats the specified text using this color and styling, then prints the
     * result to {@code stdout}. Using this method is a shorthand/convenience
     * version of {@code System.out.println(format(text))}.
     */
    public void println(String text) {
        System.out.println(format(text));
    }
}
