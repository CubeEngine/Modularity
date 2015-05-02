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
    // TODO external Service Provider / (no interface)

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

        return this;
    }

    @Override
    public Object getStarted(String identifier)
    {
        DependencyInformation info = infos.get(identifier);
        if (info == null || info instanceof ServiceImplementationMetadata) // Not found OR Implementation
        {
            return null;
        }
        System.out.println("Starting " + identifier + "...");

        Instance instance = this.getStarted(info);
        Object result = instance;
        if (instance instanceof ServiceContainer)
        {
            if (!((ServiceContainer)instance).hasImplementations())
            {
                startServiceImplementation((ServiceContainer)instance);
            }
            result = ((ServiceContainer)instance).getImplementation();
        }
        System.out.println("done.\n");
        return result;
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
                startServiceImplementation((ServiceContainer)instance);
            }
        }
    }

    private void startServiceImplementation(ServiceContainer instance)
    {
        for (DependencyInformation impl : infos.values())
        {
            if (impl instanceof ServiceImplementationMetadata
                && ((ServiceImplementationMetadata)impl).getServiceName().equals(
                instance.getInterface().getName()))
            {
                start(impl, collectDependencies(impl));
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
            Instance instance;
            if (info instanceof ServiceDefinitionMetadata)
            {
                serviceManager.registerService(info, instanceClass);
                instance = serviceManager.getService(instanceClass);
            }
            else // Module or ServiceImpl
            {
                Constructor<?> constructor = getConstructor(info, deps, instanceClass);
                Object created = constructor.newInstance(getConstructorParams(deps, constructor));
                injectDependencies(deps, created);
                if (info instanceof ServiceImplementationMetadata)
                {
                    Class serviceClass = Class.forName(((ServiceImplementationMetadata)info).getServiceName(), true,
                                                       info.getClassLoader());
                    serviceManager.registerService(serviceClass, created);
                    return serviceManager.getService(serviceClass);
                }
                instance = (Instance)created;
            }
            if (instance == null)
            {
                throw new IllegalStateException();
            }

            this.instances.put(info.getIdentifier(), instance);
            return instance;
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

    private Constructor<?> getConstructor(DependencyInformation info, Map<String, Instance> deps,
                                          Class<?> instanceClass)
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getStarted(Class<T> type)
    {
        return (T)getStarted(type.getName());
    }
}
