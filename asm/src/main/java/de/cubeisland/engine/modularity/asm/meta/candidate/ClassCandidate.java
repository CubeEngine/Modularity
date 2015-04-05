package de.cubeisland.engine.modularity.asm.meta.candidate;

import java.util.HashSet;
import java.util.Set;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

import static java.util.Collections.unmodifiableSet;

public class ClassCandidate extends TypeCandidate
{
    private final TypeReference extendedClass;
    private final Set<ConstructorCandidate> constructors = new HashSet<ConstructorCandidate>();

    public ClassCandidate(String name, int modifiers, Set<TypeReference> interfaces, TypeReference extendedClass)
    {
        super(name, modifiers, interfaces);
        this.extendedClass = extendedClass;
    }

    public TypeReference getExtendedClass()
    {
        return extendedClass;
    }

    @Override
    protected String typeName()
    {
        return "class";
    }

    public void addConstructor(ConstructorCandidate constructor)
    {
        this.constructors.add(constructor);
    }

    public Set<ConstructorCandidate> getConstructors()
    {
        return unmodifiableSet(constructors);
    }
}
