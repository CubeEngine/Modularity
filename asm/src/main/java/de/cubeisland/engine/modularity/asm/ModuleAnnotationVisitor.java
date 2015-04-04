package de.cubeisland.engine.modularity.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class ModuleAnnotationVisitor extends AnnotationVisitor
{
    private final ASMModuleParser discoverer;
    private final boolean array;
    private final String name;
    private final boolean isSubAnnotation;

    public ModuleAnnotationVisitor(ASMModuleParser discoverer)
    {
        super(Opcodes.ASM5);
        this.discoverer = discoverer;
    }

    public ModuleAnnotationVisitor(ASMModuleParser discoverer, String name)
    {
        this.array = true;
        this.name = name;
        discoverer.addAnnotationArray(name);
    }

    public ModuleAnnotationVisitor(ASMModuleParser discoverer, boolean isSubAnnotation)
    {
        this(discoverer);
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
        return new ModuleAnnotationVisitor(discoverer, name);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc)
    {
        discoverer.addSubAnnotation(name, desc);
        return new ModuleAnnotationVisitor(discoverer, true);
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
