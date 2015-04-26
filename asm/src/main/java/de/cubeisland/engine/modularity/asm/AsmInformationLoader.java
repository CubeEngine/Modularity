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
import de.cubeisland.engine.modularity.asm.marker.Service;
import de.cubeisland.engine.modularity.asm.marker.ServiceImpl;
import de.cubeisland.engine.modularity.asm.marker.Version;
import de.cubeisland.engine.modularity.asm.meta.TypeReference;
import de.cubeisland.engine.modularity.asm.meta.candidate.ClassCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.InterfaceCandidate;
import de.cubeisland.engine.modularity.asm.meta.candidate.TypeCandidate;
import de.cubeisland.engine.modularity.asm.visitor.ModuleClassVisitor;
import de.cubeisland.engine.modularity.core.InformationLoader;
import de.cubeisland.engine.modularity.core.Modularity;
import de.cubeisland.engine.modularity.core.ModularityClassLoader;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.graph.DependencyInformation;
import de.cubeisland.engine.modularity.core.graph.meta.ModuleMetadata;
import org.objectweb.asm.ClassReader;

public class AsmInformationLoader implements InformationLoader
{
    private final Map<String, TypeCandidate> knownTypes = new HashMap<String, TypeCandidate>();
    private final Map<File, ModularityClassLoader> classLoaders = new HashMap<File, ModularityClassLoader>();

    private final Modularity modularity;

    public AsmInformationLoader(AsmModularity modularity)
    {
        this.modularity = modularity;
    }

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

            // Sort candidates and add additional Information
            List<ClassCandidate> modules = new ArrayList<ClassCandidate>();
            List<InterfaceCandidate> services = new ArrayList<InterfaceCandidate>();
            List<ClassCandidate> servicesImpl = new ArrayList<ClassCandidate>();

            for (TypeCandidate candidate : candidates)
            {
                knownTypes.put(candidate.getName(), candidate);
                if (candidate.isAnnotatedWith(ModuleInfo.class))
                {
                    if (candidate instanceof ClassCandidate)
                    {
                        modules.add((ClassCandidate)candidate);
                    }
                    else
                    {
                        System.err.println("Type '" + candidate.getName() + "' has the @ModuleInfo annotation, but cannot be a module!");
                    }
                }
                else if (candidate.isAnnotatedWith(Service.class))
                {
                    if (candidate instanceof InterfaceCandidate)
                    {
                        services.add((InterfaceCandidate)candidate);
                    }
                    else
                    {
                        System.err.println("Type '" + candidate.getName() + "' has the @Service annotation, but cannot be a service!");
                    }
                }
                else if (candidate.isAnnotatedWith(ServiceImpl.class))
                {
                    if (candidate instanceof ClassCandidate)
                    {
                        servicesImpl.add((ClassCandidate)candidate);
                    }
                    else
                    {
                        System.err.println("Type '" + candidate.getName() + "' has the @ServiceImpl annotation, but cannot be a service-implementation!");
                    }
                }

                // Version info:
                if (candidate.isAnnotatedWith(Version.class))
                {
                    candidate.setVersion((String)candidate.getAnnotation(Version.class).property("value"));
                }
                else
                {
                    // TODO get version info from maven version or version in manifest same as sourceversion
                }
            }

            for (Iterator<ClassCandidate> iterator = modules.iterator(); iterator.hasNext(); )
            {
                final TypeCandidate module = iterator.next();
                if (!implemented(module, Module.class))
                {
                    System.err.println("Type '" + module.getName() + "' has the @ModuleInfo annotation, but doesn't implement the Module interface!");
                    iterator.remove();
                }
            }



            ModularityClassLoader classLoader = null;
            LinkedHashSet<String> dependencies = new LinkedHashSet<String>();
            if (source.getName().endsWith(".jar"))
            {
                classLoader = new ModularityClassLoader(modularity, source.toURI().toURL(), dependencies, null); // TODO parentClassLoader;
            }
            else
            {
                // TODO
            }

            for (ClassCandidate module : modules)
            {
                module.setClassLoader(classLoader);
                result.add(new AsmModuleMetadata(module));
            }

            for (InterfaceCandidate service : services)
            {
                service.setClassLoader(classLoader);
                result.add(new AsmServiceDefinitionMetadata(service));
            }

            for (ClassCandidate serviceImpl : servicesImpl)
            {
                serviceImpl.setClassLoader(classLoader);
                result.add(new AsmServiceImplementationMetadata(serviceImpl));
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

    private Set<TypeCandidate> getCandidates(File file) throws IOException
    {
        // TODO set classloader
        String sourceVersion = getSourceVersion(file);
        Set<TypeCandidate> candidates = new HashSet<TypeCandidate>();
        for (InputStream stream : getStreams(file))
        {
            ModuleClassVisitor classVisitor = new ModuleClassVisitor(file);
            new ClassReader(stream).accept(classVisitor, 0);
            TypeCandidate candidate = classVisitor.getCandidate();
            if (candidate != null)
            {
                candidate.setSourceVersion(sourceVersion);
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private String getSourceVersion(File file) throws IOException
    {
        String sourceVersion = "unknown-unknown";
        try
        {
            JarFile jarFile = new JarFile(file);
            sourceVersion = jarFile.getManifest().getMainAttributes().getValue("sourceVersion");
        }
        catch (ZipException ignored)
        {}
        return sourceVersion;
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
            return implemented(knownTypes.get(((ClassCandidate)current).getExtendedClass().getReferencedClass()), interfaceToCheck);
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


}
