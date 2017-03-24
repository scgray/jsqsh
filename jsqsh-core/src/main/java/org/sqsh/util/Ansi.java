package org.sqsh.util;

/**
 * Handy methods for generating ANSI terminal escape sequence
 */
public class Ansi {

    private static final String TERM_ESC = "\033[";

    public static String cursorUp(int nUp) {

        return TERM_ESC + nUp + "A";
    }

    public static String cursorDown(int nDown) {

        return TERM_ESC + nDown + "B";
    }

    public static String cursorRight(int nRight) {

        return TERM_ESC + nRight + "C";
    }

    public static String cursorLeft(int nLeft) {

        return TERM_ESC + nLeft + "D";
    }

    public static String clearScreen() {

        return TERM_ESC + "2J";
    }

    public static String cursorMove(int x, int y) {

        return TERM_ESC + x + ";" + y + "H";
    }

    public static String clearToEnd() {

        return TERM_ESC + "K";
    }

    public static String eraseLine() {

        return TERM_ESC + "2K";
    }
}
