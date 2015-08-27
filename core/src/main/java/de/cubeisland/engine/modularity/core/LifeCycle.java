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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import javax.inject.Provider;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
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
    private Map<Integer, Method> setup = new TreeMap<Integer, Method>();

    private SettableMaybe maybe;
    private Queue<LifeCycle> impls = new PriorityQueue<LifeCycle>();

    public LifeCycle(Modularity modularity)
    {
        this.modularity = modularity;
    }

    public LifeCycle init(DependencyInformation info)
    {
        System.out.print("Start Lifecycle of " + info.getIdentifier().name() + ":" + info.getIdentifier().version() +  "\n");
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
                    if (info instanceof ServiceDefinitionMetadata)
                    {
                        Class<?> instanceClass = Class.forName(info.getClassName(), true, info.getClassLoader());
                        instance = new ServiceProvider(instanceClass, impls);
                        // TODO find impls in modularity and link them to this
                    }
                    else
                    {
                        this.instance = info.injectionPoints().get(INSTANTIATED.name(0)).inject(modularity, this);
                        info.injectionPoints().get(INSTANTIATED.name(1)).inject(modularity, this);
                        if (instance instanceof Module)
                        {
                            MODULE_META_FIELD.set(instance, info);
                            MODULE_MODULARITY_FIELD.set(instance, modularity);
                            MODULE_LIFECYCLE.set(instance, this);
                        }
                        findMethods();
                    }
                    break;
                case SETUP_COMPLETE:
                    for (Method method : setup.values())
                    {
                        invoke(method);
                    }
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
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException(e);
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

            if (method.isAnnotationPresent(Setup.class))
            {
                info.injectionPoints().get(SETUP_COMPLETE.name(method.getAnnotation(Setup.class).value())).inject(modularity, this);
            }
            else if (method.isAnnotationPresent(Enable.class))
            {
                info.injectionPoints().get(ENABLED.name()).inject(modularity, this);
            }
            else
            {
                method.invoke(instance);
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
            this.transition(INSTANTIATED).transition(SETUP_COMPLETE);
            if (!(info instanceof ModuleMetadata))
            {
                transition(ENABLED); // All But Modules get Enabled
            }
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

        ;

        public String name(Integer value)
        {
            return value == null ? name() : name() + ":" + value;
        }
    }
}
