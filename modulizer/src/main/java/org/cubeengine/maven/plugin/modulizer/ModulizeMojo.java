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
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @goal modulize
 * @phase generate-sources
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
    private Map<String, String> annotationValues = Collections.emptyMap();

    /**
     * @parameter default-value="${project.build.sourceDirectory}"
     */
    private File sourceFolder;

    /**
     * @parameter default-value="${project.build.directory}/modulized-classes"
     */
    private File targetFolder;

    /**
     * @parameter default-value="${project.build.sourceEncoding}"
     */
    private String encoding = "UTF-8";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (project.getPackaging().equalsIgnoreCase("pom"))
        {
            getLog().info("Aggregator project, skipping...");
            return;
        }

        int splitAt = mainClass.lastIndexOf('.');
        String mainClassName = mainClass;
        String mainPackage = null;
        if (splitAt != -1)
        {
            mainClassName = mainClass.substring(splitAt + 1);
            mainPackage = mainClass.substring(0, splitAt);
        }

        String relativePath = mainClassName + ".java";
        if (mainPackage != null)
        {
            relativePath = mainPackage.replace('.', File.separatorChar) + File.separator + relativePath;
        }

        File source = new File(sourceFolder, relativePath);

        if (!source.exists() || !source.isFile())
        {
            throw new MojoFailureException("Source main class file does not exist or is not a file: " + source);
        }

        String sourceCode;
        try
        {
            sourceCode = readSource(source, encoding);
        }
        catch (IOException e)
        {
            throw new MojoFailureException("Failed to read source file!", e);
        }


        File target = new File(targetFolder, relativePath);

        try
        {
            String annotation = constructAnnotation(annotationType, annotationValues);
            getLog().info("Constructed this annotation: " + annotation);
            writeTarget(target, encoding, addAnnotationToClass(sourceCode, mainClassName, annotation));
        }
        catch (IOException e)
        {
            throw new MojoFailureException("Failed to write the target!", e);
        }

        getLog().info("Class " + mainClass + " successfully modulized to " + target);
        project.addCompileSourceRoot(targetFolder.toString());

    }

    public static String addAnnotationToClass(String sourceCode, String className, String annotationCode)
    {
        return sourceCode.replaceFirst("class\\s+" + Pattern.quote(className), annotationCode.replaceAll("\\$", "\\$") + " $0");
    }

    public static String constructAnnotation(String annotationType, Map<String, String> annotationValues)
    {
        StringBuilder annotationBuilder = new StringBuilder("@");
        annotationBuilder.append(annotationType).append('(');

        String separator = "";
        for (Map.Entry<String, String> entry : annotationValues.entrySet())
        {
            annotationBuilder.append(separator).append(entry.getKey()).append(" = ").append(entry.getValue());
            separator = ", ";
        }

        annotationBuilder.append(')');
        return annotationBuilder.toString();
    }

    public static String readSource(File source, String charset) throws IOException
    {
        FileInputStream in = new FileInputStream(source);
        byte[] data = new byte[(int) source.length()];
        try
        {
            in.read(data);
        }
        finally
        {
            in.close();
        }

        return new String(data, charset);
    }

    public static void writeTarget(File target, String charset, String sourceCode) throws MojoFailureException, IOException
    {
        File targetDir = target.getParentFile();
        if (!targetDir.exists())
        {
            if (!targetDir.mkdirs())
            {
                throw new MojoFailureException("Failed to create the target directory: " + targetDir);
            }
        }
        else if (!targetDir.isDirectory())
        {
            throw new MojoFailureException("The target directory exists, but is not a directory: " + targetDir);
        }

        FileOutputStream out = new FileOutputStream(target);
        try
        {
            out.write(sourceCode.getBytes(charset));
        }
        finally
        {
            out.close();
        }
    }

}
