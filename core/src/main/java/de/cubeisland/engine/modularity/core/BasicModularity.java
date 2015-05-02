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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.core.graph.DependencyGraph;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.Node;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceDefinitionMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceImplementationMetadata;
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
        if (loaded.isEmpty())
        {
            System.out.println("No DependencyInformation could be extracted from target source!"); // TODO
        }
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

        /*
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
        */
        return this;
    }

    @Override
    public boolean start(String identifier)
    {
        System.out.println("Starting " + identifier + "...");
        DependencyInformation info = infos.get(identifier);
        if (info == null)
        {
            return false;
        }
        this.getStarted(info);
        System.out.println("done.\n");
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
        depth++;
        String identifier = info.getIdentifier();
        if (info instanceof ServiceImplementationMetadata)
        {
            identifier = ((ServiceImplementationMetadata)info).getServiceName();
        }
        Instance result = instances.get(identifier);
        if (result == null)
        {
            Map<String, Instance> instances = collectDependencies(info);
            result = start(info, instances);
            startServiceImplementations(instances);
        }
        else
        {
            show("- get", info);
        }
        depth--;
        return result;
    }

    private void startServiceImplementations(Map<String, Instance> instances)
    {
        show("Search for impls:", null);
        for (Instance instance : instances.values())
        {
            if (instance instanceof ServiceContainer && !((ServiceContainer)instance).hasImplementations())
            {
                for (DependencyInformation impl : infos.values())
                {
                    if (impl instanceof ServiceImplementationMetadata
                        && ((ServiceImplementationMetadata)impl).getServiceName().equals(((ServiceContainer)instance).getInterface().getName()))
                    {
                        start(impl, collectDependencies(impl));
                    }
                }
            }
        }
    }

    private Map<String, Instance> collectDependencies(DependencyInformation info)
    {
        show("Collect dependencies of", info);
        Map<String, Instance> result = new HashMap<String, Instance>();
        collectDependencies(info.requiredDependencies(), result, true);
        collectDependencies(info.optionalDependencies(), result, false);
        return result;
    }

    private void collectDependencies(Set<String> deps, Map<String, Instance> collected, boolean required)
    {
        for (String dep : deps)
        {
            DependencyInformation dependency = infos.get(dep);
            if (dependency == null)
            {
                if (required)
                {
                    throw new IllegalStateException("Missing required dependency to: " + dep); // TODO custom Exception
                }
                System.out.println("Missing optional dependency to: " + dep);
                continue;
            }
            collected.put(dependency.getIdentifier(), getStarted(dependency));
        }
    }

    private Instance start(DependencyInformation info, Map<String, Instance> deps)
    {
        try
        {
            show("Start", info);

            Class<?> instanceClass = Class.forName(info.getIdentifier(), true, info.getClassLoader());
            if (info instanceof ServiceDefinitionMetadata)
            {
                serviceManager.registerService(info, instanceClass);
                ServiceContainer<?> service = serviceManager.getService(instanceClass);
                this.instances.put(info.getIdentifier(), service);
                return service;
            }

            Constructor<?> constructor = getConstructor(info, deps, instanceClass);
            Object instance = constructor.newInstance(getConstructorParams(deps, constructor));

            if (info instanceof ModuleMetadata)
            {
                this.instances.put(info.getIdentifier(), (Instance)instance);
                injectDependencies(deps, instance);

                return (Instance)instance;
            }
            else if (info instanceof ServiceImplementationMetadata)
            {
                Class serviceClass = Class.forName(((ServiceImplementationMetadata)info).getServiceName(), true,
                                                   info.getClassLoader());
                serviceManager.registerService(serviceClass, instance);
                ServiceContainer service = serviceManager.getService(serviceClass);
                this.instances.put(service.getInfo().getIdentifier(), service);
                return service;
            }
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        System.out.println("An error occurred!");

        return null;
    }

    private void injectDependencies(Map<String, Instance> deps, Object instance) throws IllegalAccessException
    {
        for (Field field : instance.getClass().getDeclaredFields())
        {
            if (field.isAnnotationPresent(Inject.class))
            {
                Object toSet = deps.get(field.getType().getName());
                if (toSet == null)
                {
                    throw new IllegalStateException();
                }
                if (toSet instanceof ServiceContainer)
                {
                    toSet = ((ServiceContainer)toSet).getImplementation();
                }
                field.setAccessible(true);
                field.set(instance, toSet);
            }
        }
    }

    private Object[] getConstructorParams(Map<String, Instance> deps, Constructor<?> instanceConstructor)
    {
        Object[] parameters;
        Class<?>[] parameterTypes = instanceConstructor.getParameterTypes();
        parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++)
        {
            final Class<?> type = parameterTypes[i];
            Instance instance = deps.get(type.getName());
            if (instance instanceof ServiceContainer)
            {
                parameters[i] = ((ServiceContainer)instance).getImplementation();
            }
            else
            {
                parameters[i] = instance;
            }
        }
        return parameters;
    }

    private Constructor<?> getConstructor(DependencyInformation info, Map<String, Instance> deps, Class<?> instanceClass)
    {
        Constructor<?> instanceConstructor = null;
        for (Constructor<?> constructor : instanceClass.getConstructors())
        {
            boolean ok = true;
            for (Class<?> dep : constructor.getParameterTypes())
            {
                ok = ok && deps.containsKey(dep.getName());
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
            throw new IllegalStateException(info.getIdentifier() + " has no usable Constructor");// TODO error
        }
        instanceConstructor.setAccessible(true);
        return instanceConstructor;
    }

    private int depth = -1;

    private void show(String show, DependencyInformation clazz)
    {
        for (int i1 = 0; i1 < depth; i1++)
        {
            System.out.print("\t");
        }
        String name = clazz == null ? "" : clazz.getIdentifier();
        name = name.substring(name.lastIndexOf(".") + 1);
        if (clazz != null)
        {
            String id = clazz.getIdentifier();
            if (clazz instanceof ServiceImplementationMetadata)
            {
                id = ((ServiceImplementationMetadata)clazz).getServiceName();
            }
            Instance instance = instances.get(id);
            if (instance != null)
            {
                if (instance instanceof ServiceContainer && !((ServiceContainer)instance).hasImplementations())
                {
                    name = "[" + name + "]";
                }
            }
            else
            {
                name = "[" + name + "]";
            }
        }
        System.out.println(show + " " + name);
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
    public Class<?> getClazz(String name, Set<String> dependencies)
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
