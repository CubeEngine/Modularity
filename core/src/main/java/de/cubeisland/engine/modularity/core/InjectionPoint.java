package de.cubeisland.engine.modularity.core;

import java.util.List;
import de.cubeisland.engine.modularity.core.graph.Dependency;

public abstract class InjectionPoint<T>
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

    public abstract Object inject(Modularity modularity, T into);

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

    protected Class getClazz()
    {
        try
        {
            return Class.forName(self.name(), true, cl); // TODO get ClassLoader
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
