//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
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
public class AnsiColor {

    public static final AnsiColor RED = new AnsiColor("\u001B[31m");
    public static final AnsiColor GREEN = new AnsiColor("\u001B[32m");
    public static final AnsiColor YELLOW = new AnsiColor("\u001B[33m");
    public static final AnsiColor BLUE = new AnsiColor("\u001B[34m");
    public static final AnsiColor MAGENTA = new AnsiColor("\u001B[35m");
    public static final AnsiColor CYAN = new AnsiColor("\u001B[36m");

    public static final AnsiColor RED_BOLD = RED.bold();
    public static final AnsiColor GREEN_BOLD = GREEN.bold();
    public static final AnsiColor YELLOW_BOLD = YELLOW.bold();
    public static final AnsiColor BLUE_BOLD = BLUE.bold();
    public static final AnsiColor MAGENTA_BOLD = MAGENTA.bold();
    public static final AnsiColor CYAN_BOLD = CYAN.bold();

    private String colorCode;
    private boolean bold;

    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_RESET = "\u001B[0m";

    private AnsiColor(String colorCode) {
        this.colorCode = colorCode;
        this.bold = false;
    }

    private AnsiColor bold() {
        AnsiColor result = new AnsiColor(colorCode);
        result.bold = true;
        return result;
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
