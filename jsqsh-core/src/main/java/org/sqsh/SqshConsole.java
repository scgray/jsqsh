package org.sqsh;

import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.BufferImpl;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.sqsh.jline.JLineCompleter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Wrapper around the JLine LineReader console that provides some
 * additional functionality.
 */
public class SqshConsole {

    /**
     * Enumerates the reason that a call to readLine accepted its input and returned
     * to the caller
     */
    public static enum AcceptCause {

        /**
         * Means that the user's input "normally" was complete.  That is, they
         * ran a command or the terminated the statement with a terminator
         */
        NORMAL,

        /**
         * The user explicitly asked for the statement to be executed (typically
         * via a "CTRL-G").
         */
        EXECUTE
    }

    private SqshContext context;
    private LineReader reader;
    private AcceptCause acceptCause = AcceptCause.NORMAL;

    /**
     * Enables or disables JLine's ability to do multi-line editing.
     */
    private boolean multiLineEnabled = true;

    public SqshConsole(SqshContext context) {

        this.context = context;
        Terminal terminal;
        try {

            terminal = TerminalBuilder.builder()
                    .nativeSignals(true)
                    .build();
        }
        catch (IOException e) {

            System.err.println("Unable to create terminal: " + e.getMessage()
                    + ". Falling back to dumb terminal");
            try {
                terminal = TerminalBuilder.builder()
                        .dumb(true)
                        .build();
            }
            catch (IOException e2) {

                System.err.println("Unable to create dumb terminal: " + e2.getMessage()
                        + ". Giving up");
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        File readlineHistory = new File(context.getConfigDirectory(), "readline_history");
        reader = LineReaderBuilder.builder()
                .appName("jsqsh")
                .terminal(terminal)
                .completer(new JLineCompleter(context))
                .parser(new DefaultParser())
                .variable(LineReader.HISTORY_FILE, readlineHistory.toString())
                .build();

        /*
         * This installs a widget that intercepts \n and attempts to determine if the
         * input is "finished" and should be run or if the user should keep typing
         * the current statement. I hate that I have to do this for all of the keymaps
         * that JLine3 has...
         */
        for (String keyMap : reader.getKeyMaps().keySet()) {

            // Bind to CTRL-M (enter)
            reader.getKeyMaps().get(keyMap).bind(new Reference("jsqsh-accept"), ctrl('M'));
            // Bind CTRL-G to automatically do "go"
            reader.getKeyMaps().get(keyMap).bind(new Reference("jsqsh-go"), ctrl('G'));
        }
        reader.getWidgets().put("jsqsh-accept", new JLineAcceptBufferWidget());
        reader.getWidgets().put("jsqsh-go", new JLineExecuteBufferWidget());

        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
    }

    public String readLine(String prompt) {

        reset();
        return reader.readLine(prompt, null);
    }

    public String readLine(String prompt, Character mask) {

        reset();
        return reader.readLine(prompt, mask);
    }

    public String readLine(String prompt, Character mask, String initialInput) {

        reset();
        return reader.readLine(prompt, mask, initialInput);
    }

    public String readLine(int lineOffset, String prompt, String prompt2, Character mask, String initialInput) {

        reset();
        reader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, prompt2);
        reader.setVariable(LineReader.LINE_OFFSET, lineOffset);
        try {

            return reader.readLine(prompt, mask, initialInput);
        }
        finally {

            reader.setVariable(LineReader.LINE_OFFSET, 0);
            reader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, null);
        }
    }

    /**
     * Toggles whether or not readLine will save its read input to the history
     * @param isHistoryEnabled true if history saving is enabled.
     */
    public void setHistoryEnabled(boolean isHistoryEnabled) {

        if (isHistoryEnabled) {

            reader.setVariable(LineReader.DISABLE_HISTORY, Boolean.FALSE);
        }
        else {

            reader.setVariable(LineReader.DISABLE_HISTORY, Boolean.TRUE);
        }
    }

    /**
     * When called after <code>readLine()</code> returns normally, this indicates
     * the reason that it returned (@see AcceptCause}.
     *
     * @return The reason that the user's input was accepted.
     */
    public AcceptCause getAcceptCause() {

        return acceptCause;
    }

    public void addToHistory(String input) {

        reader.getHistory().add(input);
    }

    public void saveHistory() {

        reader.getHistory().save();
    }

    /**
     * @return true if multi-line editing is enabled when JLine is in use
     *   (during interactive mode)
     */
    public boolean isMultiLineEnabled() {

        return multiLineEnabled;
    }

    /**
     * Changes whether or not multi-line editing is enabled when JLine is in use.
     *
     * @param multiLineEnabled true if multi-line editing is enabled, false
     *    otherwise.
     */
    public void setMultiLineEnabled(boolean multiLineEnabled) {

        this.multiLineEnabled = multiLineEnabled;
    }

    /**
     * @return The terminal being used by the underlying JLine reader.
     */
    public Terminal getTerminal() {

        return reader.getTerminal();
    }

    /**
     * Sets the editing mode for the console reader.  Logically supported names are
     * 'vi' and 'emacs', however any valid keymap supported by JLine3 is supported.
     *
     * @param name The name of the editing mode.
     */
    public void setEditingMode(String name) {

        Map<String, KeyMap<Binding>> keyMaps = reader.getKeyMaps();
        if ("vi".equals(name)) {

            keyMaps.put(LineReader.MAIN, keyMaps.get(LineReader.VIINS));
        }
        else if ("vi-move".equals(name)) {

            keyMaps.put(LineReader.MAIN, keyMaps.get(LineReader.VICMD));
        }
        else if ("vi-insert".equals(name)) {

            keyMaps.put(LineReader.MAIN, keyMaps.get(LineReader.VIINS));
        }
        else if ("emacs".equals(name)) {

            keyMaps.put(LineReader.MAIN, keyMaps.get(LineReader.EMACS));
        }
        else {

            if (keyMaps.containsKey(name)) {

                keyMaps.put(LineReader.MAIN, keyMaps.get(name));
            }
        }
    }

    /**
     * @return The current line editing mode. This should be one of "vi" or "emacs"
     */
    public String getEditingMode() {

        Map<String, KeyMap<Binding>> keyMaps = reader.getKeyMaps();
        KeyMap<Binding> currentKeyMap = keyMaps.get(LineReader.MAIN);
        if (currentKeyMap == keyMaps.get(LineReader.VICMD)
                || currentKeyMap == keyMaps.get(LineReader.VIOPP)) {

            return "vi";
        }
        else if (currentKeyMap == keyMaps.get(LineReader.VICMD)) {

            return "vi-move";
        }
        else if (currentKeyMap == keyMaps.get(LineReader.EMACS)) {

            return "emacs";
        }
        else {

            for (Map.Entry<String, KeyMap<Binding>> e : keyMaps.entrySet()) {

                if (e.getValue() == currentKeyMap) {

                    return e.getKey();
                }
            }
        }

        return "unknown";
    }

    private void reset() {

        acceptCause = AcceptCause.NORMAL;
    }

    private String ctrl(char ch) {

        return Character.toString((char) (ch & 0x1f));
    }

    /**
     * JLine widget that is used to intercept ENTER/RETURN (CTRL-M) and
     * determine if it should "accept" the input, meaning that the user's
     * done typing input and jsqsh should use it, or if it should continue
     * prompting the user for input.
     */
    private class JLineAcceptBufferWidget implements Widget {

        @Override
        public boolean apply() {

            final BufferImpl buffer = reader.getBuffer();

            if (context.getCurrentSession().isInputComplete(buffer.toString(), buffer.cursor())) {

                acceptCause = AcceptCause.NORMAL;
                reader.callWidget(LineReader.ACCEPT_LINE);
            }
            else {

                buffer.write('\n');
            }

            return true;
        }
    }

    private class JLineExecuteBufferWidget implements Widget {

        @Override
        public boolean apply() {

            acceptCause = AcceptCause.EXECUTE;
            reader.callWidget(LineReader.ACCEPT_LINE);
            return true;
        }
    }
}
