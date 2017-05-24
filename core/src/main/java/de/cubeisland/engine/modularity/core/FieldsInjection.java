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
    public Object inject(Modularity modularity, LifeCycle lifeCycle)
    {
        try
        {
            Class<?> clazz = getClazz(getSelf(), lifeCycle);
            Object[] deps = collectDependencies(modularity, lifeCycle);
            for (int i = 0; i < fieldNames.size(); i++)
            {
                Field field = clazz.getDeclaredField(fieldNames.get(i));
                field.setAccessible(true);
                field.set(lifeCycle.getInstance(), deps[i]);
                modularity.runPostInjectHandler(field, deps[i], lifeCycle.getInstance());
            }
            return lifeCycle.getInstance();
        }
        catch (NoSuchFieldException e)
        {
            throw new IllegalStateException(getSelf().name(), e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(getSelf().name(), e);
        }
    }

    public List<String> getFieldNames()
    {
        return fieldNames;
    }
}
