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
package org.sqsh.jaql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.sqsh.DataFormatter;
import org.sqsh.Session;

import com.ibm.jaql.io.serialization.text.TextFullSerializer;
import com.ibm.jaql.json.type.JsonBinary;
import com.ibm.jaql.json.type.JsonDate;
import com.ibm.jaql.json.type.JsonDecimal;
import com.ibm.jaql.json.type.JsonDouble;
import com.ibm.jaql.json.type.JsonEncoding;
import com.ibm.jaql.json.type.JsonLong;
import com.ibm.jaql.json.type.JsonValue;
import com.ibm.jaql.json.util.JsonIterator;
import com.ibm.jaql.util.FastPrintStream;

/**
 * Interface for a class capable of print JSON.
 */
public abstract class JaqlFormatter {
    
    protected Session session;
    protected JaqlSignalHandler sigHandler = null;
    protected DataFormatter formatter;
    private String doubleFormat = null;
    private int currentScale = -1;
    
    public JaqlFormatter (Session session) {
        
        this.session = session;
        this.formatter = session.getDataFormatter();
        setScale();
    }
    
    /**
     * @return the name of the formatter.
     */
    public abstract String getName();
    
    /**
     * Installs a flagging signal handler that will be (well, SHOULD be)
     * polled by the formatter while it is doing its work to determine
     * if it should abort its work.
     * 
     * @param sigHandler The signal handler to install, or null if you
     *   wish to totally uninstall the handler.
     */
    public void setSignalHandler (JaqlSignalHandler sigHandler) {
        
        this.sigHandler = sigHandler;
    }
    
    /**
     * This should be called by the formatter to determine if the current
     * query was canceled. If it was, then the formatter should attempt
     * to abort its work as soon as possible.
     * 
     * @return true if the query was canceled.
     */
    public final boolean isCanceled() {
        
        if (sigHandler == null)
            return false;
        
        return sigHandler.isTriggered();
    }
    
    
    /**
     * Write the results from a {@link JsonIterator}
     * @param iter The iterator
     * @return The number of "rows" returned, which typically corresponds
     *   to the number of elements in the outer most array.
     * @throws Exception
     */
    public abstract int write (FastPrintStream out, JsonIterator iter)
        throws Exception;
    
    /**
     * Write the results from a {@link JsonValue}
     * @param v The value to write
     * @return The number of "rows" returned, which typically corresponds
     *   to the number of elements in the outer most array.
     * @throws Exception
     */
    public abstract int write (FastPrintStream out, JsonValue v) 
        throws Exception;
    
    
    private static final char  HEX_CHARS[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
        'A', 'B', 'C', 'D', 'E', 'F'
    };    
    
    private Date date = new Date(0L);
    private TextFullSerializer serializer = TextFullSerializer.getDefault();
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private FastPrintStream byteStream = new FastPrintStream(buffer);
    
    /**
     * Writes a simple scalar value (i.e. not a record or array) to the
     * session's output, taking care to ensure that settings such as
     * "scale" are honored.
     * 
     * @param v The value to print.
     */
    protected void writeScalar (FastPrintStream out, JsonValue v, boolean isJsonFormat)
        throws IOException {
        
        if (!isJsonFormat) {
            
            switch (v.getEncoding()) {
            
                case BINARY: {
                        int len = ((JsonBinary)v).bytesLength();
                        byte[] bytes = ((JsonBinary)v).getInternalBytes();
                        for (int i = 0; i < len; i++) {
                            
                            byte b = bytes[i];
                            out.append(HEX_CHARS[(b >> 4) & 0x0f]);
                            out.append(HEX_CHARS[b & 0x0f]);
                        }
                        return;
                    }
                    
                case DATE:
                    date.setTime(((JsonDate)v).get());
                    out.append(formatter.getDatetimeFormatter().format(date));
                    return;
                    
                case DOUBLE:
                    double d = ((JsonDouble)v).doubleValue();
                    if (!isJsonFormat && doubleFormat != null) {
                    
                        out.append(String.format(doubleFormat, d));
                    }
                    else {
                        
                        out.append(Double.toString(d));
                    }
                    return;
                    
                case LONG:
                case STRING:
                    out.append(v.toString());
                    return;
                    
                default:
                    break;
            }
        }
            
        serializer.write(out, v);
    }
    
    /**
     * Returns a scalar value as a string. This string is not quoted or
     * protected in any way.
     * 
     * @param v The value to print.
     */
    protected String getScalar (JsonValue v, boolean isJsonFormat) {
        
        JsonEncoding encoding = v.getEncoding();
        
        /*
         * The normal serializer/encoder will throw double quotes around 
         * the string, which this method must not allow, so we never let
         * that happen.
         */
        if (encoding == JsonEncoding.STRING)
            return v.toString();
        
        if (!isJsonFormat) {
            
            switch (encoding) {
            
                case BINARY: {
                        StringBuilder sb = new StringBuilder();
                        int len = ((JsonBinary)v).bytesLength();
                        byte[] bytes = ((JsonBinary)v).getInternalBytes();
                        for (int i = 0; i < len; i++) {
                            
                            byte b = bytes[i];
                            sb.append(HEX_CHARS[(b >> 4) & 0x0f]);
                            sb.append(HEX_CHARS[b & 0x0f]);
                        }
                        return sb.toString();
                    }
                    
                case BOOLEAN:
                    return v.toString();
                    
                case DATE:
                    date.setTime(((JsonDate)v).get());
                    return formatter.getDatetimeFormatter().format(date);
                    
                case DECFLOAT:
                    BigDecimal bd = ((JsonDecimal)v).decimalValue();
                    return bd.toString();
                    
                case DOUBLE:
                    double d = ((JsonDouble)v).doubleValue();
                    if (!isJsonFormat && doubleFormat != null) {
                    
                        return String.format(doubleFormat, d);
                    }
                    else {
                    
                        return Double.toString(d);
                    }
                    
                case LONG:
                    return Long.toString(((JsonLong)v).longValue());
                   
                default:
                    break;
            }
        }
            
        buffer.reset();
        try {
                
            serializer.write(byteStream, v);
            byteStream.flush();
            return buffer.toString();
        }
        catch (IOException e) {
               
            return e.getMessage();
        }
    }
    
    /**
     * This method should be called at the beginning of each batch of output
     * from the formatter. It retrieves the current "scale" variable setting
     * and sets things up so that doubles that are printed, are printed with
     * the correct scale.
     */
    protected void setScale() {
        int scale = session.getDataFormatter().getScale();
        if (scale != currentScale) {
            if (scale >= 0) {
                doubleFormat = "%1$." + scale + "f";
            }
            else {
                doubleFormat = null;
            }
            currentScale = scale;
        }
    }
}
