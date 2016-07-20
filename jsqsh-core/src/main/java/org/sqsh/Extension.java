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
 *  Definition of a jsqsh extension.
 */
public class Extension {
    
    private static final String EMPTY_STRING_ARRAY[] = new String[0];
    
    private String name;        // Short name of the extension
    private String directory;   // The directory in which the extension is located
    private String []classpath; // Classpath of the extension
    private String []drivers = EMPTY_STRING_ARRAY; // Drivers that can trigger extension being loaded
    private String configClass = null;
    private ClassLoader classloader; // Classloader used to load the extension
    private Properties config;  // Configuration properties for the extension
    private boolean isDisabled = false;
    boolean isLoadOnStart = false;
    private boolean isLoaded = false;
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
                
                configurator.configure(context);
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