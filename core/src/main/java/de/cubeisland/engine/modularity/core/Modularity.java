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
package de.cubeisland.engine.modularity.core;

import java.io.File;
import java.util.Set;
import de.cubeisland.engine.modularity.core.graph.DependencyGraph;
import de.cubeisland.engine.modularity.core.service.ProxyServiceContainer;
import de.cubeisland.engine.modularity.core.service.ServiceContainer;
import de.cubeisland.engine.modularity.core.service.ServiceManager;

public interface Modularity
{
    /**
     * Loads a all DependencyInformation from given source
     *
     * @param source the source
     *
     * @return fluent interface
     */
    BasicModularity load(File source);

    /**
     * Attempts to start a Module with given identifier.
     *
     * @param identifier the identifier
     *
     * @return true if the module was loaded
     */
    Object getStarted(String identifier);

    /**
     * Returns the InformationLoader
     *
     * @return the InformationLoader
     */
    InformationLoader getLoader();

    <T> T getStarted(Class<T> type);

    Set<Instance> getNodes();

    Set<Module> getModules();

    Set<ServiceContainer<?>> getServices();

    /**
     * Returns the loaded class with given name. Searching first in the ClassLoaders of the dependencies.
     *
     * @param name         the name of the class to load
     * @param dependencies the dependencies
     *
     * @return the loaded class or null if not found
     */
    Class<?> getClazz(String name, Set<String> dependencies);

    DependencyGraph getGraph();

    ServiceManager getServiceManager();

    <T> void registerProvider(Class<T> clazz, ValueProvider<T> provider);

    <T> ValueProvider<T> getProvider(Class<T> clazz);
}
