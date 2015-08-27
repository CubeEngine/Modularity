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
package de.cubeisland.engine.modularity.asm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.Version;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;
import de.cubeisland.engine.modularity.asm.meta.candidate.AnnotationCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.ConstructorCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.FieldCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.MethodCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate;
import de.cubeisland.engine.modularity.core.ConstructorInjection;
import de.cubeisland.engine.modularity.core.FieldsInjection;
import de.cubeisland.engine.modularity.core.Maybe;
import de.cubeisland.engine.modularity.core.ModularityClassLoader;
import de.cubeisland.engine.modularity.core.graph.BasicDependency;
import de.cubeisland.engine.modularity.core.graph.Dependency;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;

/**
 * The base for DependencyInformation from Asm
 */
public abstract class AsmDependencyInformation implements DependencyInformation
{
    private final Dependency identifier;
    private final String sourceVersion;
    private final ModularityClassLoader classLoader;
    private final Set<Dependency> requiredDependencies = new HashSet<Dependency>();
    private final Set<Dependency> optionalDependencies = new HashSet<Dependency>();

    public AsmDependencyInformation(TypeCandidate candidate, Set<ConstructorCandidate> constructors)
    {
        identifier = new BasicDependency(candidate.getName(), candidate.getVersion());
        this.sourceVersion = candidate.getSourceVersion();
        this.classLoader = candidate.getClassLoader();

        // Search dependencies:
        ConstructorCandidate constructor = findConstructor(candidate, constructors);
        if (constructor != null)
        {
            List<Dependency> list = new ArrayList<Dependency>();
            for (TypeReference reference : constructor.getParameterTypes())
            {
                boolean optional = reference.getReferencedClass().equals(Maybe.class.getName());
                reference = optional ? reference.getGenericType() : reference;
                list.add(getIdentifier(reference, null, !optional));
            }
            new ConstructorInjection(identifier, list); // TODO save injectionpoints
        }


        List<Dependency> fieldDeps = new ArrayList<Dependency>();
        List<String> fields = new ArrayList<String>();
        for (FieldCandidate field : candidate.getFields())
        {
            if (field.isAnnotatedWith(Inject.class))
            {
                boolean optional = Maybe.class.getName().equals(field.getType().getReferencedClass());
                TypeReference reference = optional ? field.getType().getGenericType() : field.getType();
                fieldDeps.add(getIdentifier(reference, field.getAnnotation(Version.class), !optional));
                fields.add(field.getName());
            }
        }
        if (!fieldDeps.isEmpty())
        {
            new FieldsInjection(identifier, fieldDeps, fields); // TODO save injectionpoint
        }

        for (MethodCandidate method : candidate.getMethods())
        {
            if (method.isAnnotatedWith(Inject.class))
            {
                for (TypeReference reference : method.getParameterTypes())
                {
                    // TODO version
                    if (reference.getReferencedClass().equals(Maybe.class.getName()))
                    {
                        addOptionaldDependency(reference.getGenericType(), null);
                    }
                    else
                    {
                        addRequiredDependency(reference, null);
                    }
                }
            }
        }
    }

    private ConstructorCandidate findConstructor(TypeCandidate candidate, Set<ConstructorCandidate> constructors)
    {
        ConstructorCandidate constructorCandidate = null;
        for (ConstructorCandidate constructor : constructors)
        {
            if (constructor.isAnnotatedWith(Inject.class))
            {
                if (constructorCandidate != null)
                {
                    throw new IllegalStateException("Multiple Injection Constructors found in " + candidate.getSimpleName());
                }
                constructorCandidate = constructor;
            }
        }
        return constructorCandidate;
    }

    void addOptionaldDependency(TypeReference type, AnnotationCandidate version)
    {
        optionalDependencies.add(getIdentifier(type, version, true));
    }

    private Dependency getIdentifier(TypeReference type, AnnotationCandidate version, boolean required)
    {
        return new BasicDependency(type.getReferencedClass(), version != null ? version.property("value").toString() : null, required);
    }

    void addRequiredDependency(TypeReference type, AnnotationCandidate version)
    {
        requiredDependencies.add(getIdentifier(type, version, true));
    }

    @Override
    public Dependency getIdentifier()
    {
        return identifier;
    }

    @Override
    public String getClassName()
    {
        return identifier.name();
    }

    @Override
    public String getSourceVersion()
    {
        return sourceVersion;
    }

    @Override
    public String getVersion()
    {
        return identifier.version();
    }


    @Override
    public Set<Dependency> requiredDependencies()
    {
        return Collections.unmodifiableSet(requiredDependencies);
    }

    @Override
    public Set<Dependency> optionalDependencies()
    {
        return Collections.unmodifiableSet(optionalDependencies);
    }

    @Override
    public ModularityClassLoader getClassLoader()
    {
        return classLoader;
    }
}
