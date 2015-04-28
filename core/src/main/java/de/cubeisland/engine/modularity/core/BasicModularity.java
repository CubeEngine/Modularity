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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import de.cubeisland.engine.modularity.core.graph.DependencyGraph;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.Node;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import de.cubeisland.engine.modularity.core.service.ServiceContainer;
import de.cubeisland.engine.modularity.core.service.ServiceManager;

public abstract class BasicModularity implements Modularity
{
    private final Map<ClassLoader, Set<DependencyInformation>> infosByClassLoader = new HashMap<ClassLoader, Set<DependencyInformation>>();
    private final Map<String, DependencyInformation> infos = new HashMap<String, DependencyInformation>();
    private final DependencyGraph graph = new DependencyGraph();

    private final Map<String, Instance> instances = new HashMap<String, Instance>();

    private final ServiceManager serviceManager = new ServiceManager();

    @Override
    public BasicModularity load(File source)
    {
        Set<DependencyInformation> loaded = getLoader().loadInformation(source);
        // TODO info when nothing was loaded
        for (DependencyInformation info : loaded)
        {
            infos.put(info.getIdentifier(), info);
            Set<DependencyInformation> set = infosByClassLoader.get(info.getClassLoader());
            if (set == null)
            {
                set = new HashSet<DependencyInformation>();
                infosByClassLoader.put(info.getClassLoader(), set);
            }
            set.add(info);
        }

        for (DependencyInformation info : infos.values())
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

    @Override
    public boolean start(String identifier)
    {
        DependencyInformation info = infos.get(identifier);
        if (info == null)
        {
            return false;
        }
        this.getStarted(info);
        Set<DependencyInformation> infos = infosByClassLoader.get(info.getClassLoader());
        if (infos != null)
        {
            for (DependencyInformation depInfo : infos)
            {
                if (depInfo != info)
                {
                    this.getStarted(depInfo);
                }
            }
        }
        return true;
    }

    private Instance getStarted(DependencyInformation info)
    {
        Instance instance = instances.get(info.getIdentifier());
        if (instance != null)
        {
            return instance;
        }
        Map<String, Instance> instances = new HashMap<String, Instance>();
        for (String dep : info.requiredDependencies())
        {
            DependencyInformation dependency = infos.get(dep);
            if (dependency == null)
            {
                throw new IllegalStateException("Missing required dependency to: " + dep); // TODO custom Exception
            }
            instances.put(dependency.getIdentifier(), getStarted(dependency));
        }
        for (String dep : info.optionalDependencies())
        {
            DependencyInformation dependency = infos.get(dep);
            if (dependency == null)
            {
                // TODO debug message?
            }
            instances.put(dependency.getIdentifier(), getStarted(dependency));
        }
        return start(info, instances);
    }

    private Instance start(DependencyInformation info, Map<String, Instance> instances)
    {
        if (info instanceof ModuleMetadata)
        {
            try
            {
                Class<?> instanceClass = Class.forName(info.getIdentifier(), true, info.getClassLoader());
                Constructor<?> instanceConstructor = null;
                for (Constructor<?> constructor : instanceClass.getConstructors())
                {
                    boolean ok = true;
                    for (Class<?> depdendency : constructor.getParameterTypes())
                    {
                        ok = ok && instances.containsKey(depdendency.getName());
                        // TODO what if it was optional?
                    }
                    if (ok)
                    {
                        instanceConstructor = constructor;
                        break;
                    }
                }
                if (instanceConstructor == null)
                {
                    // TODO error
                }

                Class<?>[] parameterTypes = instanceConstructor.getParameterTypes();
                Object[] parameters = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++)
                {
                    final Class<?> type = parameterTypes[i];
                    parameters[i] = instances.get(type.getName());
                }
                instanceConstructor.setAccessible(true);
                Instance result = (Instance)instanceConstructor.newInstance(parameters);
                this.instances.put(info.getIdentifier(), result);
                return result;
            }
            catch (ClassNotFoundException e)
            {
                // TODO error
            }
            catch (InvocationTargetException e)
            {
                // TODO error
            }
            catch (InstantiationException e)
            {
                // TODO error
            }
            catch (IllegalAccessException e)
            {
                // TODO error
            }
            return null;
        }
        else
        {
            // TODO services
            return null;
        }
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
    public Class<?> getClazz(Set<String> dependencies, String name)
    {
        if (name == null)
        {
            return null;
        }
        Set<String> checked = new HashSet<String>();
        Class clazz = getClazz(name, checked, dependencies);
        if (clazz == null)
        {
            clazz = getClazz(name, checked, infos.keySet());
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
                ModularityClassLoader loader = infos.get(dep).getClassLoader();
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
