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

import java.util.Collections;
import java.util.List;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.core.Instance;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceProviderMetadata;
import de.cubeisland.engine.modularity.core.service.ProxyServiceContainer.Implementation;
import de.cubeisland.engine.modularity.core.service.ProxyServiceContainer.Priority;

public class ProvidedServiceContainer<T> implements ServiceContainer<T>, Instance
{
    private final Implementation impl;
    private Class<T> interfaceClass;
    private ServiceProviderMetadata info;
    private Provider<T> provider;

    public ProvidedServiceContainer(Class<T> interfaceClass, ServiceProviderMetadata info, Provider<T> provider)
    {
        this.interfaceClass = interfaceClass;
        this.info = info;
        this.provider = provider;
        this.impl = new Implementation(this.provider, Priority.HIGHEST);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getInterface()
    {
        return interfaceClass;
    }

    @Override
    public T getImplementation()
    {
        return provider.get();
    }

    @Override
    public List<Implementation> getImplementations()
    {
        return Collections.singletonList(impl);
    }

    @Override
    public boolean hasImplementations()
    {
        return true;
    }

    @Override
    public ProxyServiceContainer<T> addImplementation(T implementation, Priority priority)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProxyServiceContainer<T> removeImplementation(T implementation)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProxyServiceContainer<T> removeImplementations(ClassLoader loader)
    {
        throw new UnsupportedOperationException();
    }

    public Provider<T> getProvider()
    {
        return provider;
    }

    @Override
    public DependencyInformation getInformation()
    {
        return this.info;
    }
}
