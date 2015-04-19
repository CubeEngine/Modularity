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

import java.util.List;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

public class MethodCandidate extends Candidate
{
    private final TypeReference declaringClass;
    private final int modifiers;
    private final TypeReference returnType;
    private final List<TypeReference> parameterTypes;

    public MethodCandidate(TypeReference declaringClass, String name, int modifiers, TypeReference returnType, List<TypeReference> parameterTypes)
    {
        super(name);
        this.declaringClass = declaringClass;
        this.modifiers = modifiers;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    public TypeReference getDeclaringClass()
    {
        return declaringClass;
    }

    public int getModifiers()
    {
        return modifiers;
    }

    public TypeReference getReturnType()
    {
        return returnType;
    }

    public List<TypeReference> getParameterTypes()
    {
        return parameterTypes;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof MethodCandidate))
        {
            return false;
        }

        final MethodCandidate that = (MethodCandidate)o;

        if (modifiers != that.modifiers)
        {
            return false;
        }
        if (!declaringClass.equals(that.declaringClass))
        {
            return false;
        }
        if (!parameterTypes.equals(that.parameterTypes))
        {
            return false;
        }
        if (!returnType.equals(that.returnType))
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
        result = 31 * result + returnType.hashCode();
        result = 31 * result + parameterTypes.hashCode();
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
        s.append(returnType).append(' ');
        s.append(getName());
        s.append('(');

        String splitter = "";
        for (final TypeReference parameterType : this.parameterTypes)
        {
            s.append(splitter);
            s.append(parameterType);
            splitter = ", ";
        }

        s.append(')');
        return s.append(';').toString();
    }
}