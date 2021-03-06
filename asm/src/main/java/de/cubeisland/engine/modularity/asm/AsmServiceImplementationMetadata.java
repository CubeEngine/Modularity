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
package de.cubeisland.engine.modularity.asm;

import de.cubeisland.engine.modularity.asm.marker.ServiceImpl;
import de.cubeisland.engine.modularity.asm.marker.Version;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;
import de.cubeisland.engine.modularity.asm.meta.candidate.ClassCandidate;
import de.cubeisland.engine.modularity.core.graph.meta.ServiceImplementationMetadata;
import org.objectweb.asm.Type;

/**
 * ServiceImplementationMetadata from Asm
 */
public class AsmServiceImplementationMetadata extends AsmDependencyInformation implements ServiceImplementationMetadata
{
    private final String serviceName;

    public AsmServiceImplementationMetadata(ClassCandidate candidate)
    {
        super(candidate, candidate.getConstructors());

        Type type = candidate.getAnnotation(ServiceImpl.class).property("value");
        this.serviceName = type.getClassName();
        addRequiredDependency(new TypeReference(serviceName), candidate.getAnnotation(Version.class));
        ensureIsImplemented(candidate);
    }

    private void ensureIsImplemented(ClassCandidate candidate)
    {
        boolean implemented = false;
        for (TypeReference ref : candidate.getImplementedInterfaces())
        {
            if (ref.getReferencedClass().equals(serviceName))
            {
                implemented = true;
            }
        }
        if (!implemented)
        {
            throw new IllegalArgumentException("Class Candidate " + candidate.getName() + " declares implementing a Service but is not!"); // TODO check further up
        }
    }

    @Override
    public String getActualClass()
    {
        return serviceName;
    }
}
