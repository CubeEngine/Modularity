package de.cubeisland.engine.modularity.asm;

import de.cubeisland.engine.modularity.asm.meta.candidate.MethodCandidate;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static de.cubeisland.engine.modularity.asm.ModuleClassVisitor.visit;

public class ModuleMethodVisitor extends MethodVisitor
{
    private final MethodCandidate methodCandidate;

    public ModuleMethodVisitor(MethodCandidate methodCandidate)
    {
        super(Opcodes.ASM5);
        this.methodCandidate = methodCandidate;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, boolean visible)
    {
        return visit(methodCandidate, name);
    }
}
