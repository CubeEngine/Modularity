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
package org.cubeengine.maven.plugin.modulizer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import spoon.Launcher;
import spoon.OutputType;
import spoon.compiler.Environment;
import spoon.compiler.SpoonCompiler;
import spoon.processing.AbstractProcessor;
import spoon.processing.ProcessingManager;
import spoon.processing.Processor;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.QueueProcessingManager;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @requiresDependencyResolution test
 * @goal modulize
 * @threadSafe false
 */
@SuppressWarnings("JavaDoc")
public class ModulizeMojo extends AbstractMojo
{
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project = null;

    /**
     * @parameter
     * @required
     */
    private String mainClass = null;

    /**
     * @parameter
     * @required
     */
    private String annotationType = "org.cubeengine.modularity.ModuleInfo";

    /**
     * @parameter
     * @required
     */
    private Map<String, Object> annotationValues = Collections.emptyMap();

    /**
     * @parameter default-value="${project.build.sourceDirectory}"
     */
    private File sourceFolder;

    /**
     * @parameter default-value="${project.build.directory}/generated-sources"
     */
    private File targetFolder;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (project.getPackaging().equals("pom")) {
            getLog().info("Parent project, skipping...");
            return;
        }
        getLog().info("Modulizing class " + mainClass + " from " + sourceFolder);
        Launcher l = new Launcher();
        SpoonCompiler c = l.createCompiler();
        c.addInputSource(sourceFolder);
        c.setSourceClasspath(this.loadClasspath(System.getProperty("java.class.path").split(File.pathSeparator)));
        if (!targetFolder.exists())
        {
            if (!targetFolder.mkdirs())
            {
                throw new MojoFailureException("Failed to create the target folder!");
            }
        }
        else if (targetFolder.exists() && !targetFolder.isDirectory())
        {
            throw new MojoFailureException("The target directory seems to be a file!");
        }
        c.setSourceOutputDirectory(targetFolder);
        final Factory f = c.getFactory();
        ProcessingManager p = new QueueProcessingManager(f);
        ClassProcessor proc = new ClassProcessor(f, getLog());
        p.addProcessor(proc);
        Environment e = f.getEnvironment();
        e.setNoClasspath(true);
        e.setManager(p);
        e.setLevel("ALL");
        if (c.build())
        {
            getLog().info("Successfully modulized " + proc.getTagged() + " classes to " + targetFolder + "!");
            c.generateProcessedSourceFiles(OutputType.CLASSES);
        }
        else
        {
            throw new MojoFailureException("Failed to build the source model!");
        }
    }

    private String[] loadClasspath(String[] classpathEntries)
    {
        List<String> classpath = new ArrayList<String>(classpathEntries.length);
        for (String entry : classpathEntries)
        {
            File file = new File(entry);

            if (!file.exists())
            {
                getLog().warn("The classpath entry '" + entry + "' was removed. It doesn't exist.");
                continue;
            }

            classpath.add(entry);
        }

        if (classpath.isEmpty())
        {
            getLog().warn("The classpath is empty.");
        }

        return classpath.toArray(new String[classpath.size()]);
    }

    private class ClassProcessor extends AbstractProcessor<CtClass> {
        private final Factory f;
        private final Log log;
        private int tagged = 0;

        public ClassProcessor(Factory f, Log log) {
            this.f = f;
            this.log = log;
        }

        public int getTagged() {
            return tagged;
        }

        @Override
        public void process(CtClass clazz)
        {
            log.info(clazz.getQualifiedName());
            if (clazz.getQualifiedName().equals(mainClass)) {
                CtAnnotation<Annotation> a = f.Core().createAnnotation();
                a.setAnnotationType(f.Type().<Annotation>createReference(annotationType));
                a.setElementValues(annotationValues);
                clazz.addAnnotation(a);
            }
        }
    }
}
