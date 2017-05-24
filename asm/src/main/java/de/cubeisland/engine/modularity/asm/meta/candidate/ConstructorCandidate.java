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

import java.util.List;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

import static de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate.simpleName;

/**
 * Represents a Constructor potentially containing DependencyInformation
 */
public class ConstructorCandidate extends MethodCandidate
{
    public ConstructorCandidate(TypeReference declaringClass, String name, int modifiers, List<TypeReference> parameterTypes)
    {
        super(declaringClass, name, modifiers, new TypeReference("void"), parameterTypes);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        s.append(stringModifiers(getModifiers()));
        s.append(simpleName(getDeclaringClass().getReferencedClass()));
        s.append('(');

        String splitter = "";
        for (final TypeReference parameterType : getParameterTypes())
        {
            s.append(splitter);
            s.append(parameterType);
            splitter = ", ";
        }

        s.append(')');
        return s.append(';').toString();
    }
}
