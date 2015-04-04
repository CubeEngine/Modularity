package de.cubeisland.engine.modularity.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class ModuleClassVisitor extends ClassVisitor
{
    private final ASMModuleParser discoverer;

    public ModuleClassVisitor(ASMModuleParser discoverer)
    {
        super(Opcodes.ASM5);
        this.discoverer = discoverer;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
    {
        this.discoverer.beginNewTypeName(name, version, superName);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, boolean visible)
    {
        discoverer.startClassAnnotation(name);
        return new ModuleAnnotationVisitor(this.discoverer);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
    {
        return new ModuleFieldVisitor(name, discoverer);
    }

    // TODO implement dependency declaration through fields
}

