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
import java.lang.reflect.Field;
import java.util.Stack;
import de.cubeisland.engine.modularity.asm.annotation.BaseAnnotation;
import de.cubeisland.engine.modularity.asm.annotation.FieldAnnotation;
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
    public void testAnnotation() throws Exception
    {
        final String path = TARGET_PATH + separatorChar + getClass().getPackage().getName().replace('.', separatorChar) + separatorChar + "SuchTestingModule.class";
        final FileInputStream stream = new FileInputStream(new File(path));
        final ASMModuleParser parser = new ASMModuleParser(new ClassReader(stream));

        assertTrue(ASMModuleParserTest.<Stack>getValue(parser, "annotationStack").isEmpty());

        int i = 0;
        for (final BaseAnnotation annotation : parser.getAnnotations())
        {
            if (annotation.getType().getClassName().equals(TestModule.class.getName()))
            {
                i++;
                assertEquals("wow", annotation.getData().getProperties().get("name"));
            }
            else if (annotation.getType().getClassName().equals(InjectedService.class.getName()) && annotation instanceof FieldAnnotation)
            {
                i++;
                assertEquals(String.class.getName(), ((FieldAnnotation)annotation).getFieldType().getClassName());
            }
        }

        assertEquals("Not all tested annotations were found", 2, i);
    }
}
