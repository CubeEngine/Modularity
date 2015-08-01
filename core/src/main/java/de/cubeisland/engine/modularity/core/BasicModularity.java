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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.core.LifeCycle.State;
import de.cubeisland.engine.modularity.core.graph.DependencyGraph;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.Node;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceDefinitionMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceImplementationMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceProviderMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ValueProviderMetadata;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.core.marker.Setup;
import de.cubeisland.engine.modularity.core.service.InstancedServiceContainer;
import de.cubeisland.engine.modularity.core.service.ProvidedServiceContainer;
import de.cubeisland.engine.modularity.core.service.ProxyServiceContainer;
import de.cubeisland.engine.modularity.core.service.ServiceContainer;
import de.cubeisland.engine.modularity.core.service.ServiceManager;

import static de.cubeisland.engine.modularity.core.LifeCycle.State.DISABLED;
import static de.cubeisland.engine.modularity.core.LifeCycle.State.ENABLED;
import static de.cubeisland.engine.modularity.core.LifeCycle.State.SETUP;

public class BasicModularity implements Modularity
{
    private InformationLoader loader;
    private final DependencyGraph graph = new DependencyGraph();
    private final Map<ClassLoader, Set<DependencyInformation>> infosByClassLoader = new HashMap<ClassLoader, Set<DependencyInformation>>();

    private final Map<String, Instance> instances = new HashMap<String, Instance>(); // Modules and ServiceContainers
    private final Map<String, ValueProvider<?>> providers = new HashMap<String, ValueProvider<?>>();

    private final ServiceManager serviceManager = new ServiceManager();

    // Loaded Metadata
    private final Map<String, ModuleMetadata> modules = new HashMap<String, ModuleMetadata>();
    private final Map<String, TreeMap<Integer, ServiceDefinitionMetadata>> services = new HashMap<String, TreeMap<Integer, ServiceDefinitionMetadata>>();
    private final Map<String, TreeMap<Integer, ServiceImplementationMetadata>> serviceImpls = new HashMap<String, TreeMap<Integer, ServiceImplementationMetadata>>();
    private final Map<String, ValueProviderMetadata> valueProviders = new HashMap<String, ValueProviderMetadata>();
    private final Map<String, ServiceProviderMetadata> serviceProviders = new HashMap<String, ServiceProviderMetadata>();

    private final Map<String, List<SettableMaybe>> maybes = new HashMap<String, List<SettableMaybe>>();
    private final Map<DependencyInformation, LifeCycle> lifeCycles = new HashMap<DependencyInformation, LifeCycle>();

    private final Field MODULE_META_FIELD;
    private final Field MODULE_MODULARITY_FIELD;

    public BasicModularity(InformationLoader loader)
    {
        this.loader = loader;
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

        this.registerProvider(URL.class, new SourceURLProvider());
    }

    @Override
    public Set<Node> load(File source)
    {
        Set<DependencyInformation> loaded = getLoader().loadInformation(source);
        Set<Node> nodes = new HashSet<Node>();
        if (loaded.isEmpty())
        {
            System.out.println("No DependencyInformation could be extracted from target source!"); // TODO
        }
        for (DependencyInformation info : loaded)
        {
            if (!(info instanceof ServiceImplementationMetadata))
            {
                nodes.add(graph.addNode(info));
            }
            String className = info.getActualClass();
            if (info instanceof ModuleMetadata)
            {
                modules.put(className, (ModuleMetadata)info);
            }
            else if (info instanceof ValueProviderMetadata)
            {
                valueProviders.put(className, (ValueProviderMetadata)info);
            }
            else if (info instanceof ServiceProviderMetadata)
            {
                serviceProviders.put(className, ((ServiceProviderMetadata)info));
            }
            else
            {
                String version = info.getVersion();
                if ("unknown".equals(version))
                {
                    version = "0";
                }
                if (info instanceof ServiceDefinitionMetadata)
                {
                    TreeMap<Integer, ServiceDefinitionMetadata> services = this.services.get(className);
                    if (services == null)
                    {
                        services = new TreeMap<Integer, ServiceDefinitionMetadata>();
                        this.services.put(className, services);
                    }
                    try
                    {
                        services.put(Integer.valueOf(version), ((ServiceDefinitionMetadata)info));
                    }
                    catch (NumberFormatException e)
                    {
                        services.put(0, ((ServiceDefinitionMetadata)info));
                    }
                }
                else if (info instanceof ServiceImplementationMetadata)
                {
                    TreeMap<Integer, ServiceImplementationMetadata> services = this.serviceImpls.get(className);
                    if (services == null)
                    {
                        services = new TreeMap<Integer, ServiceImplementationMetadata>();
                        this.serviceImpls.put(className, services);
                    }
                    try
                    {
                        services.put(Integer.valueOf(version), ((ServiceImplementationMetadata)info));
                    }
                    catch (NumberFormatException e)
                    {
                        services.put(0, ((ServiceImplementationMetadata)info));
                    }
                }
            }
            Set<DependencyInformation> set = infosByClassLoader.get(info.getClassLoader());
            if (set == null)
            {
                set = new HashSet<DependencyInformation>();
                infosByClassLoader.put(info.getClassLoader(), set);
            }
            set.add(info);
        }
        return nodes;
    }

    @Override
    public Object getInstance(Node node)
    {
        if (node == null)
        {
            return null;
        }
        DependencyInformation info = node.getInformation();
        if (info == null || info instanceof ServiceImplementationMetadata) // Not found OR Implementation
        {
            return null;
        }
        Object instance = getInstance(info);
        if (instance == null)
        {
            this.setup(info);
            instance = this.enable(info);
        }
        Object result = instance;
        if (instance instanceof ServiceContainer)
        {
            if (instance instanceof ProxyServiceContainer)
            {
                if (!((ProxyServiceContainer)instance).hasImplementations())
                {
                    startServiceImplementation((ProxyServiceContainer)instance);
                }
            }
            result = ((ServiceContainer)instance).getImplementation();
        }
        return result;
    }


    private Object setup(DependencyInformation info)
    {
        Object result = getInstance(info);
        if (result != null)
        {
            return result;
        }
        show("Start Setup ", info);
        Map<String, Object> instances = collectDependencies(info);
        result = newInstance(info, instances);
        show("Setup Instance", info);
        setupInstance(info, result, instances);
        return result;
    }

    private void setupInstance(DependencyInformation info, Object instance, Map<String, Object> instances)
    {
        startServiceImplementations(instances);
        show("setup", info);
        lifeCycles.get(info).transition(SETUP);
    }

    private Object enable(DependencyInformation info)
    {
        depth++;
        Object result = getInstance(info);
        if (result != null)
        {
            enableInstance(info, result);
            List<SettableMaybe> list = maybes.get(info.getActualClass());
            if (list != null)
            {
                for (SettableMaybe maybe : list)
                {
                    maybe.provide(result);
                }
            }

            show("enabled.", info);
        }
        depth--;
        return result;
    }

    private void enableInstance(DependencyInformation info, Object instance)
    {
        show("enable", info);
        if (instance instanceof InstancedServiceContainer)
        {
            instance = ((InstancedServiceContainer)instance).getImplementation();
        }
        if (instance instanceof ProvidedServiceContainer)
        {
            instance = ((ProvidedServiceContainer)instance).getProvider();
        }
        lifeCycles.get(info).transition(ENABLED);
    }

    private Object getInstance(DependencyInformation info)
    {
        String identifier = info.getActualClass();

        try
        {
            ServiceContainer<?> service = serviceManager.getService(Class.forName(identifier, true, info.getClassLoader()));
            if (service != null)
            {
                return service;
            }
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException(e);
        }

        return instances.get(identifier);
    }

    private void startServiceImplementations(Map<String, Object> instances)
    {
        for (Object instance : instances.values())
        {
            if (instance instanceof ProxyServiceContainer && !((ProxyServiceContainer)instance).hasImplementations())
            {
                show("Search for impl =>", null);
                startServiceImplementation((ProxyServiceContainer)instance);
            }
        }
    }

    private void startServiceImplementation(ProxyServiceContainer container)
    {
        for (TreeMap<Integer, ServiceImplementationMetadata> map : serviceImpls.values())
        {
            for (ServiceImplementationMetadata impl : map.values())
            {
                if (impl.getActualClass().equals(container.getInterface().getName()))
                {
                    Map<String, Object> deps = collectDependencies(impl);
                    Object instance = newInstance(impl, deps);
                    startServiceImplementations(deps);
                    enableInstance(impl, instance);
                }
            }
        }
    }

    private Map<String, Object> collectDependencies(DependencyInformation info)
    {
        depth++;
        show("Collect dependencies of", info);
        Map<String, Object> result = new HashMap<String, Object>();
        collectDependencies(info, info.requiredDependencies(), result, true);
        collectDependencies(info, info.optionalDependencies(), result, false);
        depth--;
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
            try
            {
                ServiceContainer<?> service = serviceManager.getService(Class.forName(dep, true, info.getClassLoader()));
                if (service!=null)
                {
                    collected.put(dep, service);
                    continue;
                }
            }
            catch (ClassNotFoundException ignored)
            {}
            catch (NullPointerException e)
            {
                System.out.println(e);
            }
            DependencyInformation dependency = getDependencyInformation(dep);
            if (dependency == null)
            {
                if (required)
                {
                    throw new MissingDependencyException(info, dep);
                }
                System.out.println("Missing optional dependency to: " + dep);
                continue;
            }
            if (dependency instanceof ValueProviderMetadata)
            {
                collected.put(dep, setup(dependency));
            }
            else
            {
                collected.put(dependency.getActualClass(), setup(dependency));
            }
        }
    }

    private DependencyInformation getDependencyInformation(String dependencyString)
    {
        ModuleMetadata module = modules.get(dependencyString);
        if (module != null)
        {
            return module;
        }
        ValueProviderMetadata value = valueProviders.get(dependencyString);
        if (value != null)
        {
            return value;
        }

        ServiceProviderMetadata service = serviceProviders.get(dependencyString);
        if (service != null)
        {
            return service;
        }

        int versionAt = dependencyString.indexOf(':');
        Integer version = null;
        if (versionAt != -1)
        {
            version = Integer.parseInt(dependencyString.substring(versionAt + 1, dependencyString.length()));
            dependencyString = dependencyString.substring(0, versionAt);
        }

        TreeMap<Integer, ServiceDefinitionMetadata> services = this.services.get(dependencyString);
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

    private Object newInstance(DependencyInformation info, Map<String, Object> deps)
    {
        try
        {
            show("new Instance ", info);
            Class<?> instanceClass = Class.forName(info.getClassName(), true, info.getClassLoader());
            Object instance;
            if (info instanceof ServiceDefinitionMetadata)
            {
                serviceManager.registerService(instanceClass, info);
                instance = serviceManager.getService(instanceClass);
            }
            else // Module, ServiceImpl, ServiceProvider, ValueProvider
            {
                Constructor<?> constructor = getConstructor(info, deps, instanceClass);
                final Object created = constructor.newInstance(getConstructorParams(deps, constructor));
                injectDependencies(deps, created, info);
                if (info instanceof ServiceImplementationMetadata)
                {
                    Class serviceClass = Class.forName(info.getActualClass(), true,
                                                       info.getClassLoader());
                    serviceManager.registerService(serviceClass, created);
                    lifeCycles.put(info, new LifeCycle(created));
                    return created;
                }
                if (info instanceof ServiceProviderMetadata)
                {
                    serviceManager.registerService((ServiceProviderMetadata)info, created);
                }
                instance = created;
            }
            if (instance == null)
            {
                throw new IllegalStateException();
            }

            if (instance instanceof Instance)
            {
                this.instances.put(info.getActualClass(), (Instance)instance);
            }
            else if (instance instanceof ValueProvider)
            {
                this.providers.put(info.getActualClass(), (ValueProvider<?>)instance);
            }
            lifeCycles.put(info, new LifeCycle(instance));
            return instance;
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalStateException(e);
        }
        catch (InstantiationException e)
        {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
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
                boolean isMaybe = Maybe.class.equals(type);
                if (isMaybe)
                {
                    type = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
                }
                Object toSet = deps.get(type.getName());
                if (!isMaybe && toSet == null)
                {
                    throw new IllegalStateException();
                }
                if (toSet instanceof ServiceContainer)
                {
                    toSet = ((ServiceContainer)toSet).getImplementation();
                }
                if (toSet instanceof Provider)
                {
                    toSet = ((Provider)toSet).get();
                }
                if (toSet instanceof ValueProvider)
                {
                    toSet = ((ValueProvider)toSet).get(info, this);
                }
                if (isMaybe)
                {
                    SettableMaybe maybe = new SettableMaybe(toSet);
                    List<SettableMaybe> list = maybes.get(type.getName());
                    if (list == null)
                    {
                        list = new ArrayList<SettableMaybe>();
                    }
                    list.add(maybe);
                    maybes.put(type.getName(), list);
                    toSet = maybe;
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
            else if (instance instanceof Provider)
            {
                parameters[i] = ((Provider)instance).get();
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
        Set<Module> modules = new HashSet<Module>();
        for (Instance instance : instances.values())
        {
            if (instance instanceof Module)
            {
                modules.add(((Module)instance));
            }
        }
        return modules;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> type)
    {
        ServiceContainer<T> service = serviceManager.getService(type);
        if (service instanceof InstancedServiceContainer)
        {
            return service.getImplementation();
        }
        return (T)getInstance(graph.getNode(type.getName()));
    }

    @Override
    public void startAll()
    {
        startRecursive(getGraph().getRoot());
    }

    @Override
    public void setupModules()
    {
        for (ModuleMetadata meta : modules.values())
        {
            try
            {
                setup(meta);
            }
            catch (IllegalStateException e)
            {
                // TODO
                e.printStackTrace();
            }
        }
    }

    @Override
    public void enableModules()
    {
        for (ModuleMetadata module : modules.values())
        {
            try
            {
                enable(module);
            }
            catch (IllegalStateException e)
            {
                // TODO
                e.printStackTrace();
            }
        }
    }

    @Override
    public void disableModules()
    {
        for (ModuleMetadata module : modules.values())
        {
            Object instance = getInstance(module);
            if (instance != null)
            {
                stop(module, instance);
            }
        }
    }

    @Override
    public Set<Node> unload(Node node)
    {
        if (node.getInformation() == null)
        {
            return Collections.emptySet();
        }
        Object instance = getInstance(node.getInformation());
        if (instance == null)
        {
            System.out.println(node.getInformation().getClassName() + " is not loaded");
            return Collections.emptySet();
        }

        if (node.getInformation() instanceof ServiceImplementationMetadata)
        {
            Node serviceNode = graph.getNode(node.getInformation().getActualClass());
            Object serviceInstance = getInstance(serviceNode.getInformation());
            if (!(serviceInstance instanceof ProxyServiceContainer))
            {
                throw new IllegalStateException("Service was not in a Container");
            }
            if (((ProxyServiceContainer)serviceInstance).getImplementations().size() > 1)
            {
                stop(node.getInformation(), instance);
                return Collections.singleton(node);
            }
            // else unload predecessors too
        }
        Set<Node> unloaded = new HashSet<Node>();
        for (Node pre : node.getPredecessors())
        {
            unloaded.addAll(unload(pre));
        }
        stop(node.getInformation(), instance);
        unloaded.add(node);
        return unloaded;
    }

    @SuppressWarnings("unchecked")
    private void stop(DependencyInformation info, Object instance)
    {
        try
        {
            Class<?> clazz = Class.forName(info.getActualClass(), true, info.getClassLoader());
            List<SettableMaybe> list = maybes.get(clazz.getName());
            if (list != null)
            {
                for (SettableMaybe<?> maybe : list)
                {
                    maybe.remove();
                }
            }
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException(e);
        }
        lifeCycles.get(info).transition(DISABLED);

        if (instance instanceof Module)
        {
            instances.values().remove(instance);
        }
        else if (instance instanceof ProxyServiceContainer)
        {
            if (info instanceof ServiceDefinitionMetadata)
            {
                // TODO stop all implementations
                for (Object impl : ((ProxyServiceContainer)instance).getImplementations())
                {
                    ((ProxyServiceContainer)instance).removeImplementation(impl);
                }
                instances.values().remove(instance);
            }
            else
            {
                Object found = null;
                for (Object impl : ((ProxyServiceContainer)instance).getImplementations())
                {
                    if (info.getClassName().equals(impl.getClass().getName()))
                    {
                        found = impl;
                        break;
                    }
                }
                if (found == null)
                {
                    throw new IllegalStateException("Tried to remove missing Implementation: " + info.getClassName());
                }

                ProxyServiceContainer service = ((ProxyServiceContainer)instance).removeImplementation(found);
                if (!service.hasImplementations())
                {
                    instances.values().remove(service);
                }
            }
        }
    }

    @Override
    public void reload(Node node)
    {
        Set<Node> unloaded = unload(node);
        for (Node reload : unloaded)
        {
            getInstance(reload);
        }
    }

    private void startRecursive(Node node)
    {
        if (node.getInformation() != null)
        {
            getInstance(node);
        }
        for (Node suc : node.getSuccessors())
        {
            startRecursive(suc);
        }
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
        ValueProvider<T> provider = (ValueProvider<T>)providers.get(clazz.getName());
        if (provider != null)
        {
            return provider;
        }
        ValueProviderMetadata meta = valueProviders.get(clazz.getName());
        if (meta != null)
        {
            return (ValueProvider<T>)enable(meta);
        }
        return null;
    }

    private void show(String show, DependencyInformation clazz)
    {
        if (1 == 1)
        {
               return;
        }
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
            Object instance = getInstance(clazz);
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
    public InformationLoader getLoader()
    {
        return loader;
    }

    @Override
    public Set<Instance> getInstances()
    {
        return new HashSet<Instance>(instances.values());
    }
}
