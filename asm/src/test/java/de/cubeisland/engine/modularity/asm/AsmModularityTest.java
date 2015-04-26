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
import de.cubeisland.engine.modularity.asm.info.BasicModule;
import de.cubeisland.engine.modularity.core.Modularity;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsmModularityTest
{

    public static final File TEST_JAR = new File("target/test-classes/test.jar");

    @BeforeClass
    public static void setup() throws IOException
    {
        String BASE_PATH = ASMModuleInfoParserTest.getPath(BasicModule.class);

        JarOutputStream out = new JarOutputStream(new FileOutputStream(TEST_JAR));
        out.putNextEntry(new JarEntry("de/cubeisland/engine/modularity/asm/info/"));
        for (File file : new File(BASE_PATH).listFiles())
        {
            out.putNextEntry(new JarEntry("de/cubeisland/engine/modularity/asm/info/" + file.getName()));

            RandomAccessFile f = new RandomAccessFile(file, "r");
            byte[] b = new byte[(int)f.length()];
            f.read(b);
            out.write(b);
            out.closeEntry();
        }
        out.putNextEntry(new JarEntry("META-INF/"));

        out.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
        RandomAccessFile f = new RandomAccessFile("src/test/resources/MANIFEST.MF", "r");
        byte[] b = new byte[(int)f.length()];
        f.read(b);
        out.write(b);
        out.closeEntry();

        out.close();
    }

    @Test
    public void testModularity()
    {
        Modularity modularity = new AsmModularity().load(new File("target/test-classes/"));

    }
}