package de.cubeisland.engine.modularity.asm.meta.candidate;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

import static java.util.Collections.unmodifiableSet;

public abstract class TypeCandidate extends Candidate
{
    private final Set<TypeReference> interfaces;
    private final Set<FieldCandidate> fields = new HashSet<FieldCandidate>();
    private final Set<MethodCandidate> methods = new HashSet<MethodCandidate>();
    private final int modifiers;

    public TypeCandidate(String name, int modifiers, Set<TypeReference> interfaces)
    {
        super(name);
        this.modifiers = modifiers;
        this.interfaces = unmodifiableSet(interfaces);
    }

    public static String simpleName(String name)
    {
        String[] parts = name.split("\\.");
        return parts[parts.length - 1];
    }

    public String getSimpleName()
    {
        return simpleName(getName());
    }

    public Set<TypeReference> getImplementedInterfaces()
    {
        return interfaces;
    }

    public void addField(FieldCandidate fieldCandidate)
    {
        this.fields.add(fieldCandidate);
    }

    public Set<FieldCandidate> getFields()
    {
        return unmodifiableSet(fields);
    }

    public void addMethod(MethodCandidate candidate)
    {
        this.methods.add(candidate);
    }

    public Set<MethodCandidate> getMethods()
    {
        return unmodifiableSet(methods);
    }

    public TypeReference newReference()
    {
        return new TypeReference(getName());
    }

    public int getModifiers()
    {
        return modifiers;
    }

    protected abstract String typeName();

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for (AnnotationCandidate candidate : getAnnotations())
        {
            s.append(candidate).append("\n");
        }
        s.append(stringModifiers(modifiers));
        s.append(typeName()).append(' ');
        s.append(getName());
        if (this instanceof ClassCandidate)
        {
            final ClassCandidate clazz = (ClassCandidate)this;
            final String extendingClass = clazz.getExtendedClass().getReferencedClass();
            if (!Object.class.getName().equals(extendingClass))
            {
                s.append(" extends ").append(clazz.getExtendedClass());
            }
            Set<TypeReference> interfaces = getImplementedInterfaces();
            if (!interfaces.isEmpty())
            {
                s.append(" implements ");
                String splitter = "";
                for (TypeReference reference : interfaces)
                {
                    s.append(splitter);
                    s.append(reference);
                    splitter = ", ";
                }
            }
        }
        else
        {
            Set<TypeReference> interfaces = getImplementedInterfaces();
            if (!interfaces.isEmpty())
            {
                s.append(" extends ");
                String splitter = "";
                for (TypeReference reference : interfaces)
                {
                    s.append(splitter);
                    s.append(reference);
                    splitter = ", ";
                }
            }
        }
        s.append(" {");

        for (FieldCandidate field : this.fields)
        {
            s.append("\n\t").append(field);
        }
        if (this instanceof ClassCandidate)
        {
            for (final ConstructorCandidate constructor : ((ClassCandidate)this).getConstructors())
            {
                s.append("\n\t").append(constructor);
            }
        }
        for (MethodCandidate method : this.methods)
        {
            s.append("\n\t").append(method);
        }

        return s.append("\n}").toString();
    }
}
