package de.cubeisland.engine.modularity.asm.meta.candidate;

import java.util.Set;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

public class InterfaceCandidate extends TypeCandidate
{
    public InterfaceCandidate(String name, int modifiers, Set<TypeReference> interfaces)
    {
        super(name, modifiers, interfaces);
    }

    @Override
    protected String typeName()
    {
        return "interface";
    }
}
