/*
 * Copyright 2007-2016 Scott C. Gray
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
 * Defines an interface for a class wishing to be notified by the {@link org.sqsh.SQLDriverManager}
 * when a driver becomes fully available (that is, when it successfully is
 * loaded by a class loader).  The primary purpose of this interface is to
 * notify the {@link ExtensionManager} so that it can automatically load
 * extensions when a particular driver becomes available.
 */
public interface SQLDriverListener {
    
    /**
     * Called when a driver becomes available for use
     * 
     * @param dm The SQLDriverManager for which the driver has become available
     * @param driver The driver that has become available
     */
    void driverAvailable (SQLDriverManager dm, SQLDriver driver);
}
