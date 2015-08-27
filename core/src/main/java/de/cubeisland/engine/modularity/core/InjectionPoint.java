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

    public abstract Object inject(Modularity modularity, LifeCycle lifeCycle);

    protected Object[] collectDependencies(Modularity modularity, LifeCycle lifeCycle)
    {
        Object[] result = new Object[dependencies.size()];
        for (int i = 0; i < dependencies.size(); i++)
        {
            final Dependency dependency = dependencies.get(i);
            try
            {
                LifeCycle dep = modularity.getLifecycle(dependency);
                if (dependency.required())
                {
                    result[i] = dep.getProvided(lifeCycle);
                }
                else
                {
                    result[i] = dep.getMaybe(); // TODO maybes for Provided
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
            Dependency dep = dependencies.get(i);
            classes[i] = dep.required() ? getClazz(dep, modularity.getLifecycle(dep)) : Maybe.class;
        }
        return classes;
    }

    protected Class<?> getClazz(Dependency dep, LifeCycle lifeCycle)
    {
        try
        {
            return Class.forName(dep.name(), true, lifeCycle.getInformation().getClassLoader());
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
