package de.cubeisland.engine.modularity.core;

import java.lang.reflect.Field;
import java.util.List;
import de.cubeisland.engine.modularity.core.graph.Dependency;

public class FieldsInjection extends InjectionPoint
{
    private List<String> fieldNames;

    public FieldsInjection(Dependency self, List<Dependency> dependencies, List<String> fieldNames)
    {
        super(self, dependencies);
        this.fieldNames = fieldNames;
    }

    @Override
    public Object inject(Modularity modularity, Object into)
    {
        try
        {
            Class<?> clazz = getClazz(modularity, getSelf());
            Object[] deps = collectDependencies(modularity);
            for (int i = 0; i < fieldNames.size(); i++)
            {
                Field field = clazz.getField(fieldNames.get(i));
                field.set(into, deps[i]);
            }
            return into;
        }
        catch (NoSuchFieldException e)
        {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
