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
package de.cubeisland.engine.modularity.core.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceProviderMetadata;
import de.cubeisland.engine.modularity.core.service.ProxyServiceContainer.Priority;

public class ServiceManager
{
    private final Map<Class<?>, ServiceContainer<?>> services = new HashMap<Class<?>, ServiceContainer<?>>();

    @SuppressWarnings("unchecked")
    public <S> ServiceContainer<S> getService(Class<S> service)
    {
        synchronized (this.services)
        {
            return (ServiceContainer<S>)this.services.get(service);
        }
    }

    public <S> S getServiceImplementation(Class<S> service)
    {
        return this.getService(service).getImplementation();
    }

    public <S> ServiceContainer<S> registerService(Class<S> interfaceClass, DependencyInformation info)
    {
        synchronized (this.services)
        {
            ProxyServiceContainer<S> service = new ProxyServiceContainer<S>(interfaceClass, info);
            this.services.put(interfaceClass, service);
            return service;
        }

    }

    public <S> ServiceContainer<S> registerService(Class<S> interfaceClass, S implementation)
    {
        return this.registerService(interfaceClass, implementation, Priority.NORMAL);
    }

    @SuppressWarnings("unchecked")
    public <S> ServiceContainer<S> registerService(Class<S> interfaceClass, S implementation, Priority priority)
    {
        synchronized (this.services)
        {
            ServiceContainer<S> service = (ServiceContainer<S>)this.services.get(interfaceClass);
            if (service == null || !interfaceClass.isInterface())
            {
                InstancedServiceContainer<S> container = new InstancedServiceContainer<S>(interfaceClass, implementation);
                services.put(interfaceClass, container);
                return container;
            }
            if (implementation != null)
            {
                service.addImplementation(implementation, priority);
            }
            return service;
        }
    }

    /**
     * Registers a new service-provider
     *
     * @param info the ServiceProviderMetadata
     * @param provider a {@link Provider} for the Service or an instance
     * @param <S> the service class
     * @return a ServiceContainer for the service
     */
    @SuppressWarnings("unchecked")
    public <S> ServiceContainer<S> registerService(ServiceProviderMetadata info, Object provider)
    {
        synchronized (this.services)
        {
            try
            {
                Class serviceClass = Class.forName(info.getActualClass(), true, info.getClassLoader());
                if (provider instanceof Provider)
                {
                    ProvidedServiceContainer<S> container = new ProvidedServiceContainer<S>(serviceClass, info, ((Provider)provider));
                    services.put(serviceClass, container);
                    return container;
                }
                if (serviceClass.isAssignableFrom(provider.getClass()))
                {
                    InstancedServiceContainer<S> container = new InstancedServiceContainer<S>(serviceClass, (S)provider);
                    services.put(serviceClass, container);
                    return container;
                }
                throw new IllegalArgumentException("Object does not provide Service");
            }
            catch (ClassNotFoundException e)
            {
                throw new IllegalStateException(e);
            }
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
            Iterator<Entry<Class<?>, ServiceContainer<?>>> it = this.services.entrySet().iterator();
            while (it.hasNext())
            {
                if (it.next().getValue().getClass().getClassLoader() == module.getInformation().getClassLoader())
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
            for (ServiceContainer<?> service : this.services.values())
            {
                service.removeImplementations(module.getClass().getClassLoader());
            }
        }
    }

    public Set<ServiceContainer<?>> getServices()
    {
        synchronized (this.services)
        {
            return new HashSet<ServiceContainer<?>>(this.services.values());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> boolean isImplemented(Class<T> serviceInterface)
    {
        ProxyServiceContainer<T> service;
        synchronized (this.services)
        {
            service = (ProxyServiceContainer<T>)this.services.get(serviceInterface);
        }
        return service != null && service.hasImplementations();
    }

}

