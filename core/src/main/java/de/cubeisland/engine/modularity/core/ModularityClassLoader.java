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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;

/**
 * The ClassLoader for a single file
 */
public class ModularityClassLoader extends URLClassLoader
{
    private final Map<String, Class> classMap = new ConcurrentHashMap<String, Class>();
    private final Modularity modularity;
    private final DependencyInformation info;

    public ModularityClassLoader(Modularity modularity, URL jarURL, DependencyInformation info, ClassLoader parent)
    {
        super(new URL[]{jarURL}, parent);
        this.modularity = modularity;
        this.info = info;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        return findClass(name, true);
    }

    public Class<?> findClass(String name, boolean global) throws ClassNotFoundException
    {
        Class clazz = classMap.get(name);
        if (clazz == null)
        {
            try
            {
                clazz = super.findClass(name);
            }
            catch (ClassNotFoundException ignored)
            {}

            if (clazz == null && global)
            {
                clazz = modularity.getClazz(info, name);
            }

            if (clazz == null)
            {
                throw new ClassNotFoundException(name);
            }
            this.classMap.put(name, clazz);
        }
        return clazz;
    }

    @Override
    public URL getResource(String name)
    {
        return super.getResource(name); // TODO
    }
}
