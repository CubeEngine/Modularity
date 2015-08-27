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

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;
import de.cubeisland.engine.modularity.asm.meta.candidate.AnnotationCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.Candidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.ClassCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.ConstructorCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.FieldCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.InterfaceCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.MethodCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate;
import de.cubeisland.engine.modularity.core.Maybe;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import static org.objectweb.asm.Opcodes.*;

/**
 * Visits classes and creates TypeCandidates
 */
public class ModuleClassVisitor extends ClassVisitor
{
    private static Type MAYBE_TYPE = Type.getType(Maybe.class);

    private final File file;
    private TypeCandidate candidate;

    public ModuleClassVisitor(File file)
    {
        super(Opcodes.ASM5);
        this.file = file;
    }

    public TypeCandidate getCandidate()
    {
        return this.candidate;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
    {
        final String typeName = Type.getObjectType(name).getClassName();
        final int modifiers = parseClassModifiers(access);
        final Set<TypeReference> interfaceReferences = refsForTypes(interfaces);

        if (check(access, ACC_INTERFACE))
        {
            candidate = new InterfaceCandidate(file, typeName, modifiers, interfaceReferences);
        }
        else if (check(access, ACC_ANNOTATION))
        {}
        else if (check(access, ACC_ENUM))
        {}
        else
        {
            candidate = new ClassCandidate(file, typeName, modifiers, interfaceReferences, refForObjectType(superName));
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, boolean visible)
    {
        if (candidate == null)
        {
            return super.visitAnnotation(name, visible);
        }
        return visit(candidate, name);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
    {
        if (candidate == null)
        {
            return super.visitField(access, name, desc, signature, value);
        }
        FieldCandidate fieldCandidate = new FieldCandidate(candidate.newReference(), name, parseFieldModifiers(access), refForType(desc), value);
        candidate.addField(fieldCandidate);
        if (signature != null)
        {
            new SignatureReader(signature).acceptType(new ModuleSignatureVisitor(fieldCandidate.getType()));
        }
        return new ModuleFieldVisitor(fieldCandidate);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
        if (candidate == null)
        {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        final TypeReference self = candidate.newReference();
        final int modifiers = parseMethodModifiers(access);
        final TypeReference returnType = refForReturnType(desc);
        final List<TypeReference> params = refsForParams(desc, signature);

        MethodCandidate method;
        if (candidate instanceof ClassCandidate && name.equals("<init>") && returnType.getReferencedClass().equals("void"))
        {
            method = new ConstructorCandidate(self, name, modifiers, params);
            ((ClassCandidate)candidate).addConstructor((ConstructorCandidate)method);
        }
        else
        {
            method = new MethodCandidate(self, name, modifiers, returnType, params);
            candidate.addMethod(method);
        }

        return new ModuleMethodVisitor(method);
    }

    static List<TypeReference> refsForParams(String desc, String signature)
    {
        List<TypeReference> refs = new ArrayList<TypeReference>();

        int index = 0;
        for (final Type type : Type.getArgumentTypes(desc))
        {
            TypeReference ref = new TypeReference(type.getClassName());
            refs.add(ref);
            if (type.equals(MAYBE_TYPE) && signature != null)
            {
                new SignatureReader(signature).accept(new MethodSignatureVisitor(ref, index));
            }
            index++;
        }
        return refs;
    }

    static ModuleAnnotationVisitor visit(Candidate candidate, String name)
    {
        AnnotationCandidate annotation = new AnnotationCandidate(refForType(name));
        candidate.addAnnotation(annotation);
        return new ModuleAnnotationVisitor(annotation);
    }

    static TypeReference refForReturnType(String desc)
    {
        return new TypeReference(Type.getReturnType(desc).getClassName());
    }

    static TypeReference refForType(String name)
    {
        return new TypeReference(Type.getType(name).getClassName());
    }

    static TypeReference refForObjectType(String name)
    {
        return new TypeReference(Type.getObjectType(name).getClassName());
    }

    static Set<TypeReference> refsForTypes(String[] names)
    {
        Set<TypeReference> references = new HashSet<TypeReference>();
        for (final String name : names)
        {
            references.add(refForObjectType(name));
        }
        return references;
    }

    static boolean check(int set, int flag)
    {
        return (set & flag) == flag;
    }

    static int add(int set, int flag, int modifiers, int mod)
    {
        if (check(set, flag))
        {
            return modifiers | mod;
        }
        return modifiers;
    }

    static Integer parseFieldModifiers(int access)
    {
        int m = parseModifiers(access);

        m = add(access, ACC_TRANSIENT, m, Modifier.TRANSIENT);

        return m;
    }

    static int parseClassModifiers(int access)
    {
        int m = parseModifiers(access);

        m = add(access, ACC_ABSTRACT, m, Modifier.ABSTRACT);

        return m;
    }

    static int parseMethodModifiers(int access)
    {
        int m = parseModifiers(access);

        m = add(access, ACC_NATIVE, m, Modifier.NATIVE);
        m = add(access, ACC_ABSTRACT, m, Modifier.ABSTRACT);
        m = add(access, ACC_SYNCHRONIZED, m, Modifier.SYNCHRONIZED);

        return m;
    }

    static int parseModifiers(int access)
    {
        int m = 0;

        // access
        m = add(access, ACC_PRIVATE, m, Modifier.PRIVATE);
        m = add(access, ACC_PROTECTED, m, Modifier.PROTECTED);
        m = add(access, ACC_PUBLIC, m, Modifier.PUBLIC);

        m = add(access, ACC_STATIC, m, Modifier.STATIC);
        m = add(access, ACC_FINAL, m, Modifier.FINAL);

        return m;
    }
}

