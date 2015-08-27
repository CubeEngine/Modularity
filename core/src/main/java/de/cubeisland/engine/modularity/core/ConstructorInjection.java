package de.cubeisland.engine.modularity.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

import de.cubeisland.engine.modularity.core.graph.Dependency;

public class ConstructorInjection extends InjectionPoint
{
    public ConstructorInjection(Dependency self, List<Dependency> dependencies)
    {
        super(self, dependencies);
    }

    @Override
    public Object inject(Modularity modularity, Object into)
    {
        try
        {
            Constructor constructor = getClazz(modularity, getSelf()).getConstructor(getDependencies(modularity));
            return constructor.newInstance(collectDependencies(modularity));
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
        catch (NoSuchMethodException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
