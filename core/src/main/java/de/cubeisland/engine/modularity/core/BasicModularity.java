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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import de.cubeisland.engine.modularity.core.graph.DependencyGraph;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.Node;
import de.cubeisland.engine.modularity.core.service.ServiceContainer;
import de.cubeisland.engine.modularity.core.service.ServiceManager;

// TODO actually instantiate the modules and servicimpls
public abstract class BasicModularity implements Modularity
{
    private final InformationLoader loader;
    private final Map<ClassLoader, Set<DependencyInformation>> infosByClassLoader = new HashMap<ClassLoader, Set<DependencyInformation>>();
    private final Set<DependencyInformation> infos = new HashSet<DependencyInformation>();
    private final Map<String, ModularityClassLoader> classLoaders = new HashMap<String, ModularityClassLoader>();
    private final DependencyGraph graph = new DependencyGraph();

    private final ServiceManager serviceManager = new ServiceManager();

    public BasicModularity(InformationLoader loader)
    {
        this.loader = loader;
    }

    @Override
    public BasicModularity load(File source)
    {
        Set<DependencyInformation> loaded = getLoader().loadInformation(source);
        // TODO info when nothing was loaded
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

        for (DependencyInformation info : infos)
        {
            graph.addNode(info);
        }

        Node root = graph.getRoot();

        showChildren(root, 0);

        System.out.println("\n");
        System.out.println("Unresolved Dependencies:");
        for (Entry<String, List<Node>> entry : graph.getUnresolved().entrySet())
        {
            System.out.println(entry.getKey() + ":");
            for (Node node : entry.getValue())
            {
                System.out.println("\t" + node.getInformation().getIdentifier());
            }
        }
        return this;
    }

    private void showChildren(Node root, int depth)
    {
        for (Node node : root.getChildren())
        {
            for (int i = 0; i < depth; i++)
            {
                System.out.print("  ");
            }
            System.out.println(node.getInformation().getIdentifier());
        }
        for (Node node : root.getChildren())
        {
            if (!node.getChildren().isEmpty())
            {
                showChildren(node, depth + 1);
            }
        }
    }

    @Override
    public Class<?> getClazz(DependencyInformation info, String name)
    {
        if (name == null)
        {
            return null;
        }
        Set<String> checked = new HashSet<String>();
        Class clazz = getClazz(name, checked, info.optionalDependencies());
        if (clazz == null)
        {
            clazz = getClazz(name, checked, info.requiredDependencies());
        }
        if (clazz == null)
        {
            clazz = getClazz(name, checked, classLoaders.keySet());
        }
        return clazz;
    }

    private Class<?> getClazz(String name, Set<String> checked, Set<String> strings)
    {
        for (String dep : strings)
        {
            if (checked.contains(dep))
            {
                continue;
            }
            checked.add(dep);
            try
            {
                ModularityClassLoader loader = classLoaders.get(dep);
                if (loader != null)
                {
                    return loader.findClass(name, false);
                }
            }
            catch (ClassNotFoundException ignored)
            {
            }
        }
        return null;
    }

    @Override
    public InformationLoader getLoader()
    {
        return loader;
    }


    @Override
    public Set<ServiceContainer<?>> getServices()
    {
        return serviceManager.getServices();
    }

    @Override
    public Set<Module> getModules()
    {
        return null; // TODO
    }
}
