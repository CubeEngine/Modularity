package de.cubeisland.engine.modularity.asm.meta.candidate;

import de.cubeisland.engine.modularity.asm.meta.TypeReference;

public class FieldCandidate extends Candidate
{
    private final TypeReference declaringClass;
    private final int modifiers;
    private final TypeReference type;
    private final Object value;

    public FieldCandidate(TypeReference declaringClass, String name, int modifiers, TypeReference type, Object value)
    {
        super(name);
        this.declaringClass = declaringClass;
        this.modifiers = modifiers;
        this.type = type;
        this.value = value;
    }

    public TypeReference getDeclaringClass()
    {
        return declaringClass;
    }

    public int getModifiers()
    {
        return modifiers;
    }

    public TypeReference getType()
    {
        return type;
    }

    public Object getValue()
    {
        return value;
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
        s.append(type).append(' ');
        s.append(getName());
        if (value != null)
        {
            s.append(" = ").append(value);
        }
        return s.append(';').toString();
    }
}
