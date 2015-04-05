package de.cubeisland.engine.modularity.asm.meta.candidate;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

public abstract class Candidate
{
    private final String name;
    private final Set<AnnotationCandidate> annotations = new HashSet<AnnotationCandidate>();

    public Candidate(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void addAnnotation(AnnotationCandidate candidate)
    {
        this.annotations.add(candidate);
    }

    public Set<AnnotationCandidate> getAnnotations()
    {
        return unmodifiableSet(annotations);
    }

    public boolean isAnnotatedWith(Class clazz)
    {
        return isAnnotatedWith(clazz.getName());
    }

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
