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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import org.junit.Assert;
import org.junit.Test;

import static de.cubeisland.engine.modularity.asm.ASMModuleInfoParserTest.getPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsmInformationLoaderTest
{
    @Test
    public void testJar()
    {
        Set<DependencyInformation> infos = new AsmInformationLoader().loadInformation(new File("src/test/resources/test.jar"));
        assertEquals(1, infos.size());
        for (DependencyInformation info : infos)
        {
            assertEquals("branch-somehashvalue", info.getSourceVersion());
        }
    }

    @Test
    public void testFolder()
    {
        Set<DependencyInformation> infos = new AsmInformationLoader().loadInformation(new File(getPath()));

        for (DependencyInformation info : infos)
        {
            if (info.getIdentifier().equals(SuchTestingModule.class.getName()))
            {
                // SuchTestingModule:
                assertTrue(info instanceof ModuleMetadata);
                assertEquals("wow", ((ModuleMetadata)info).getName());
                Iterator<String> actual = ((ModuleMetadata)info).loadAfter().iterator();
                for (final String expected : Arrays.asList("no.thing", "a.lot"))
                {
                    assertEquals(expected, actual.next());
                }
                assertTrue(info.requiredDependencies().contains(SuchTestingModule.class.getName()));
                assertTrue(info.optionalDependencies().contains(MuchService.class.getName()));
            }

        }
    }
}
