package de.cubeisland.engine.modularity.asm;

public class ModuleFieldVisitor
{
    private final String name;
    private final ASMModuleParser discoverer;

    public ModuleFieldVisitor(String name, ASMModuleParser discoverer)
    {
        super(OpCodes.ASM5);
        this.name = name;
        this.discoverer = discoverer;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String annotationName, boolean runtimeVisible)
    {
        discoverer.startFieldAnnotation(name, annotationName);
        return new ModuleAnnotationVisitor(discoverer);
    }
}
