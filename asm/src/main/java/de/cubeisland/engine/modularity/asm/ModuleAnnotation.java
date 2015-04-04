package de.cubeisland.engine.modularity.asm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.cubeisland.engine.modularity.asm.ASMModuleParser.AnnotationType;
import org.objectweb.asm.Type;

public class ModuleAnnotation
{

    private final Type annotationClass;
    private final AnnotationType type;
    private final String member;

    private String arrayName;
    private List<Object> arrayList;
    private Map<String, Object> values = new HashMap<String, Object>();

    public ModuleAnnotation(Type annotationClass, AnnotationType type, String member)
    {
        this.annotationClass = annotationClass;
        this.type = type;
        this.member = member;
    }

    public void addProperty(String name, Object value)
    {
        if (arrayList == null)
        {
            values.put(name, value);
        }
        else
        {
            arrayList.add(value);
        }
    }
}
