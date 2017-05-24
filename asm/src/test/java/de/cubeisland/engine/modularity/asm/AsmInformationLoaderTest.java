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

import java.io.File;
import java.io.IOException;
import java.util.Set;
import de.cubeisland.engine.modularity.asm.info.module1.BasicService;
import de.cubeisland.engine.modularity.asm.info.module1.BasicModule;
import de.cubeisland.engine.modularity.core.BasicModularity;
import de.cubeisland.engine.modularity.core.graph.BasicDependency;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import org.junit.BeforeClass;
import org.junit.Test;

import static de.cubeisland.engine.modularity.asm.ASMModuleInfoParserTest.getPath;
import static de.cubeisland.engine.modularity.asm.AsmInformationLoader.newModularity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsmInformationLoaderTest
{

    @BeforeClass
    public static void setup() throws IOException
    {
        AsmModularityTest.setup();
    }

    @Test
    public void testJar1()
    {
        Set<DependencyInformation> infos = newModularity(new BasicModularity()).getLoader().loadInformation(new File("target/test-classes/module1.jar"));
        assertEquals(3, infos.size());
    }

    @Test
    public void testJar2()
    {
        Set<DependencyInformation> infos = newModularity(new BasicModularity()).getLoader().loadInformation(new File("target/test-classes/module2.jar"));
        assertEquals(6, infos.size());
    }


    @Test
    public void testFolder()
    {
        Set<DependencyInformation> infos = newModularity(new BasicModularity()).getLoader().loadInformation(new File(getPath(BasicModule.class)));

        for (DependencyInformation info : infos)
        {
            if (info.getClassName().equals(BasicModule.class.getName()))
            {
                // SuchTestingModule:
                assertTrue(info instanceof ModuleMetadata);
                assertEquals("basic", ((ModuleMetadata)info).getName());
                assertTrue(info.optionalDependencies().contains(new BasicDependency(BasicService.class.getName(),"1")));
            }
        }
    }
}
