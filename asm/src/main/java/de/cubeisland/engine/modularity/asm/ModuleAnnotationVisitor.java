package de.cubeisland.engine.modularity.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class ModuleAnnotationVisitor extends AnnotationVisitor
{
    private final ASMModuleParser discoverer;

    public ModuleAnnotationVisitor(ASMModuleParser discoverer)
    {
        super(Opcodes.ASM5);
        this.discoverer = discoverer;
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
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc)
    {

    }

    @Override
    public void visitEnd()
    {

    }
}
