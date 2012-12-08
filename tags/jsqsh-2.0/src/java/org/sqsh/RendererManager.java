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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.sqsh.renderers.PerfectPrettyRenderer;

/**
 * The renderer manager is responsible for doling out renderers as needed.
 * and providing configuration hints to the renderers (such as whether or
 * not to show headers).
 */
public class RendererManager {
    
    private static final Logger LOG = 
        Logger.getLogger(RendererManager.class.getName());
    
    private boolean showHeaders = true;
    private boolean showFooters = true;
    private int maxColumnWidth = 35;
    private int perfectSampleSize = 500;
    private String defaultRenderer = "perfect";
    
    private Map<String, Class<? extends Renderer>> renderers = 
        new HashMap<String, Class<? extends Renderer>>();
    
    /**
     * Creates a renderer manager.
     * 
     * @param session The parent session.
     */
    public RendererManager () {
        
        /*
         * Normally I would have the manager load its configuration of
         * renderers from an XML file, but there is really no reason to
         * do this and I'm getting lazy.
         */
        renderers.put("pretty",
            org.sqsh.renderers.PrettyRenderer.class);
        renderers.put("perfect",
            org.sqsh.renderers.PerfectPrettyRenderer.class);
        renderers.put("csv",
            org.sqsh.renderers.CSVRenderer.class);
        renderers.put("insert",
            org.sqsh.renderers.InsertRenderer.class);
        renderers.put("graphical",
            org.sqsh.renderers.GraphicalRenderer.class);
        renderers.put("graph",
            org.sqsh.renderers.GraphicalRenderer.class);
        renderers.put("tree", 
            org.sqsh.renderers.GraphicalTreeRenderer.class);
        renderers.put("discard",
            org.sqsh.renderers.DiscardRenderer.class);
        renderers.put("isql",
            org.sqsh.renderers.ISQLRenderer.class);
        renderers.put("vert",
            org.sqsh.renderers.VerticalRenderer.class);
        renderers.put("vertical",
            org.sqsh.renderers.VerticalRenderer.class);
        renderers.put("json",
            org.sqsh.renderers.JsonRenderer.class);
    }
    
    /**
     * Returns the renderer that should be used by commands when displaying
     * their output.
     * 
     * @return The renderer.
     */
    public Renderer getCommandRenderer(Session session) {
        
        return getRenderer(session, "perfect");
    }
    
    /**
     * Creates an instance of the default renderer.
     * 
     * @return an instance of the default renderer.
     */
    public Renderer getRenderer(Session session) {
        
        return getRenderer(session, defaultRenderer);
    }
    
    /**
     * Returns a renderer by name.
     * 
     * @param session The session that the renderer is to render to.
     * @param name The name of the renderer to create
     * @return A newly created renderer.
     */
    public Renderer getRenderer(Session session, String name) {
        
        Class<? extends Renderer> renderer = renderers.get(name);
        try {
            
            Constructor<? extends Renderer> constructor = 
                renderer.getConstructor(Session.class, RendererManager.class);
            
            return constructor.newInstance(session, this);
        }
        catch (Exception e) {
            
            LOG.severe("Unable to instantiate renderer '"
                + name + "': " + e.getMessage());
            
            return new PerfectPrettyRenderer(session, this);
        }
    }
    
    /**
     * Sets the name of the default renderer that will be used.
     * 
     * @param renderer The name of the default renderer that will be used.
     */
    public void setDefaultRenderer(String renderer) {
        
        if (renderers.containsKey(renderer)) {
            
            defaultRenderer = renderer;
            return;
        }
        
        throw new CannotSetValueError("Display style '" + renderer
            + "' is not a valid SQL display style. See \"help \\style\"");
    }
    
    /**
     * Returns the name of the default renderer.
     * @return the name of the default renderer.
     */
    public String getDefaultRenderer() {
        
        return defaultRenderer;
    }
    
    /**
     * @return whether or not headers will be displayed.
     */
    public boolean isShowHeaders () {
    
        return showHeaders;
    }
    
    /**
     * @param showHeaders Sets whether or not headers will be displayed
     *   by renderers that are created by this manager.
     */
    public void setShowHeaders (boolean showHeaders) {
    
        this.showHeaders = showHeaders;
    }
    
    /**
     * @return whether or not the results will show "footer" information
     *   (row counts, query timings, etc).
     */
    public boolean isShowFooters () {
    
        return showFooters;
    }

    /**
     * @param showFooters Sets whether or not the results will
     *   show "footer" information (row counts, query timings, etc).
     */
    public void setShowFooters (boolean showFooters) {
    
        this.showFooters = showFooters;
    }

    /**
     * @return The suggested maximum column width. Not every renderer
     *   will pay attention to this session.
     */
    public int getMaxColumnWidth () {
    
        return maxColumnWidth;
    }

    /**
     * @param maxColumnWidth The new suggested maximum column width.
     */
    public void setMaxColumnWidth (int maxColumnWidth) {
    
        this.maxColumnWidth = maxColumnWidth;
    }

    
    /**
     * @return the number of rows that will be sampled before performing
     *    "perfect" rendering. A value <= 0 indicates that all rows will
     *    be sampled before rendering.
     */
    public int getPerfectSampleSize() {
    
        return perfectSampleSize;
    }
    
    /**
     * @param perfectSampleSize Indicates the number of rows that will be
     *    sampled before performing "perfect" rendering. A value <= 0 indicates 
     *    that all rows will be sampled before rendering.
     */
    public void setPerfectSampleSize(int perfectSampleSize) {
    
        this.perfectSampleSize = perfectSampleSize;
    }
}
