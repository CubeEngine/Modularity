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
package de.cubeisland.engine.modularity.asm.meta.candidate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

import static java.util.Collections.unmodifiableMap;

public class AnnotationCandidate extends Candidate
{
    private final TypeReference type;
    // reference to Class/Field/Method ?
    private final Map<String, Object> properties = new HashMap<String, Object>();

    public AnnotationCandidate(TypeReference type)
    {
        super(type.getReferencedClass());
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public void addProperty(String name, Object value)
    {
        Object val = properties.get(name);
        if (val instanceof List)
        {
            ((List)val).add(value);
        }
        else
        {
            properties.put(name, value);
        }
    }

    public Map<String, Object> getProperties()
    {
        return unmodifiableMap(properties);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof AnnotationCandidate))
        {
            return false;
        }

        final AnnotationCandidate that = (AnnotationCandidate)o;

        if (!properties.equals(that.properties))
        {
            return false;
        }
        if (!type.equals(that.type))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 31 * result + properties.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("@").append(getName());
        if (!properties.isEmpty())
        {
            sb.append('(');
            String splitter = "";
            for (Entry<String, Object> entry : properties.entrySet())
            {
                sb.append(splitter);
                if (properties.size() != 1 || !"value".equals(entry.getKey()))
                {
                    sb.append(entry.getKey()).append(" = ");
                }
                sb.append(entry.getValue());
                splitter = ", ";
            }
            sb.append(')');
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public <T> T property(String property)
    {
        T result = (T)properties.get(property);
        if (result == null)
        {
            // TODO how do we want to handle this?
            try
            {
                // if not found get default from annotation
                result = (T)Class.forName(this.getName()).getMethod(property).getDefaultValue();
                if (result.getClass().isArray())
                {
                    ArrayList list = new ArrayList();
                    for (int i = 0; i < Array.getLength(result); i++)
                    {
                        list.add(Array.get(result, i));
                    }
                    result = (T)list;
                }
            }
            catch (ClassNotFoundException ignore)
            {
                return null;
            }
            catch (NoSuchMethodException ignore)
            {
                return null;
            }
        }
        return result;
    }
}
