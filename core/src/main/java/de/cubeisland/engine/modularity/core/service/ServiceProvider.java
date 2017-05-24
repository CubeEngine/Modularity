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
package de.cubeisland.engine.modularity.core.service;

import java.lang.reflect.InvocationHandler;
import java.util.Queue;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.core.LifeCycle;

import static java.lang.reflect.Proxy.newProxyInstance;

public class ServiceProvider<T> implements Provider<T>
{
    private Class clazz;
    private final T proxy;
    private final InvocationHandler invocationHandler;

    @SuppressWarnings("unchecked")
    public ServiceProvider(Class clazz, Queue<LifeCycle> impls)
    {
        this.clazz = clazz;
        this.invocationHandler = new ServiceInvocationHandler(this, impls);
        this.proxy = (T)newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this.invocationHandler);
    }

    public ServiceProvider(Class clazz, Provider<T> proxy)
    {
        this.clazz = clazz;
        this.invocationHandler = new ProxyInvocationHandler(this, proxy);
        this.proxy = (T)newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this.invocationHandler);
    }

    @Override
    public T get()
    {
        return proxy;
    }

    public Class getInterface()
    {
        return clazz;
    }
}
