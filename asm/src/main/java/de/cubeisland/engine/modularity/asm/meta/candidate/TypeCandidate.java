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
import de.cubeisland.engine.modularity.core.ModularityClassLoader;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

/**
 * The Base for Class or Interface Candidates
 */
public abstract class TypeCandidate extends Candidate
{
    private final Set<TypeReference> interfaces;
    private final Map<String, FieldCandidate> fields = new HashMap<String, FieldCandidate>();
    private final Map<String, MethodCandidate> methods = new HashMap<String, MethodCandidate>();
    private final File sourceFile;
    private final int modifiers;
    private String sourceVersion = "unknown-unknown";
    private String version = "unknown";

    private ModularityClassLoader classLoader;

    public TypeCandidate(File sourceFile, String name, int modifiers, Set<TypeReference> interfaces)
    {
        super(name);
        this.sourceFile = sourceFile;
        this.modifiers = modifiers;
        this.interfaces = unmodifiableSet(interfaces);
    }

    /**
     * Returns the source-version of this candidate
     *
     * @return the source-version
     */
    public String getSourceVersion()
    {
        return sourceVersion;
    }

    /**
     * Returns the version of this candidate
     *
     * @return the version
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Sets the source-version of this candidate
     *
     * @param sourceVersion the source-version to set
     */
    public void setSourceVersion(String sourceVersion)
    {
        this.sourceVersion = sourceVersion;
    }

    /**
     * Sets the version of this candidate
     *
     * @param version the version to set
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * Returns the source-file of this candidate.
     * This may be a class or jar file
     *
     * @return the source-file
     */
    public File getSourceFile()
    {
        return sourceFile;
    }

    /**
     * Returns the simple name of the type of this candidate
     *
     * @return the simple name
     */
    public String getSimpleName()
    {
        return simpleName(getName());
    }

    /**
     * Gets the simple name of a class name
     *
     * @param name the class name
     * @return the simple name
     */
    public static String simpleName(String name)
    {
        String[] parts = name.split("\\.");
        return parts[parts.length - 1];
    }

    /**
     * Returns whether this candidate has given interface
     *
     * @param interfaze the interface class to check
     * @return true if this candidate has given interface
     */
    public boolean hasInterface(Class interfaze)
    {
        return hasInterface(interfaze.getName());
    }

    /**
     * Returns whether this candidate has given interface
     *
     * @param interfaze the interface name to check
     * @return true if this candidate has given interface
     */
    public boolean hasInterface(String interfaze)
    {
        for (final TypeReference anInterface : this.interfaces)
        {
            if (anInterface.getReferencedClass().equals(interfaze))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a FieldCandidate to this Candidate
     *
     * @param candidate the candidate to add
     */
    public void addField(FieldCandidate candidate)
    {
        this.fields.put(candidate.getName(), candidate);
    }

    /**
     * Returns the FieldCandidate for given name
     *
     * @param name the name
     * @return the FieldCandidate
     */
    public FieldCandidate getField(String name)
    {
        return fields.get(name);
    }

    /**
     * Adds given MethodCandidate to this candidate
     *
     * @param candidate the candidate to add
     */
    public void addMethod(MethodCandidate candidate)
    {
        this.methods.put(candidate.getName(), candidate);
    }

    /**
     * Returns the MethodCandidate for given name
     *
     * @param name the name
     * @return the MethodCandidate
     */
    public MethodCandidate getMethod(String name)
    {
        return methods.get(name);
    }

    /**
     * Returns a new TypeReference to the Type represented by this candidate
     *
     * @return the TypeReference
     */
    public TypeReference newReference()
    {
        return new TypeReference(getName());
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
     * Returns the ClassLoader for this candidate
     *
     * @return the ClassLoader
     */
    public ModularityClassLoader getClassLoader()
    {
        return classLoader;
    }

    /**
     * Sets the ClassLoader for this candidate
     *
     * @param classLoader the ClassLoader
     */
    public void setClassLoader(ModularityClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof TypeCandidate))
        {
            return false;
        }

        final TypeCandidate that = (TypeCandidate) o;

        if (modifiers != that.modifiers)
        {
            return false;
        }
        if (!fields.equals(that.fields))
        {
            return false;
        }
        if (!interfaces.equals(that.interfaces))
        {
            return false;
        }
        if (!methods.equals(that.methods))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = interfaces.hashCode();
        result = 31 * result + fields.hashCode();
        result = 31 * result + methods.hashCode();
        result = 31 * result + modifiers;
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (AnnotationCandidate candidate : getAnnotations())
        {
            s.append(candidate).append("\n");
        }
        s.append(stringModifiers(modifiers));
        s.append(typeName()).append(' ');
        s.append(getName());
        if (this instanceof ClassCandidate)
        {
            final ClassCandidate clazz = (ClassCandidate) this;
            final String extendingClass = clazz.getExtendedClass().getReferencedClass();
            if (!Object.class.getName().equals(extendingClass))
            {
                s.append(" extends ").append(clazz.getExtendedClass());
            }
            Set<TypeReference> interfaces = getImplementedInterfaces();
            if (!interfaces.isEmpty())
            {
                s.append(" implements ");
                String splitter = "";
                for (TypeReference reference : interfaces)
                {
                    s.append(splitter);
                    s.append(reference);
                    splitter = ", ";
                }
            }
        }
        else
        {
            Set<TypeReference> interfaces = getImplementedInterfaces();
            if (!interfaces.isEmpty())
            {
                s.append(" extends ");
                String splitter = "";
                for (TypeReference reference : interfaces)
                {
                    s.append(splitter);
                    s.append(reference);
                    splitter = ", ";
                }
            }
        }
        s.append(" {");

        for (FieldCandidate field : getFields())
        {
            s.append("\n\t").append(field);
        }
        if (this instanceof ClassCandidate)
        {
            for (final ConstructorCandidate constructor : ((ClassCandidate) this).getConstructors())
            {
                s.append("\n\t").append(constructor);
            }
        }
        for (MethodCandidate method : getMethods())
        {
            s.append("\n\t").append(method);
        }

        return s.append("\n}").toString();
    }

    protected abstract String typeName();

    /**
     * Returns the TypeReferences to implemented interfaces
     *
     * @return the TypeReferences to implemented interfaces
     */
    public Set<TypeReference> getImplementedInterfaces()
    {
        return interfaces;
    }

    /**
     * Returns the FieldCandidates of this candidate
     *
     * @return the FieldCandidates
     */
    public Set<FieldCandidate> getFields()
    {
        return new HashSet<FieldCandidate>(fields.values());
    }

    /**
     * Returns the MethodCandidates of this candidate
     *
     * @return the MethodCandidates
     */
    public Set<MethodCandidate> getMethods()
    {
        return new HashSet<MethodCandidate>(methods.values());
    }
}
