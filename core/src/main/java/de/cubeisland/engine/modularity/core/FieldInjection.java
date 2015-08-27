package de.cubeisland.engine.modularity.core;

import java.util.List;
import de.cubeisland.engine.modularity.core.graph.Dependency;

public class FieldInjection extends InjectionPoint<Object>
{
    public FieldInjection(Dependency self, List<Dependency> dependencies)
    {
        super(self, dependencies);
    }

    @Override
    public Object inject(Modularity modularity, Object into)
    {


        return null;
    }
}
