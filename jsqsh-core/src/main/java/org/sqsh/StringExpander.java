package org.sqsh;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Expands variables in strings. Under the hood, this is using the {@link VelocityEngine} to do the work, so
 * you have the full velocity syntax at your disposal if needed.
 */
public class StringExpander {
    private static final VelocityEngine VELOCITY = new VelocityEngine();
    static {
        VELOCITY.init();
    }

    // Singleton context that wraps system environment variables
    private static final VelocityContext ENV_CONTEXT = new VelocityContext(
            System.getenv().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    /**
     * Expander that expands system environment variables.
     */
    public static final StringExpander ENVIRONMENT_EXPANDER = new StringExpander(ENV_CONTEXT, () -> System.err);

    private final VelocityContext context;

    // A total hack. For error messages, I'd like them to go to the stderr of the session (which can
    // be redirected at any time.  However, for the ENV_EXPANDER there is no session. So, instead, I
    // keep ahold of whatever I have available at the time of construction that can feed me the
    // place to send errors, if needed.
    private final Supplier<PrintStream> stderrSupplier;

    /**
     * Creates an expander that will expand session variables as well as environment variables. If the same variable
     * is present in both, the session will always take precedence.
     *
     * @param session the session from which variables will be expanded.
     */
    public StringExpander(Session session) {
        this(new VelocityContext(session.getVariableManager(), ENV_CONTEXT), () -> session.err);
    }

    public StringExpander(Map<String, Object> variables, Session session) {
        this(new VelocityContext(variables, new VelocityContext(session.getVariableManager(), ENV_CONTEXT)), () -> session.err);
    }

    public StringExpander(Map<String, Object> variables) {
        this(new VelocityContext(variables, ENV_CONTEXT), () -> System.err);
    }

    private StringExpander(VelocityContext context, Supplier<PrintStream> stderrSupplier) {
        this.context = context;
        this.stderrSupplier = stderrSupplier;
    }

    /**
     * Expands variables in a string.
     * @param str the string to expand
     * @return the expanded string
     */
    public String expand(String str) {
        Writer writer = new StringWriter(str.length());
        try {
            VELOCITY.evaluate(context, writer, "<string>", str);
        } catch (Exception e) {
            stderrSupplier.get().println("Error during variable expansion: " + e.getMessage());
        }
        return writer.toString();
    }
}
