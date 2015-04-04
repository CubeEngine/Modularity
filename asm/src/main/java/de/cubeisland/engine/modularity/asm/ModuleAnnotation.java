package de.cubeisland.engine.modularity.asm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.sun.xml.internal.ws.org.objectweb.asm.Type;
import de.cubeisland.engine.modularity.asm.ASMModuleParser.AnnotationType;

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

    public ModuleAnnotation(Type type, AnnotationType subtype, ModuleAnnotation parent)
    {
        this.type = subtype;
        this.annotationClass = type;
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

    public void addArray(String name)
    {
        arrayList = new ArrayList<Object>();
        arrayName = name;
    }

    public ModuleAnnotation addChildAnnotation(String name, String desc)
    {
        ModuleAnnotation child = new ModuleAnnotation(Type.getType(desc), AnnotationType.SUBTYPE, this);
        if (arrayList != null)
        {
            arrayList.add(child.values);
        }
        return child;
    }

    public void endArray()
    {

        values.put(arrayName, arrayList);
        arrayList = null;
    }
}
