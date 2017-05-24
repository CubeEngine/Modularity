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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.core.graph.Dependency;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceDefinitionMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceImplementationMetadata;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceProviderMetadata;
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
    private Map<Integer, Method> setup = new TreeMap<Integer, Method>();

    private Map<Dependency, SettableMaybe> maybes = new HashMap<Dependency, SettableMaybe>();
    private Queue<LifeCycle> impls = new LinkedList<LifeCycle>();

    public LifeCycle(Modularity modularity)
    {
        this.modularity = modularity;
    }

    public LifeCycle load(DependencyInformation info)
    {
        this.info = info;
        this.current = LOADED;
        return this;
    }


    public LifeCycle provide(ValueProvider provider)
    {
        this.instance = provider;
        this.current = PROVIDED;
        return this;
    }

    public LifeCycle initProvided(Object object)
    {
        this.instance = object;
        this.current = PROVIDED;
        return this;
    }

    public boolean isIn(State state)
    {
        return current == state;
    }

    public LifeCycle instantiate()
    {
        if (isIn(NONE))
        {
            throw new IllegalStateException("Cannot instantiate when not loaded");
        }

        if (isIn(LOADED))
        {
            try
            {
                if (info instanceof ServiceDefinitionMetadata)
                {
                    ClassLoader classLoader = info.getClassLoader();
                    if (classLoader == null) // may happen when loading from classpath
                    {
                        classLoader = modularity.getClass().getClassLoader(); // get parent classloader then
                    }
                    Class<?> instanceClass = Class.forName(info.getClassName(), true, classLoader);
                    instance = new ServiceProvider(instanceClass, impls);
                    // TODO find impls in modularity and link them to this

                    // TODO transition all impls to INSTANTIATED?
                }
                else
                {
                    this.instance = info.injectionPoints().get(INSTANTIATED.name(0)).inject(modularity, this);
                    if (instance instanceof Module)
                    {
                        MODULE_META_FIELD.set(instance, info);
                        MODULE_MODULARITY_FIELD.set(instance, modularity);
                        MODULE_LIFECYCLE.set(instance, this);
                    }
                    info.injectionPoints().get(INSTANTIATED.name(1)).inject(modularity, this);
                    findMethods();
                }
            }
            catch (ClassNotFoundException e)
            {
                throw new IllegalStateException(e);
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalStateException(e);
            }
            current = INSTANTIATED;
        }

        // else State already reached or provided
        return this;
    }

    public LifeCycle setup()
    {
        if (isIn(NONE))
        {
            throw new IllegalStateException("Cannot instantiate when not loaded");
        }

        if (isIn(LOADED))
        {
            this.instantiate();
        }

        if (isIn(INSTANTIATED))
        {
            // TODO abstract those methods away
            for (Method method : setup.values())
            {
                invoke(method);
            }

            for (LifeCycle impl : impls)
            {
                impl.setup();
            }

            current = SETUP;
        }

        // else reached or provided
        return this;
    }

    public LifeCycle enable()
    {
        if (isIn(NONE))
        {
            throw new IllegalStateException("Cannot instantiate when not loaded");
        }

        if (isIn(LOADED))
        {
            this.instantiate();
        }

        if (isIn(INSTANTIATED))
        {
            this.setup();
        }

        if (isIn(SETUP))
        {
            this.modularity.log("Enable " + info.getIdentifier().name());
            modularity.runEnableHandlers(getInstance());

            invoke(enable);
            for (SettableMaybe maybe : maybes.values())
            {
                maybe.provide(getProvided(this));
            }

            for (LifeCycle impl : impls)
            {
                impl.enable();
            }

            current = ENABLED;
        }

        return this;
    }

    public LifeCycle disable()
    {
        if (isIn(ENABLED))
        {
            modularity.runDisableHandlers(getInstance());
            invoke(disable);

            for (SettableMaybe maybe : maybes.values())
            {
                maybe.remove();
            }

            // TODO if active impl replace in service with inactive OR disable service too
            // TODO if service disable all impls too
            modularity.getGraph().getNode(info.getIdentifier()).getPredecessors(); // TODO somehow implement reload too
            // TODO disable predecessors

            for (LifeCycle impl : impls)
            {
                impl.disable();
            }

            current = DISABLED;
        }

        return this;
    }

    private void invoke(Method method)
    {
        if (method != null)
        {
            if (method.isAnnotationPresent(Setup.class))
            {
                info.injectionPoints().get(SETUP.name(method.getAnnotation(Setup.class).value()))
                    .inject(modularity, this);
            }
            else if (method.isAnnotationPresent(Enable.class))
            {
                info.injectionPoints().get(ENABLED.name()).inject(modularity, this);
            }
            else
            {
                try
                {
                    method.invoke(instance);
                }
                catch (IllegalAccessException e)
                {
                    throw new IllegalStateException(e);
                }
                catch (IllegalArgumentException e)
                {
                    throw new IllegalStateException(e);
                }
                catch (InvocationTargetException e)
                {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public boolean isInstantiated()
    {
        return instance != null;
    }

    private void findMethods()
    {
        // find enable and disable methods
        Class<?> clazz = instance.getClass();
        for (Method method : clazz.getMethods())
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
                int value = method.getAnnotation(Setup.class).value();
                setup.put(value, method);
            }
        }
    }

    public Object getInstance()
    {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public Maybe getMaybe(LifeCycle other)
    {
        Dependency identifier = other == null ? null : other.getInformation().getIdentifier();
        SettableMaybe maybe = maybes.get(identifier);
        if (maybe == null)
        {
            maybe = new SettableMaybe(getProvided(other));
            maybes.put(identifier, maybe);
        }
        return maybe;
    }

    public Object getProvided(LifeCycle lifeCycle)
    {
        boolean enable = true;
        if (info instanceof ModuleMetadata)
        {
            enable = false;
        }
        if (instance == null)
        {
            this.instantiate();
        }
        if (enable)
        {
            this.enable(); // Instantiate Setup and enable dependency before providing it to someone else
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
        SETUP,
        ENABLED,
        DISABLED,
        SHUTDOWN,

        PROVIDED // TODO prevent changing / except shutdown?

        ;

        public String name(Integer value)
        {
            return value == null ? name() : name() + ":" + value;
        }
    }

    @Override
    public String toString() {
        return info.getIdentifier().name() + " " + super.toString();
    }
}
