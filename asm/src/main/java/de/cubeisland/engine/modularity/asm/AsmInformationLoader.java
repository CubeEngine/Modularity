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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import de.cubeisland.engine.modularity.asm.meta.candidate.ClassCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate;
import de.cubeisland.engine.modularity.core.InformationLoader;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import org.objectweb.asm.ClassReader;

public class AsmInformationLoader implements InformationLoader
{
    @Override
    public Set<DependencyInformation> loadInformation(File file)
    {
        try
        {
            Set<TypeCandidate> candidates = new HashSet<TypeCandidate>();
            for (InputStream stream : getStreams(file))
            {
                ModuleClassVisitor classVisitor = new ModuleClassVisitor();
                new ClassReader(stream).accept(classVisitor, 0);
                TypeCandidate candidate = classVisitor.getCandidate();
                if (candidate != null)
                {
                    candidates.add(candidate);
                }
            }
            return Collections.emptySet();
        }
        catch (IOException e)
        {
            return Collections.emptySet();
        }
    }

    private List<InputStream> getStreams(File file) throws FileNotFoundException
    {
        List<InputStream> list = new ArrayList<InputStream>();
        if (file.getName().endsWith(".class"))
        {
            list.add(new FileInputStream(file));
            return list;
        }
        // TODO read JAR-File
        return list;
    }
}
