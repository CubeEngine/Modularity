/**
 * The MIT License
 * Copyright (c) 2014 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.engine.modularity.core.graph;

import java.util.Set;
import de.cubeisland.engine.modularity.core.ModularityClassLoader;

/**
 * Describes basic DependencyInformation
 */
public interface DependencyInformation
{
    /**
     * Returns the identifier with version if known
     *
     * @return the identifier
     */
    String getIdentifier();

    /**
     * The class to instantiate
     *
     * @return the class
     */
    String getClassName();

    /**
     * The actual class this dependency is providing
     *
     * @return the actual class
     */
    String getActualClass();

    /**
     * Returns the source version if available
     *
     * @return the source version or null
     */
    String getSourceVersion();

    /**
     * Returns the version
     *
     * @return the version
     */
    String getVersion();

    /**
     * Returns a set of required dependencies
     *
     * @return the required dependencies
     */
    Set<String> requiredDependencies();

    /**
     * Returns a set of optional dependencies
     *
     * @return the optional dependencies
     */
    Set<String> optionalDependencies();

    /**
     * Returns the responsible ModularityClassLoader
     *
     * @return the ModularityClassLoader
     */
    ModularityClassLoader getClassLoader();

    /**
     * Returns the name of the method to call when setting up
     *
     * @return the name of the method to call when setting up
     */
    String getSetupMethod();

    /**
     * Returns the name of the method to call when enabling
     *
     * @return the name of the method to call when enabling
     */
    String getEnableMethod();

    /**
     * Returns the name of the method to call when disabling
     *
     * @return the name of the method to call when disabling
     */
    String getDisableMethod();
}
