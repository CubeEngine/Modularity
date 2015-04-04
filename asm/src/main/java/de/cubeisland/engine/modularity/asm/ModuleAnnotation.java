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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.cubeisland.engine.modularity.asm.ASMModuleParser.AnnotationType;
import org.objectweb.asm.Type;

public class ModuleAnnotation
{

    private final Type annotationClass;
    private final AnnotationType type;
    private final String member;

    private String arrayName;
    private List<Object> arrayList;
    private Map<String, Object> values = new HashMap<String, Object>();

    public ModuleAnnotation(Type annotationClass, AnnotationType type, String member)
    {
        this.annotationClass = annotationClass;
        this.type = type;
        this.member = member;
    }

    public ModuleAnnotation(Type type, AnnotationType subtype, ModuleAnnotation parent)
    {
        this(type, subtype, parent.type.getDeclaringClass().getName());
    }

    public void addProperty(String name, Object value)
    {
        if (arrayList == null)
        {
            values.put(name, value);
        }
        else
        {
            arrayList.add(value);
        }
    }

    public void addArray(String name)
    {
        arrayList = new ArrayList<Object>();
        arrayName = name;
    }

    public ModuleAnnotation addChildAnnotation(String name, String desc)
    {
        ModuleAnnotation child = new ModuleAnnotation(Type.getType(desc), AnnotationType.SUBTYPE, this);
        if (arrayList != null)
        {
            arrayList.add(child.values);
        }
        return child;
    }

    public void endArray()
    {

        values.put(arrayName, arrayList);
        arrayList = null;
    }
}
