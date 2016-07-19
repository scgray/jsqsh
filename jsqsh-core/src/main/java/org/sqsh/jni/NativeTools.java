/*
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2014
 * The source code for this program is not published or otherwise divested of
 * its trade secrets, irrespective of what has been deposited with the U.S. 
 * Copyright Office.
 */
package org.sqsh.jni;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NativeTools {

    /**
     * Load a library from current JAR archive
     */
    public static void loadLibraryFromJar(String path) throws IOException {

        // Obtain filename from path
        String[] parts = path.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;

        // Split filename to prefix and suffix (extension)
        String prefix = "";
        String suffix = null;
        if (filename != null) {
            
            parts = filename.split("\\.", 2);
            prefix = parts[0];
            suffix = (parts.length > 1) ? "." + parts[parts.length - 1] : null;
        }

        // Check if the filename is okay
        if (filename == null || prefix.length() < 3) {
            
            throw new IllegalArgumentException(
                "The filename has to be at least 3 characters long.");
        }

        // Prepare temporary file
        File temp = File.createTempFile(prefix, suffix);
        temp.deleteOnExit();

        if (!temp.exists()) {
            
            throw new FileNotFoundException("File " + temp.getAbsolutePath()
                + " does not exist.");
        }

        // Prepare buffer for data copying
        byte[] buffer = new byte[1024];
        int readBytes;

        // Open and check input stream
        InputStream is = NativeTools.class.getResourceAsStream(path);
        if (is == null) {
            
            throw new FileNotFoundException("File " + path
                + " was not found inside JAR.");
        }

        // Open output stream and copy data between source file in JAR and the
        // temporary file
        OutputStream os = new FileOutputStream(temp);
        try {
            
            while ((readBytes = is.read(buffer)) != -1) {
                
                os.write(buffer, 0, readBytes);
            }
        }
        finally {
            
            // If read/write fails, close streams safely before throwing an
            // exception
            os.close();
            is.close();
        }

        // Finally, load the library
        System.load(temp.getAbsolutePath());
    }
}
