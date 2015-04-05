package de.cubeisland.engine.modularity.asm.meta.candidate;

import java.util.List;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

public class MethodCandidate extends Candidate
{
    private final TypeReference declaringClass;
    private final int modifiers;
    private final TypeReference returnType;
    private final List<TypeReference> parameterTypes;

    public MethodCandidate(TypeReference declaringClass, String name, int modifiers, TypeReference returnType, List<TypeReference> parameterTypes)
    {
        super(name);
        this.declaringClass = declaringClass;
        this.modifiers = modifiers;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    public TypeReference getDeclaringClass()
    {
        return declaringClass;
    }

    public int getModifiers()
    {
        return modifiers;
    }

    public TypeReference getReturnType()
    {
        return returnType;
    }

    public List<TypeReference> getParameterTypes()
    {
        return parameterTypes;
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (AnnotationCandidate candidate : getAnnotations())
        {
            s.append(candidate).append("\n\t");
        }
        s.append(stringModifiers(modifiers));
        s.append(returnType).append(' ');
        s.append(getName());
        s.append('(');

        String splitter = "";
        for (final TypeReference parameterType : this.parameterTypes)
        {
            s.append(splitter);
            s.append(parameterType);
            splitter = ", ";
        }

        s.append(')');
        return s.append(';').toString();
    }
}
