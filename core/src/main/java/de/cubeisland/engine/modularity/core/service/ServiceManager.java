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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.service.Service.Priority;

public class ServiceManager
{
    private final Map<Class<?>, Service<?>> services = new HashMap<Class<?>, Service<?>>();

    @SuppressWarnings("unchecked")
    public <S> Service<S> getService(Class<S> service)
    {
        synchronized (this.services)
        {
            return (Service<S>)this.services.get(service);
        }
    }

    public <S> S getServiceImplementation(Class<S> service)
    {
        return this.getService(service).getImplementation();
    }

    public <S> Service<S> registerService(Module module, Class<S> interfaceClass, S implementation)
    {
        return this.registerService(module, interfaceClass, implementation, Priority.NORMAL);
    }

    @SuppressWarnings("unchecked")
    public <S> Service<S> registerService(Module module, Class<S> interfaceClass, S implementation, Priority priority)
    {
        if (!interfaceClass.isInterface())
        {
            throw new IllegalArgumentException("Services have to be interfaces!");
        }

        synchronized (this.services)
        {
            Service<S> service = (Service<S>)this.services.get(interfaceClass);
            if (service == null)
            {
                this.services.put(interfaceClass, service = new Service<S>(module, interfaceClass));
            }
            service.addImplementation(module, implementation, priority);
            return service;
        }
    }

    public void unregisterService(Class interfaceClass)
    {
        synchronized (this.services)
        {
            this.services.remove(interfaceClass);
        }
    }

    public void unregisterServices(Module module)
    {
        synchronized (this.services)
        {
            Iterator<Entry<Class<?>, Service<?>>> it = this.services.entrySet().iterator();
            while (it.hasNext())
            {
                if (it.next().getValue().getModule() == module)
                {
                    it.remove();
                }
            }
        }
    }

    public void removeImplementations(Module module)
    {
        synchronized (this.services)
        {
            for (Service<?> service : this.services.values())
            {
                service.removeImplementations(module);
            }
        }
    }

    public Set<Service<?>> getServices()
    {
        synchronized (this.services)
        {
            return new HashSet<Service<?>>(this.services.values());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> boolean isImplemented(Class<T> serviceInterface)
    {
        Service<T> service;
        synchronized (this.services)
        {
            service = (Service<T>)this.services.get(serviceInterface);
        }
        return service != null && service.hasImplementations();
    }
}

