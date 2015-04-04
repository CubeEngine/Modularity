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

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import static de.cubeisland.engine.modularity.asm.ASMModuleParser.AnnotationType.CLASS;
import static de.cubeisland.engine.modularity.asm.ASMModuleParser.AnnotationType.FIELD;

public class ASMModuleParser
{
    private Type type;
    private int classVersion;
    private Type superType;
    private final Set<ModuleAnnotation> annotations = new HashSet<ModuleAnnotation>();

    private final Stack<ModuleAnnotation> annotationStack = new Stack<ModuleAnnotation>();




    static enum AnnotationType
    {
        CLASS,
        FIELD,
        METHOD,
        SUBTYPE
    }

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
        annotationStack.clear();
        ModuleAnnotation annotation = new ModuleAnnotation(Type.getType(name), CLASS, this.type.getClassName());
        this.annotations.add(annotation);
        annotationStack.push(annotation);
    }

    public void startFieldAnnotation(String name, String annotationName)
    {
        annotationStack.clear();
        ModuleAnnotation annotation = new ModuleAnnotation(Type.getType(annotationName), FIELD, name);
        annotations.add(annotation);
        annotationStack.push(annotation);
    }

    private ModuleAnnotation getLastAnnotation()
    {
        return annotationStack.peek();
    }

    public void addAnnotationProperty(String name, Object value)
    {
        getLastAnnotation().addProperty(name, value);
    }

    public void addAnnotationArray(String name)
    {
        getLastAnnotation().addArray(name);
    }

    public void addSubAnnotation(String name, String desc)
    {
        ModuleAnnotation annotation = getLastAnnotation().addChildAnnotation(name, desc);
        annotations.add(annotation);
        annotationStack.push(annotation);
    }

    public void endArray()
    {
        getLastAnnotation().endArray();
    }

    public void endSubAnnotation()
    {
        annotationStack.pop();
    }
}
