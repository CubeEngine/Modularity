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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class ModuleAnnotationVisitor extends AnnotationVisitor
{
    private final ASMModuleParser discoverer;
    private final boolean array;
    private final boolean isSubAnnotation;

    public ModuleAnnotationVisitor(ASMModuleParser discoverer)
    {
        this(discoverer, false, false);
    }

    private ModuleAnnotationVisitor(ASMModuleParser discoverer, boolean array, boolean isSubAnnotation)
    {
        super(Opcodes.ASM5);
        this.discoverer = discoverer;
        this.array = array;
        this.isSubAnnotation = isSubAnnotation;
    }

    @Override
    public void visit(String name, Object value)
    {
        this.discoverer.addAnnotationProperty(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value)
    {
        this.discoverer.addAnnotationProperty(name, new EnumHolder(desc, value));
    }

    @Override
    public AnnotationVisitor visitArray(String name)
    {
        discoverer.addAnnotationArray(name);
        return forArrayAnnotation();
    }

    private AnnotationVisitor forArrayAnnotation()
    {
        return new ModuleAnnotationVisitor(discoverer, true, false);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc)
    {
        discoverer.addSubAnnotation(name, desc);
        return forSubAnnotation();
    }

    private ModuleAnnotationVisitor forSubAnnotation()
    {
        return new ModuleAnnotationVisitor(discoverer, false, true);
    }

    @Override
    public void visitEnd()
    {
        if (array)
        {
            discoverer.endArray();
        }
        if (isSubAnnotation)
        {
            discoverer.endSubAnnotation();
        }
    }
}
