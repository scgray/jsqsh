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

import org.apache.commons.digester3.Digester;
import org.sqsh.variables.StringVariable;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * Provides an interface to manipulating session variables. This class
 * goes to some length so that it can be utilized as a Map but does not
 * properly implement all of the methods that Map requires, as these
 * were not necessary for my purposes.
 */
public class VariableManager
    implements Map<String, Object> {
    
    private static final Logger LOG = 
        Logger.getLogger(VariableManager.class.getName());
    
    private final VariableManager parent;
    
    private Map<String, Variable>variables = 
        new HashMap<String, Variable>();
    
    /**
     * Map of named beans that the PropertyVariable can refer to.
     */
    private Map<String, Object>beans = new HashMap<String, Object>();
    
    private static class VarEntry
        implements Entry<String, Object> {
        
        private Variable var;
        
        public VarEntry (Variable var) {
            
            this.var = var;
        }

        /**
         * @see java.util.Map.Entry#getKey()
         */
        @Override
        public String getKey () {

            return var.getName();
        }

        /**
         * @see java.util.Map.Entry#getValue()
         */
        @Override
        public Object getValue () {

            return var.toString();
        }

        /**
         * @see java.util.Map.Entry#setValue(java.lang.Object)
         */
        @Override
        public Object setValue (Object value) {

            String oldValue = var.toString();
            var.setValue(value == null ? null : value.toString());
            
            return oldValue;
        }
    }
    
    /**
     * Creates a variable manager.
     */
    public VariableManager () {

        this.parent = null;
    }
    
    /**
     * Creates a variable manager with a parent manager. In this case,
     * gets will attempt to defer to the parent manager if this one
     * doesn't have the value.
     * 
     * @param parent The parent manager.
     */
    public VariableManager (VariableManager parent) {
        
        this.parent = parent;
    }
    
    /**
     * Adds a bean to the context.
     * @param name The name of the bean
     * @param bean The bean
     */
    public void addBean(String name, Object bean) {
        
        beans.put(name, bean);
    }
    
    /**
     * Returns a named bean.
     * @param name The name of the bean
     * @return the bean or null if it does not exist.
     */
    public Object getBean(String name) {
        
        return beans.get(name);
    }
    
    /**
     * Returns the complete list of variables.
     * 
     * @return The complete list of variables.
     */
    public Variable[] getVariables() {
        
        return getVariables(false);
    }
    
    /**
     * Returns the complete list of variables.
     * 
     * @param includeParent If true, then the variables that are defined
     *   in the parent's context are included in the list.
     *   
     * @return The complete list of variables.
     */
    public Variable[] getVariables(boolean includeParent) {
        
        if (includeParent == false || parent == null) {
            
            return variables.values().toArray(new Variable[0]);
        }
        
        List<Variable> vars = new ArrayList<Variable>();
        vars.addAll(variables.values());
        
        for (Variable var : parent.getVariables()) {
            
            vars.add(var);
        }
        
        return vars.toArray(new Variable[0]);
    }
    
    
    /**
     * Clears the variable map.
     */
    @Override
    public void clear() {
        
        variables.clear();
    }
    
    /**
     * Sets a variable.
     * 
     * @param name The name of the variable.
     * @param value The value of the variable.
     * 
     * @return The old value of the variable.
     */
    @Override
    public String put(String name, Object value) {
        
        return put(name, value, false);
    }

    @Override
    public void putAll(Map<? extends String, ?> map) {
        for (Entry<?, ?> e : map.entrySet()) {
            put(e.getKey().toString(), e.getValue());
        }
    }

    /**
     * Sets the value of a session variable.
     * 
     * @param name The name of the variable to set.
     * @param value The value of the variable to set.
     * @param isExported Set to true if the variable is to be exported
     *   to processes spawned by this session.
     * @return The current state of the variable.
     * 
     * @throws CannotSetValueError if the value of the variable cannot be
     * changed for one reason or another.
     */
    public String put(String name, Object value, boolean isExported)
        throws CannotSetValueError {

        final String valueStr = value == null ? null : value.toString();
        Variable var = variables.get(name);
        if (var == null && parent != null) {

            var = parent.getVariable(name);
        }

        String oldValue = null;
        if (var == null) {
            
            var = new StringVariable(name, valueStr, isExported);
            var.setExported(isExported);
            
            put(var);
        }
        else {
            
            oldValue = var.toString();
            var.setValue(valueStr);
        }
        
        return oldValue;
    }
    
    /**
     * Adds a variable.
     * @param var
     */
    public void put(Variable var) {
        
        var.setManager(this);
        variables.put(var.getName(), var);
    }

    /**
     * Given a string of the form <code>name=value</code> set the variable of
     * the specified name to the provided value. If the value is missing or there
     * is no = then the value is set to null.
     *
     * @param nameValue A string of the form <code>name[=[value]]</code>
     */
    public void put(String nameValue) {

        int idx = nameValue.indexOf('=');
        String name;
        String value = null;

        if (idx < 0) {

            name = nameValue;
        }
        else {

            name  = nameValue.substring(0, idx);
            if (idx < (nameValue.length() - 1)) {

                value = nameValue.substring(idx+1);
            }
        }

        put(name, value);
    }

    /**
     * Adds a variable that can never be removed.
     * @param var
     */
    public void putUnremoveable(Variable var) {
        
    	var.setRemoveable(false);
        var.setManager(this);
        variables.put(var.getName(), var);
    }
    
    /**
     * Returns true of the manager contains the provided key.
     * 
     * @param key The name of a variable.
     */
    @Override
    public boolean containsKey(Object key) {
        
        boolean ok = variables.containsKey(key);
        if (ok == false && parent != null) {
            
            ok = parent.containsKey(key);
        }
        
        return ok;
    }
    
    /**
     * Same as containsKey() except that it only looks in the local manager
     * and not the parent.
     * 
     * @param key The key to look for.
     * @return true if the key is contained locally.
     */
    public boolean containsLocal(Object key) {
        
        return variables.containsKey(key);
    }
    
    /**
     * Returns true if the manager contains the provided variable.
     * 
     * @param value The value we are searching for.
     * @return true on match.
     */
    @Override
    public boolean containsValue(Object value) {
        
        boolean ok = false;
        for (Variable var : variables.values()) {
            
            if (value.equals(var.toString())) {
                
                ok = true;
                break;
            }
        }
        
        if (ok == false && parent != null) {
            
            ok = parent.containsValue(value);
        }
        
        return ok;
    }
    
    /**
     * Returns the entry set for the map.
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        
        Set<Entry<String, Object>> set = new HashSet<>();
        
        for (Variable var : variables.values()) {
            
            set.add(new VarEntry(var));
        }
        
        if (parent != null) {
            
            set.addAll(parent.entrySet());
        }
        
        return set;
    }
    
    /**
     * Returns false.
     */
    @Override
    public boolean equals(Object o) {
        
        return false;
    }
    
    /**
     * Returns the current state of a variable.
     * 
     * @param name The name of the variable to return.
     * @return The current variable state or null if the variable
     *   is not defined.
     */
    @Override
    public String get(Object name) {
        
        Variable var = variables.get(name);
        if (var == null && parent != null) {
            
            return parent.get(name);
        }
        
        if (var != null) {
            
            return var.toString();
        }
        
        return null;
    }
    
    /**
     * Provides direct access to the underlying variable.
     * 
     * @param name The name of the variable to be retrieved.
     * @return The variable or null if there is none.
     */
    public Variable getVariable(String name) {
        
        Variable var = variables.get(name);
        if (var == null && parent != null) {
            
            return parent.getVariable(name);
        }
        
        return var;
    }
    
    /**
     * Returns true of the map is empty.
     */
    @Override
    public boolean isEmpty() {
        
        boolean ok = (variables.size() == 0);
        if (ok == false && parent != null) {
            
            ok = parent.isEmpty();
        }
        
        return ok;
    }
    
    /**
     * Returns the set of keys installed in this map.
     */
    @Override
    public Set<String> keySet() {
        
        if (parent == null) {
            
            return variables.keySet();
        }
        
        Set<String> set = new HashSet<String>();
        set.addAll(variables.keySet());
        set.addAll(parent.keySet());
        
        return set;
    }
    
    /**
     * Removes a variable.
     * @param name The name of the variable.
     * @return The variable that was removed or null if the variable
     *   was not defined in the first place.
     */
    @Override
    public String remove(Object name) {
        
        Variable var = variables.get(name);
        if (var == null && parent != null) {
            
            return parent.remove(name);
        }
        
        if (!var.isRemoveable()) {
       
            throw new IllegalAccessError("Variable \"" + name + "\" is a "
                + "configuration variable and cannot be unset");
        }
        
        variables.remove(name);
        return var.toString();
    }
    
    /**
     * Returns the number of entries in the map.
     */
    @Override
    public int size() {
        
        int size = variables.size();
        if (parent != null) {
            
            size += parent.size();
        }
        
        return size;
    }
    
    /**
     * Returns the values o the map.
     */
    @Override
    public Collection<Object> values() {
        
        List<Object> values = new ArrayList<>();
        for (Variable var : variables.values()) {
            
            values.add(var.toString());
        }
        
        if (parent != null) {
            
            values.addAll(parent.values());
        }
        
        return values;
    }

    /**
     * Initializes the variable manager with variables as described by
     * the XML file pointed to by URL.
     */
    public void load(String location, InputStream in) {
        
        load(null, location, in);
    }
    
    /**
     * Initializes the variable manager with variables as described by
     * the XML file pointed to by URL.
     */
    public void load(ClassLoader loader, String location, InputStream in) {
        
        String path;
        Digester digester = new Digester();
        digester.setValidating(false);
        
        if (loader != null) {
            
            digester.setClassLoader(loader);
        }
        
        path = "Variables/String";
        digester.addObjectCreate(path, "org.sqsh.variables.StringVariable");
        digester.addSetNext(path, "putUnremoveable", "org.sqsh.Variable");
        digester.addCallMethod(path, 
            "setName", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "name");
        digester.addCallMethod(path, 
            "setValue", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "value");
            
        path = "Variables/Integer";
        digester.addObjectCreate(path, "org.sqsh.variables.IntegerVariable");
        digester.addSetNext(path, "putUnremoveable", "org.sqsh.Variable");
        digester.addCallMethod(path, 
            "setName", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "name");
        digester.addCallMethod(path, 
            "setMinValue", 1, new Class[] { java.lang.Integer.class });
            digester.addCallParam(path, 0, "min");
        digester.addCallMethod(path, 
            "setMaxValue", 1, new Class[] { java.lang.Integer.class });
            digester.addCallParam(path, 0, "max");
        digester.addCallMethod(path, 
            "setValue", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        path = "Variables/Dynamic";
        digester.addObjectCreate(path, "org.sqsh.Variable", "class");
        digester.addSetNext(path, "putUnremoveable", "org.sqsh.Variable");
        digester.addCallMethod(path, 
            "setName", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "name");
        digester.addCallMethod(path, 
            "setValue", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "value");
            
        path = "Variables/Property";
        digester.addObjectCreate(path,  "org.sqsh.variables.PropertyVariable");
        digester.addSetNext(path, "putUnremoveable", "org.sqsh.Variable");
        digester.addCallMethod(path, 
            "setName", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "name");
        digester.addCallMethod(path, 
            "setBean", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "bean");
        digester.addCallMethod(path, 
            "setProperty", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0, "property");
        digester.addCallMethod(path, 
            "setSettable", 1, new Class[] { java.lang.Boolean.class });
            digester.addCallParam(path, 0, "settable");
        digester.addCallMethod(path, 
            "setQuiet", 1, new Class[] { java.lang.Boolean.class });
            digester.addCallParam(path, 0, "quiet");
            
        path = "*/Description";
        digester.addCallMethod(path, 
            "setDescription", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
        path = "*/HelpLocation";
        digester.addCallMethod(path, 
            "setHelpLocation", 1, new Class[] { java.lang.String.class });
            digester.addCallParam(path, 0);
            
        digester.push(this); 
        try {
            
            digester.parse(in);
        }
        catch (Exception e) {
            
            LOG.severe("Failed to parse variable definition file '"
                + location + "': " + e.getMessage());
        }
    }

}
