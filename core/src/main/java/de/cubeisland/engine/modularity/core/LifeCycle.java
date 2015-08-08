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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.core.graph.Dependency;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceDefinitionMetadata;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.core.marker.Setup;
import de.cubeisland.engine.modularity.core.service.ServiceProvider;

import static de.cubeisland.engine.modularity.core.LifeCycle.State.*;

public class LifeCycle
{
    private static final Field MODULE_META_FIELD;
    private static final Field MODULE_MODULARITY_FIELD;
    private static final Field MODULE_LIFECYCLE;

    static
    {
        try
        {
            MODULE_META_FIELD = Module.class.getDeclaredField("metadata");
            MODULE_META_FIELD.setAccessible(true);
            MODULE_MODULARITY_FIELD = Module.class.getDeclaredField("modularity");
            MODULE_MODULARITY_FIELD.setAccessible(true);
            MODULE_LIFECYCLE = Module.class.getDeclaredField("lifeCycle");
            MODULE_LIFECYCLE.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            throw new IllegalStateException();
        }
    }

    private Modularity modularity;
    private DependencyInformation info;
    private State current = NONE;
    private Object instance;

    private Method enable;
    private Method disable;
    private Method setup;
    private HashMap<String, LifeCycle> deps = new HashMap<String, LifeCycle>();

    private SettableMaybe maybe;
    private Queue<LifeCycle> impls = new PriorityQueue<LifeCycle>();

    public LifeCycle(Modularity modularity)
    {
        this.modularity = modularity;
    }

    public LifeCycle init(DependencyInformation info)
    {
        System.out.print("Start Lifecycle of " + info.getIdentifier().name() + "\n");
        this.info = info;
        this.current = LOADED;
        return this;
    }


    public LifeCycle init(ValueProvider provider)
    {
        System.out.print("Registered external provider " + provider.getClass().getName() + "\n");
        this.instance = provider;
        this.current = PROVIDED;
        return this;
    }

    public LifeCycle initProvided(Object object)
    {
        System.out.print("Registered external provided object " + object.getClass().getName() + "\n");
        this.instance = object;
        this.current = PROVIDED;
        return this;
    }

    public LifeCycle transition(State state)
    {
        if (state == NONE)
        {
            return this;
        }
        if (current == state)
        {
            return this;
        }

        System.out.print(info.getIdentifier().name() + " transition to " + state + "... \n");
        try
        {
            switch (state)
            {
                case INSTANTIATED:
                    collectDependencies(info.requiredDependencies(), true);
                    collectDependencies(info.optionalDependencies(), false);
                    this.instance = newInstance();
                    findMethods();
                    break;
                case SETUP_COMPLETE:
                    invoke(setup);
                    break;
                case ENABLED:
                    invoke(enable);
                    updateMaybe(state);
                    break;
                case DISABLED:
                    invoke(disable);
                    updateMaybe(state);
                    // TODO if active impl replace in service with inactive OR disable service too
                    // TODO if service disable all impls too
                    modularity.getGraph().getNode(info.getIdentifier()).getPredecessors(); // TODO somehow implement reload too
                    // TODO disable predecessors
                    break;
                case SHUTDOWN:
                    // TODO unregister mysqlf
                    break;
            }
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalStateException(e.getCause()); // TODO better exception
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e); // TODO better exception
        }

        for (LifeCycle lifeCycle : impls)
        {
            lifeCycle.transition(state);
        }
        current = state;
        System.out.print("done\n");
        return this;
    }

    @SuppressWarnings("unchecked")
    private void updateMaybe(State state)
    {
        if (maybe != null)
        {
            if (state == ENABLED)
            {
                maybe.provide(getProvided(this));
            }
            else if (state == DISABLED)
            {
                maybe.remove();
            }

        }
    }

    private void invoke(Method method) throws InvocationTargetException, IllegalAccessException
    {
        if (method != null)
        {
            System.out.print(instance.getClass().getName() + " invoke " + method.getName() + "\n");
            method.invoke(instance);
        }
    }

    public boolean isInstantiated()
    {
        return instance != null;
    }

    private void collectDependencies(Set<Dependency> deps, boolean required)
    {
        for (Dependency dep : deps)
        {
            try
            {
                LifeCycle lifecycle = modularity.getLifecycle(dep);
                this.deps.put(dep.name(), lifecycle);
            }
            catch (MissingDependencyException e)
            {
                if (required)
                {
                    throw e;
                }
                modularity.maybe(dep);

                System.out.println("Missing optional dependency to: " + dep);
            }
        }
    }

    private void findMethods()
    {
        // find enable and disable methods
        for (Method method : getProvided(this).getClass().getMethods())
        {
            if (method.isAnnotationPresent(Enable.class))
            {
                enable = method;
            }
            if (method.isAnnotationPresent(Disable.class))
            {
                disable = method;
            }
            if (method.isAnnotationPresent(Setup.class))
            {
                setup = method;
            }
        }
    }

    private Object newInstance()
    {
        try
        {
            Class<?> instanceClass = Class.forName(info.getClassName(), true, info.getClassLoader());
            Object instance;
            if (info instanceof ServiceDefinitionMetadata)
            {
                instance = new ServiceProvider(instanceClass, impls);
                // TODO find impls in modularity and link them to this
            }
            else // Module, ServiceImpl, ServiceProvider, ValueProvider
            {
                instance = injectDependencies(newInstance(getConstructor(instanceClass)));
            }
            if (instance == null)
            {
                throw new IllegalStateException();
            }
            return instance;
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new IllegalStateException(e);
        }
        catch (InstantiationException e)
        {
            throw new IllegalStateException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private Object newInstance(Constructor<?> constructor) throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        return constructor.newInstance(getConstructorParams(constructor));
    }

    private Object injectDependencies(Object instance) throws IllegalAccessException
    {
        if (instance instanceof Module)
        {
            MODULE_META_FIELD.set(instance, info);
            MODULE_MODULARITY_FIELD.set(instance, modularity);
            MODULE_LIFECYCLE.set(instance, this);
        }
        for (Field field : instance.getClass().getDeclaredFields())
        {
            if (field.isAnnotationPresent(Inject.class))
            {
                Class<?> type = field.getType();
                boolean isMaybe = Maybe.class.equals(type);
                if (isMaybe)
                {
                    type = (Class)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
                }
                field.setAccessible(true);
                field.set(instance, getDependency(type, isMaybe));
            }
        }
        return instance;
    }

    private Object[] getConstructorParams(Constructor<?> instanceConstructor)
    {
        Object[] parameters;
        Class<?>[] parameterTypes = instanceConstructor.getParameterTypes();
        parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++)
        {
            parameters[i] = getDependency(parameterTypes[i], false);
        }
        return parameters;
    }

    private Object getDependency(Class<?> type, boolean isMaybe)
    {
        LifeCycle lifeCycle = deps.get(type.getName());
        if (!isMaybe && lifeCycle.current == NONE)
        {
            throw new IllegalStateException();
        }

        return isMaybe ? lifeCycle.getMaybe() : lifeCycle.getProvided(this);
    }


    private Constructor<?> getConstructor(Class<?> instanceClass)
    {
        Constructor<?> instanceConstructor = null;
        for (Constructor<?> constructor : instanceClass.getConstructors())
        {
            if (constructor.isAnnotationPresent(Inject.class))
            {
                instanceConstructor = constructor;
                break;
            }
        }
        if (instanceConstructor == null)
        {
            try
            {
                instanceConstructor = instanceClass.getConstructor();
            }
            catch (NoSuchMethodException e)
            {
                throw new IllegalStateException(info.getClassName() + " has no usable Constructor");// TODO error
            }
        }
        instanceConstructor.setAccessible(true);
        return instanceConstructor;
    }

    public Object getInstance()
    {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public Maybe getMaybe()
    {
        if (maybe == null)
        {
            maybe = new SettableMaybe(getProvided(this));
        }
        return maybe;
    }

    public Object getProvided(LifeCycle lifeCycle)
    {
        if (instance == null)
        {
            this.transition(INSTANTIATED).transition(SETUP_COMPLETE).transition(ENABLED);
        }
        Object toSet = instance;
        if (toSet instanceof Provider)
        {
            toSet = ((Provider)toSet).get();
        }
        if (toSet instanceof ValueProvider)
        {
            toSet = ((ValueProvider)toSet).get(lifeCycle, modularity);
        }
        return toSet;
    }

    public void addImpl(LifeCycle impl)
    {
        this.impls.add(impl);
    }

    public DependencyInformation getInformation()
    {
        return info;
    }

    public enum State
    {
        NONE,
        LOADED,
        INSTANTIATED,
        SETUP_COMPLETE,
        ENABLED,
        DISABLED,
        SHUTDOWN,

        PROVIDED // TODO prevent changing / except shutdown?

    }
}
