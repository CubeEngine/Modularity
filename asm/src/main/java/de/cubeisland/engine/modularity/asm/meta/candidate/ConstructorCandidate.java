package de.cubeisland.engine.modularity.asm.meta.candidate;

import java.util.List;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

import static de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate.simpleName;

public class ConstructorCandidate extends MethodCandidate
{
    public ConstructorCandidate(TypeReference declaringClass, String name, int modifiers, List<TypeReference> parameterTypes)
    {
        super(declaringClass, name, modifiers, new TypeReference("void"), parameterTypes);
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        s.append(stringModifiers(getModifiers()));
        s.append(simpleName(getDeclaringClass().getReferencedClass()));
        s.append('(');

        String splitter = "";
        for (final TypeReference parameterType : getParameterTypes())
        {
            s.append(splitter);
            s.append(parameterType);
            splitter = ", ";
        }

        s.append(')');
        return s.append(';').toString();
    }
}
