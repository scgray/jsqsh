/*
 * Copyright (C) 2007 by Scott C. Gray
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, write to the Free Software Foundation, 675 Mass Ave,
 * Cambridge, MA 02139, USA.
 */
package org.sqsh.options;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

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
    
    private OptionInfo[] options = null;
    private List argList = null;
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
     *   the {@link Option} annotation.
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
        
        StringBuilder optChars = new StringBuilder();
        LongOpt []longOpts = new LongOpt[options.length];
        
        /*
         * Build up the arguments required for GetOpt.
         */
        for (int i = 0; i < options.length; i++) {
            
            longOpts[i] = options[i].longOpt;
            optChars.append((char) (options[i].longOpt.getVal()));
            switch (options[i].longOpt.getHasArg()) {
                
                case LongOpt.OPTIONAL_ARGUMENT:
                    optChars.append("::");
                    break;
                
                case LongOpt.REQUIRED_ARGUMENT:
                    optChars.append(":");
                    break;
                    
                default:
                    break;
            }
        }
        
        Getopt getopt = new Getopt("<none>", argv, optChars.toString(),
            longOpts);
        getopt.setOpterr(false);
        
        /*
         * Ok, now process the command line arguments.
         */
        int rc;
        while ((rc = getopt.getopt()) != -1) {
            
            /*
             * If we hit a bad option, then throw up.
             */
            if (rc == '?') {
                
                throw new OptionException(program
                    + ": Invalid option '-"
                    + ((char) getopt.getOptopt()) + "'");
            }
            
            /*
             * Otherwise, go back and try to find out which field we are
             * supposed to set when we see that option.
             */
            for (int i = 0; i < options.length; i++) {
                
                if (options[i].longOpt.getVal() == rc) {
                    
                    setValue(options[i], getopt.getOptarg());
                    break;
                }
            }
        }
        
        /*
         * JIC, we will try to clear out any existing contents
         * of the list.
         */
        if (argList.size() > 0) {
                    
            argList.clear();
        }
                
        /*
         * Now, populate our list.
         */
        for (int i = getopt.getOptind(); i < argv.length; i++) {
                    
            argList.add(argv[i]);
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
        
        for (OptionInfo optionInfo : options) {
            
            int start = sb.length();
            sb.append("   ");
            sb.append('-').append(optionInfo.option.option());
            if (optionInfo.option.longOption().length() != 0) {
                
                sb.append(", --").append(optionInfo.option.longOption());
            }
            
            ArgumentRequired req = optionInfo.option.arg();
            if (req == ArgumentRequired.OPTIONAL) {
                
                sb.append('[');
            }
            
            if (req != ArgumentRequired.NONE) {
                
                if (optionInfo.option.longOption().length() != 0) {
                    
                    sb.append('=');
                }
                else {
                    
                    sb.append('=');
                }
                
                sb.append(optionInfo.option.argName());
            }
            
            if (req == ArgumentRequired.OPTIONAL) {
                
                sb.append(']');
            }
            
            sb.append(' ');
            
            int end = sb.length();
            for (; end < start + 30; ++end) {
                
                sb.append(' ');
            }
            
            sb.append(optionInfo.option.description());
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
    private void setValue(OptionInfo option, String value)
        throws OptionException {
        
        try {
            
            Field field = optionBean.getClass().getField(option.fieldName);
            Class type = field.getType();
            
            if (field.isAccessible() == false) {
                
                field.setAccessible(true);
            }
            
            /*
         	 * This is a special case. If the option takes no arguments
         	 * then we have to assume that it is toggling a boolean 
         	 * value.
         	 */
            if (option.longOpt.getHasArg() == LongOpt.NO_ARGUMENT) {
                
                /*
                 * Options with no arguments, *must* correspond to a 
                 * boolean field.
                 */
                if (type == Boolean.TYPE
                        || type == Boolean.class) {
                    
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

                    List c = (List)field.get(optionBean);
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
    private void toggleBoolean (OptionInfo optionInfo, Field field)
        throws OptionException {
        
        try {
            
            boolean oldValue = field.getBoolean(optionBean);
            field.setBoolean(optionBean, !oldValue);
        }
        catch (Exception e) {
            
            throw new OptionException(program
                + ": Unable to toggle value of field "
                + field.getName() + " via option "
                + optionInfo.toString() + ": " + e.getMessage());
        }
    }
    
    /**
     * Traverses an object looking for annotations that are meaningful
     * to the OptionProcessor.
     * 
     * @param opts The object to traverse.
     */
    private void doAnnotations() {
        
        /*
         * First, look for @Options.
         */
        ArrayList<OptionInfo> optionsList = new ArrayList<OptionInfo>();
        Field []fields = optionBean.getClass().getFields();
        for (int i = 0;i < fields.length; i++) {
            
            Option option = (Option) fields[i].getAnnotation(Option.class);
            if (option != null) {
                
                optionsList.add(new OptionInfo(option, fields[i].getName()));
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
                
                argList      = (List)val;
                program      = argv.program();
                programUsage = argv.usage();
                minArgs      = argv.min();
                maxArgs      = argv.max();
            }
        }
        
        options = optionsList.toArray(new OptionInfo[0]);
        if (argList == null) {
            
            System.err.println("WARNING: Option object must contain "
                + "a field of type java.util.List annotated with @Argv");
        }
    }
    
    /**
     * Used internally to hold everything necessary for the option
     * processing.
     */
    private static class OptionInfo {
        
        /**
         * The actual option.
         */
        public Option option;
        
        /**
         * This it the option information for actually doing the 
         * command line processing.
         */
        public LongOpt longOpt;
        
        /**
         * This is the field in the object that is annotated with 
         * the {@link Option} annotation.
         */
        public String fieldName;
        
        public OptionInfo (Option option, String fieldName) {
            
            this.option = option;
            this.fieldName = fieldName;
            
            String longName = option.longOption();
            int required = LongOpt.NO_ARGUMENT;
            char optChar = option.option();
                
            if (option.arg() == ArgumentRequired.NONE) {
                    
                required = LongOpt.NO_ARGUMENT;
            }
            else if (option.arg() == ArgumentRequired.OPTIONAL) {
                    
                required = LongOpt.OPTIONAL_ARGUMENT;
            }
            else if (option.arg() == ArgumentRequired.REQUIRED) {
                    
                required = LongOpt.REQUIRED_ARGUMENT;
            }
                
            this.longOpt = new LongOpt(longName, required, null, optChar);
            this.fieldName = fieldName;
        }
        
        public String toString() {
            
            StringBuilder sb = new StringBuilder();
            sb.append('-').append((char) longOpt.getVal());
            if (longOpt.getName() != null) {
                
                sb.append(" (")
                    .append("--").append(longOpt.getName())
                    .append(")");
            }
            
            return sb.toString();
        }
    }
}
