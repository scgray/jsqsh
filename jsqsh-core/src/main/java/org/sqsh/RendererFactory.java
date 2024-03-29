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

/**
 * A <code>RendererFactory</code> can be registered with a {@link RendererManager}
 * as a resource that is capable for manufacturing renderers.
 */
public interface RendererFactory {

    /**
     * Called to return a renderer by a given name
     *
     * @param name The name of the renderer
     * @return An instance of a renderer or null if there is no renderer by
     *    the name provided
     */
    public Renderer get(String name);
}
