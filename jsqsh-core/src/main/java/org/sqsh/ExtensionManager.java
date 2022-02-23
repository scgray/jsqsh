/*
 * Copyright 2007-2022 Scott C. Gray
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sqsh;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sqsh.util.ProcessUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages jsqsh extensions. A jsqsh extension consists of a directory containing any of the following:
 * <ul>
 *   <li> A Commands.xml file that describes the commands to be provided by
 *     the extension
 *   <li> A Variables.xml file that describes the variables to be provided by
 *     the extension
 *   <li> One or more .jar files that implement the extension
 *   <li> A 'jsqsh-extension.conf' file that provides various configuration
 *     values for the extension (see below)
 * </ul>
 *
 * <p>Extensions are, by default, located in <code>$JSQSH_HOME/extensions</code>
 * (for globally available extensions) or <code>$HOME/.jsqsh/extensions</code>
 * (for extensions that are private to the user).   Each subdirectory underneath
 * represents an extension.
 *
 * <h2>jsqsh-extension.conf</h2>
 * <p>
 * The file <code>jsqsh-extension.conf</code> contains configuration properties
 * for the extension.  This file is automatically read at jsqsh start-up time for
 * extensions located in <code>$JSQSH_HOME/extensions</code> or
 * <code>$HOME/.jsqsh/extensions</code>, and may contain the following configuration
 * properties:
 * <ul>
 *   <li> <code>load.on.start</code> - A boolean indicating whether or not the
 *     extension should be automatically loaded when jsqsh starts.  If false
 *     the driver may be loaded using the jsqsh <code>\import</code> command.
 *   <li> <code>load.on.drivers</code> - A comma delimited list of drivers
 *     which, when available, will trigger the extension to be loaded. When
 *     an extension is loaded via this property the extension will also
 *     automatically inherit the classpath that was used to load the JDBC
 *     driver.
 *   <li> <code>classpath.script.win</code> - The name of a script that will
 *     be executed when on a Windows environment when loading the extension.
 *     This script is expected to return (to its stdout) a classpath that
 *     should be used for loading the extensions.
 *   <li> <code>classpath.script.unix</code> - The name of a script that will
 *     be executed when on a Unix (specifically, non-windows) environment when
 *     loading the extension. This script is expected to return (to its stdout)
 *     a classpath that should be used for loading the extensions.
 *   <li> <code>load.config.class</code> - The name of a class that extends
 *     {@link ExtensionConfigurator} that will be instantiated and invoked
 *     when the extension is loaded.  A configurator class can perfom low
 *     level programmatic operations within jsqsh, such as changing
 *     configuration variables. Note that the extension configurator class
 *     also has access to any properties that are set in the <code>jsqsh-extensions.conf</code>
 *     file and, thus additional properties may be present in the file
 *     that are specific to an extension configurator.
 *   <li> <code>load.disabled</code> - If true, the extension will never be
 *     loaded under any circumstances.
 * </ul>
 * When processing the extension configuration file, all environment variables
 * that are set in jsqsh will be available along with the following additional
 * variables:
 * <ul>
 *    <li> <code>${extension.dir}</code> The directory from which the extension
 *       is being loaded
 *    <li> <code>${extension.name}</code> The name of the extension
 * </ul>
 */
public class ExtensionManager {

    static final Logger LOG = Logger.getLogger(ExtensionManager.class.getName());

    public static final String EXTENSIONS_DIR = "extensions";
    public static final String CONFIG_FILE = "jsqsh-extension.conf";

    public static final String LOAD_DISABLED = "load.disabled";
    public static final String LOAD_ON_START = "load.on.start";
    public static final String LOAD_ON_DRIVERS = "load.on.drivers";
    public static final String LOAD_CONFIG_CLASS = "load.config.class";
    public static final String CLASSPATH_SCRIPT_WIN = "classpath.script.win";
    public static final String CLASSPATH_SCRIPT_UNIX = "classpath.script.unix";

    static final String[] EMPTY_STRING_ARRAY = new String[0];

    SqshContext sqshContext;
    private VelocityEngine velocity;
    private final VelocityContext velocityContext;
    private final Map<String, Extension> extensions = new HashMap<String, Extension>();

    public ExtensionManager(SqshContext sqshContext) {
        this.sqshContext = sqshContext;
        // We use velocity to expand the variables in the property file,
        // so we need to initialize a new context here.
        try {
            velocity = new VelocityEngine();
            velocity.init();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to initialize velocity", e);
        }

        velocityContext = new VelocityContext(
                System.getenv().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        // First load all global extensions
        String jsqshHome = System.getenv("JSQSH_HOME");
        if (jsqshHome != null) {
            importExtensions(new File(jsqshHome, EXTENSIONS_DIR));
        }

        // Next load all private extensions for the user
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            importExtensions(new File(new File(userHome, ".jsqsh"), EXTENSIONS_DIR));
        }
    }

    /**
     * This method should be called when a JDBC driver is loaded for which an extension is set to auto-load via the
     * load.on.drivers property
     *
     * @param loader The classloader that was used to load the JDBC driver
     * @param drivername The name of the driver that was loaded
     * @throws ExtensionException Thrown if one or more extensions failed to load.
     */
    public void triggerAutoExtensionsForDriver(ClassLoader loader, String drivername) throws ExtensionException {
        StringBuilder sb = new StringBuilder();

        for (Extension ext : extensions.values()) {
            if (ext.isLoadedByDriver(drivername)) {
                try {
                    ext.load(sqshContext, loader);
                } catch (ExtensionException e) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append("Attempt to auto-load extension \"" + ext.getName() + "\" by driver \"" + drivername
                            + "\" failed: " + e.getMessage());
                }
            }
        }

        if (sb.length() > 0) {
            throw new ExtensionException(sb.toString());
        }
    }

    /**
     * Returns an extension by name
     *
     * @param name The name of the extension
     * @return The extension definition, or null if it isn't defined
     */
    public Extension getExtension(String name) {
        return extensions.get(name);
    }

    /**
     * @return The set of extensions currently available
     */
    public Extension[] getExtensions() {
        Extension[] exts = extensions.values().toArray(new Extension[0]);
        Arrays.sort(exts, Comparator.comparing(Extension::getName));
        return exts;
    }

    /**
     * Attempts to load an extension
     *
     * @param name If the name is a short extension name, then it is expected to live in $JSQSH_HOME/extensions,
     *         otherwise this may specify the full path to an extension to be loaded and the name of the extension will
     *         be base name of the directory in which it is defined.
     */
    public void load(String name) throws ExtensionException {
        Extension extension;
        File dirName = new File(name);

        if (dirName.isAbsolute()) {
            if (!dirName.exists()) {
                throw new ExtensionException(dirName + " does not exists");
            } else if (!dirName.isDirectory()) {
                throw new ExtensionException(dirName + " is not a directory");
            }
            extension = loadExtensionDefinition(dirName);
        } else {
            extension = extensions.get(name);
        }

        if (extension == null) {
            throw new ExtensionException("Extension \"" + name + "\" does not exist");
        }
        extension.load(sqshContext, null);
    }

    private Extension loadExtensionDefinition(File extensionDir) {
        LOG.fine("Inspecting extension directory: " + extensionDir);

        String extensionName = extensionDir.getName();
        Properties properties = new Properties();

        File configFile = new File(extensionDir, CONFIG_FILE);
        if (configFile.exists()) {
            Properties tmpProperties = new Properties();
            LOG.fine("  Loading configuration " + configFile);
            try (InputStream configIn = new FileInputStream(configFile)) {
                tmpProperties.load(configIn);
            } catch (IOException e) {
                LOG.warning("Error loading " + configFile + ": " + e.getMessage());
            }

            // Give the velocity expander a little bean containing metadata
            // about this extension
            ExtensionBean bean = new ExtensionBean(extensionName, extensionDir.toString());
            Map<String, Object> beans = new HashMap<>();
            beans.put("extension", bean);
            VelocityContext vCtx = new VelocityContext(beans, velocityContext);
            StringWriter writer = new StringWriter();

            // Expand the properties of any variables that may have been referenced
            // in the extension
            for (Object key : tmpProperties.keySet()) {
                String value = tmpProperties.getProperty((String) key);
                if (value != null) {
                    writer.flush();
                    writer.getBuffer().setLength(0);
                    velocity.evaluate(vCtx, writer, "<string>", value);
                    value = writer.toString();
                }
                properties.put(key, value);
            }
        }

        return new Extension(extensionName, extensionDir.toString(), properties);
    }

    /**
     * Runs a program / script and interprets its output as a classpath
     *
     * @param script The script to run
     * @return The classpath that was returned from the script
     * @throws CommandImportException Thrown if the execution of the script fails, or the script returns an error
     *                                (non-zero) exit code) or the output is a malformed classpath.
     */
    static String[] getClasspathFromScript(File script) throws ExtensionException {
        LOG.log(Level.FINE, "Executing classpath script: " + script);
        ProcessBuilder pb = new ProcessBuilder().command(script.toString());
        int rc = 0;

        try {
            Process proc = pb.start();
            ProcessUtils.Consumer stdOut = new ProcessUtils.Consumer(proc.getInputStream());
            ProcessUtils.Consumer stdErr = new ProcessUtils.Consumer(proc.getErrorStream());
            stdOut.start();
            stdErr.start();

            rc = proc.waitFor();
            if (rc != 0) {
                throw new ExtensionException(
                        "Classpath script '" + script + "' exited with error #" + rc + ": \n" + stdErr.getOutput());
            }

            String cp = stdOut.getOutput();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Output from script '" + script + "' is");
                LOG.fine(cp);
            }

            String[] parts = cp.split(File.pathSeparator);
            String[] entries = new String[parts.length];
            int idx = 0;
            for (String part : parts) {
                entries[idx++] = part.trim();
            }

            return entries;
        } catch (ExtensionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtensionException("Error running classpath script " + script + ": " + e.getMessage(), e);
        }
    }

    /**
     * Small bean that is made available during variable expansion of the jsqsh-extension.conf configuration file.
     */
    public static class ExtensionBean {

        private String name;
        private String directory;

        public ExtensionBean(String name, String directory) {
            this.name = name;
            this.directory = directory;
        }


        public String getName() {
            return name;
        }


        public void setName(String name) {
            this.name = name;
        }


        public String getDir() {
            return directory;
        }

        public void setDir(String directory) {
            this.directory = directory;
        }
    }

    /**
     * Given a directory known to contain extension subdirectories, iterates through each subdirectory, loading the
     * extension definition from the jsqsh-extensions.conf file and, if necessary, loading the extension itself.
     *
     * @param extensionsDir A directory containing zero or more extensions
     */
    private void importExtensions(File extensionsDir) {
        File[] extensionSubDirs = extensionsDir.listFiles(File::isDirectory);

        // Inspect all of the extensions
        if (extensionSubDirs != null) {
            for (File extensionDir : extensionSubDirs) {
                Extension extension = loadExtensionDefinition(extensionDir);
                extensions.put(extension.getName(), extension);
                if (extension.isLoadOnStart) {
                    try {
                        extension.load(sqshContext, null);
                    } catch (ExtensionException e) {
                        LOG.warning("Failed to import extension \"" + extension.getName() + "\" from "
                                + extension.getDirectory() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}
