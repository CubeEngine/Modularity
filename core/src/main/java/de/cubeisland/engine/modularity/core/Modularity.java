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
import java.util.Collection;
import java.util.Set;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.core.graph.Dependency;
import de.cubeisland.engine.modularity.core.graph.DependencyGraph;

public interface Modularity
{
    /**
     * Loads a all DependencyInformation from given source
     *
     * @param source the source
     * @param filter optional package filters to be matched
     *
     * @return fluent interface
     */
    void load(File source, String... filter);
    void loadFromClassPath(String... filter);

    <T> T provide(Class<T> type);
    LifeCycle getLifecycle(Class type);

    LifeCycle getLifecycle(Dependency dep);

    void setupModules();

    void enableModules();

    void disableModules();

    /**
     * Returns the InformationLoader
     *
     * @return the InformationLoader
     */
    InformationLoader getLoader();

    Set<LifeCycle> getModules();

    /**
     * Returns the loaded class with given name. Searching first in the ClassLoaders of the dependencies.
     *
     * @param name         the name of the class to load
     * @param dependencies the dependencies
     *
     * @return the loaded class or null if not found
     */
    Class<?> findClass(String name, Set<Dependency> dependencies);

    DependencyGraph getGraph();

    <T> void registerProvider(Class<T> clazz, ValueProvider<T> provider);

    <T> void register(Class<T> gameClass, T game);
    <T> void register(Class<T> gameClass, Provider<T> game);

    /**
     * Registers a handler for when modules get enabled or disabled
     * @param handler the handler
     */
    void registerHandler(ModuleHandler handler);

    Collection<ModuleHandler> getHandlers();

    LifeCycle maybe(Dependency dep);
}
