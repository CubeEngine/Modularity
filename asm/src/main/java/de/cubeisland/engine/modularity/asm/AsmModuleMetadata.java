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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;
import de.cubeisland.engine.modularity.asm.meta.candidate.AnnotationCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.ClassCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;

/**
 * ModuleMetadata from Asm
 */
public class AsmModuleMetadata extends AsmDependencyInformation implements ModuleMetadata
{
    private final String name;
    private final String description;
    private final Set<String> loadAfter;
    private final Set<String> authors = null; // TODO from maven?

    public AsmModuleMetadata(ClassCandidate candidate)
    {
        super(candidate, candidate.getConstructors());
        AnnotationCandidate moduleInfo = candidate.getAnnotation(ModuleInfo.class);
        this.name = moduleInfo.property("name");
        this.description = moduleInfo.property("description");
        List<String> loadAfter = moduleInfo.property("loadAfter");
        this.loadAfter = new LinkedHashSet<String>(loadAfter);
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Set<String> getAuthors()
    {
        return authors;
    }

    @Override
    public Set<String> loadAfter()
    {
        return loadAfter;
    }

    @Override
    public String getActualClass()
    {
        return getClassName();
    }
}
