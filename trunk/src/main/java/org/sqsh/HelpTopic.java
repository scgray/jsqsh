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
package org.sqsh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class HelpTopic
    implements Comparable<HelpTopic> {
    
    private String topic;
    private String description;
    private String helpText;
    
    public HelpTopic() {
        
    }
    
    /**
     * Base constructor for a help topic.
     * @param topic The topic of the topic.
     * @param description A brief description of the topic
     * @param helpText The full help text for the topic.
     */
    public HelpTopic (String topic, String description, String helpText) {
        
        this.topic = topic;
        this.description = description;
        this.helpText = helpText;
    }
    
    /**
     * Returns help text for this topic.
     * 
     * @return The help text for the topic, or null if none is available.
     */
    public String getHelp() {
        
        return helpText;
    }
    
    /**
     * Sets the help text. Note that this method does a little
     * extra processing that "cleans up" the text that is passed it. That is,
     * if the text has leading spaces or leading or trailing blank lines,
     * they are removed.
     * 
     * @param help The help text to be set.
     */
    @SuppressWarnings("unused")
    public void setHelp(String help) {
        
        this.helpText = help;
        
        if (false) {
        StringBuilder buffer = new StringBuilder();
        String lineSep = System.getProperty("line.separator");
        
        try {
            
            BufferedReader reader = new BufferedReader(new StringReader(help));
            
            /*
             * The first line always already has its white-space removed
             * due to the XML parser
             */
            reader.readLine();
            
            /*
             * First, we'll take a pass through the help text to see if we need
             * to trim off leading spaces. We skip the first line because
             * we alreader trimmed leading spaces from the help text as a whole,
             * above.
             */
            int leadingSpaces = 999;
            String line = reader.readLine();
            while (line != null) {
                
                int nSpaces = 0;
                for (; nSpaces < line.length()
                    && Character.isWhitespace(line.charAt(nSpaces)); ++nSpaces);
                
                if (nSpaces != line.length()
                        && nSpaces < leadingSpaces) {
                    
                    leadingSpaces = nSpaces;
                }
                
                line = reader.readLine();
            }
            
            /*
             * Now, start over and remove the leading spaces.
             */
            reader = new BufferedReader(new StringReader(help));
            
            line = reader.readLine();
            while (line != null) {
                
                int nSpaces = 0;
                for (; nSpaces < line.length()
                    && Character.isWhitespace(line.charAt(nSpaces)); ++nSpaces);
                
                if (nSpaces >= leadingSpaces) {
                        
                    buffer.append(line, leadingSpaces, line.length());
                }
                else {
                        
                    buffer.append(line);
                }
                    
                buffer.append(lineSep);
                    
                line = reader.readLine();
            }
        }
        catch (IOException e) {
            
            /* CANNOT REALLY HAPPEN */
        }
        
        helpText = buffer.toString();
        }
    }
    
    /**
     * Returns a brief description of the topic
     * @return A brief description of the topic
     */
    public String getDescription() {
        
        return description;
    }
    
    /**
     * Sets a description of the topic.
     * @param description A description of the topic.
     */
    public void setDescription(String description) {
        
        this.description = description.trim();
    }
    
    /**
     * Set the topic of the topic.
     * 
     * @param topic The topic of the topic.
     */
    public void setTopic(String topic) {
        
        this.topic = topic;
    }
    
    /**
     * Returns the name of the topic.
     * 
     * @return The name of the topic.
     */
    public String getTopic() {
        
        return topic;
    }
    
    /**
     * Compares the names of two topics. This method is provided primarily
     * to allow for easy sorting of topics on display.
     * 
     * @param o The object to compare to.
     * @return The results of the comparison.
     */
    public int compareTo(HelpTopic that) {
        
        return this.topic.compareTo(that.getTopic());
    }
    
    /**
     * Compares two topics for equality. topics are considered
     * equal if their names match.
     * 
     * @param o The object to test equality.
     * @return true if o is a Command that has the same name as this.
     */
    public boolean equals(Object o) {
        
        if (o instanceof Command) {
            
            return ((HelpTopic) o).getTopic().equals(topic);
        }
        
        return false;
    }
    
    /**
     * Returns a hash value for the topics. The hash code is nothing
     * more than the hash code for the command name itself.
     * 
     * @return The hash code of the command's name.
     */
    public int hashCode() {
        
        return topic.hashCode();
    }
}
