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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.Disable;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.Version;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;
import de.cubeisland.engine.modularity.asm.meta.candidate.AnnotationCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.ConstructorCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.FieldCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.MethodCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate;
import de.cubeisland.engine.modularity.core.Maybe;
import de.cubeisland.engine.modularity.core.ModularityClassLoader;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;

/**
 * The base for DependencyInformation from Asm
 */
public abstract class AsmDependencyInformation implements DependencyInformation
{
    private final String identifier;
    private final String version;
    private final String sourceVersion;
    private final ModularityClassLoader classLoader;
    private final Set<String> requiredDependencies = new HashSet<String>();
    private final Set<String> optionalDependencies = new HashSet<String>();
    private String enableMethod;
    private String disableMethod;

    public AsmDependencyInformation(TypeCandidate candidate, Set<ConstructorCandidate> constructors)
    {
        this.identifier = candidate.getName();
        this.version = candidate.getVersion();
        this.sourceVersion = candidate.getSourceVersion();
        this.classLoader = candidate.getClassLoader();

        // Search dependencies:
        for (FieldCandidate field : candidate.getFields())
        {
            if (field.isAnnotatedWith(Inject.class))
            {
                if (Maybe.class.getName().equals(field.getType().getReferencedClass()))
                {
                    addOptionaldDependency(field.getType().getGenericType(), field.getAnnotation(Version.class));
                }
                else
                {
                    addRequiredDependency(field.getType(), field.getAnnotation(Version.class));
                }
            }
        }

        for (ConstructorCandidate constructor : constructors)
        {
            if (constructor.isAnnotatedWith(Inject.class))
            {
                for (TypeReference reference : constructor.getParameterTypes())
                {
                    addRequiredDependency(reference, null); // TODO version
                    // TODO optional
                }
            }
        }

        // find enable and disable methods
        for (MethodCandidate method : candidate.getMethods())
        {
            if (method.isAnnotatedWith(Enable.class))
            {
                this.enableMethod = method.getName();
            }
            if (method.isAnnotatedWith(Disable.class))
            {
                this.disableMethod = method.getName();
            }
        }
    }

    void addOptionaldDependency(TypeReference type, AnnotationCandidate version)
    {
        optionalDependencies.add(getIdentifier(type, version));
    }

    private String getIdentifier(TypeReference type, AnnotationCandidate version)
    {
        String identifier = type.getReferencedClass();
        if (version != null)
        {
            identifier += ":" + version.property("value").toString();
        }
        return identifier;
    }

    void addRequiredDependency(TypeReference type, AnnotationCandidate version)
    {
        requiredDependencies.add(getIdentifier(type, version));
    }

    @Override
    public String getIdentifier()
    {
        return identifier + ":" + getVersion();
    }

    @Override
    public String getClassName()
    {
        return identifier;
    }

    @Override
    public String getSourceVersion()
    {
        return sourceVersion;
    }

    @Override
    public String getVersion()
    {
        return version;
    }


    @Override
    public Set<String> requiredDependencies()
    {
        return Collections.unmodifiableSet(requiredDependencies);
    }

    @Override
    public Set<String> optionalDependencies()
    {
        return Collections.unmodifiableSet(optionalDependencies);
    }

    @Override
    public ModularityClassLoader getClassLoader()
    {
        return classLoader;
    }

    @Override
    public String getEnableMethod()
    {
        return this.enableMethod;
    }

    @Override
    public String getDisableMethod()
    {
        return this.disableMethod;
    }
}
