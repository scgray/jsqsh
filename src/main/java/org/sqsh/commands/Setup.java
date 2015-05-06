/*
 * Copyright 2007-2012 Scott C. Gray
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
package org.sqsh.commands;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.sqsh.Command;
import org.sqsh.ConnectionDescriptor;
import org.sqsh.ConnectionDescriptorManager;
import org.sqsh.SQLConnectionContext;
import org.sqsh.SQLDriver;
import org.sqsh.SQLDriverManager;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.SQLDriver.DriverVariable;
import org.sqsh.analyzers.ANSIAnalyzer;
import org.sqsh.analyzers.NullAnalyzer;
import org.sqsh.analyzers.PLSQLAnalyzer;
import org.sqsh.analyzers.SQLAnalyzer;
import org.sqsh.analyzers.TSQLAnalyzer;
import org.sqsh.input.ConsoleLineReader;
import org.sqsh.options.Argv;

public class Setup extends Command {
    
    private static class Options extends SqshOptions {
        
        @Argv(program="\\setup", min=0, max=1, usage=" [connections | drivers]")
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {

        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions opts) throws Exception {
        
        Options options = (Options)opts;
        ConsoleLineReader in = session.getContext().getConsole();
        PrintStream out = AnsiConsole.out();
        
        Ansi cls = new Ansi();
        cls.eraseScreen();
        cls.cursor(0, 0);
        
        if (options.arguments.size() > 0) {
            
            String path = options.arguments.get(0);
            if (path.equals("connections")) {
                
                doConnectionWizard(session, out, in, cls);
            }
            else if (path.equals("drivers")) {
                
                doDriverWizard(session, out, in, cls);
            }
            else {
                
                session.err.println("Unrecognized setup type \"" + path + "\"");
            }
        }
        else {
            
            doWelcome(session, out, in, cls);
        }
            
        return 0;
    }
    
    public void doWelcome(Session session, PrintStream out, ConsoleLineReader in, Ansi cls) throws Exception {
        
        boolean done = false;
        
        while (! done) {
            
            out.print(cls);
            out.println("JSQSH SETUP WIZARD");
            out.println();
            out.println("Welcome to the jsqsh setup wizard! This wizard provides a (crude) menu");
            out.println("driven interface for managing several jsqsh configuration files. These");
            out.println("files are all located in $HOME/.jsqsh, and the name of the file being");
            out.println("edited by a given screen will be indicated on the title of the screen");
            out.println();
            out.println("Note that many wizard screens require a relative large console screen");
            out.println("size, so you may want to resize your screen now.");
            out.println();
            out.println("(C)onnection management wizard");
            out.println("   The connection management wizard allows you to define named connections");
            out.println("   using any JDBC driver that jsqsh recognizes. Once defined, jsqsh only");
            out.println("   needs the connection name in order to establish a JDBC connection");
            out.println();
            out.println("(D)river management wizard");
            out.println("   The driver management wizard allows you to introduce new JDBC drivers");
            out.println("   to jsqsh, or to edit the definition of an existing driver. The most");
            out.println("   common activity here is to provide the classpath for a given JDBC driver");
            out.println();
            
            String str = in.readline("Choose (Q)uit, (C)onnection wizard, or (D)river wizard: ", false);
            str = str.trim();
            if ("q".equalsIgnoreCase(str) || "b".equalsIgnoreCase(str)) {
                
                done = true;
            }
            else if ("d".equalsIgnoreCase(str)) {
                
                done = doDriverWizard(session, out, in, cls);
            }
            else if ("c".equalsIgnoreCase(str)) {
                
                done = doConnectionWizard(session, out, in, cls);
            }
        }
        
        out.println();
        out.println("You may re-enter the jsqsh setup wizard at any time with the \\setup command");
        out.println("or by starting jsqsh with the --setup option");
        out.println();
    }
    
    public boolean doConnectionWizard(Session session, PrintStream out, ConsoleLineReader in, Ansi cls) throws Exception {
        
        SQLDriverManager driverMan = session.getDriverManager();
        boolean done = false;
        boolean doQuit = false;
        while (! done) {
        
            out.print(cls);
            out.println("JSQSH CONNECTION WIZARD - (edits $HOME/.jsqsh/connections.xml");
            
            ConnectionDescriptor []connDescs = 
                session.getConnectionDescriptorManager().getAll();
            
            Arrays.sort(connDescs);
            
            if (connDescs == null || connDescs.length == 0) {
                
                done = doConnectionChooseDriver(session, out, in, cls);
                continue;
            }
            else {
                
                out.println("The following connections are currently defined:");
                out.println();
                out.println("     Name                 Driver     Host                           Port");
                out.println("---  -------------------- ---------- ------------------------------ ------");
                
                boolean hasStar = false;
                for (int i = 0; i < connDescs.length; i++) {
                    
                    ConnectionDescriptor desc = connDescs[i];
                    SQLDriver driver = driverMan.getDriver(desc.getDriver());
                    
                    if (desc.isAutoconnect()) {
                        
                        hasStar = true;
                    }
                    
                    String server = desc.getServer();
                    if (server == null) {
                        
                        server = driver.getVariable(SQLDriver.SERVER_PROPERTY);
                    }
                    
                    server = limit(server, 30);
                    
                    int port = desc.getPort();
                    String portStr = null;
                    if (port < 0) {
                        
                        portStr = driver.getVariable(SQLDriver.PORT_PROPERTY);
                        if (portStr == null) {
                            
                            portStr = "-";
                        }
                    }
                    else {
                        
                        portStr = Integer.toString(port);
                    }
                    
                    out.printf("%3d %s%-20s %-10s %-30s %6s\n",
                        i+1, 
                        desc.isAutoconnect() ? "*" : " ",
                        limit(desc.getName(), 20),
                        limit(desc.getDriver(), 10),
                        server,
                        portStr);
                    
                }
                
                if (hasStar) {
                    
                    out.println();
                    out.println("  * = Connection is set for autoconnect");
                }
            }
            out.println();
            
            if (connDescs == null || connDescs.length > 0) {
                
                out.println("Enter a connection number above to edit the connection, or:");
            }
            out.flush();
            
            String str = in.readline("(B)ack, (Q)uit, or (A)dd connection: ", false);
            str = str.trim();
            
            if (str.equalsIgnoreCase("b")) {
                
                done = true;
            }
            else if (str.equalsIgnoreCase("q")) {
                
                doQuit = done = true;
            }
            else if (str.equalsIgnoreCase("a")) {
                
                doQuit = doConnectionChooseDriver(session, out, in, cls);
                done = doQuit;
            }
            else {
                
                int id = toInt(str);
                if (id >= 1 && id <= connDescs.length) {
                    
                    doQuit = doConfigConnection(session, out, in, cls, 
                        connDescs[id-1], driverMan.getDriver(connDescs[id-1].getDriver()));
                    done = doQuit;
                }
            }
        }
        
        return doQuit;
    }
    
    /**
     * Render the "add" screen
     * @throws Exception
     */
    public boolean doConnectionChooseDriver(Session session, PrintStream out, ConsoleLineReader in, Ansi cls) throws Exception {
        
        SQLDriverManager driverMan = session.getDriverManager();
        boolean done = false;
        boolean doQuit = false;
        
        List<SQLDriver> drivers = new ArrayList<SQLDriver>();
        List<SQLDriver> unavail = new ArrayList<SQLDriver>();
        
        SQLDriver all[] = driverMan.getDrivers();
        Arrays.sort(all);
        for (int i = 0; i < all.length; i++) {
            
            if (all[i].isAvailable()) {
                
                drivers.add(all[i]);
            }
            else {
                
                unavail.add(all[i]);
            }
        }
        drivers.addAll(unavail);
        
        while (!done) {
            
            out.print(cls);
            
            out.println("JSQSH CONNECTION WIZARD - (edits $HOME/.jsqsh/connections.xml)");
            out.println();
            out.println("Choose a driver for use by your new connection");
            out.println();
            
            out.println("     Name             Target               Class");
            out.println("---  ---------------- -------------------- --------------------------------------------------");
            
            for (int i = 0; i < drivers.size(); i++) {
                
                SQLDriver driver = drivers.get(i);
                out.printf("%3d %s%-15s %-20s %-50s\n", 
                    i+1,
                    (driver.isAvailable() ? "*" : " "),
                    crop(driver.getName(), 15),
                    crop(driver.getTarget(), 20),
                    limitFront(driver.getDriverClass(), 50));
            }
            out.println();
            out.println("  * = Driver is availabe. If a driver is unavailable you may choose (D) below ");
            out.println("      to jump to the driver wizard to provide a classpath");
            
            out.println();
            out.flush();
            String str = in.readline("Enter the driver number, (D)river wizard, (B)ack or (Q)uit: ", false);
            str = str.trim();
            if (str.equalsIgnoreCase("b")) {
                
                done = true;
            }
            else if (str.equalsIgnoreCase("q")) {
                
                done = true;
                doQuit = true;
            }
            else if (str.equalsIgnoreCase("d")) {
                
                doQuit = doDriverWizard(session, out, in, cls);
                if (doQuit) {
                    
                    done = true;
                }
            }
            else {
                
                int driver = toInt(str);
                if (driver >= 1 && driver <= drivers.size()) {
                    
                    doQuit = doConfigConnection(session, out, in, cls, null, drivers.get(driver-1));
                    done = true;
                }
            }
        }
        
        return doQuit;
    }
    
    public boolean doConfigConnection(Session session, PrintStream out, ConsoleLineReader in, Ansi cls, 
        ConnectionDescriptor conDesc, SQLDriver driver) throws Exception {
        
        boolean doQuit = false;
        boolean done = false;
        boolean isNew = (conDesc == null);
        
        if (conDesc == null) {
            
            conDesc = new ConnectionDescriptor("_temp_");
            conDesc.setDriver(driver.getName());
        }
        else {
            
            conDesc = (ConnectionDescriptor) conDesc.clone();
        }
        
        List<DriverVariable> vars = driver.getVariableDescriptions();
        int longestName = 0;
        for (int i = 0; i < vars.size(); i++) {
            
            String name = vars.get(i).getDisplayName();
            if (name.length() > longestName) {
                
                longestName = name.length();
            }
        }
        if (longestName < 15) {
            
            longestName = 15;
        }
        
        String format = "%-3d %" + longestName + "s : %s\n";
        
        while (! done) {
            
            out.print(cls);
            out.println("JSQSH CONNECTION WIZARD - (edits $HOME/.jsqsh/connections.xml)");
            out.println();
            out.println("The following configuration properties are supported by this driver.");
            out.println();
            
            out.printf("    %" + longestName + "s : %s\n", "Connection name", conDesc.getName());
            out.printf("    %" + longestName + "s : %s\n", "Driver", driver.getTarget());
            out.printf("    %" + longestName + "s : %s\n", "JDBC URL", driver.getUrl());
            out.println();
            out.println("Connection URL Variables");
            out.println("------------------------");
            
            if (vars.size() == 0) {
                
                out.println("None");
            }
            
            int idx = 0;
            for (; idx < vars.size(); idx++) {
                
                DriverVariable var = vars.get(idx);
                String value = conDesc.getValueOf(var.getName());
                if (value == null) {
                    
                    value = var.getDefaultValue();
                }
                
                if (value != null && var.getName().equals("password")) {
                    
                    String stars = "";
                    for (int j = 0; j < value.length(); j++) {
                        
                        stars = stars + "*";
                    }
                    value = stars;
                }
                
                out.printf(format, idx+1, vars.get(idx).getName(), value == null ? "" : value);
            }
            
            out.printf(format, idx+1, "Autoconnect", (conDesc.isAutoconnect() ? "true" : "false"));
            int autoConnectAt = idx++;
            
            out.println();
            out.println("JDBC Driver Properties");
            out.println("------------------------");
            String props[] = conDesc.getPropertiesMap().keySet().toArray(new String[0]);
            if (props.length == 0) {
                
                out.println("None");
            }
            Arrays.sort(props);
            int propsStartAt = idx;
            for (int i = 0; i < props.length; i++) {
                
                out.printf(format, idx+1, props[i], conDesc.getPropertiesMap().get(props[i]));
                ++idx;
            }
            
            out.println();
            out.println("Enter a number to change a given configuration property, or");
            out.flush();
            
            String prompt = (isNew 
                ? "(T)est, (B)ack, (Q)uit, Add (P)roperty, or (S)ave: "
                : "(T)est, (D)elete, (B)ack, (Q)uit, Add (P)roperty, or (S)ave: ");
            String str = in.readline(prompt, false);
            
            str = str.trim();
            if (str.equalsIgnoreCase("b")) {
                
                done = true;
            }
            if (str.equalsIgnoreCase("q")) {
                
                done = true;
                doQuit = true;
            }
            if (str.equalsIgnoreCase("t")) {
                
                doTest(session, out, in, conDesc);
            }
            if (! isNew && str.equalsIgnoreCase("d")) {
                
                ConnectionDescriptorManager connMan = session.getConnectionDescriptorManager();
                
                out.println();
                str = in.readline("Are you sure (Y/N)? ", false);
                if ("y".equalsIgnoreCase(str)) {
                    
                    connMan.remove(conDesc.getName());
                    connMan.save();
                    done = true;
                }
            }
            if (str.equalsIgnoreCase("s")) {
                
                out.println();
                
                boolean doSave = true;
                if (conDesc.getName().equals("_temp_")) {
                    
                    str = "";
                    while (str.length() == 0) {
                        
                        str = in.readline("Please provide a connection name: ", false);
                        str = str.trim();
                    }
                    conDesc.setName(str);
                }
                else {
                
                    str = in.readline("Are you sure (Y/N)? ", false);
                    doSave = "y".equalsIgnoreCase(str);
                }
                
                if (doSave) {
                    
                    ConnectionDescriptorManager connMan = session.getConnectionDescriptorManager();
                    connMan.put(conDesc);
                    connMan.save();
                    done = true;
                }
            }
            if (str.equalsIgnoreCase("p")) {
                
                // Hack to re-used the JDBC driver property screen
                SQLDriver drv = session.getDriverManager().getDriver(conDesc.getDriver()).copy();
                drv.getDriverProperties().clear();
                newDriverProperty(session, out, in, cls, drv);
                
                for (Entry<String, String> e : drv.getDriverProperties().entrySet()) {
                    
                    conDesc.setProperty(e.getKey(), e.getValue());
                }
            }
            else {
                
                int val = toInt(str);
                if (val >= 1 && val <= vars.size()) {
                    
                    DriverVariable var = vars.get(val-1);
                    boolean isPassword = var.getName().equals(SQLDriver.PASSWORD_PROPERTY);
                    
                    out.println();
                    out.println("Please enter a new value:");
                    if (isPassword) {
                        
                        str = in.readPassword(var.getName() + ": ");
                    }
                    else {
                        
                        str = in.readline(var.getName() + ": ", false);
                    }
                    
                    str = str.trim();
                    conDesc.setValueOf(var.getName(), str);
                }
                else if (val == autoConnectAt+1) {
                        
                    conDesc.setAutoconnect(! conDesc.isAutoconnect());
                }
                else if (props.length > 0 && 
                    val >= propsStartAt && val <= propsStartAt + props.length) {
                    
                    String prop = props[(val-1) - propsStartAt];
                    out.println();
                    out.println("An empty value removes the property definition");
                    str = in.readline("Enter new value for \"" + prop + "\": ", false);
                    str = str.trim();
                    if (str.length() == 0) {
                        
                        conDesc.removeProperty(prop);
                    }
                    else {
                        
                        conDesc.setProperty(prop, str);
                    }
                }
            }
        }
        
        return doQuit;
    }
    
    private static void doTest(Session session, PrintStream out, ConsoleLineReader in, ConnectionDescriptor connDesc) 
        throws Exception {
        
        SQLDriverManager driverMan = session.getDriverManager();
        try {
            
            out.println();
            out.println("Attempting connection...");
            
            SQLConnectionContext conn = driverMan.connect(session, connDesc);
            conn.close();
            
            out.println("Succeeded!");
        }
        catch (SQLException e) {
            
            out.println("Failed!");
            SQLTools.printException(session, e);
        }
        catch (Throwable e) {
            
            out.println("Failed!");
            session.printException(e);
        }
        
        out.println();
        in.readline("Hit enter to continue:", false);
    }
    
    public boolean doDriverWizard(Session session, PrintStream out, ConsoleLineReader in, Ansi cls) 
        throws Exception {
        
        boolean done = false;
        boolean doQuit = false;
        
        SQLDriverManager driverMan = session.getDriverManager();
        
        while (!done) {
            
            SQLDriver []drivers = driverMan.getDrivers();
            Arrays.sort(drivers);
    
            out.print(cls);
            out.println("JSQSH DRIVER WIZARD - (edits $HOME/.jsqsh/drivers.xml)");
            
            out.println();
            out.println("The following drivers are currently defined:");
            out.println();
            out.println("     Name            Target               Class");
            out.println("---  --------------- -------------------- --------------------------------------------------");
            
            for (int i = 0; i < drivers.length; i++) {
                
                SQLDriver driver = drivers[i];
                out.printf("%-3d %s%-15s %-20s %-50s\n", 
                    i+1,
                    driver.isAvailable() ? "*" : " ",
                    crop(driver.getName(), 15),
                    crop(driver.getTarget(), 20),
                    crop(driver.getDriverClass(), 50));
            }
            out.println();
            out.println("  * = Driver is available");
            
            out.println();
            out.println("Enter a number to change an existing driver, or:");
            out.flush();
            
            String str = in.readline("(B)ack, (Q)uit, or (A)dd new driver: ", false);
            str = str.trim();
            if ("q".equalsIgnoreCase(str)) {
                
                done = true;
                doQuit = true;
            }
            else if ("b".equalsIgnoreCase(str)) {
                
                done = true;
            }
            else if ("a".equalsIgnoreCase(str)) {
                
                doQuit = doEditDriver (session, out, in, cls, null);
                done = doQuit;
            }
            else {
                
                int id = toInt(str);
                if (id >= 1 && id <= drivers.length) {
                    
                    doQuit = doEditDriver (session, out, in, cls, drivers[id-1]);
                    done = doQuit;
                }
            }
        }
        
        return doQuit;
    }
    
    private boolean doEditDriver (Session session, PrintStream out, ConsoleLineReader in, Ansi cls, SQLDriver origDriver) 
        throws Exception {
        
        boolean isNewDriver = (origDriver == null);
        SQLDriver driver = null;
        boolean doSave = false;
        
        if (origDriver == null) {
            
            driver = new SQLDriver();
        }
        else {
            
            driver = origDriver.copy();
        }
        
        boolean done = false;
        boolean doQuit = false;
        while (! done) {
            
            out.print(cls);
            out.println("JDBC WIZARD DRIVER EDITOR - (edits $HOME/.jsqsh/drivers.xml)");
            out.println();
            out.println("The following are standard driver configuration variables for jsqsh JDBC URL's:");
            out.println("  ${server}, ${port}, ${db}, ${domain}, ${user}, ${password}, ${SID}");
            out.println("You may also define your own variables by placing them in the URL");
            out.println();
            out.println("Example URL: jdbc:db2://${server}:${port}/${db}");
            out.println();
            out.println("Base Configuration");
            out.println("---------------------");
            
            int idx = 1;
            String format       = "%-2d %s %15s : %s\n";
            String promptFormat = "%-2d %s %15s : ";
            int nameIdx = idx++;
            if (driver.getName() == null) {
                
                driver.setName(getEntry(out, in, 
                    String.format(promptFormat, nameIdx, "*", "Name"), true));
            }
            else {
                
	            out.format(format, nameIdx, "*", "Name", driver.getName());
            }
            
            int descIdx = idx++;
            if (driver.getTarget() == null) {
                
                driver.setTarget(getEntry(out, in, 
                    String.format(promptFormat, descIdx, "*", "Description"), true));
            }
            else {
                
	            out.format(format, descIdx, "*", "Description", driver.getTarget());
            }
            
            int classIdx = idx++;
            if (driver.getDriverClass() == null) {
                
                driver.setDriverClass(getEntry(out, in, 
                    String.format(promptFormat, classIdx, "*", "Class"), true));
            }
            else {
                
	            out.format(format, idx++, "*", "Class", driver.getDriverClass());
            }
            
            int urlIdx = idx++;
            if (driver.getUrl() == null) {
                
                driver.setUrl(getEntry(out, in, 
                    String.format(promptFormat, urlIdx, "*", "URL"), true));
            }
            else {
                
	            out.format(format, urlIdx, "*", "URL", driver.getUrl());
            }
            
            int parserIdx = idx++;
            out.format(format, 
                parserIdx, " ", "SQL Parser", driver.getAnalyzer().getName());
            int classpathIdx = idx++;
            out.format(format, 
                classpathIdx, " ", "Classpath", driver.getClasspath());
            int schemaQueryIdx = idx++;
            out.format(format, 
                schemaQueryIdx, " ", "Schema query", 
                driver.getCurrentSchemaQuery() == null ? "(none)" : driver.getCurrentSchemaQuery());
            out.format("     %15s : %s\n", "Status",
                driverStatus(session, driver));
            
            out.println();
            out.println("URL Variable Defaults");
            out.println("---------------------");
            List<DriverVariable> vars = driver.getVariableDescriptions(false);
            int varsStartIdx = idx;
            int varsEndIdx = idx;
            for (int i = 0; i < vars.size(); i++) {
                
                DriverVariable var = vars.get(i);
                
                varsEndIdx = idx;
                out.format(format, 
                    idx++, " ", var.getName(), emptyIfNull(var.getDefaultValue()));
            }
            
            out.println();
            out.println("Enter a number to change a existing driver, or:");
            out.flush();
            
            String prompt = (isNewDriver 
                ? "(B)ack, (Q)uit, Show (C)lasspath, (A)dvanced options, or (S)ave: "
                : "(B)ack, (Q)uit, (D)elete, Show (C)lasspath, (A)dvanced options, or (S)ave: ");
            String str = in.readline(prompt, false);
            str = str.trim();
            if ("q".equalsIgnoreCase(str)) {
                
                done = true;
                doQuit = true;
            }
            else if ("b".equalsIgnoreCase(str)) {
                
                done = true;
            }
            else if ("c".equalsIgnoreCase(str)) {
                
                out.println("Driver computed classpath: ");
                List<URL> urls = driver.getExpandedClasspath();
                for (URL url : urls) {
                    
                    out.println("  " + url);
                }
                
                out.println();
                in.readline("Hit enter to continue: ", false);
            }
            else if ("a".equalsIgnoreCase(str)) {
                
                ScreenReturn ret = doEditDriverAdvanced(session, out, in, cls, driver);
                if (ret == ScreenReturn.SAVE) {
                    
                    done = true;
                    doSave = true;
                }
                else if (ret == ScreenReturn.QUIT) {
                    
                    done = doQuit = true;
                }
            }
            else if (! isNewDriver && "d".equalsIgnoreCase(str)) {
                
                out.println();
                str = in.readline("Are you sure (Y/N)? ", false);
                if ("y".equalsIgnoreCase(str)) {
                    
                    SQLDriverManager driverMan = session.getDriverManager();
                    driverMan.removeDriver(origDriver.getName());
                    driverMan.save(new File(session.getContext().getConfigDirectory(),
                        "drivers.xml"));
                    done = true;
                }
            }
            else if ("s".equalsIgnoreCase(str)) {
                
                done = true;
                doSave = true;
            }
            else {
                
                int opt = toInt(str);
                if (opt >= 1 && opt <= idx) {
                    
                    if (opt == nameIdx) {
                        
                        out.println();
                        driver.setName(getEntry(out, in, "Enter new name: ", true));
                    }
                    else if (opt == descIdx) {
                        
                        out.println();
                        driver.setTarget(getEntry(out, in, "Enter new description: ", true));
                    }
                    else if (opt == classIdx) {
                        
                        out.println();
                        driver.setDriverClass(getEntry(out, in, "Enter new class: ", true));
                    }
                    else if (opt == urlIdx) {
                        
                        out.println();
                        driver.setUrl(getEntry(out, in, "Enter new URL: ", true));
                    }
                    else if (opt == parserIdx) {
                        
                        driver.setAnalyzer(chooseAnalyzer(out, in));
                    }
                    else if (opt == classpathIdx) {
                        
                        out.println();
                        driver.setClasspath(getEntry(out, in, "Enter new classpath: ", false));
                    }
                    else if (opt == schemaQueryIdx) {
                        
                        out.println();
                        driver.setCurrentSchemaQuery(getEntry(out, in, "Enter query to fetch current schema: ", false));
                    }
                    else if (opt >= varsStartIdx && opt <= varsEndIdx) {
                        
                        DriverVariable var = vars.get(opt-varsStartIdx);
                        out.println();
                        str = in.readline("Enter new value for \"" 
                            + var.getName() + "\": ", false);
                        str = str.trim();
                        
                        if (str.length() == 0) {
                            
                            driver.removeVariable(var.getName());
                        }
                        else {
                            
                            driver.setVariable(var.getName(), str);
                        }
                    }
                }
                
            }
        }
        
        if (doSave) {
                
            SQLDriverManager driverMan = session.getDriverManager();
                
            /*
             * Check for a driver rename. Note that renaming an internal driver
             * cannot ever really get rid of it.
             */
            if (! isNewDriver && ! origDriver.getName().equals(driver.getName())) {
                
                driverMan.removeDriver(origDriver.getName());
            }
            driverMan.addDriver(driver);
            driverMan.save(new File(session.getContext().getConfigDirectory(),
                "drivers.xml"));
        }
        
        return doQuit;
    }
    
    private ScreenReturn doEditDriverAdvanced (Session session, PrintStream out, 
            ConsoleLineReader in, Ansi cls, SQLDriver driver) 
        throws Exception {
        
        while (true) {
            
            out.print(cls);
            out.println("JDBC DRIVER EDITOR - ADVANCED OPTIONS");
            out.println();
            out.println("Driver Information");
            out.println("---------------------");
            
            String format = "   %15s : %s\n";
            out.format(format, "Name", driver.getName());
            out.format(format, "Description", driver.getTarget());
            out.format(format, "Class", driver.getDriverClass());
            out.format(format, "URL", driver.getUrl());
            
            out.println();
            out.println("Session variables to set upon connect");
            out.println("--------------------------------------");
            out.println("The following jsqsh variables will be set upon connecting to the data source");
            out.println("These may be used to change the prompt, or to affect jsqsh configuration changes");
            out.println();
            String sessionVars[] = driver.getSessionVariables().keySet().toArray(new String[0]);
            Arrays.sort(sessionVars);
            
            if (sessionVars.length == 0) {
                
                out.println("None");
            }
            
            int idx = 1;
            format = "%-2d %s %15s : %s\n";
            for (int i = 0; i < sessionVars.length; i++) {
                
                out.format(format, idx++, "", sessionVars[i], 
                    driver.getSessionVariable(sessionVars[i]));
            }
            int driverPropsStartAt = idx;
            
            out.println();
            out.println("JDBC driver connection properties");
            out.println("--------------------------------------");
            out.println("The following are configuration properties specific to this JDBC driver");
            out.println();
            
            String driverProps[] = driver.getDriverProperties().keySet().toArray(new String[0]);
            Arrays.sort(driverProps);
            
            if (driverProps.length == 0) {
                
                out.println("None");
            }
            
            format = "%-2d %s %15s : %s\n";
            for (int i = 0; i < driverProps.length; i++) {
                
                out.format(format, idx++, "", driverProps[i], 
                    driver.getProperty(driverProps[i]));
            }
            
            out.println();
            out.println("Enter a number to change a existing setting, or:");
            out.flush();
            
            String str = in.readline("(B)ack, (Q)uit, New (V)ariable, New (P)roperty, or (S)ave: ", false);
            str = str.trim();
            if ("q".equalsIgnoreCase(str)) {
                
                return ScreenReturn.QUIT;
            }
            else if ("b".equalsIgnoreCase(str)) {
                
                return ScreenReturn.BACK;
            }
            else if ("s".equalsIgnoreCase(str)) {
                
                return ScreenReturn.SAVE;
            }
            else if ("v".equalsIgnoreCase(str)) {
                
                out.println();
                String name = in.readline("Session variable name: ", false);
                String value = in.readline("Session variable value: ", false);
                name = name.trim();
                value = value.trim();
                if (value.length() > 0) {
                    
                    driver.setSessionVariable(name, value);
                }
            }
            else if ("p".equalsIgnoreCase(str)) {
                
                newDriverProperty(session, out, in, cls, driver);
            }
            else {
                
                int opt = toInt(str);
                if (opt >= 1 && opt < driverPropsStartAt) {
                    
                    String var = sessionVars[opt-1];
                    out.println();
                    out.println("An empty value removes the variable definition");
                    str = in.readline("Enter new value for \"" + var + "\": ", false);
                    str = str.trim();
                    if (str.length() == 0) {
                        
                        driver.removeSessionVariable(var);
                    }
                    else {
                        
                        driver.setSessionVariable(var, str);
                    }
                }
                else if (opt >= driverPropsStartAt && opt < idx) {
                    
                    String var = driverProps[opt - driverPropsStartAt];
                    out.println();
                    out.println("An empty value removes the property definition");
                    str = in.readline("Enter new value for \"" + var + "\": ", false);
                    str = str.trim();
                    if (str.length() == 0) {
                        
                        driver.removeProperty(var);
                    }
                    else {
                        
                        driver.setProperty(var, str);
                    }
                }
            }
        }
    }
    
    private SQLAnalyzer chooseAnalyzer(PrintStream out, ConsoleLineReader in) 
        throws Exception {
        
        out.println();
        out.println("1.  None");
        out.println("2.  ANSI SQL");
        out.println("3.  PL/SQL");
        out.println("4.  T-SQL");
        
        while (true) {
            
            String str = in.readline("Choose a SQL analyzer: ", false);
            str = str.trim();
            int id = toInt(str);
            if (id < 1 || id > 4) {
                
                Ansi ansi = new Ansi();
                ansi.cursorUp(1);
                ansi.eraseLine();
                ansi.cursorUp(1);
                out.println(ansi);
            }
            else
            {
                switch (id) {
                
                case 1: return new NullAnalyzer();
                case 2: return new ANSIAnalyzer();
                case 3: return new PLSQLAnalyzer();
                case 4: return new TSQLAnalyzer();
                default:
                    break;
                }
            }
        }
            
    }
    
    private void newDriverProperty(Session session, PrintStream out, 
            ConsoleLineReader in, Ansi cls, SQLDriver driver)  
        throws Exception {
        
        DriverPropertyInfo info[] = null;
        
        try {
            
           ClassLoader loader = driver.getClassLoader(session.getDriverManager().getClassLoader());
           Class<? extends Driver> driverClass = 
               loader.loadClass(driver.getDriverClass()).asSubclass(Driver.class);
           Driver driverInst = driverClass.newInstance();
           
           info = driverInst.getPropertyInfo(driver.getUrl(), 
               new Properties());
        }
        catch (Throwable e) {
            
            /* SILENTLY IGNORED */
            System.out.println(e.getMessage());
        }
        
        if (info == null) {
            
            out.println("Available driver properties could not be retrieved (this may happen");
            out.println("if the JDBC driver cannot be loaded), you may manually enter properties:");
            
            String name = in.readline("Property name: ", false);
            String value = in.readline("Property value: ", false);
            name = name.trim();
            value = value.trim();
            if (value.length() > 0) {
                    
                driver.setProperty(name, value);
            }
            return;
        }
        
        boolean done = false;
        
        while (! done) {
            
            out.print(cls);
            out.println("DRIVER PROPERTIES");
            out.println();
            out.println("The following properties are published by the driver. Note that ");
            out.println("not all properties may be published by your driver, and you may ");
            out.println("manually enter a property if needed.");
            out.println();
            int halfIdx = (info.length / 2);
            if ((info.length - halfIdx) > halfIdx) {
                
                ++halfIdx;
            }
            
            int longest = 0;
            for (int i = 0; i < halfIdx; i++) {
                
                if (info[i].name.length() > longest) {
                    
                    longest = info[i].name.length();
                }
            }
            
            String leftFormat  = "%-2d %-" + longest + "s   ";
            String rightFormat = "%-2d %s\n";
            int leftIdx  = 0;
            int rightIdx = halfIdx;
            
            while (leftIdx < halfIdx) {
                
                out.format(leftFormat,  leftIdx+1, info[leftIdx].name);
                if (rightIdx < info.length) {
                    
                    out.format(rightFormat, rightIdx+1, info[rightIdx].name);
                }
                else {
                    
                    out.println();
                }
                
                
                ++leftIdx;
                ++rightIdx;
            }
            
            out.println();
            out.println("Enter a property number to edit that property. A question mark after");
            out.println("the property name (e.g. \"2?\") will display a description, if available: ");
            String str = in.readline("(M)anually enter, or (B)ack: ", false);
            str = str.trim();
            
            if (str.equalsIgnoreCase("b")) {
                
                done = true;
            }
            else if (str.equalsIgnoreCase("m")) {
                
                done = true;
                out.println();
                String name = in.readline("Property name: ", false);
                String value = in.readline("Property value: ", false);
                name = name.trim();
                value = value.trim();
                if (value.length() > 0) {
                        
                    driver.setProperty(name, value);
                }
            }
            else {
                
                if (str.endsWith("?")) {
                    
                    str = str.substring(0, str.length()-1);
                    int idx = toInt(str);
                    out.println();
                    if (idx >= 1 || idx <= info.length) {
                        
                        if (info[idx-1].description != null && info[idx].description.length() > 0) {
                            
                            out.println(info[idx-1].description);
                        }
                        else {
                            
                            out.println("No help is avalable");
                        }
                        out.println();
                        in.readline("Hit enter to continue: ", false);
                        continue;
                    }
                }
                
                int idx = toInt(str);
                if (idx >= 1 && idx <= info.length) {
                    
                    if (info[idx-1].choices == null || info[idx-1].choices.length == 0) {
                        
                        String value = in.readline("New value for \"" 
                            + info[idx-1].name + "\": ", false);
                        value = value.trim();
                        if (value.length() > 0) {
                        
                            driver.setProperty(info[idx-1].name, value);
                        }
                        done = true;
                    }
                    else {
                        
                        out.println();
                        
                        String choices[] = info[idx-1].choices;
                        String format  = "%-2d %s\n";
                        for (int i = 0; i < choices.length; i++) {
                            
                            out.format(format, (i+1), choices[i]);
                        }
                        
                        out.println();
                        while (! done) {
                            
                            str = in.readline("Enter choice or (B)ack: ", false);
                            str = str.trim();
                            if ("b".equalsIgnoreCase(str)) {
                                
                                break;
                            }
                            else if (str.length() > 0) {
                                
                                int choice = toInt(str);
                                if (choice >= 1 && choice < choices.length) {
                                    
                                    done = true;
                                    driver.setProperty(info[idx-1].name, choices[choice-1]);
                                }
                            }
                            
                            if (! done) {
                                
                                Ansi ansi = new Ansi();
                                ansi.cursorUp(1);
                                ansi.eraseLine();
                                ansi.cursorUp(1);
                                out.println(ansi);
                            }
                        }
                    }
                }
            }
        }
    }
        
    private String driverStatus(Session session, SQLDriver driver) {
        
       try {
           
           ClassLoader loader = driver.getClassLoader(session.getDriverManager().getClassLoader());
           loader.loadClass(driver.getDriverClass());
           return "Available";
       }
       catch (Throwable e) {
           
           return "Cannot load driver class (" + e.getMessage() + ")";
       }
    }
    
    private String getEntry(PrintStream out, ConsoleLineReader in, String prompt, boolean isRequired) throws Exception {
        
        String str = "";
        while (str.length() == 0) {
            
            str = in.readline(prompt, false);
            str = str.trim();
            if (! isRequired) {
                
                if (str.length() == 0) {
                    
                    str = null;
                }
                break;
            }
            if (str.length() == 0) {
                
                Ansi ansi = new Ansi();
                ansi.cursorUp(1);
                ansi.eraseLine();
                ansi.cursorUp(1);
                out.println(ansi);
            }
        }
        
        return str;
    }
    
    public static String emptyIfNull(String str) {
        
        if (str == null) {
            
            return "";
        }
        
        return str;
    }
    
    public static int toInt(String str) {
        
        try {
            
            return Integer.valueOf(str);
        }
        catch (NumberFormatException e) {
            
            return -1;
        }
    }
    
    public static String crop(String str, int len) {
        
        if (str != null && str.length() > len) {
            
            str = str.substring(0, len);
        }
        
        return str;
    }
    
    public static String limit(String str, int len) {
        
        if (str != null && str.length() > len) {
            
            str = str.substring(0, len - 3) + "...";
        }
        
        return str;
    }
    
    public static String limitFront(String str, int len) {
        
        if (str != null && str.length() > len) {
            
            str = "..." + str.substring(0, len - 3);
        }
        
        return str;
    }
    
    private static enum ScreenReturn {
        
        BACK,
        QUIT,
        SAVE
    }
    
}
