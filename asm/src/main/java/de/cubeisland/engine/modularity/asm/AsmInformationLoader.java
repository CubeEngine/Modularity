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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import de.cubeisland.engine.modularity.core.graph.Dependency;
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

    public Set<DependencyInformation> loadInformationFromClasspath(String... filters)
    {
        Set<DependencyInformation> result = new HashSet<DependencyInformation>();
        if (modularity.getClass().getClassLoader() instanceof URLClassLoader)
        {
            try
            {
                for (URL url : ((URLClassLoader)modularity.getClass().getClassLoader()).getURLs())
                {
                    URI uri = url.toURI();
                    if (uri.getScheme().equals("file"))
                    {
                        result.addAll(loadInformation(new File(uri), true, filters));
                    }
                }
            }
            catch (URISyntaxException e)
            {
                throw new IllegalStateException("", e);
            }
        }
        return result;
    }

    @Override
    public Set<DependencyInformation> loadInformation(File source, String... filters)
    {
        return loadInformation(source, false, filters);
    }

    private Set<DependencyInformation> loadInformation(File source, boolean deep, String[] filters)
    {
        Set<DependencyInformation> result = new HashSet<DependencyInformation>();
        if (source.isDirectory()) // if source is directory load each file
        {
            File[] files = source.listFiles();
            if (files != null)
            {
                for (File file : files)
                {
                    if (deep || !file.isDirectory()) // do not search recursively
                    {
                        result.addAll(loadInformation(file, deep, filters));
                    }
                }
                return result;
            }
            else
            {
                System.out.print("No Files found in: " + source.getName() + "\n");
            }
        }
        try
        {
            Set<TypeCandidate> candidates = getCandidates(source, filters); // Get all candidates from source
            ModularityClassLoader classLoader = null;
            LinkedHashSet<Dependency> dependencies = new LinkedHashSet<Dependency>();
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

    private Set<TypeCandidate> getCandidates(File file, String... filters) throws IOException
    {
        Set<TypeCandidate> candidates = new HashSet<TypeCandidate>();
        for (InputStream stream : getStreams(file, filters))
        {
            ModuleClassVisitor classVisitor = new ModuleClassVisitor(file);
            new ClassReader(stream).accept(classVisitor, 0);
            TypeCandidate candidate = classVisitor.getCandidate();
            if (candidate != null)
            {
                String version = "unknown";
                // Version info:
                if (candidate.isAnnotatedWith(Version.class))
                {
                    version = candidate.getAnnotation(Version.class).property("value").toString();
                }

                // SourceVersion for Module

                if (candidate.isAnnotatedWith(ModuleInfo.class))
                {
                    ClassLoader cl = candidate.getClassLoader();
                    if (cl == null)
                    {
                         cl = getClass().getClassLoader();
                    }
                    InputStream is = cl.getResourceAsStream("resources/" + candidate.getAnnotation(ModuleInfo.class).getProperties().get("name").toString() + ".properties");
                    if (is != null)
                    {
                        Properties properties = new Properties();
                        properties.load(is);
                        candidate.setSourceVersion(properties.getProperty("sourceVersion"));
                        if (version == null)
                        {
                            candidate.setVersion(properties.getProperty("version"));
                        }
                    }
                }
                candidate.setVersion(version);

                candidates.add(candidate);
            }
        }
        return candidates;
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

    private List<InputStream> getStreams(File file, String... filters) throws IOException
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
                    if (filters.length == 0)
                    {
                        list.add(zipFile.getInputStream(entry));
                    }
                    else
                    {
                        for (String filter : filters)
                        {
                            if (entry.getName().startsWith(filter))
                            {
                                list.add(zipFile.getInputStream(entry));
                                break;
                            }
                        }
                    }
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
