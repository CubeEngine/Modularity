package de.cubeisland.engine.modularity.asm.meta;

public class TypeReference
{
    private final String referencedClass;

    public TypeReference(String referencedClass)
    {
        this.referencedClass = referencedClass;
    }

    public String getReferencedClass()
    {
        return referencedClass;
    }

    @Override
    public String toString()
    {
        return referencedClass;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final TypeReference that = (TypeReference)o;

        if (referencedClass != null ? !referencedClass.equals(that.referencedClass) : that.referencedClass != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return referencedClass != null ? referencedClass.hashCode() : 0;
    }
}
