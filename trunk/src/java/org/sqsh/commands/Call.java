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
package org.sqsh.commands;

import static org.sqsh.options.ArgumentRequired.REQUIRED;
import static org.sqsh.options.ArgumentRequired.NONE;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.sqsh.BufferManager;
import org.sqsh.Command;
import org.sqsh.DatabaseCommand;
import org.sqsh.SQLRenderer;
import org.sqsh.SQLTools;
import org.sqsh.Session;
import org.sqsh.SqshOptions;
import org.sqsh.options.Argv;
import org.sqsh.options.Option;
import org.sqsh.util.CSVReader;

/**
 * Implements the \call command.
 */
public class Call
    extends Command
    implements DatabaseCommand {
    
    private static class Options
    extends SqshOptions {
        
        @Option(
            option='f', longOption="file", arg=REQUIRED, argName="file",
            description="CSV file to be used for parameters to query")
        public String inputFile = null;
        
        @Option(
            option='i', longOption="ignore-header", arg=NONE,
            description="Ignore headers in input file")
        public boolean hasHeaders = false;
        
        @Argv(program="\\call", min=0)
        public List<String> arguments = new ArrayList<String>();
    }
    
    /**
     * Return our overridden options.
     */
    @Override
    public SqshOptions getOptions() {
        
        return new Options();
    }

    @Override
    public int execute (Session session, SqshOptions opts)
        throws Exception {
        
        Options options = (Options)opts;
        String argv[] = options.arguments.toArray(new String[0]);
        
        BufferManager bufferMan = session.getBufferManager();
        String sql = bufferMan.getCurrent().toString();
        
        /*
         * If this is an interactive session, then we create a new
         * buffer to work with, otherwise we just re-use the current
         * buffer.
         */
        if (session.isInteractive()) {
            
            bufferMan.newBuffer();
        }
        else {
            
            bufferMan.getCurrent().clear();
        }
        
        /*
         * Parse the command line arguments so we know how they are
         * going to be handled.
         */
        ParameterValue []params = new ParameterValue[argv.length];
        for (int i = 0; i < argv.length; i++) {
            
            try {
                
                params[i] = new ParameterValue(argv[i], i+1);
            }
            catch (Exception e) {
                
                session.err.println("Unable to parse '"
                    + argv[i] + "': " + e.getMessage());
                return 1;
            }
        }
        
        try {
            
            if (options.inputFile == null) {
                
                doNoInputFile(session, sql, params);
            }
            else {
                
                doInputFile(session, sql, options.inputFile,
                    options.hasHeaders, params);
            }
        }
        catch (SQLException e) {
            
            SQLTools.printException(session.err, e);
            return 1;
        }
        
        return 0;
    }
    
    /**
     * Called to execute the SQL buffer when there is no input file
     * supplied by the caller.
     * 
     * @param session The session context.
     * @param sql The block of SQL to execute.
     * @param params Parameters to the block.
     * @return 0 if it works ok, 1 otherwise.
     * @throws SQLException Thrown if there is an exception.
     */
    private int doNoInputFile(Session session, String sql,
            ParameterValue []params)
        throws SQLException {
        
        Connection conn = session.getConnection();
        CallableStatement statement = conn.prepareCall(sql);
        
        try {
            
            if (!setParameters(session, statement, 0, null, params)) {
                
                return 1;
            }
            
            SQLRenderer sqlRenderer = session.getSQLRenderer();
            sqlRenderer.execute(session, statement);
        }
        finally {
            
            SQLTools.close(statement);
        }
        
        return 0;
    }
    
    private int doInputFile(Session session, String sql, String file,
            boolean hasHeaders, ParameterValue []params)
        throws SQLException {
        
        Connection conn = session.getConnection();
        CallableStatement statement = conn.prepareCall(sql);
        SQLRenderer sqlRenderer = session.getSQLRenderer();
        
        InputStream in = null;
        int line = 0;
        int rc = 0;
        
        try {
            
            in = new FileInputStream(file);
            CSVReader reader = new CSVReader(in, hasHeaders);
            
            String row[] = reader.next();
            
            /*
             * If the user passed no parameter description to us, then
             * we'll create them ourselves based upon the file provided.
             */
            if (params.length == 0) {
                
                params = new ParameterValue[row.length];
                for (int i = 0; i < row.length; i++) {
                    
                    params[i] = new ParameterValue("S:#" + (i+1), i+1);
                }
            }
            
            while (row != null) {
                
                ++line;
                if (!setParameters(session, statement, line, row, params)) {
                    
                    return 1;
                }
                sqlRenderer.execute(session, statement);
                
                row = reader.next();
            }
        }
        catch (IOException e) {
            
            session.err.println("I/O error while reading '"
                + file + "': " + e.getMessage());
            rc = 1;
        }
        finally {
            
            SQLTools.close(statement);
        }
        
        if (in != null) {
            
            try {
                
                in.close();
            }
            catch (IOException e) {
                
                /* IGNORED */
            }
        }
        
        return rc;
    }
    
    private boolean setParameters(Session session,
            CallableStatement statement, int line,
            String []row, ParameterValue []params)
        throws SQLException {
        
        for (ParameterValue param : params) {
            
            String value = null;
            try {
                
                value = param.getValue(row);
            }
            catch (ArrayIndexOutOfBoundsException e) {
                
                session.err.println("Line #" + line 
                    + " does not contain requested column #"
                    + param.getColumnIdx());
                return false;
            }
            
            try {
                
                switch (param.getType()) {
                    
                    case 'S':
                    case 'C':
                        statement.setString(param.getIdx(), value);
                        break;
                    
                    case 'Z':
                        if (value == null || value.length() == 0) {
                            
                            statement.setNull(param.getIdx(), Types.BOOLEAN);
                        }
                        else {
                            
                            statement.setBoolean(param.getIdx(), 
                                Boolean.valueOf(value));
                        }
                        break;
                        
                    case 'D':
                        if (value == null || value.length() == 0) {
                            
                            statement.setNull(param.getIdx(), Types.DOUBLE);
                        }
                        else {
                            
                            statement.setDouble(param.getIdx(),
                                Double.valueOf(value));
                        }
                        break;
                        
                    case 'F':
                        if (value == null || value.length() == 0) {
                            
                            statement.setNull(param.getIdx(), Types.FLOAT);
                        }
                        else {
                            
                            statement.setFloat(param.getIdx(),
                                Float.valueOf(value));
                        }
                        break;
                        
                    case 'I':
                        if (value == null || value.length() == 0) {
                            
                            statement.setNull(param.getIdx(), Types.INTEGER);
                        }
                        else {
                            
                            statement.setInt(param.getIdx(),
                                Integer.valueOf(value));
                        }
                        break;
                        
                    case 'J':
                        if (value == null || value.length() == 0) {
                            
                            statement.setNull(param.getIdx(), Types.BIGINT);
                        }
                        else {
                            
                            statement.setLong(param.getIdx(),
                                Long.valueOf(value));
                        }
                        break;
                        
                    default:
                        session.err.println("Invalid type specifier '"
                            + param.getType()
                            + "'. Valid specifiers are SCZDFIJ");
                        return false;
                }
            }
            catch (NumberFormatException e) {
                
                session.err.println("Invalid number format '"
                    + value + "' provided for type '" + param.getType() + "'");
                return false;
            }
        }
        
        return true;
    }
    
    private static class ParameterValue {
        
        private int parameterIdx;
        private int columnIdx = -1;
        private String value;
        private String description;
        private char type = 'S';
        
        public ParameterValue (String description, int idx) {
            
            /*
             * By default, the value is the description.
             */
            this.description = description;
            this.value = description;
            this.parameterIdx = idx;
            
            if (description.length() > 2
                && value.charAt(1) == ':') {
            
                type = Character.toUpperCase(value.charAt(0));
                value = value.substring(2);
            }
            
            if (value.length() > 0 
                    && value.charAt(0) == '#') {
                
                if (value.length() == 1) {
                    
                    columnIdx = parameterIdx - 1;
                }
                else {
                    
                    columnIdx = Integer.parseInt(value.substring(1)) - 1;
                }
                
                value = null;
            }
        }
        
        public int getIdx() {
            
            return parameterIdx;
        }
        
        public int getColumnIdx() {
            
            return columnIdx;
        }
        
        public String getDescription() {
            
            return description;
        }
        
        public char getType() {
            
            return type;
        }
        
        public String getValue(String []row) {
            
            if (row == null) {
                
                return value;
            }
                
            return row[columnIdx];
        }
    }
}
