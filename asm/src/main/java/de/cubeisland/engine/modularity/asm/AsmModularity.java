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
package de.cubeisland.engine.modularity.asm;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import de.cubeisland.engine.modularity.core.InformationLoader;
import de.cubeisland.engine.modularity.core.Instance;
import de.cubeisland.engine.modularity.core.Modularity;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.service.ServiceContainer;

public class AsmModularity implements Modularity
{
    private final InformationLoader loader = new AsmInformationLoader();

    private final Map<ClassLoader, Set<DependencyInformation>> infosByClassLoader = new HashMap<ClassLoader, Set<DependencyInformation>>();
    private final Set<DependencyInformation> infos = new HashSet<DependencyInformation>();

    public AsmModularity(File source)
    {
        load(source);
    }

    // TODO add to interface?
    public void load(File source)
    {
        Set<DependencyInformation> loaded = loader.loadInformation(source);
        infos.addAll(loaded);
        for (DependencyInformation info : loaded)
        {
            Set<DependencyInformation> set = infosByClassLoader.get(info.getClassLoader());
            if (set == null)
            {
                set = new HashSet<DependencyInformation>();
                infosByClassLoader.put(info.getClassLoader(), set);
            }
            set.add(info);
        }

        // TODO build dependency Graph

    }

    @Override
    public InformationLoader getLoader()
    {
        return loader;
    }

    @Override
    public Instance getNode(String identifier)
    {
        return null;
    }

    @Override
    public <T extends Instance> T getNode(Class<T> type)
    {
        return null;
    }

    @Override
    public Set<Instance> getNodes()
    {
        return null;
    }

    @Override
    public Set<Module> getModules()
    {
        return null;
    }

    @Override
    public Set<ServiceContainer<?>> getServices()
    {
        return null;
    }
}
