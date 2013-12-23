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

import java.util.ArrayList;
import java.util.List;

/**
 * Manager that is responsible for managing sqsh aliases.
 */
public class AliasManager {
    
    private List<Alias> aliases = new ArrayList<Alias>();
    
    /**
     * Creates an alias manager.
     */
    public AliasManager() {
        
    }
    
    /**
     * Adds an alias to this manager.
     * 
     * @param alias The alias to add.
     */
    public void addAlias (Alias alias) {
        
        for (int i = 0; i < aliases.size(); i++) {
            
            if (alias.getName().equals(aliases.get(i).getName())) {
                
                aliases.remove(i);
                break;
            }
        }
        
        aliases.add(alias);
    }
    
    /**
     * Returns the set of aliases managed by this manager.
     * 
     * @return The list of aliases.
     */
    public Alias[] getAliases() {
        
        return aliases.toArray(new Alias[0]);
    }
    
    /**
     * Looks up an alias by name and returns it.
     * 
     * @param name The name of the alias.
     * @return The alias matching the supplied name or null if there is
     *    no such alias.
     */
    public Alias getAlias (String name) {
        
        for (Alias alias : aliases) {
            
            if (name.equals(alias.getName())) {
                
                return alias;
            }
        }
        
        return null;
    }
    
    /**
     * Given a line of input from a user, processes that line of any aliases
     * it may contain.
     * 
     * @param line The line of text to be evaluated.
     * @return If the line contains an alias, the alias will be expanded at
     *   the point it is located. If there are no aliases, the line will be
     *   left untouched.
     */
    public String process (String line) {
        
        /*
         * The following may look at little awkward. What we should be doing
         * is tearing the input apart word-by-word and comparing it to our
         * aliases, but it seems a shame to create all those objects if the
         * line contains no aliases to expand (which should be the case 99%
         * of the line). So, instead we go through two phases: first analyze
         * the line to see if there are aliases to expand, then expand them.
         */
        List <ExpansionPoint> expansions = getExpansionPoints(line);
        
        /*
         * If no expansion points are found, then just return the original
         * line untouched.
         */
        if (expansions == null) {
            
            return line;
        }
        
        StringBuilder sb = new StringBuilder();
        int startIdx = 0;
        
        for (ExpansionPoint point : expansions) {
            
            /*
             * Append segment of original line that didn't contain the
             * alias.
             */
            if (point.idx > startIdx) {
                
                sb.append(line, startIdx, point.idx);
            }
            
            /*
             * Append the alias.
             */
            sb.append(point.alias.getText());
            
            /*
             * The next segment of our original input line appears just
             * after where the alias occurred.
             */
            startIdx = point.idx + point.alias.getName().length();
        }
        
        if (startIdx < line.length()) {
            
            sb.append(line, startIdx, line.length());
        }
        
        return sb.toString();
    }
    
    /**
     * Analyzes a line of input to see if any aliases need to be expanded.
     * @param line The line to analyze.
     * @return A list of expansions points or null if there are none to
     *   be had.
     */
    private List<ExpansionPoint> getExpansionPoints(String line)  {
        
        ArrayList <ExpansionPoint>expansions = null;
        
        int len = line.length();
        int start = 0;
        int idx = 0;
        
        
        /*
         * First, skip whitepaces on the line.
         */
        for (; start < len
            && Character.isWhitespace(line.charAt(start)); ++start);
        
        
        /*
         * Iterate through our aliases.
         */
        for (Alias alias : aliases) {
            
            String name = alias.getName();
            
            /*
             * First we do a rudamentary check to see if the name of the
             * alias exists anywhere in the line at all.
             */
            idx = line.indexOf(name);
            while (idx >= 0) {
                
                /*
                 * Look at the character immediately following the location
                 * at which we found the alias.
                 */
                char ch = (idx + name.length() == len) 
                    ? ' ' : line.charAt(idx + name.length());
                
                /*
                 * If it is a white-space then we have found an alias 
                 * that we (probably) need to expand.
                 */
                if (!Character.isLetter(ch)
                        && !Character.isDigit(ch)
                        && ch != '_') {
                    
                    /*
                     * If the alias is not global the alias must be found 
                     * at the start of the string.
                     */
                    if (alias.isGlobal() || idx == start) {
                        
                        if (expansions == null) {
                            
                            expansions = new ArrayList<ExpansionPoint>();
                        }
                        
                        expansions.add(new ExpansionPoint(alias, idx));
                    }
                }
                
                /*
                 * Don't bother moving forward for another occurance of
                 * the alias if it isn't global or if we have hit the
                 * end of the line.
                 */
                if (alias.isGlobal() == false 
                        || idx + name.length() >= len) {
                    
                    idx = -1;
                }
                else {
                    
                    idx = line.indexOf(name, idx + name.length());
                }
            }
        }
        
        return expansions;
    }
    
    /**
     * Used internally to represent a location within the input string 
     * at which we need to expand an alias.
     */
    private static class ExpansionPoint {
        
        public Alias alias;
        public int idx;
        
        public ExpansionPoint (Alias alias, int idx) {
            
            this.alias = alias;
            this.idx = idx;
        }
    }
}
