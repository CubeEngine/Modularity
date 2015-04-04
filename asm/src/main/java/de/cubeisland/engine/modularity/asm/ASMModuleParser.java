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
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import de.cubeisland.engine.modularity.asm.annotation.BaseAnnotation;
import de.cubeisland.engine.modularity.asm.annotation.ChildAnnotation;
import de.cubeisland.engine.modularity.asm.annotation.ClassAnnotation;
import de.cubeisland.engine.modularity.asm.annotation.FieldAnnotation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

public class ASMModuleParser
{
    private Type type;
    private int classVersion;
    private Type superType;
    private final Set<BaseAnnotation> annotations = new HashSet<BaseAnnotation>();
    private final Stack<BaseAnnotation> annotationStack = new Stack<BaseAnnotation>();

    public ASMModuleParser(ClassReader reader)
    {
        reader.accept(new ModuleClassVisitor(this), 0);
    }

    public void beginNewTypeName(String name, int version, String superName)
    {
        this.type = Type.getObjectType(name);
        this.classVersion = version;
        this.superType = (superName == null || superName.isEmpty()) ? null : Type.getObjectType(superName);
    }

    public void startClassAnnotation(String name)
    {
        BaseAnnotation annotation = new ClassAnnotation(Type.getType(name), this.type.getClassName());
        this.annotations.add(annotation);
        push(annotation);
    }

    public void startFieldAnnotation(String name, String desc, String annotationName)
    {
        final FieldAnnotation annotation = new FieldAnnotation(Type.getType(annotationName), Type.getType(desc), name);
        annotations.add(annotation);
        push(annotation);
    }

    private BaseAnnotation getLastAnnotation()
    {
        return annotationStack.peek();
    }

    public void addAnnotationProperty(String name, Object value)
    {
        getLastAnnotation().getData().addProperty(name, value);
        push(getLastAnnotation());
    }

    public void addAnnotationArray(String name)
    {
        getLastAnnotation().getData().addProperty(name, new ArrayList<Object>());
        push(getLastAnnotation());
    }

    public void addSubAnnotation(String name, String desc)
    {
        BaseAnnotation base = getLastAnnotation();
        ChildAnnotation child = new ChildAnnotation(Type.getType(desc), base);
        base.getData().addProperties(name, child.getData());
        push(child);
    }

    private void push(BaseAnnotation a)
    {
        annotationStack.push(a);
    }

    private BaseAnnotation pop()
    {
        assert !annotationStack.empty(): "Trying to pop empty stack!";

        return annotationStack.pop();
    }

    public void end()
    {
        annotations.add(pop());
    }

    public Set<BaseAnnotation> getAnnotations()
    {
        return annotations;
    }
}
