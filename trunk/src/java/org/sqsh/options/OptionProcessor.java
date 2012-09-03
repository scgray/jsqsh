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
package org.sqsh.options;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The option processor is responsible for analyzing objects that were
 * annotated with the @Option, and @Argv annotations, describing
 * command line options that can be processed by the OptionProcessor.
 * 
 * <p>The OptionProcessor concept is stolen whole-heartedly from 
 * <a href="https://args4j.dev.java.net">args4j</a>), but this implementation
 * is wrapped around the GNU Getopt processing library, which means it
 * follows more the style of command line arguments that most applications
 * take</p>
 */
public class OptionProcessor {
    
    private Option[] options = null;
    private List<String> argList = null;
    private String program = null;
    private String programUsage = "";
    private int minArgs = 0;
    private int maxArgs = -1;
    private Object optionBean;
    
    /**
     * Creates an option process for a provided bean. In this case,
     * the object is not a "bean" in the strict java sense, but has 
     * member fields that are annotated with the @Option annotation.
     * 
     * @param optionBean An object that has member fields annotated with
     *   the {@link OptionProperty} annotation.
     */
    public OptionProcessor (Object optionBean) {
        
        this.optionBean = optionBean;
        doAnnotations();
    }
    
    /**
     * Parses command line options.
     * 
     * @param argv Set of command line options to parse.
     * @throws OptionException Thrown if there is a failure to parse
     *    the command line options.
     */
    public void parseOptions (String []argv)
        throws OptionException {
        
        final int sz = argv.length;
        int idx = 0;
        
        /*
         * JIC, we will try to clear out any existing contents of the list.
         */
        if (argList.size() > 0) {
                    
            argList.clear();
        }
        
        // Set to true when we hit a naked "--"
        boolean noMoreArgs = false;
        
        while (idx < sz) {
            
            String arg = argv[idx++];
            Option opt = null;
            
            // If we had previously hit a raw "--", then just absorbe the args
            if (noMoreArgs) {
                
                argList.add(arg);
                continue;
            }
            
            /*
             * If the argument is part of the option (e.g. "--foo=bar" or 
             * "-Usa" then this will point to the start of the argument.
             */
            int argIdx = -1;
            
            /*
             * Check for long option name.
             */
            if (arg.startsWith("--")) {
                
                // Once we hit a "--" all by itself, we are done parsing arguments
                if (arg.equals("--")) {
                    
                    noMoreArgs = true;
                    continue;
                }
                
                int eqIdx = arg.indexOf('=');
                String optName = arg.substring(2, (eqIdx < 0 ? arg.length() : eqIdx));
                
                opt = findOption(optName);
                
                if (eqIdx > 0) {
                    
                    argIdx = eqIdx+1;
                }
            }
            else if (arg.startsWith("-")) {
                
                if (arg.length() == 1) {
                    
                    throw new OptionException(program + ": Missing option character "
                        + " following \"-\"");
                }
                
                char optChar = arg.charAt(1);
                opt = findOption(optChar);
                
                if (arg.length() > 2) {
                    
                    argIdx = 2;
                }
            }
            
            /*
             * If there was no option, then just stuff it in the argument list
             */
            if (opt == null) {
                
                argList.add(arg);
            }
            else {
                
                String optArg = null;
                
                // Does the option take an argument?
                if (opt.hasArg()) {
                    
                    
                    // If we had an "=" then the argument is part of the string
                    if (argIdx > 0) {
                        
                        optArg = arg.substring(argIdx);
                    }
                    else {
                        
                        // No "=" so the argument could be the next string
                        if (opt.getOptRequired() == ArgumentRequired.REQUIRED) {
                            
                            // No more arguments or next is an option, then error
                            if (idx == sz || argv[idx].startsWith("-")) {
                                
                                throw new OptionException(program
                                    + ": option " + opt + " is missing an argument");
                            }
                            
                            // Consume the next entry as an argument
                            optArg = argv[idx++];
                        }
                        else {     // ArgumentRequired.OPTIONAL
                            
                            // For optional argument, if there is an argument left
                            // and it isn't an option, then use it
                            if (idx < sz || ! argv[idx].startsWith("-")) {
                                
                                optArg = argv[idx++];
                            }
                        }
                    }
                }
                else {
                    
                    if (argIdx >= 0) {
                        
                        throw new OptionException(program
                            + ": option " + opt + " does not take an argument");
                    }
                }
                
                setValue(opt, optArg);
            }
        }
        
        
        /*
         * Check if we have hit the minimum required arguments.
         */
        if (minArgs > 0 &&  argList.size() < minArgs) {
                    
            throw new OptionException(program
                + ": A minimum of " + minArgs + " arguments "
                + "are required");
        }
                
        if (maxArgs > 0 && argList.size() > maxArgs) {
                    
            throw new OptionException(program
                + ": A maximum of " + maxArgs + " arguments "
                + "are allowed");
        }
    }
    
    /**
     * Returns the usage as a string.
     * 
     * @return The usage.
     */
    public String getUsage() {
        
        StringBuilder sb = new StringBuilder();
        String linesep = System.getProperty("line.separator");
        
        sb.append("Use: ").append(program).append(' ').append(programUsage)
            .append(linesep);
        
        for (Option option : options) {
            
            int start = sb.length();
            sb.append("   ");
            sb.append('-').append(option.getShortOpt());
            
            String longOpt = option.getLongOpt();
            if (longOpt != null && longOpt.length() == 0) {
                
                longOpt = null;
            }
                
            if (longOpt != null) {
                
                sb.append(", --").append(option.getLongOpt());
            }
            
            ArgumentRequired req = option.getOptRequired();
            if (req == ArgumentRequired.OPTIONAL) {
                
                sb.append('[');
            }
            
            if (req != ArgumentRequired.NONE) {
                
                if (longOpt != null) {
                    
                    sb.append('=');
                }
                else {
                    
                    sb.append('=');
                }
                
                sb.append(option.getArgName());
            }
            
            if (req == ArgumentRequired.OPTIONAL) {
                
                sb.append(']');
            }
            
            sb.append(' ');
            
            int end = sb.length();
            for (; end < start + 30; ++end) {
                
                sb.append(' ');
            }
            
            sb.append(option.getDescription());
            sb.append(linesep);
        }
        
        return sb.toString();
    }
    
    /**
     * Called when it is time to set the value of a field based
     * upon an option provided by a user.
     * 
     * @param option The option description.
     * @param value The value provided.
     * @throws OptionException Thrown if there is a problem setting
     *   the value of that option.
     */
    private void setValue(Option option, String value)
        throws OptionException {
        
        try {
            
            Field field = optionBean.getClass().getField(option.fieldName);
            Class<?> type = field.getType();
            
            if (field.isAccessible() == false) {
                
                field.setAccessible(true);
            }
            
            /*
              * This is a special case. If the option takes no arguments
              * then we have to assume that it is toggling a boolean 
              * value.
              */
            if (! option.hasArg()) {
                
                /*
                 * Options with no arguments, *must* correspond to a 
                 * boolean field.
                 */
                if (type == Boolean.TYPE || type == Boolean.class) {
                    
                    toggleBoolean(option, field);
                }
                else {
                    
                    throw new OptionException(program + ": Option "
                        + option.toString()
                        + ", defined for field " 
                        + option.fieldName + " is defined as taking no "
                        + " arguments, but the field is not a boolean");
                }
            }
            else {
                
                /*
                 * Ok, so this could be a bit more object oriented :)
                 */
                if (type == Integer.TYPE
                        || type == Integer.class) {
                    
                    field.setInt(optionBean, 
                        (value == null ? -1 : Integer.parseInt(value)));
                }
                else if (type == Long.TYPE
                        || type ==  Long.class) {
                    
                    field.setLong(optionBean, 
                        (value == null ? 0 : Long.parseLong(value)));
                }
                else if (type == Double.TYPE
                        || type == Double.class) {
                    
                    field.setDouble(optionBean, 
                        (value == null ? Double.NaN : Double.parseDouble(value)));
                }
                else if (type == Float.TYPE
                        || type == Float.class) {
                    
                    field.setFloat(optionBean, 
                        (value == null ? Float.NaN : Float.parseFloat(value)));
                }
                else if (type == String.class) {
                    
                    field.set(optionBean, value);
                }
                else if (type.isAssignableFrom(java.util.List.class)) {

                    @SuppressWarnings("unchecked")
                    List<String> c = (List<String>)field.get(optionBean);
                    c.add(value);
                }
                else {
                    
                    throw new OptionException(program + 
                        ": Unable to set field '"
                        + option.fieldName + "' via option "
                        + option.toString() + ", due to unrecognized field "
                        + "type : " + type.getName());
                }
            }
        }
        catch (NumberFormatException e) {
            
            throw new OptionException(program
                + ": Invalid value '"
                + value + "' provided for option "
                + option.toString()
                + ": " + e.getMessage());
            
        }
        catch (IllegalAccessException e) {
            
            throw new OptionException(program 
                + ": Unable to set the value of field '"
                + option.fieldName + "' via option "
                + option.toString() + ": " + e.getMessage());
        }
        catch (NoSuchFieldException e) {
            
            /* Shouldn't happen, but.... */
            throw new OptionException(program
                + ": Error while attempting to set field '"
                + option.fieldName + "' via option " 
                + option.toString() + ": " + e.getMessage());
        }
    }
    
    /**
     * Toggles a boolean field.
     * 
     * @param optionInfo Description of the field.
     * @param field The field to be toggled.
     * @throws OptionException Thrown if the toggle fails.
     */
    private void toggleBoolean (Option option, Field field)
        throws OptionException {
        
        try {
            
            boolean oldValue = field.getBoolean(optionBean);
            field.setBoolean(optionBean, !oldValue);
        }
        catch (Exception e) {
            
            throw new OptionException(program
                + ": Unable to toggle value of field "
                + field.getName() + " via option "
                + option + ": " + e.getMessage());
        }
    }
    
    /**
     * Traverses an object looking for annotations that are meaningful
     * to the OptionProcessor.
     * 
     * @param opts The object to traverse.
     */
    @SuppressWarnings("unchecked")
    private void doAnnotations() {
        
        /*
         * First, look for @Options.
         */
        ArrayList<Option> optionsList = new ArrayList<Option>();
        Field []fields = optionBean.getClass().getFields();
        for (int i = 0;i < fields.length; i++) {
            
            OptionProperty option = (OptionProperty) fields[i].getAnnotation(OptionProperty.class);
            if (option != null) {
                
                optionsList.add(new Option(option, fields[i].getName()));
            }
            
            Argv argv = (Argv) fields[i].getAnnotation(Argv.class);
            if (argv != null) {
                
                Object val = null;
                
                try {
                    
                    if (fields[i].isAccessible() == false) {
                        
                        fields[i].setAccessible(true);
                    }
                    
                    val = fields[i].get(optionBean);
                }
                catch (IllegalAccessException e) {
                    
                    System.err.println("WARNING: "
                        + "Unable to access field '"
                        + fields[i].getName() + "': " + e.getMessage());
                }
                
                if (!(val instanceof List)) {
                        
                    System.err.println("WARNING: "
                        + "Field '"
                        + fields[i].getName()
                        + "' is annotated with @Argv but "
                        + "is not suitable for storing arguments. Fields "
                        + "annotated in this way must implement java.util.List");
                }
                
                argList      = (List<String>) val;
                program      = argv.program();
                programUsage = argv.usage();
                minArgs      = argv.min();
                maxArgs      = argv.max();
            }
        }
        
        options = optionsList.toArray(new Option[0]);
        if (argList == null) {
            
            System.err.println("WARNING: Option object must contain "
                + "a field of type java.util.List annotated with @Argv");
        }
    }
    
    private Option findOption (String longOpt) throws OptionException {
        
        for (Option opt : options) {
            
            if (longOpt.equals(opt.getLongOpt())) {
                
                return opt;
            }
        }
        
        throw new OptionException(program
            + ": Unrecognized option \"--" + longOpt + "\"");
    }
    
    private Option findOption (char shortOpt) throws OptionException {
        
        for (Option opt : options) {
            
            if (shortOpt == opt.getShortOpt()) {
                
                return opt;
            }
        }
        
        throw new OptionException(program
            + ": Unrecognized option \"-" + shortOpt + "\"");
    }
}
