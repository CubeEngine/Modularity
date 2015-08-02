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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import de.cubeisland.engine.modularity.asm.info.module1.BasicModule;
import de.cubeisland.engine.modularity.asm.info.module2.ComplexModule;
import de.cubeisland.engine.modularity.asm.info.module3.BasicModule2;
import de.cubeisland.engine.modularity.asm.info.module3.ProvidedService;
import de.cubeisland.engine.modularity.asm.info.module3.ProvidedServiceImpl;
import de.cubeisland.engine.modularity.core.Modularity;
import de.cubeisland.engine.modularity.core.graph.Dependency;
import org.junit.BeforeClass;
import org.junit.Test;

import static de.cubeisland.engine.modularity.asm.AsmInformationLoader.newModularity;
import static org.junit.Assert.*;

public class AsmModularityTest
{

    public static final File JAR_TARGET_DIR = new File("target/test-classes/");
    public static final File CLASS_SOURCE_DIR = new File("target/test-classes/de/cubeisland/engine/modularity/asm/info/");

    private static Modularity modularity;

    @BeforeClass
    public static void setup() throws IOException
    {
        for (File dir : CLASS_SOURCE_DIR.listFiles())
        {
            if (dir.isDirectory())
            {
                JarOutputStream out = new JarOutputStream(new FileOutputStream(JAR_TARGET_DIR + "/" + dir.getName() + ".jar"));
                String pack = "de/cubeisland/engine/modularity/asm/info/" + dir.getName() + "/";
                out.putNextEntry(new JarEntry(pack));
                for (File file : dir.listFiles())
                {
                    out.putNextEntry(new JarEntry(pack + file.getName()));

                    RandomAccessFile f = new RandomAccessFile(file, "r");
                    byte[] b = new byte[(int)f.length()];
                    f.read(b);
                    out.write(b);
                    out.closeEntry();
                }
                out.putNextEntry(new JarEntry("META-INF/"));
                out.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
                RandomAccessFile f = new RandomAccessFile("src/test/resources/" + dir.getName() + ".MF", "r");
                byte[] b = new byte[(int)f.length()];
                f.read(b);
                out.write(b);
                out.closeEntry();

                out.close();
            }
        }
        modularity = newModularity();
        modularity.load(new File("target/test-classes/"));
        for (Dependency missing : modularity.getGraph().getUnresolved().keySet())
        {
            System.out.println("Missing dependency: " + missing);
        }

        assertEquals(0, modularity.getGraph().getUnresolved().size());
    }

    @Test
    public void testBasicModule()
    {
        assertNotNull(modularity.provide(BasicModule.class));
    }

    @Test
    public void testComplexModule()
    {
        assertNotNull(modularity.provide(ComplexModule.class));
    }

    @Test
    public void testBasicModule2()
    {
        assertNotNull(modularity.provide(BasicModule2.class)); // Starts Module
        ProvidedService started = modularity.provide(ProvidedService.class);  // Starts Service + Impl
        assertNotNull(started);
        assertEquals(ProvidedServiceImpl.stripper, started.provideString());
        assertNull(modularity.provide(ProvidedServiceImpl.class)); // Returns null. Not allowed to query for implementation
    }

    @Test
    public void testProvidedService()
    {
        modularity.getServiceManager().registerService(String.class, "providedStringService");
        assertEquals("providedStringService", modularity.provide(String.class));
    }
}