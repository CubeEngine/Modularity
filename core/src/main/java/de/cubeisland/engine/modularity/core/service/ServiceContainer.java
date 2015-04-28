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
/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cubeisland.engine.modularity.core.service;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import de.cubeisland.engine.modularity.core.Instance;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;

public class ServiceContainer<T> implements Instance
{
    private final Class<T> interfaceClass;
    private final DependencyInformation info;

    private final PriorityQueue<Implementation> implementations;

    private final T proxy;
    private final ServiceInvocationHandler invocationHandler;
    @SuppressWarnings("unchecked")
    public ServiceContainer(Class<T> interfaceClass, DependencyInformation info)
    {
        this.interfaceClass = interfaceClass;
        this.info = info;
        this.implementations = new PriorityQueue<Implementation>();
        this.invocationHandler = new ServiceInvocationHandler(this, this.implementations);
        this.proxy = (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass},
                                               this.invocationHandler);
    }

    public Class<T> getInterface()
    {
        return interfaceClass;
    }

    public T getImplementation()
    {
        return this.proxy;
    }

    public List<Implementation> getImplementations()
    {
        return new ArrayList<Implementation>(implementations);
    }

    public boolean hasImplementations()
    {
        synchronized (this.implementations)
        {
            return !this.implementations.isEmpty();
        }
    }

    public ServiceContainer<T> addImplementation(T implementation, Priority priority)
    {
        synchronized (this.implementations)
        {
            this.implementations.add(new Implementation(implementation, priority));
        }
        return this;
    }

    public ServiceContainer<T> removeImplementation(T implementation)
    {
        synchronized (this.implementations)
        {
            Iterator<Implementation> it = this.implementations.iterator();
            while (it.hasNext())
            {
                if (it.next().getTarget() == implementation)
                {
                    it.remove();
                }
            }
        }
        return this;
    }

    public ServiceContainer<T> removeImplementations(Module module)
    {
        synchronized (this.implementations)
        {
            Iterator<Implementation> it = this.implementations.iterator();
            while (it.hasNext())
            {
                if (it.next().getClass().getClassLoader() == module.getInformation().getClassLoader())
                {
                    it.remove();
                }
            }
        }
        return this;
    }

    @Override
    public DependencyInformation getInformation()
    {
        return info;
    }

    public DependencyInformation getInfo()
    {
        return info;
    }

    static class Implementation implements Comparable<Implementation>
    {
        private final Object target;
        private final Priority priority;

        public Implementation(Object target, Priority priority)
        {
            this.target = target;
            this.priority = priority;
        }

        public Object getTarget()
        {
            return target;
        }

        public Priority getPriority()
        {
            return priority;
        }

        public int compareTo(Implementation other)
        {
            if (priority.ordinal() == other.priority.ordinal())
            {
                return 0;
            }
            else
            {
                return priority.ordinal() < other.priority.ordinal() ? 1 : -1;
            }
        }


    }
    public static enum Priority
    {
        LOWEST,
        LOWER,
        NORMAL,
        HIGHER,
        HIGHEST;
    }
}
