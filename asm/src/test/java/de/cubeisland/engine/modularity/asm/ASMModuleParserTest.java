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
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import static java.io.File.separatorChar;
import static java.util.Arrays.asList;

public class ASMModuleParserTest
{
    private static final String TARGET_PATH = "target" + separatorChar + "test-classes";

    @SuppressWarnings("unchecked")
    public static <T> T getValue(Object o, String field) throws Exception
    {
        Field f = o.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return (T)f.get(o);
    }

    @Test
    public void testLoadCandidates() throws Exception
    {
        Set<TypeCandidate> candidates = new HashSet<TypeCandidate>(asList(
            readCandidate(SuchTestingModule.class),
            readCandidate(MuchService.class),
            readCandidate(VeryService.class)
        ));

        for (final TypeCandidate candidate : candidates)
        {
            System.out.println(candidate);
        }
    }

    private TypeCandidate readCandidate(Class clazz) throws IOException
    {
        ModuleClassVisitor v = new ModuleClassVisitor();
        classReaderFor(clazz).accept(v, 0);
        return v.getCandidate();
    }

    private ClassReader classReaderFor(Class clazz) throws IOException
    {
        return classReaderFor(clazz.getSimpleName() + ".class");
    }

    private ClassReader classReaderFor(String file) throws IOException
    {
        return new ClassReader(new FileInputStream(new File(getPath(file))));
    }

    private String getPath(String file)
    {
        return TARGET_PATH + separatorChar + getClass().getPackage().getName().replace('.', separatorChar) + separatorChar + file;
    }
}
