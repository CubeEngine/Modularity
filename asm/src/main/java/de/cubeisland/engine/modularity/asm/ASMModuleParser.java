package de.cubeisland.engine.modularity.asm;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import static de.cubeisland.engine.modularity.asm.ASMModuleParser.AnnotationType.CLASS;

public class ASMModuleParser
{
    private Type type;
    private int classVersion;
    private Type superType;
    private final List<ModuleAnnotation> annoations = new ArrayList<ModuleAnnotation>();

    static enum AnnotationType
    {
        CLASS, FIELD, METHOD, SUBTYPE;

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
        this.annoations.add(new ModuleAnnotation(Type.getType(name), CLASS, this.type.getClassName()));
    }

    private ModuleAnnotation getLastAnnotation() {
        return this.annoations.get(this.annoations.size() - 1);
    }

    public void addAnnotationProperty(String name, Object value)
    {
        getLastAnnotation().addProperty(name, value);
    }
}
/*
public ASMModParser(InputStream stream) throws IOException
    {
        try
        {
            ClassReader reader = new ClassReader(stream);
            reader.accept(new ModClassVisitor(this), 0);
        }
        catch (Exception ex)
        {
            FMLLog.log(Level.ERROR, ex, "Unable to read a class file correctly");
            throw new LoaderException(ex);
        }
    }
 */
