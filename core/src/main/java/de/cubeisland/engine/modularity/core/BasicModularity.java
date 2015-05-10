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
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.core.graph.DependencyGraph;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceDefinitionMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceImplementationMetadata;
import de.cubeisland.engine.modularity.core.service.ProxyServiceContainer;
import de.cubeisland.engine.modularity.core.service.ServiceContainer;
import de.cubeisland.engine.modularity.core.service.ServiceManager;

public abstract class BasicModularity implements Modularity
{
    private final Map<ClassLoader, Set<DependencyInformation>> infosByClassLoader = new HashMap<ClassLoader, Set<DependencyInformation>>();
    private final Map<String, ModuleMetadata> modules = new HashMap<String, ModuleMetadata>();
    private final Map<String, TreeMap<Integer, DependencyInformation>> services = new HashMap<String, TreeMap<Integer, DependencyInformation>>();
    private Map<String, ValueProvider<?>> providers = new HashMap<String, ValueProvider<?>>();

    private final DependencyGraph graph = new DependencyGraph();

    private final Map<String, Instance> instances = new HashMap<String, Instance>();

    private final ServiceManager serviceManager = new ServiceManager();
    private final Field MODULE_META_FIELD;
    private final Field MODULE_MODULARITY_FIELD;

    public BasicModularity()
    {
        try
        {
            MODULE_META_FIELD = Module.class.getDeclaredField("metadata");
            MODULE_META_FIELD.setAccessible(true);
            MODULE_MODULARITY_FIELD = Module.class.getDeclaredField("modularity");
            MODULE_MODULARITY_FIELD.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            throw new IllegalStateException();
        }
    }

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
            if (!(info instanceof ServiceImplementationMetadata))
            {
                graph.addNode(info);
            }
            if (info instanceof ModuleMetadata)
            {
                modules.put(info.getClassName(), (ModuleMetadata)info);
            }
            else
            {
                TreeMap<Integer, DependencyInformation> services = this.services.get(info.getClassName());
                if (services == null)
                {
                    services = new TreeMap<Integer, DependencyInformation>();
                    this.services.put(info.getClassName(), services);
                }
                services.put(Integer.valueOf(info.getVersion()), info);
            }
            Set<DependencyInformation> set = infosByClassLoader.get(info.getClassLoader());
            if (set == null)
            {
                set = new HashSet<DependencyInformation>();
                infosByClassLoader.put(info.getClassLoader(), set);
            }
            set.add(info);
        }
        return this;
    }

    @Override
    public Object getStarted(String identifier)
    {
        DependencyInformation info = getDependencyInformation(identifier);
        if (info == null || info instanceof ServiceImplementationMetadata) // Not found OR Implementation
        {
            return null;
        }
        System.out.println("Starting " + identifier + "...");

        Object instance = this.getStarted(info);
        Object result = instance;
        if (instance instanceof ProxyServiceContainer)
        {
            if (!((ProxyServiceContainer)instance).hasImplementations())
            {
                startServiceImplementation((ProxyServiceContainer)instance);
            }
            result = ((ProxyServiceContainer)instance).getImplementation();
        }
        System.out.println("done.\n");
        return result;
    }

    private Object getStarted(DependencyInformation info)
    {
        depth++;
        String identifier = info.getClassName();
        if (info instanceof ServiceImplementationMetadata)
        {
            identifier = ((ServiceImplementationMetadata)info).getServiceName();
        }
        Object result = instances.get(identifier);
        if (result == null)
        {
            Map<String, Object> instances = collectDependencies(info);
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

    private void startServiceImplementations(Map<String, Object> instances)
    {
        show("Search for impls <", null);
        for (Object instance : instances.values())
        {
            if (instance instanceof ProxyServiceContainer && !((ProxyServiceContainer)instance).hasImplementations())
            {
                startServiceImplementation((ProxyServiceContainer)instance);
            }
        }
        show(">", null);
    }

    private void startServiceImplementation(ProxyServiceContainer instance)
    {
        for (TreeMap<Integer, DependencyInformation> map : services.values())
        {
            for (DependencyInformation impl : map.values())
            {
                if (impl instanceof ServiceImplementationMetadata
                    && ((ServiceImplementationMetadata)impl).getServiceName().equals(instance.getInterface().getName()))
                {
                    start(impl, collectDependencies(impl));
                }
            }
        }
    }

    private Map<String, Object> collectDependencies(DependencyInformation info)
    {
        show("Collect dependencies of", info);
        Map<String, Object> result = new HashMap<String, Object>();
        collectDependencies(info, info.requiredDependencies(), result, true);
        collectDependencies(info, info.optionalDependencies(), result, false);
        return result;
    }

    private void collectDependencies(DependencyInformation info, Set<String> deps, Map<String, Object> collected, boolean required)
    {
        for (String dep : deps)
        {
            ValueProvider<?> provider = providers.get(dep);
            if (provider != null)
            {
                collected.put(dep, provider.get(info, this));
                continue;
            }
            DependencyInformation dependency = getDependencyInformation(dep);
            if (dependency == null)
            {
                if (required)
                {
                    throw new IllegalStateException("Missing required dependency to: " + dep); // TODO custom Exception
                }
                System.out.println("Missing optional dependency to: " + dep);
                continue;
            }
            collected.put(dependency.getClassName(), getStarted(dependency));
        }
    }

    private DependencyInformation getDependencyInformation(String dependencyString)
    {
        ModuleMetadata meta = modules.get(dependencyString);
        if (meta != null)
        {
            return meta;
        }
        int versionAt = dependencyString.indexOf(':');
        Integer version = null;
        if (versionAt != -1)
        {
            version = Integer.parseInt(dependencyString.substring(versionAt + 1, dependencyString.length()));
            dependencyString = dependencyString.substring(0, versionAt);
        }

        TreeMap<Integer, DependencyInformation> services = this.services.get(dependencyString);
        if (services != null)
        {
            if (version == null)
            {
                return services.lastEntry().getValue();
            }
            else
            {
                return services.get(version);
            }
        }
        return null;
    }

    private Object start(DependencyInformation info, Map<String, Object> deps)
    {
        try
        {
            show("Start", info);
            Class<?> instanceClass = Class.forName(info.getClassName(), true, info.getClassLoader());
            Object instance;
            if (info instanceof ServiceDefinitionMetadata)
            {
                serviceManager.registerService(instanceClass, info);
                instance = serviceManager.getService(instanceClass);
            }
            else // Module or ServiceImpl
            {
                Constructor<?> constructor = getConstructor(info, deps, instanceClass);
                Object created = constructor.newInstance(getConstructorParams(deps, constructor));
                injectDependencies(deps, created, info);
                if (info instanceof ServiceImplementationMetadata)
                {
                    Class serviceClass = Class.forName(((ServiceImplementationMetadata)info).getServiceName(), true,
                                                       info.getClassLoader());
                    serviceManager.registerService(serviceClass, created);
                    return serviceManager.getService(serviceClass);
                }
                instance = created;
            }
            if (instance == null)
            {
                throw new IllegalStateException();
            }

            if (instance instanceof Instance)
            {
                this.instances.put(info.getClassName(), (Instance)instance);
            }

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

    private void injectDependencies(Map<String, Object> deps, Object instance, DependencyInformation info) throws IllegalAccessException
    {
        if (instance instanceof Module)
        {
            MODULE_META_FIELD.set(instance, info);
            MODULE_MODULARITY_FIELD.set(instance, this);
        }
        for (Field field : instance.getClass().getDeclaredFields())
        {
            if (field.isAnnotationPresent(Inject.class))
            {
                Class<?> type = field.getType();
                boolean maybe = Maybe.class.equals(type);
                if (maybe)
                {
                    type = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
                }
                Object toSet = deps.get(type.getName());
                if (!maybe && toSet == null)
                {
                    throw new IllegalStateException();
                }
                if (toSet instanceof ProxyServiceContainer)
                {
                    toSet = ((ProxyServiceContainer)toSet).getImplementation();
                }
                if (maybe)
                {
                    toSet = new SettableMaybe(toSet); // TODO save to be able to provide/remove module later
                }
                field.setAccessible(true);
                field.set(instance, toSet);
            }
        }
    }


    private Object[] getConstructorParams(Map<String, Object> deps, Constructor<?> instanceConstructor)
    {
        Object[] parameters;
        Class<?>[] parameterTypes = instanceConstructor.getParameterTypes();
        parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++)
        {
            final Class<?> type = parameterTypes[i];
            Object instance = deps.get(type.getName());
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

    private Constructor<?> getConstructor(DependencyInformation info, Map<String, Object> deps, Class<?> instanceClass)
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
            throw new IllegalStateException(info.getClassName() + " has no usable Constructor");// TODO error
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
            clazz = getClazz(name, checked, modules.keySet());
        }
        return clazz;
    }

    @Override
    public DependencyGraph getGraph()
    {
        return this.graph;
    }

    @Override
    public ServiceManager getServiceManager()
    {
        return this.serviceManager;
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
                DependencyInformation info = getDependencyInformation(dep);
                if (info != null)
                {
                    return info.getClassLoader().findClass(name, false);
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
        String name = "";
        if (clazz != null)
        {
            name = clazz.getClassName();
            name = name.substring(name.lastIndexOf(".") + 1);
            name += ":" + clazz.getVersion();
        }
        if (clazz != null)
        {
            String id = clazz.getClassName();
            if (clazz instanceof ServiceImplementationMetadata)
            {
                id = ((ServiceImplementationMetadata)clazz).getServiceName();
            }
            Instance instance = instances.get(id);
            if (instance != null)
            {
                if (instance instanceof ProxyServiceContainer
                    && !((ProxyServiceContainer)instance).hasImplementations())
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

    @Override
    public <T> void registerProvider(Class<T> clazz, ValueProvider<T> provider)
    {
        this.providers.put(clazz.getName(), provider);
        graph.provided(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ValueProvider<T> getProvider(Class<T> clazz)
    {
        return (ValueProvider<T>)providers.get(clazz.getName());
    }
}
