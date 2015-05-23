/**
 * The MIT License
 * Copyright (c) 2014 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.engine.modularity.asm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.asm.marker.Provider;
import de.cubeisland.engine.modularity.asm.marker.Service;
import de.cubeisland.engine.modularity.asm.marker.ServiceImpl;
import de.cubeisland.engine.modularity.asm.marker.ServiceProvider;
import de.cubeisland.engine.modularity.asm.marker.Version;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;
import de.cubeisland.engine.modularity.asm.meta.candidate.ClassCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.InterfaceCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate;
import de.cubeisland.engine.modularity.asm.visitor.ModuleClassVisitor;
import de.cubeisland.engine.modularity.core.BasicModularity;
import de.cubeisland.engine.modularity.core.InformationLoader;
import de.cubeisland.engine.modularity.core.Modularity;
import de.cubeisland.engine.modularity.core.ModularityClassLoader;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.ValueProvider;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import org.objectweb.asm.ClassReader;

/**
 * A InformationLoader implementation using Asm
 */
public class AsmInformationLoader implements InformationLoader
{
    private final Map<String, TypeCandidate> knownTypes = new HashMap<String, TypeCandidate>();

    private Modularity modularity;

    public Set<DependencyInformation> loadInformation(Set<File> files)
    {
        Set<DependencyInformation> information = new HashSet<DependencyInformation>();

        for (final File file : files)
        {
            information.addAll(loadInformation(file));
        }

        return information;
    }

    @Override
    public Set<DependencyInformation> loadInformation(File source)
    {
        Set<DependencyInformation> result = new HashSet<DependencyInformation>();
        if (source.isDirectory()) // if source is directory load each file
        {
            for (File file : source.listFiles())
            {
                // TODO configurable
                if (!file.isDirectory()) // do not search recursively
                {
                    result.addAll(loadInformation(file));
                }
            }
            return result;
        }
        try
        {
            Set<TypeCandidate> candidates = getCandidates(source); // Get all candidates from source

            ModularityClassLoader classLoader = null;
            LinkedHashSet<String> dependencies = new LinkedHashSet<String>();
            if (source.getName().endsWith(".jar"))
            {
                classLoader = new ModularityClassLoader(modularity, source.toURI().toURL(), dependencies,
                                                        modularity.getClass().getClassLoader());
            }

            // Sort candidates and add additional Information
            for (TypeCandidate candidate : candidates)
            {
                candidate.setClassLoader(classLoader);
                knownTypes.put(candidate.getName(), candidate);

                try
                {
                    checkFor(candidate, ModuleInfo.class, ClassCandidate.class, AsmModuleMetadata.class, result);
                    checkFor(candidate, Service.class, InterfaceCandidate.class, AsmServiceDefinitionMetadata.class, result);
                    checkFor(candidate, ServiceImpl.class, ClassCandidate.class, AsmServiceImplementationMetadata.class, result);
                    checkFor(candidate, ServiceProvider.class, ClassCandidate.class, AsmServiceProviderMetadata.class, result);
                    checkFor(candidate, Provider.class, ClassCandidate.class, AsmValueProviderMetadata.class, result);
                }
                catch (NoSuchMethodException ignored)
                {
                }
                catch (IllegalAccessException ignored)
                {
                }
                catch (InvocationTargetException ignored)
                {
                }
                catch (InstantiationException ignored)
                {
                }
            }

            for (DependencyInformation info : result)
            {
                dependencies.addAll(info.requiredDependencies());
                dependencies.addAll(info.optionalDependencies());
                if (info instanceof ModuleMetadata)
                {
                    dependencies.addAll(((ModuleMetadata)info).loadAfter());
                }
            }

            return result;
        }
        catch (IOException e)
        {
            // TODO log error
            return Collections.emptySet();
        }
    }

    private void checkFor(TypeCandidate candidate, Class<? extends Annotation> annotation, Class<? extends TypeCandidate> candidateType,
                          Class<? extends AsmDependencyInformation> metaClass, Set<DependencyInformation> result) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        if (candidate.isAnnotatedWith(annotation))
        {
            if (candidateType.isAssignableFrom(candidateType))
            {
                if (annotation == ModuleInfo.class && !implemented(candidate, Module.class))
                {
                    System.err.println("Type '" + candidate.getName()
                                           + "' has the @ModuleInfo annotation, but doesn't implement the Module interface!");
                    return;
                }
                if (annotation == Provider.class && !candidate.hasInterface(ValueProvider.class))
                {
                    System.err.println("Type '" + candidate.getName()
                                           + "' has the @Provider annotation, but cannot be a value-provider!");
                    return;
                }
                result.add(metaClass.getConstructor(candidateType).newInstance(candidate));
            }
            else
            {
                System.err.println("Type '" + candidate.getName() + "' has the @" + annotation.getSimpleName() + " annotation, but is not a " + candidateType.getName());
            }
        }
    }

    private Set<TypeCandidate> getCandidates(File file) throws IOException
    {
        String sourceVersion = getManifestInfo(file, "sourceVersion", "unknown-unknown");
        String version = getManifestInfo(file, "version", "unknown");
        Set<TypeCandidate> candidates = new HashSet<TypeCandidate>();
        for (InputStream stream : getStreams(file))
        {
            ModuleClassVisitor classVisitor = new ModuleClassVisitor(file);
            new ClassReader(stream).accept(classVisitor, 0);
            TypeCandidate candidate = classVisitor.getCandidate();
            if (candidate != null)
            {
                candidate.setSourceVersion(sourceVersion);
                // Version info:
                if (candidate.isAnnotatedWith(Version.class))
                {
                    version = candidate.getAnnotation(Version.class).property("value").toString();
                }
                candidate.setVersion(version);

                candidates.add(candidate);
            }
        }
        return candidates;
    }

    public static String getManifestInfo(File file, String key, String def) throws IOException
    {
        String result = def;
        try
        {
            JarFile jarFile = new JarFile(file);
            result = jarFile.getManifest().getMainAttributes().getValue(key);
        }
        catch (ZipException ignored)
        {
        }
        if (result == null)
        {
            result = def;
        }
        return result;
    }

    private boolean implemented(TypeCandidate current, Class interfaceToCheck)
    {
        if (current == null)
        {
            return false;
        }
        if (current.hasInterface(interfaceToCheck))
        {
            return true;
        }
        for (final TypeReference anInterface : current.getImplementedInterfaces())
        {
            if (implemented(knownTypes.get(anInterface.getReferencedClass()), interfaceToCheck))
            {
                return true;
            }
        }
        if (current instanceof ClassCandidate)
        {

            String extended = ((ClassCandidate)current).getExtendedClass().getReferencedClass();
            if (extended.equals(interfaceToCheck.getName()))
            {
                return true;
            }
            return implemented(knownTypes.get(extended), interfaceToCheck);
        }
        return false;
    }

    private List<InputStream> getStreams(File file) throws IOException
    {
        List<InputStream> list = new ArrayList<InputStream>();
        if (file.getName().endsWith(".class"))
        {
            list.add(new FileInputStream(file));
            return list;
        }
        else
        {
            ZipFile zipFile = new ZipFile(file);
            for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements(); )
            {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class"))
                {
                    list.add(zipFile.getInputStream(entry));
                }
            }
        }
        return list;
    }

    public static Modularity newModularity()
    {
        AsmInformationLoader loader = new AsmInformationLoader();
        BasicModularity modularity = new BasicModularity(loader);
        loader.modularity = modularity;
        return modularity;
    }
}
