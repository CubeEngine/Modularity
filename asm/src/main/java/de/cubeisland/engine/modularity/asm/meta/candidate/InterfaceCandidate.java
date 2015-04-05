package de.cubeisland.engine.modularity.asm.meta.candidate;

import java.io.File;
import java.util.Set;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

public class InterfaceCandidate extends TypeCandidate
{
    public InterfaceCandidate(File sourceFile, String name, int modifiers, Set<TypeReference> interfaces)
    {
        super(sourceFile, name, modifiers, interfaces);
    }

    @Override
    protected String typeName()
    {
        return "interface";
    }
}
