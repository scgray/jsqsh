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

import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.sqsh.input.ConsoleLineReader;
import org.sqsh.options.Argv;

public class Setup extends Command {
    
    private static class Options extends SqshOptions {
     
        @Argv(program="\\sestup", min=0, max=0, usage="")
        public List<String> arguments = new ArrayList<String>();
    }

    @Override
    public SqshOptions getOptions() {

        return new Options();
    }

    @Override
    public int execute(Session session, SqshOptions options) throws Exception {
        
        ConsoleLineReader in = session.getContext().getConsole();
        SQLDriverManager driverMan = session.getDriverManager();
        PrintStream out = AnsiConsole.out();
        
        Ansi cls = new Ansi();
        cls.eraseScreen();
        cls.cursor(0, 0);
        boolean done = false;
        
        while (!done) {
        
            out.print(cls);
            
            ConnectionDescriptor []connDescs = 
                session.getConnectionDescriptorManager().getAll();
            
            Arrays.sort(connDescs);
            
            if (connDescs == null || connDescs.length == 0) {
                
                done = doAddChooseDriver(session, out, in, cls);
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
            
            String str = in.readline("(A)dd or (Q)uit setup: ", false);
            str = str.trim();
            
            if (str.equalsIgnoreCase("q")) {
                
                done = true;
            }
            else if (str.equalsIgnoreCase("a")) {
                
                done = doAddChooseDriver(session, out, in, cls);
            }
            else {
                
                int id = toInt(str);
                if (id >= 1 && id <= connDescs.length) {
                    
                    done = doConfigConnection(session, out, in, cls, 
                        connDescs[id-1], driverMan.getDriver(connDescs[id-1].getDriver()));
                }
            }
        }
            
        return 0;
    }
    
    /**
     * Render the "add" screen
     * @throws Exception
     */
    public boolean doAddChooseDriver(Session session, PrintStream out, ConsoleLineReader in, Ansi cls) throws Exception {
        
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
	        
	        out.println("JSqsh comes with definitions for the a number of JDBC drivers, below.");
	        out.println("Note that drivers with \"N\" in the \"Loaded\" column are not available");
	        out.println("in your classpath. You can run \"\\help drivers\" at the jsqsh prompt");
	        out.println("for details on adding driver jars to your classpath");
	        out.println();
	        
	        out.println("    Target               Class                                              Loaded");
	        out.println("--- -------------------- -------------------------------------------------- ------");
	        
	        for (int i = 0; i < drivers.size(); i++) {
	            
	            SQLDriver driver = drivers.get(i);
	            out.printf("%3d %-20s %-50s %s\n", 
	                i+1,
	                driver.getName(),
	                limit(driver.getDriverClass(), 50),
	                driver.isAvailable() ? "Y" : "N");
	        }
	        out.println();
	        
	        out.println();
	        out.flush();
	        String str = in.readline("Enter the driver number, (B)ack or (Q)uit: ", false);
            str = str.trim();
            if (str.equalsIgnoreCase("b")) {
                
                done = true;
            }
            if (str.equalsIgnoreCase("q")) {
                
                done = true;
                doQuit = true;
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
        
        
        String format = "%-3d %" + longestName + "s : %s\n";
        
        while (! done) {
            
	        out.print(cls);
	        
	        out.println("The following configuration properties are supported by this driver.");
	        out.println("Some properties are configured with default values specific to the driver,");
	        out.println("but may require changes to suit your environment");
	        out.println();
	        
	        out.printf("    %" + longestName + "s : %s\n", "Driver", driver.getTarget());
	        out.println();
	        
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
	            
	            out.printf(format, idx+1, vars.get(idx).getDisplayName(), value == null ? "" : value);
	        }
	        
	        out.printf(format, idx+1, "Autoconnect", (conDesc.isAutoconnect() ? "true" : "false"));
	        
	        out.println();
	        out.println("Enter a number to change a given configuration property, or");
	        out.flush();
	        
	        String prompt = (isNew 
	            ? "(T)est, (B)ack, (Q)uit, or (S)ave: "
	            : "(T)est, (D)elete, (B)ack, (Q)uit, or (S)ave: ");
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
            else {
                
                int val = toInt(str);
                if (val >= 1 && val <= vars.size()) {
                    
                    DriverVariable var = vars.get(val-1);
                    boolean isPassword = var.getName().equals(SQLDriver.PASSWORD_PROPERTY);
                    
                    out.println();
                    out.println("Please enter a new value:");
                    if (isPassword) {
                        
                        str = in.readPassword(var.getDisplayName() + ": ");
                    }
                    else {
                        
                        str = in.readline(var.getDisplayName() + ": ", false);
                    }
                    
                    str = str.trim();
                    conDesc.setValueOf(var.getName(), str);
                }
                else if (val == vars.size()+1) {
                        
                    conDesc.setAutoconnect(! conDesc.isAutoconnect());
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
    
    
    private static int toInt(String str) {
        
        try {
            
            return Integer.valueOf(str);
        }
        catch (NumberFormatException e) {
            
            return -1;
        }
    }
    
    private static String limit(String str, int len) {
        
        if (str != null && str.length() > len) {
            
            str = str.substring(0, len - 3) + "...";
        }
        
        return str;
    }
    
}
