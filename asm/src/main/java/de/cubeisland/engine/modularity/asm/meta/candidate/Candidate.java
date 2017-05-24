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

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

/**
 * The base for Candidates representing some kind of DependencyInformation
 */
public abstract class Candidate
{
    private final String name;
    private final Set<AnnotationCandidate> annotations = new HashSet<AnnotationCandidate>();

    public Candidate(String name)
    {
        this.name = name;
    }

    /**
     * Returns the name of the Candidate
     *
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Adds an annotation to this candidate
     *
     * @param candidate the annotation to add
     */
    public void addAnnotation(AnnotationCandidate candidate)
    {
        this.annotations.add(candidate);
    }

    /**
     * Returns the annotations of this candidate
     *
     * @return the annotations
     */
    public Set<AnnotationCandidate> getAnnotations()
    {
        return unmodifiableSet(annotations);
    }

    /**
     * Returns whether the candidate is annotated with an annotation of given class
     *
     * @param clazz the class of the annotation to check
     * @return true if the candidate is annotated with an annotation of given class
     */
    public boolean isAnnotatedWith(Class clazz)
    {
        return isAnnotatedWith(clazz.getName());
    }

    /**
     * Returns whether the candidate is annotated with an annotation of given class
     *
     * @param annotationType the name of the annotations class
     * @return
     */
    public boolean isAnnotatedWith(String annotationType)
    {
        for (final AnnotationCandidate annotation : annotations)
        {
            if (annotation.getName().equals(annotationType))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the annotation for given class or null if not present
     * @param clazz the class of the annotation
     * @return the annotation or null
     */
    public AnnotationCandidate getAnnotation(Class<? extends Annotation> clazz)
    {
        if (this.isAnnotatedWith(clazz))
        {
            for (AnnotationCandidate annotation : annotations)
            {
                if (annotation.getName().equals(clazz.getName()))
                {
                    return annotation;
                }
            }
        }
        return null;
    }

    protected static String stringModifiers(int modifiers)
    {
        StringBuilder mods = new StringBuilder();

        if (Modifier.isPublic(modifiers))
        {
            mods.append("public ");
        }
        if (Modifier.isProtected(modifiers))
        {
            mods.append("protected ");
        }
        if (Modifier.isPrivate(modifiers))
        {
            mods.append("private ");
        }
        if (Modifier.isStatic(modifiers))
        {
            mods.append("static ");
        }
        if (Modifier.isFinal(modifiers))
        {
            mods.append("final ");
        }
        if (Modifier.isAbstract(modifiers))
        {
            mods.append("abstract ");
        }
        if (Modifier.isTransient(modifiers))
        {
            mods.append("transient ");
        }
        if (Modifier.isSynchronized(modifiers))
        {
            mods.append("synchronized ");
        }
        return mods.toString();
    }
}
