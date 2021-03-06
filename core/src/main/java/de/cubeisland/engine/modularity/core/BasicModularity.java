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
package de.cubeisland.engine.modularity.core;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.core.graph.BasicDependency;
import de.cubeisland.engine.modularity.core.graph.Dependency;
import de.cubeisland.engine.modularity.core.graph.DependencyGraph;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.Node;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceDefinitionMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceImplementationMetadata;

public class BasicModularity implements Modularity
{
    private InformationLoader loader;
    private final DependencyGraph graph = new DependencyGraph();

    private final Map<Dependency, LifeCycle> lifeCycles = new HashMap<Dependency, LifeCycle>();
    private final Map<Dependency, ModuleMetadata> moduleInfos = new HashMap<Dependency, ModuleMetadata>();
    private final Map<Dependency, ServiceImplementationMetadata> serviceImpls = new HashMap<Dependency, ServiceImplementationMetadata>();

    private final List<ModularityHandler> modularityHandlers = new ArrayList<ModularityHandler>();
    private Map<Class<? extends Annotation>, PostInjectionHandler> postInjectionHandlers = new HashMap<Class<? extends Annotation>, PostInjectionHandler>();

    public void init(InformationLoader loader)
    {
        this.loader = loader;
        this.register(Modularity.class, this);
    }

    @Override
    public void load(File source, String... filters)
    {
        Set<DependencyInformation> loaded = getLoader().loadInformation(source);
        if (loaded.isEmpty())
        {
            this.log("No DependencyInformation could be extracted from target source: " + source.getName()); // TODO
            return;
        }
        this.log("Loaded DependencyInformation for " + loaded.size() + " objects from " + source.getName());
        addLoaded(loaded);
    }

    @Override
    public void loadFromClassPath(String... filter)
    {
        Set<DependencyInformation> loaded = getLoader().loadInformationFromClasspath(filter);
        if (loaded.isEmpty())
        {
            this.log("No DependencyInformation could be extracted from classpath!"); // TODO
            return;
        }
        this.log("Loaded DependencyInformation for " + loaded.size() + " objects from classpath");
        addLoaded(loaded);
    }

    private void addLoaded(Set<DependencyInformation> loaded)
    {
        for (DependencyInformation info : loaded)
        {
            if (info instanceof ServiceImplementationMetadata)
            {
                serviceImpls.put(info.getIdentifier(), ((ServiceImplementationMetadata)info));
            }
            else
            {
                graph.addNode(info);
                if (info instanceof ModuleMetadata)
                {
                    moduleInfos.put(info.getIdentifier(), (ModuleMetadata)info);
                }
            }
        }
    }

    @Override
    public Class<?> findClass(String name, Set<Dependency> dependencies)
    {
        if (name == null)
        {
            return null;
        }
        Set<Dependency> checked = new HashSet<Dependency>();
        Class clazz = findClass(name, checked, dependencies);
        if (clazz == null)
        {
            clazz = findClass(name, checked, moduleInfos.keySet());
        }
        return clazz;
    }

    private Class<?> findClass(String name, Set<Dependency> checked, Set<Dependency> dependencies)
    {
        for (Dependency dep : dependencies)
        {
            if (checked.contains(dep))
            {
                continue;
            }
            checked.add(dep);
            try
            {
                Node node = graph.getNode(dep);
                if (node != null)
                {
                    return node.getInformation().getClassLoader().findClass(name, false);
                }
            }
            catch (ClassNotFoundException ignored)
            {
            }
        }
        return null;
    }


    @Override
    public LifeCycle getLifecycle(Dependency dep)
    {
        LifeCycle lifeCycle = lifeCycles.get(dep);
        if (lifeCycle == null)
        {
            Node node = graph.getNode(dep);
            if (node == null)
            {
                try // Try to construct the dependency ...
                {
                    Class clazz = Class.forName(dep.name());
                    Object instance = inject(clazz); // Attempt to create instance
                    lifeCycle = new LifeCycle(this).initProvided(instance); // Create Provided Lifecycle
                    lifeCycles.put(dep, lifeCycle); // Register lifecycle for future use
                    return lifeCycle;
                }
                catch (Exception e) // That did not go so well :/
                {
                    throw new MissingDependencyException("Dependency is not available " + dep.name(), e);
                }
            }
            lifeCycle = lifeCycles.get(node.getInformation().getIdentifier());
            if (lifeCycle == null)
            {
                lifeCycle = new LifeCycle(this).load(node.getInformation());
                if (node.getInformation() instanceof ServiceDefinitionMetadata)
                {
                    for (ServiceImplementationMetadata impl : serviceImpls.values())
                    {
                        if (impl.getActualClass().equals(node.getInformation().getActualClass()))
                        {
                            lifeCycle.addImpl(new LifeCycle(this).load(impl));
                        }
                    }
                }
                lifeCycles.put(node.getInformation().getIdentifier(), lifeCycle);
            }
        }
        return lifeCycle;
    }


    private LifeCycle setup(Dependency dep)
    {
        LifeCycle lifecycle = getLifecycle(dep);
        if (!lifecycle.isInstantiated())
        {
            lifecycle.instantiate();
        }
        return lifecycle.setup();
    }

    private LifeCycle enable(Dependency dep)
    {
        LifeCycle lifecycle = getLifecycle(dep);
        try
        {
            return lifecycle.enable();
        }
        catch (Exception e)
        {
            lifecycle.disable();
            this.logError("Could not load module: " + dep.name(), e);
            throw new IllegalStateException(e); // TODO
        }
    }

    @Override
    public DependencyGraph getGraph()
    {
        return this.graph;
    }

    @Override
    public Set<LifeCycle> getModules()
    {
        Set<LifeCycle> modules = new HashSet<LifeCycle>();
        for (LifeCycle lifeCycle : lifeCycles.values())
        {
            Object instance = lifeCycle.getInstance();
            if (instance instanceof Module)
            {
                modules.add(lifeCycle);
            }
        }
        return modules;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T provide(Class<T> type)
    {
        try
        {
            LifeCycle lifecycle = getLifecycle(new BasicDependency(type.getName(), null));
            if (!lifecycle.isInstantiated())
            {
                lifecycle.enable();
            }
            return (T)lifecycle.getProvided(null);
        }
        catch (MissingDependencyException ignored)
        {
            return null;
        }
    }

    public LifeCycle getLifecycle(Class type)
    {
        return getLifecycle(new BasicDependency(type.getName(), null));
    }

    @Override
    public void setupModules()
    {
        for (Dependency dep : moduleInfos.keySet())
        {
            try
            {
                setup(dep);
            }
            catch (MissingDependencyException e)
            {
                System.err.println(e.getMessage());
                e.printStackTrace();
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
        for (Dependency dep : moduleInfos.keySet())
        {
            try
            {
                enable(dep);
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
        for (Dependency dep : moduleInfos.keySet())
        {
            try
            {
                disable(dep);
            }
            catch (IllegalStateException e)
            {
                // TODO
                e.printStackTrace();
            }
        }
    }

    private LifeCycle disable(Dependency dep)
    {
        LifeCycle lifecycle = getLifecycle(dep);
        return lifecycle.disable();
    }

    @Override
    public <T> void registerProvider(Class<T> clazz, ValueProvider<T> provider)
    {
        BasicDependency dep = new BasicDependency(clazz.getName(), null);
        graph.provided(dep);
        maybe(dep).provide(provider); // Get or create Lifecycle and init
        this.log("Registered external provider " + provider.getClass().getName());
    }

    @Override
    public <T> void register(Class<T> clazz, T instance)
    {
        BasicDependency dep = new BasicDependency(clazz.getName(), null);
        graph.provided(dep);
        maybe(dep).initProvided(instance);
        this.log("Registered external provided object " + instance.getClass().getName());
    }

    @Override
    public <T> void registerProvider(Class<T> clazz, Provider<T> instanceProvider)
    {
        BasicDependency dep = new BasicDependency(clazz.getName(), null);
        graph.provided(dep);
        maybe(dep).initProvided(instanceProvider);
        this.log("Registered external provided object " + instanceProvider.getClass().getName());
    }

    @Override
    public LifeCycle maybe(Dependency dep)
    {
        try
        {
            return getLifecycle(dep);
        }
        catch (MissingDependencyException ignored)
        {
            LifeCycle lifeCycle = new LifeCycle(this);
            this.lifeCycles.put(dep, lifeCycle);
            return lifeCycle;
        }
    }

    @Override
    public InformationLoader getLoader()
    {
        return loader;
    }

    @Override
    public void registerHandler(ModularityHandler handler)
    {
        modularityHandlers.add(handler);
    }

    @Override
    public <T extends Annotation> void registerPostInjectAnnotation(Class<T> annotation, PostInjectionHandler<T> handler)
    {
        postInjectionHandlers.put(annotation, handler);
    }

    @Override
    public void runPostInjectHandler(Field field, Object instance, Object owner)
    {
        for (Entry<Class<? extends Annotation>, PostInjectionHandler> entry : postInjectionHandlers.entrySet())
        {
            if (field.isAnnotationPresent(entry.getKey()))
            {
                entry.getValue().handle(field.getAnnotation(entry.getKey()), instance, owner);
            }
        }
    }

    @Override
    public void runEnableHandlers(Object instance)
    {
        for (ModularityHandler handler : modularityHandlers)
        {
            handler.onEnable(instance);
        }
    }

    @Override
    public void runDisableHandlers(Object instance)
    {
        for (ModularityHandler handler : modularityHandlers)
        {
            handler.onDisable(instance);
        }
    }

    @Override
    public Object inject(Class<?> clazz)
    {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1)
        {
            throw new IllegalArgumentException(clazz.getName() + " must have a single public Constructor");
        }

        Constructor<?> constructor = constructors[0];
        if (!constructor.isAnnotationPresent(Inject.class))
        {
            throw new IllegalArgumentException(clazz.getName() + " must have an @Inject annotation on its Constructor");
        }
        Class<?>[] types = constructor.getParameterTypes();
        Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; i++)
        {
            values[i] = provide(types[i]);
            if (values[i] == null)
            {
                values[i] = inject(types[i]);
            }
            if (values[i] == null)
            {
                throw new IllegalArgumentException("Cannot construct " + clazz.getSimpleName() + " due to missing dependency: " + types[i].getSimpleName());
            }
        }

        try
        {
            Object instance = constructor.newInstance(values);
            for (Field field : clazz.getDeclaredFields())
            {
                if (field.isAnnotationPresent(Inject.class))
                {
                    Object provided = provide(field.getType());
                    if (provided == null)
                    {
                        provided = inject(field.getType());
                    }
                    if (provided == null)
                    {
                        throw new IllegalArgumentException("Cannot construct " + clazz.getSimpleName() + " due to missing dependency: " + field.getType().getSimpleName());
                    }
                    field.set(instance, provided);
                    runPostInjectHandler(field, provided, instance);
                }
            }
            return instance;
        }
        catch (InstantiationException e)
        {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void log(String message)
    {
        System.out.println(message);
    }

    @Override
    public void logError(String message, Throwable t)
    {
        System.err.println(message);
        if (t != null)
        {
            t.printStackTrace();
        }
    }
}
