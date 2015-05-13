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
package de.cubeisland.engine.modularity.asm.visitor;

import java.util.ArrayList;
import java.util.List;
import de.cubeisland.engine.modularity.asm.meta.EnumHolder;
import de.cubeisland.engine.modularity.asm.meta.candidate.AnnotationCandidate;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Visits Annotations and creates AnnotationCandidates
 */
public class ModuleAnnotationVisitor extends AnnotationVisitor
{
    private final AnnotationCandidate candidate;

    public ModuleAnnotationVisitor(AnnotationCandidate candidate)
    {
        super(Opcodes.ASM5);
        this.candidate = candidate;
    }

    @Override
    public void visit(String name, Object value)
    {
        this.candidate.addProperty(name, value);
    }

    @Override
    public void visitEnum(String name, String desc, String value)
    {
        this.candidate.addProperty(name, new EnumHolder(desc, value));
    }

    @Override
    public AnnotationVisitor visitArray(String name)
    {
        ArrayList<Object> list = new ArrayList<Object>();
        candidate.addProperty(name, list);
        return new AnnotationArrayVisitor(list);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc)
    {
        AnnotationCandidate value = new AnnotationCandidate(ModuleClassVisitor.refForType(desc));
        candidate.addProperty(name, value);
        return new ModuleAnnotationVisitor(value);
    }

    private class AnnotationArrayVisitor extends AnnotationVisitor
    {
        private final List<Object> list;

        public AnnotationArrayVisitor(List<Object> list)
        {
            super(Opcodes.ASM5);
            this.list = list;
        }

        @Override
        public void visit(String name, Object value)
        {
            list.add(value);
        }

        @Override
        public void visitEnum(String name, String desc, String value)
        {
            list.add(new EnumHolder(desc, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String desc)
        {
            AnnotationCandidate candidate = new AnnotationCandidate(ModuleClassVisitor.refForType(desc));
            list.add(candidate);
            return new ModuleAnnotationVisitor(candidate);
        }

        @Override
        public AnnotationVisitor visitArray(String name)
        {
            ArrayList<Object> list = new ArrayList<Object>();
            this.list.add(list);
            return new AnnotationArrayVisitor(list);
        }
    }
}
