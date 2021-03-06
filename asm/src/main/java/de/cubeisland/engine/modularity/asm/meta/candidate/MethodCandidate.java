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

import java.util.List;

/**
 * Represents a Method potentially containing DependencyInformation
 */
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
     * Returns the return type of the method as TypeReference
     *
     * @return the return type as TypeReference
     */
    public TypeReference getReturnType()
    {
        return returnType;
    }

    /**
     * Returns the parameter types of the method as TypeReferences
     *
     * @return the parameter types as TypeReferences
     */
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

        if (!getName().equals(((MethodCandidate)o).getName()))
        {
            return false;
        }

        if (modifiers != that.modifiers)
        {
            return false;
        }
        if (declaringClass != null ? !declaringClass.equals(that.declaringClass) : that.declaringClass != null)
        {
            return false;
        }
        if (returnType != null ? !returnType.equals(that.returnType) : that.returnType != null)
        {
            return false;
        }
        return !(parameterTypes != null ? !parameterTypes.equals(that.parameterTypes) : that.parameterTypes != null);
    }

    @Override
    public int hashCode()
    {
        int result = declaringClass != null ? declaringClass.hashCode() : 0;
        result = 31 * result + modifiers;
        result = 31 * result + getName().hashCode();
        result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
        result = 31 * result + (parameterTypes != null ? parameterTypes.hashCode() : 0);
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
