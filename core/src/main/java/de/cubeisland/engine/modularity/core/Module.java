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

import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;

public abstract class Module implements Instance
{
    private final ModuleMetadata metadata = null;
    private final Modularity modularity = null;

    public ModuleMetadata getInformation()
    {
        return metadata;
    }
    public Modularity getModularity()
    {
        return modularity;
    }

    public <T> T getProvided(Class<T> clazz)
    {
        ValueProvider<T> provider = getModularity().getProvider(clazz);
        if (provider != null)
        {
            return provider.get(getInformation(), getModularity());
        }
        throw new IllegalArgumentException("Provider not registered for " + clazz.getName());
    }
}
