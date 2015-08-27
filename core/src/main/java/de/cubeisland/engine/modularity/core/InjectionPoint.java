package de.cubeisland.engine.modularity.core;

import java.util.List;
import de.cubeisland.engine.modularity.core.graph.Dependency;

public abstract class InjectionPoint
{
    private Dependency self;
    private List<Dependency> dependencies;

    public InjectionPoint(Dependency self, List<Dependency> dependencies)
    {
        this.self = self;
        this.dependencies = dependencies;
    }

    public List<Dependency> getDependencies()
    {
        return dependencies;
    }

    public Dependency getSelf()
    {
        return self;
    }

    public abstract Object inject(Modularity modularity, Object into);

    protected Object[] collectDependencies(Modularity modularity)
    {
        Object[] result = new Object[dependencies.size()];
        for (int i = 0; i < dependencies.size(); i++)
        {
            final Dependency dependency = dependencies.get(i);
            try
            {
                LifeCycle lifecycle = modularity.getLifecycle(dependency);
                if (dependency.required())
                {
                    result[i] = lifecycle.getProvided(modularity.getLifecycle(self));
                }
                else
                {
                    result[i] = lifecycle.getMaybe(); // TODO maybes for Provided
                }
            }
            catch (MissingDependencyException e)
            {
                if (dependency.required())
                {
                    throw e;
                }
                result[i] = modularity.maybe(dependency).getMaybe();
            }
        }
        return result;
    }

    public Class<?>[] getDependencies(Modularity modularity)
    {
        Class<?>[] classes = new Class<?>[dependencies.size()];
        for (int i = 0; i < dependencies.size(); i++)
        {
            classes[i] = getClazz(modularity, dependencies.get(i));
        }
        return classes;
    }

    protected Class<?> getClazz(Modularity modularity, Dependency dep)
    {
        try
        {
            return Class.forName(dep.name(), true, modularity.getLifecycle(dep).getInformation().getClassLoader());
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
