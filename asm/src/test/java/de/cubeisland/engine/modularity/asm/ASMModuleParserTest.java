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
import java.util.Stack;
import de.cubeisland.engine.modularity.asm.annotation.BaseAnnotation;
import de.cubeisland.engine.modularity.asm.annotation.FieldAnnotation;
import de.cubeisland.engine.modularity.asm.marker.InjectedService;
import de.cubeisland.engine.modularity.asm.marker.Module;
import de.cubeisland.engine.modularity.asm.marker.Service;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import static java.io.File.separatorChar;
import static org.junit.Assert.*;

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
    public void testModule() throws Exception
    {
        final ASMModuleParser parser = new ASMModuleParser(classReaderFor("SuchTestingModule.class"));
        assertTrue(ASMModuleParserTest.<Stack>getValue(parser, "annotationStack").isEmpty());

        int i = 0;
        for (final BaseAnnotation annotation : parser.getAnnotations())
        {
            if (annotation.getType().getClassName().equals(Module.class.getName()))
            {
                i++;
                assertEquals("wow", annotation.getData().getProperties().get("value"));
            }
            else if (annotation.getType().getClassName().equals(InjectedService.class.getName()) && annotation instanceof FieldAnnotation)
            {
                i++;
                assertEquals(MuchService.class.getName(), ((FieldAnnotation)annotation).getFieldType().getClassName());
            }
        }
        assertEquals("Not all tested annotations were found", 3, i);
    }

    @Test
    public void testService() throws Exception
    {
        final ASMModuleParser parser = new ASMModuleParser(classReaderFor("MuchService.class"));
        assertTrue(ASMModuleParserTest.<Stack>getValue(parser, "annotationStack").isEmpty());

        int i = 0;
        for (final BaseAnnotation annotation : parser.getAnnotations())
        {
            if (annotation.getType().getClassName().equals(Service.class.getName()))
            {
                i++;
            }
        }
        assertEquals("Not all tested annotations were found", 1, i);
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
