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
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import de.cubeisland.engine.modularity.core.graph.BasicDependency;
import de.cubeisland.engine.modularity.core.graph.Dependency;
import de.cubeisland.engine.modularity.core.graph.DependencyGraph;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.Node;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceDefinitionMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceImplementationMetadata;
import de.cubeisland.engine.modularity.core.service.ServiceContainer;
import de.cubeisland.engine.modularity.core.service.ServiceManager;

import static de.cubeisland.engine.modularity.core.LifeCycle.State.*;

public class BasicModularity implements Modularity
{
    private InformationLoader loader;
    private final DependencyGraph graph = new DependencyGraph();

    private final ServiceManager serviceManager = new ServiceManager();

    private final Map<Dependency, LifeCycle> lifeCycles = new HashMap<Dependency, LifeCycle>();
    private final Map<Dependency, ModuleMetadata> moduleInfos = new HashMap<Dependency, ModuleMetadata>();
    private final Map<Dependency, ServiceImplementationMetadata> serviceImpls = new HashMap<Dependency, ServiceImplementationMetadata>();

    public BasicModularity(InformationLoader loader)
    {
        this.loader = loader;
        this.registerProvider(URL.class, new SourceURLProvider());
    }

    @Override
    public void load(File source, String... filters)
    {
        Set<DependencyInformation> loaded = getLoader().loadInformation(source);
        if (loaded.isEmpty())
        {
            System.out.println("No DependencyInformation could be extracted from target source!"); // TODO
            return;
        }
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
                throw new MissingDependencyException("Dependency is not available " + dep);
            }
            lifeCycle = lifeCycles.get(node.getInformation().getIdentifier());
            if (lifeCycle == null)
            {
                lifeCycle = new LifeCycle(this).init(node.getInformation());
                if (node.getInformation() instanceof ServiceDefinitionMetadata)
                {
                    for (ServiceImplementationMetadata impl : serviceImpls.values())
                    {
                        if (impl.getActualClass().equals(node.getInformation().getActualClass()))
                        {
                            lifeCycle.addImpl(new LifeCycle(this).init(impl));
                        }
                    }
                }
                lifeCycles.put(dep, lifeCycle);
            }
        }
        return lifeCycle;
    }


    private LifeCycle setup(Dependency dep)
    {
        LifeCycle lifecycle = getLifecycle(dep);
        if (!lifecycle.isInstantiated())
        {
            lifecycle.transition(INSTANTIATED);
        }
        return lifecycle.transition(SETUP_COMPLETE);
    }

    private LifeCycle enable(Dependency dep)
    {
        LifeCycle lifecycle = getLifecycle(dep);
        return lifecycle.transition(ENABLED);
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

    @Override
    public Set<ServiceContainer<?>> getServices()
    {
        return serviceManager.getServices();
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
            ServiceContainer<T> service = serviceManager.getService(type);
            if (service != null)
            {
                return service.getImplementation();
            }
            LifeCycle lifecycle = getLifecycle(new BasicDependency(type.getName(), null));
            if (!lifecycle.isInstantiated())
            {
                lifecycle.transition(INSTANTIATED).transition(ENABLED);
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
        return getLifecycle(dep).transition(DISABLED);
    }

    @Override
    public <T> void registerProvider(Class<T> clazz, ValueProvider<T> provider)
    {
        BasicDependency dep = new BasicDependency(clazz.getName(), null);
        graph.provided(dep);
        maybe(dep).init(provider); // Get or create Lifecycle and init
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
}
