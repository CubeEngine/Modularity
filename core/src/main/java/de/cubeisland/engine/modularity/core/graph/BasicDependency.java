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
package de.cubeisland.engine.modularity.core.graph;

public class BasicDependency implements Dependency
{
    private final String name;
    private final String version;

    public BasicDependency(String name, String version)
    {
        this.name = name;
        this.version = version;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public String version()
    {
        return version;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Dependency))
        {
            return false;
        }

        final Dependency that = (Dependency)o;

        if (name() != null ? !name().equals(that.name()) : that.name() != null)
        {
            return false;
        }
        return !(version() != null ? !version().equals(that.version()) : that.version() != null);
    }

    @Override
    public int hashCode()
    {
        int result = name() != null ? name().hashCode() : 0;
        result = 31 * result + (version() != null ? version().hashCode() : 0);
        return result;
    }


    @Override
    public String toString()
    {
        return name + ":" + String.valueOf(version);
    }
}
