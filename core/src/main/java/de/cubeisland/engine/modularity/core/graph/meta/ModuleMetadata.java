/*
 * The MIT License
 * Copyright © 2014 Cube Island
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
package de.cubeisland.engine.modularity.core.graph.meta;

import java.util.Set;
import de.cubeisland.engine.modularity.core.graph.Dependency;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;

/**
 * Describes metadata for a module
 */
public interface ModuleMetadata extends DependencyInformation
{
    /**
     * Returns the name
     *
     * @return the name
     */
    String getName();

    /**
     * Returns a short description
     * @return the description
     */
    String getDescription();

    /**
     * Returns the authors
     * @return the authors
     */
    Set<String> getAuthors();

    /**
     * Returns the identifiers of other modules which should be started before this one
     * @return the modules to load before
     */
    Set<Dependency> loadAfter();
}
