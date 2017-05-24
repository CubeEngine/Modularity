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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

import static java.util.Collections.unmodifiableSet;

/**
 * Represents a Class potentially containing DependencyInformation
 */
public class ClassCandidate extends TypeCandidate
{
    private final TypeReference extendedClass;
    private final Set<ConstructorCandidate> constructors = new HashSet<ConstructorCandidate>();

    public ClassCandidate(File sourceFile, String name, int modifiers, Set<TypeReference> interfaces, TypeReference extendedClass)
    {
        super(sourceFile, name, modifiers, interfaces);
        this.extendedClass = extendedClass;
    }

    /**
     * Returns the superclass of this class
     * @return
     */
    public TypeReference getExtendedClass()
    {
        return extendedClass;
    }

    @Override
    protected String typeName()
    {
        return "class";
    }

    /**
     * Adds a Constructor to this class
     * @param constructor the constructor to add
     */
    public void addConstructor(ConstructorCandidate constructor)
    {
        this.constructors.add(constructor);
    }

    /**
     * Returns the constructors of this class
     * @return the constructors
     */
    public Set<ConstructorCandidate> getConstructors()
    {
        return unmodifiableSet(constructors);
    }
}
