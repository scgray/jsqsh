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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Definition of a jsqsh extension. All extensions that are known to jsqsh,
 * either by explicitly importing (via the <code>\import</code> command)
 * or by way of being defined in <code>$JSQSH_HOME/extensions</code> 
 * or <code>$HOME/.jsqsh/extensions</code> will have a definition, however
 * just having a definition doesn't mean that the extension has been loaded.
 * That is, the jars may not yet have been loaded, the commands may not yet
 * be defined, etc.   The extension's {@link #isLoaded()} method may be
 * used to determine if it has been loaded and the {@link #load(SqshContext, ClassLoader)}
 * method will cause it to be loaded (note that only the {@link ExtensionManager}
 * is allowed to load an extension
 */
public class Extension {
    
    private static final String EMPTY_STRING_ARRAY[] = new String[0];
    
    /**
     * The short name of the extension.  This corresponds to the name of the
     * directory in which it is found
     */
    private String name;
    
    /**
     * The directory from which the extension was/can be loaded.
     */
    private String directory;

    /**
     * The classpath used to load the extension. This will be null until the 
     * point at which the extension is loaded.  For extensions that are loaded
     * in response to a JDBC driver being loaded, this will not reflect the
     * classpath that was used to load the JDBC driver as well.
     */
    private String []classpath;
    
    /**
     * A list of driver definitions that can trigger the extension to be 
     * loaded. This will never be null, but may be empty.
     */
    private String []drivers = EMPTY_STRING_ARRAY;

    /**
     * If not null, the name of a class implementing {@link ExtensionConfigurator}
     * that will be invoked when the extension is loaded in order to configure
     * the extension.
     */
    private String configClass = null;
    
    /**
     * The classloader used to load the extension. This will be null if the
     * extension has not yet been loaded.
     */
    private ClassLoader classloader;
    
    /**
     * The properties from <code>jsqsh-extension.conf</code> that were used
     * to configure the extension.  Note that these are kept here for reference
     * only, and the configuration properties are turned into discrete class
     * member variables (e.g. isLoadOnStart) when the extension is created.
     * 
     * <p>One interesting note: the Extension class is passed to the 
     * {@link ExtensionConfigurator} when it is configured and, as a result, 
     * these properties are available to it, so it is possible to pass 
     * additional configuration information via the jsqsh-extensions.conf 
     * into the configurator class if desired.
     */
    private Properties config;

    /**
     * True if the extension is disabled
     */
    private boolean isDisabled = false;
    
    /**
     * True if the extension should be loaded when jsqsh starts up
     */
    boolean isLoadOnStart = false;

    /**
     * True if this extension has been loaded.  Note that this will be true
     * even if the extension threw an exception and failed to load.
     */
    private boolean isLoaded = false;
    
    /**
     * If not null, contains the path to a script that will be executed 
     * when the extension is loaded, that is expected to return a classpath
     * that should be used to load the extension jars.
     */
    private String scriptPath = null;
    
    protected Extension (String name, String directory, Properties config) {
        
        this.name = name;
        this.directory = directory;
        this.config = config;
        
        String str = config.getProperty(ExtensionManager.LOAD_DISABLED);
        if (str != null && "true".equals(str)) {
            
            isDisabled = true;
        }
        
        str = config.getProperty(ExtensionManager.LOAD_ON_START);
        if (str != null && "true".equals(str)) {
            
            isLoadOnStart = true;
        }
        
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            
            scriptPath = config.getProperty(ExtensionManager.CLASSPATH_SCRIPT_WIN);
        }
        else {
            
            scriptPath = config.getProperty(ExtensionManager.CLASSPATH_SCRIPT_UNIX);
        }
        
        str = config.getProperty(ExtensionManager.LOAD_ON_DRIVERS);
        if (str != null) {
            
            drivers = str.split(",");
            for (int i = 0; i < drivers.length; i++) {
                
                drivers[i] = drivers[i].trim();
            }
        }
        
        configClass = config.getProperty(ExtensionManager.LOAD_CONFIG_CLASS);
    }
    
    /**
     * @return The short name of the extension
     */
    public String getName() {
    
        return name;
    }
    
    /**
     * @return The directory in which the extension lives
     */
    public String getDirectory() {
    
        return directory;
    }
    
    /**
     * @return The classpath that is used for the extension.  This will
     *   return null if the extension has not yet been loaded.
     */
    public String[] getClasspath() {
    
        return classpath;
    }
    
    /**
     * @return The classloader used to load the extension.  This will
     *   return null if the extension has not yet been loaded.
     */
    public ClassLoader getClassloader() {
    
        return classloader;
    }
    
    /**
     * @return The configuration properties for the extension
     */
    public Properties getConfig() {
    
        return config;
    }
    
    /**
     * @return true if the extension has been loaded
     */
    public boolean isLoaded() {
    
        return isLoaded;
    }

    /**
     * @return true if this extension is disabled
     */
    public boolean isDisabled() {
    
        return isDisabled;
    }

    /**
     * @return true if this extension is to be loaded upon startup
     */
    public boolean isLoadOnStart() {
    
        return isLoadOnStart;
    }

    /**
     * @return The name of the classpath script that is to be executed
     */
    public String getClasspathScript() {
    
        return scriptPath;
    }
    
    /**
     * @return The set of drivers that can trigger this to be loaded. If none
     *   are registered an empty array is returned.
     */
    public String[] getLoadOnDrivers() {
        
        return drivers;
    }
    
    /**
     * Returns whether or not this extension will be triggered to be loaded
     * by a specific JDBC driver.
     * 
     * @param name The name of the driver
     * @return true if the driver should trigger this extension to be loaded
     */
    public boolean isLoadedByDriver (String name) {
        
        for (String driver : drivers) {
            
            if (driver.equals(name)) {
                
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * @return The name of the class that will be used to configure the
     *   extension when it is loaded or null if no class is configured.
     */
    public String getConfigurationClass() {
        
        return configClass;
    }
    
    
    /**
     * Loads the extension into a context
     * @param context The context to load it into
     * @throws ExtensionException Thrown if something goes wrong
     */
    protected void load(SqshContext context, ClassLoader parentClassloader)
        throws ExtensionException {
        
        if (parentClassloader == null) {
            
            parentClassloader = this.getClass().getClassLoader();
        }
        
        // Don't load this if it was already loaded or is currently disabled.
        if (isDisabled || isLoaded) {
            
            if (isDisabled) 
                ExtensionManager.LOG.fine("Load of extension " + name + " is disabled");
            else
                ExtensionManager.LOG.fine("Extension " + name + " is already loaded (or previously failed)");

            return;
        }
        
        ExtensionManager.LOG.fine("Loading extension \"" + name + "\" from " + directory);
        
        // Even if we fail after this point, we consider ourselves loaded
        isLoaded = true;
    
        /*
         * First, find all of the jars contained in the extension directory
         */
        File extensionDir = new File(directory);
        File []extensionJarsFiles = extensionDir.listFiles(new FilenameFilter() {
            
            @Override
            public boolean accept(File dir, String name) {
            
                return (name.endsWith(".jar"));
            }
        });
        
        // Build our final classpath
        String []scriptPaths = ExtensionManager.EMPTY_STRING_ARRAY;
        if (scriptPath != null) {
            
            scriptPaths = ExtensionManager.getClasspathFromScript(new File(scriptPath));
        }
        
        ExtensionManager.LOG.fine("  Classpath is:");

        List<String> entries = new ArrayList<String>();
        
        // First, add the jars that are in the extension directory
        for (File extensionJar : extensionJarsFiles) {
            
            String str = extensionJar.toString();
            ExtensionManager.LOG.fine("      " + str);
            entries.add(str);
        }
        
        // Next, add the entries from the classpath script
        for (String scriptPath : scriptPaths) {
            
            // Expand "*"
            if (scriptPath.endsWith("/*") || scriptPath.endsWith("\\*")) {
                
                File dir = new File(scriptPath.substring(0, scriptPath.length()-2));
                File []jars = dir.listFiles(new FilenameFilter() {
                    
                    @Override
                    public boolean accept(File dir, String name) {
                    
                        return name.endsWith(".jar") || name.endsWith(".zip");
                    }
                });
                
                for (File jarFile : jars) {
                    
                    String str = jarFile.toString();
                    ExtensionManager.LOG.fine("      " + str);
                    entries.add(str);
                }
            }
            else {
                
                ExtensionManager.LOG.fine("      " + scriptPath);
                entries.add(scriptPath);
            }
        }
        
        this.classpath = entries.toArray(new String[0]);
        URL urls[] = new URL[this.classpath.length];
        for (int i = 0; i < classpath.length; i++) {
            
            urls[i] = toURL(classpath[i]);
        }

        this.classloader = new URLClassLoader(urls, parentClassloader);
        
        CommandManager cm = context.getCommandManager();
        try {
            
            cm.importCommands(this.classloader, this.directory);
        }
        catch (CommandImportException e) {
            
            throw new ExtensionException(e.getMessage(), e);
        }
        
        // Is there a variables definition file? If so, load it!
        File vars = new File(this.directory, "Variables.xml");
        if (vars.exists()) {
            
            VariableManager vm = context.getVariableManager();
            FileInputStream in = null;
            try {
                in = new FileInputStream(vars);
                vm.load(this.classloader, vars.toString(), in);
            }
            catch (IOException e) {
                
                throw new ExtensionException("Failed to read variable definition file: "
                    + vars.toString() + ": " + e.getMessage());
            }
            finally {
                
                if (in != null) 
                    try { in.close(); } catch (IOException e2) { /* IGNORE */ }
            }
        }
        
        // If there was a configurator, then create it and invoke it.
        if (configClass != null) {
            
            ExtensionConfigurator configurator = null;
            try {
                
                Class<? extends ExtensionConfigurator> clazz =
                    classloader.loadClass(configClass).asSubclass(ExtensionConfigurator.class);
                configurator = clazz.newInstance();
            }
            catch (Exception e) {
                
                throw new ExtensionException("Failed to create configuration class "
                    + configClass + ": " + e.getMessage(), e);
            }
            
            try {
                
                configurator.configure(context, this);
            }
            catch (Exception e) {
                
                throw new ExtensionException("Extension configurator "
                    + configClass + " failed due to: " + e.getMessage(), e);
            }
        }
    }
    
    private URL toURL(String path) {
        
        try {
            
            return new File(path).toURI().toURL();
        }
        catch (MalformedURLException e) {
            
            ExtensionManager.LOG.severe("Malformed URL " + path + ": " + e.getMessage());
            return null;
        }
    }
}