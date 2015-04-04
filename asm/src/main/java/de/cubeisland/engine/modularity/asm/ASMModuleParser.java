package de.cubeisland.engine.modularity.asm;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import com.sun.xml.internal.ws.org.objectweb.asm.Type;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import static de.cubeisland.engine.modularity.asm.ASMModuleParser.AnnotationType.CLASS;
import static de.cubeisland.engine.modularity.asm.ASMModuleParser.AnnotationType.FIELD;

public class ASMModuleParser
{
    private Type type;
    private int classVersion;
    private Type superType;
    private final Set<ModuleAnnotation> annotations = new HashSet<ModuleAnnotation>();

    private final Stack<ModuleAnnotation> annotationStack = new Stack<ModuleAnnotation>();




    static enum AnnotationType
    {
        CLASS,
        FIELD,
        METHOD,
        SUBTYPE;

    }

    public ASMModuleParser(ClassReader reader)
    {
        reader.accept(new ModuleClassVisitor(this), 0);
    }

    public void beginNewTypeName(String name, int version, String superName)
    {
        this.type = Type.getObjectType(name);
        this.classVersion = version;
        this.superType = (superName == null || superName.isEmpty()) ? null : Type.getObjectType(superName);
    }

    public void startClassAnnotation(String name)
    {
        annotationStack.clear();
        ModuleAnnotation annotation = new ModuleAnnotation(Type.getType(name), CLASS, this.type.getClassName());
        this.annotations.add(annotation);
        annotationStack.push(annotation);
    }

    public void startFieldAnnotation(String name, String annotationName)
    {
        annotationStack.clear();
        ModuleAnnotation annotation = new ModuleAnnotation(Type.getType(annotationName), FIELD, name);
        annotations.add(annotation);
        annotationStack.push(annotation);
    }

    private ModuleAnnotation getLastAnnotation()
    {
        return annotationStack.peek();
    }

    public void addAnnotationProperty(String name, Object value)
    {
        getLastAnnotation().addProperty(name, value);
    }

    public void addAnnotationArray(String name)
    {
        getLastAnnotation().addArray(name);
    }

    public void addSubAnnotation(String name, String desc)
    {
        ModuleAnnotation annotation = getLastAnnotation().addChildAnnotation(name, desc);
        annotations.add(annotation);
        annotationStack.push(annotation);
    }

    public void endArray()
    {
        getLastAnnotation().endArray();
    }

    public void endSubAnnotation()
    {
        annotationStack.pop();
    }
}
