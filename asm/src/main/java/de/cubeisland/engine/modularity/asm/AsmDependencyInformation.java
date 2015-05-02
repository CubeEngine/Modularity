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
import javax.swing.text.html.Option;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;
import de.cubeisland.engine.modularity.asm.meta.candidate.ConstructorCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.FieldCandidate;
import de.cubeisland.engine.modularity.core.ModularityClassLoader;
import de.cubeisland.engine.modularity.core.Optional;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;

public abstract class AsmDependencyInformation implements DependencyInformation
{
    private final String identifier;
    private final String version;
    private final String sourceVersion;
    private final ModularityClassLoader classLoader;
    private final Set<String> requiredDependencies = new HashSet<String>();
    private final Set<String> optionalDependencies = new HashSet<String>();

    public AsmDependencyInformation(String identifier, String version, String sourceVersion, Set<FieldCandidate> fields,
                                    Set<ConstructorCandidate> constructors, ModularityClassLoader classLoader)
    {
        this.identifier = identifier;
        this.version = version;
        this.sourceVersion = sourceVersion;
        this.classLoader = classLoader;

        // Search dependencies:
        for (FieldCandidate field : fields)
        {
            if (field.isAnnotatedWith(Inject.class))
            {
                if (field.isAnnotatedWith(Optional.class))
                {
                    addOptionaldDependency(field.getType());
                }
                else
                {
                    addRequiredDependency(field.getType());
                }
            }
        }

        for (ConstructorCandidate constructor : constructors)
        {
            if (constructor.isAnnotatedWith(Inject.class))
            {
                for (TypeReference reference : constructor.getParameterTypes())
                {
                    addRequiredDependency(reference);
                }
            }
        }
    }

    void addOptionaldDependency(TypeReference type)
    {
        optionalDependencies.add(type.getReferencedClass());
    }

    void addRequiredDependency(TypeReference type)
    {
        requiredDependencies.add(type.getReferencedClass());
    }

    public String getIdentifier()
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
}
