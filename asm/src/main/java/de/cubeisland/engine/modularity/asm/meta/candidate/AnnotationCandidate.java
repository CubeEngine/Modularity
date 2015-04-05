package de.cubeisland.engine.modularity.asm.meta.candidate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;

import static java.util.Collections.unmodifiableMap;

public class AnnotationCandidate extends Candidate
{
    private final TypeReference type;
    // reference to Class/Field/Method ?
    private final Map<String, Object> properties = new HashMap<String, Object>();

    public AnnotationCandidate(TypeReference type)
    {
        super(type.getReferencedClass());
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public void addProperty(String name, Object value)
    {
        Object val = properties.get(name);
        if (val instanceof List)
        {
            ((List)val).add(value);
        }
        else
        {
            properties.put(name, value);
        }
    }

    public Map<String, Object> getProperties()
    {
        return unmodifiableMap(properties);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("@").append(getName());
        if (!properties.isEmpty())
        {
            sb.append('(');
            String splitter = "";
            for (Entry<String, Object> entry : properties.entrySet())
            {
                sb.append(splitter);
                if (properties.size() != 1 || !"value".equals(entry.getKey()))
                {
                    sb.append(entry.getKey()).append(" = ");
                }
                sb.append(entry.getValue());
                splitter = ", ";
            }
            sb.append(')');
        }
        return sb.toString();
    }
}
