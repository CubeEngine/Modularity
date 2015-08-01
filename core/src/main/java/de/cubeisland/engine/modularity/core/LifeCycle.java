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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.core.marker.Setup;

public class LifeCycle
{
    private Method enable;
    private Method disable;
    private Method setup;
    private final Object instance;

    public LifeCycle(Object instance)
    {
        this.instance = instance;
        // find enable and disable methods
        for (Method method : instance.getClass().getMethods())
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

    public void transition(State state)
    {
        try
        {
            switch (state)
            {
                case SETUP:
                    invoke(setup);
                    break;
                case ENABLED:
                    invoke(enable);
                    break;
                case DISABLED:
                    invoke(disable);
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

    }

    private void invoke(Method method) throws InvocationTargetException, IllegalAccessException
    {
        if (method != null)
        {
            method.invoke(instance);
        }
    }

    public enum State
    {
        SETUP, ENABLED, DISABLED
    }
}
