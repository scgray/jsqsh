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
package org.sqsh;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;

/**
 * This class provides a method to prompt a user for input, masking the
 * characters that they type. This is stolen entirely from:
 * http://java.sun.com/developer/technicalArticles/Security/pwordmask/
 * and I deserve no credit whatsoever (or ownership) for this logic.
 */
public class PasswordInput {
    
    /**
     * This class attempts to erase characters echoed to the console.
     */
    private static class MaskingThread
        extends Thread {

        private volatile boolean stop;
        private char echochar = '*';

        /**
         * @param prompt
         *            The prompt displayed to the user
         */
        public MaskingThread(String prompt) {

            System.out.print(prompt);
        }

        /**
         * Begin masking until asked to stop.
         */
        public void run () {

            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            try {
                stop = true;
                while (stop) {
                    
                    System.out.print("\010" + echochar);
                    
                    try {
                        // attempt masking at this rate
                        Thread.sleep(1);
                    }
                    catch (InterruptedException iex) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            finally { // restore the original priority
                
                Thread.currentThread().setPriority(priority);
            }
        }

        /**
         * Instruct the thread to stop masking.
         */
        public void stopMasking () {

            this.stop = false;
        }
    }
    
    public static final char[] getPassword (InputStream in, String prompt)
            throws IOException {

        MaskingThread maskingthread = new MaskingThread(prompt);
        Thread thread = new Thread(maskingthread);
        thread.start();

        char[] lineBuffer;
        char[] buf;
        int i;

        buf = lineBuffer = new char[128];

        int room = buf.length;
        int offset = 0;
        int c;

        loop: while (true) {
            switch (c = in.read()) {
                case -1:
                case '\n':
                    break loop;

                case '\r':
                    int c2 = in.read();
                    if ((c2 != '\n') && (c2 != -1)) {
                        if (!(in instanceof PushbackInputStream)) {
                            in = new PushbackInputStream(in);
                        }
                        ((PushbackInputStream) in).unread(c2);
                    }
                    else {
                        break loop;
                    }

                default:
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                        Arrays.fill(lineBuffer, ' ');
                        lineBuffer = buf;
                    }
                    buf[offset++] = (char) c;
                    break;
            }
        }
        maskingthread.stopMasking();
        if (offset == 0) {
            
            return null;
        }
        
        char[] ret = new char[offset];
        System.arraycopy(buf, 0, ret, 0, offset);
        Arrays.fill(buf, ' ');
        return ret;
    }
}
    