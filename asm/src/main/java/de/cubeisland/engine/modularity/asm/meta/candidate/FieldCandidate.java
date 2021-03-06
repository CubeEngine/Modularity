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
package de.cubeisland.engine.modularity.asm.meta.candidate;

import de.cubeisland.engine.modularity.asm.meta.TypeReference;

/**
 * Represents a Field potentially containing DependencyInformation
 */
public class FieldCandidate extends Candidate
{
    private final TypeReference declaringClass;
    private final int modifiers;
    private final TypeReference type;
    private final Object value;

    public FieldCandidate(TypeReference declaringClass, String name, int modifiers, TypeReference type, Object value)
    {
        super(name);
        this.declaringClass = declaringClass;
        this.modifiers = modifiers;
        this.type = type;
        this.value = value;
    }

    /**
     * Returns the declaring class as TypeReference
     *
     * @return the declaring class as TypeReference
     */
    public TypeReference getDeclaringClass()
    {
        return declaringClass;
    }

    /**
     * Returns the modifiers
     *
     * @return the modifiers
     */
    public int getModifiers()
    {
        return modifiers;
    }

    /**
     * Returns the type of the field as TypeReference
     *
     * @return the type of the field
     */
    public TypeReference getType()
    {
        return type;
    }

    /**
     * Returns the value of the field if available at compile-time
     *
     * @return the value of the field
     */
    public Object getValue()
    {
        return value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof FieldCandidate))
        {
            return false;
        }

        final FieldCandidate that = (FieldCandidate) o;

        if (modifiers != that.modifiers)
        {
            return false;
        }
        if (!declaringClass.equals(that.declaringClass))
        {
            return false;
        }
        if (!type.equals(that.type))
        {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = declaringClass.hashCode();
        result = 31 * result + modifiers;
        result = 31 * result + type.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (AnnotationCandidate candidate : getAnnotations())
        {
            s.append(candidate).append("\n\t");
        }
        s.append(stringModifiers(modifiers));
        s.append(type).append(' ');
        s.append(getName());
        if (value != null)
        {
            s.append(" = ").append(value);
        }
        return s.append(';').toString();
    }
}
