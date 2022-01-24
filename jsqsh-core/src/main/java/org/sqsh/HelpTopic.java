/*
 * Copyright 2007-2022 Scott C. Gray
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
import java.io.InputStream;
import java.io.InputStreamReader;

public class HelpTopic
    implements Comparable<HelpTopic> {

    public static final String DEFAULT_HELP_LOCATION = "org/sqsh/docs";
    
    private String topic;
    private String description;
    private String helpLocation;
    
    public HelpTopic() {
        
    }
    
    /**
     * Base constructor for a help topic.
     * @param topic The topic of the topic.
     * @param description A brief description of the topic
     * @param helpLocation The location of the full Markdown help text for the topic.
     *     The location specified must be readable using the default jsqsh classloader
     *     (i.e. it should be contained in a jar somewhere).  If this value is null, then
     *     <i>org/sqsh/docs/topicName.md</i> is assumed. If the topic name started with a
     *     a backslash, then the backslash is removed.
     */
    public HelpTopic (String topic, String description, String helpLocation) {
        
        this.topic = topic;
        this.description = description;
        this.helpLocation = helpLocation;
    }
    
    /**
     * Returns help text for this topic.
     * 
     * @return The help text for the topic, or null if none is available.
     */
    public String getHelp() {

        String resourcePath = helpLocation == null
                ? DEFAULT_HELP_LOCATION + "/" + (topic.charAt(0) == '\\' ? topic.substring(1) : topic) + ".md"
                : helpLocation;

        InputStream helpStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (helpStream == null) {

            return "Help resource \"" + resourcePath + "\" could not be found!";
        }

        try {

            BufferedReader buf = new BufferedReader(new InputStreamReader(helpStream));
            StringBuilder sb = new StringBuilder();
            String lineSep = System.getProperty("line.separator");
            String line;

            while ((line = buf.readLine()) != null) {

                sb.append(line).append(lineSep);
            }

            return sb.toString();
        }
        catch (IOException e) {

            return "Error reading help resource \"" + resourcePath + "\": " + e.getMessage();
        }
        finally {

            try { helpStream.close(); } catch (IOException e2) { /* IGNORED */ }
        }
    }

    /**
     * Sets where the help text for the topic can be found. The location specified must be
     * readable using the default jsqsh classloader (i.e. it should be contained in a
     * jar somewhere).  If this value is null, then  <i>org/sqsh/docs/topicName.md</i> is
     * assumed. If the topic name started with a backslash, then the backslash is removed.
     * @param helpLocation The help location or null to use the default location for the topic
     */
    public void setHelpLocation(String helpLocation) {

        this.helpLocation = helpLocation;
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
     * @param that The object to compare to.
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
