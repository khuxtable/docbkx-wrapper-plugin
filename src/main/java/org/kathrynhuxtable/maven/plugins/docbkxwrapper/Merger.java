/*
 * Copyright (c) 2010 Kathryn Huxtable.
 *
 * This file is part of the Image Generator Maven plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */
package org.kathrynhuxtable.maven.plugins.docbkxwrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.codehaus.plexus.i18n.DefaultI18N;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.xpath.XPath;

/**
 * Goal runs Velocity on the files in the specified directory.
 */
public class Merger {

    private MavenProject    project;
    private I18N            i18n;
    private VelocityEngine  ve;
    private Template        template;
    private AttributeMap    attributes;
    private DecorationModel decorationModel;

    /**
     * DOCUMENT ME!
     *
     * @param  templateFile    TODO
     * @param  localRepository DOCUMENT ME!
     * @param  repositories    DOCUMENT ME!
     * @param  i18n            DOCUMENT ME!
     * @param  inputEncoding   DOCUMENT ME!
     * @param  outputEncoding  DOCUMENT ME!
     * @param  siteDirectory   TODO
     * @param  project         DOCUMENT ME!
     * @param  siteTool        DOCUMENT ME!
     * @param  reactorProjects DOCUMENT ME!
     *
     * @throws MojoExecutionException DOCUMENT ME!
     *
     * @see    org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void initialize(File templateFile, ArtifactRepository localRepository, List<?> repositories, I18N i18n, String inputEncoding,
            String outputEncoding, File siteDirectory, MavenProject project, SiteTool siteTool, List<?> reactorProjects)
        throws MojoExecutionException {
        this.i18n    = i18n;
        this.project = project;
        ve           = initializeVelocityEngine();
        template     = getVelocityTemplate(ve, templateFile);

        initializeI18N(i18n);

        attributes = initializeAttributes(inputEncoding, outputEncoding, project);

        try {
            decorationModel = siteTool.getDecorationModel(project, reactorProjects, localRepository, repositories,
                                                          getRelativeFilePath(project.getBasedir(), siteDirectory),
                                                          Locale.getDefault(), inputEncoding, outputEncoding);
        } catch (SiteToolException e) {
            throw new MojoExecutionException("SiteToolException: " + e.getMessage(), e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  i18n
     *
     * @throws MojoExecutionException
     */
    private void initializeI18N(I18N i18n) throws MojoExecutionException {
        try {
            ((DefaultI18N) i18n).initialize();
        } catch (InitializationException e) {
            e.printStackTrace();
            throw new MojoExecutionException("Unable to initialize I18N object", e);
        }
    }

    /**
     * Initialize the velocity engine.
     *
     * @return the velocity engine.
     *
     * @throws MojoExecutionException
     */
    private VelocityEngine initializeVelocityEngine() throws MojoExecutionException {
        VelocityEngine ve = new VelocityEngine();

        try {
            ve.addProperty("resource.loader", "file, class");
            ve.addProperty("class.resource.loader.description", "Velocity Classpath Resource Loader");
            ve.addProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            ve.init();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Unable to initialize Velocity engine", e);
        }

        return ve;
    }

    /**
     * Get the velocity template.
     *
     * @param  ve
     * @param  templateFile TODO
     *
     * @return the velocity template.
     *
     * @throws MojoExecutionException
     */
    private Template getVelocityTemplate(VelocityEngine ve, File templateFile) throws MojoExecutionException {
        Template template = null;

        try {
            template = ve.getTemplate(getRelativeFilePath(project.getBasedir(), templateFile));
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
            throw new MojoExecutionException("Unable to locate template " + templateFile, e);
        } catch (ParseErrorException e) {
            e.printStackTrace();
            throw new MojoExecutionException("Problem parsing the template", e);
        } catch (MethodInvocationException e) {
            e.printStackTrace();
            throw new MojoExecutionException("Something invoked in the template threw an exception", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Some random template parsing error occurred", e);
        }

        return template;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  inputEncoding
     * @param  outputEncoding
     * @param  project
     *
     * @return DOCUMENT ME!
     */
    private AttributeMap initializeAttributes(String inputEncoding, String outputEncoding, MavenProject project) {
        AttributeMap attributes = new AttributeMap();

        if (attributes.get("project") == null) {
            attributes.put("project", project);
        }

        // Put any of the properties in directly into the Velocity attributes
        attributes.putAll(project.getProperties());

        if (attributes.get("inputEncoding") == null) {
            attributes.put("inputEncoding", inputEncoding);
        }

        if (attributes.get("outputEncoding") == null) {
            attributes.put("outputEncoding", outputEncoding);
        }

        Date date = new Date();

        attributes.put("currentDate", date);
        attributes.put("lastPublished", new SimpleDateFormat("dd MMM yyyy").format(date));

        return attributes;
    }

    /**
     * Merge a file with the velocity template, filtering it if necessary.
     *
     * @param  file            the file to merge
     * @param  doFiltering     DOCUMENT ME!
     * @param  filterExtension DOCUMENT ME!
     * @param  sourceDirectory DOCUMENT ME!
     * @param  targetFile      DOCUMENT ME!
     *
     * @throws MojoExecutionException
     */
    public void mergeFile(String file, boolean doFiltering, String filterExtension, File sourceDirectory, File targetFile)
        throws MojoExecutionException {
        File sourceFile = new File(sourceDirectory, file);

        FileWriter fileWriter = null;
        Reader     fileReader = null;

        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        VelocityContext context = createContext(sourceFile, sourceDirectory, doFiltering, filterExtension, decorationModel, attributes);

        try {
            StringWriter sw = new StringWriter();

            fileReader = new InputStreamReader(new FileInputStream(sourceFile), "UTF-8");
            // If file ends in filter extension, filter it through Velocity before merging with template.
            if (doFiltering) {
                if (!ve.evaluate(context, sw, "htmlfilter-site", fileReader)) {
                    throw new MojoExecutionException("Unable to evaluate html file " + sourceFile);
                }

                closeReader(fileReader);
                fileReader = new StringReader(sw.toString());
            }

            Document doc = parseXHTMLDocument(fileReader);

            addInfoFromDocument(context, decorationModel, doc, null);

            fileWriter = new FileWriter(targetFile);

            template.merge(context, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Unable to merge Velocity", e);
        } finally {
            closeReader(fileReader);
            closeWriter(fileWriter);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  filename        DOCUMENT ME!
     * @param  doFiltering     DOCUMENT ME!
     * @param  filterExtension DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String makeFinalFilename(String filename, boolean doFiltering, String filterExtension) {
        if (!doFiltering) {
            return filename;
        }

        return filename.substring(0, filename.length() - filterExtension.length()) + ".html";
    }

    /**
     * Add the information from the document to the Velocity context.
     *
     * @param context         the velocity context.
     * @param decorationModel the Doxia decoration model.
     * @param doc             the document.
     * @param createDate      the create date.
     */
    private void addInfoFromDocument(VelocityContext context, DecorationModel decorationModel, Document doc, Date createDate) {
        context.put("authors", getAuthors(doc));

        String title = "";

        if (decorationModel.getName() != null) {
            title = decorationModel.getName();
        } else if (project.getName() != null) {
            title = project.getName();
        }

        if (title.length() > 0) {
            title += " - ";
        }

        title += getTitle(doc);

        context.put("title", title);

        context.put("headContent", getHeadContent(doc));

        context.put("bodyContent", getBodyContent(doc));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        if (createDate != null) {
            context.put("dateCreation", sdf.format(createDate));
        }
    }

    /**
     * Close a Reader ignoring any exception.
     *
     * @param reader the reader to close.
     */
    private void closeReader(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Close a Writer ignoring any exceptions.
     *
     * @param writer the Writer to close.
     */
    private void closeWriter(FileWriter writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Get the relative file path for a file.
     *
     * @param  oldPath the base path.
     * @param  newPath the new path.
     *
     * @return the relative path to the newPath based on the oldPath.
     */
    private String getRelativeFilePath(File oldPath, File newPath) {
        List<String> names = new ArrayList<String>();

        oldPath = oldPath.getAbsoluteFile();
        newPath = newPath.getAbsoluteFile();

        while (newPath != null && !newPath.equals(oldPath)) {
            names.add(newPath.getName());
            newPath = newPath.getParentFile();
        }

        if (newPath == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = names.size() - 1; i >= 0; i--) {
            if (result.length() > 0) {
                result.append('/');
            }

            result.append(names.get(i));
        }

        return result.toString();
    }

    /**
     * Create the velocity context.
     *
     * @param  sourceFile      the source file.
     * @param  sourceDirectory TODO
     * @param  doFiltering     TODO
     * @param  filterExtension TODO
     * @param  decorationModel the Doxia decoration model.
     * @param  attributes      the attributes from the POM and such.
     *
     * @return the velocity context.
     */
    private VelocityContext createContext(File sourceFile, File sourceDirectory, boolean doFiltering, String filterExtension,
            DecorationModel decorationModel, AttributeMap attributes) {
        VelocityContext context      = new VelocityContext();

        // ----------------------------------------------------------------------
        // Data objects
        // ----------------------------------------------------------------------

        String          relativePath = PathTool.getRelativePath(sourceDirectory.getPath(), sourceFile.getPath());

        context.put("relativePath", relativePath);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        context.put("dateRevision", sdf.format(new Date()));

        context.put("decoration", decorationModel);

        context.put("currentDate", new Date());

        Locale locale = Locale.getDefault();

        context.put("dateFormat", DateFormat.getDateInstance(DateFormat.DEFAULT, locale));

        String currentFileName = sourceFile.getName();
        String alignedFileName = PathTool.calculateLink(getRelativeFilePath(sourceDirectory, sourceFile), relativePath);

        currentFileName = makeFinalFilename(currentFileName, doFiltering, filterExtension);
        alignedFileName = makeFinalFilename(alignedFileName, doFiltering, filterExtension);

        context.put("currentFileName", currentFileName);

        context.put("alignedFileName", alignedFileName);

        context.put("locale", locale);

        // Add global properties.
        if (attributes != null) {
            for (Object o : attributes.keySet()) {
                context.put((String) o, attributes.get(o));
            }
        }

        // ----------------------------------------------------------------------
        // Tools
        // ----------------------------------------------------------------------

        context.put("PathTool", new PathTool());

        context.put("FileUtils", new FileUtils());

        context.put("StringUtils", new StringUtils());

        context.put("i18n", i18n);

        return context;
    }

    /**
     * Parse a document from text in the reader.
     *
     * @param  reader the Reader from which to parse the document.
     *
     * @return the JDom document parsed from the XHTML file.
     *
     * @throws IOException   if the reader cannot be read.
     * @throws JDOMException if the reader cannot be parsed.
     */
    private Document parseXHTMLDocument(Reader reader) throws JDOMException, IOException {
        Document document = null;

        SAXBuilder builder = new SAXBuilder();

        builder.setEntityResolver(new DTDHandler());
        builder.setIgnoringElementContentWhitespace(false);
        builder.setIgnoringBoundaryWhitespace(false);
        document = builder.build(reader);

        return document;
    }

    /**
     * Get the document title.
     *
     * @param  document the document.
     *
     * @return the title.
     */
    private String getTitle(Document document) {
        Element element = null;

        element = selectSingleNode(document.getRootElement(), "/xhtml:html/xhtml:head/xhtml:title");
        if (element == null) {
            return null;
        }

        return element.getText();
    }

    /**
     * Get the document authors.
     *
     * @param  document the document.
     *
     * @return a list of authors.
     */
    private List<String> getAuthors(Document document) {
        List<Element> nl   = getXPathList(document, "/xhtml:html/xhtml:head/xhtml:meta[@class='author']");
        List<String>  list = new ArrayList<String>();

        for (Element elem : nl) {
            String author = selectSingleNode(elem, "xhtml:td[@class='author']").getText();

            list.add(author);
        }

        return list;
    }

    /**
     * Extract a list matching an XPath path. This surreptitiously adds the
     * XHTML namespace.
     *
     * @param  document the document.
     * @param  path     the path to select.
     *
     * @return the list of elements matching the path.
     */
    @SuppressWarnings("unchecked")
    protected List<Element> getXPathList(Document document, String path) {
        try {
            XPath xpath = XPath.newInstance(path);

            xpath.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");
            List<Element> nl = (List<Element>) xpath.selectNodes(document);

            return nl;
        } catch (JDOMException e) {
            return new ArrayList<Element>();
        }
    }

    /**
     * Get the body content as a string.
     *
     * @param  document the document.
     *
     * @return the body content.
     */
    private String getBodyContent(Document document) {
        Element element = null;

        element = selectSingleNode(document.getRootElement(), "/xhtml:html/xhtml:body");
        if (element == null) {
            return null;
        }

        return getElementContentsAsText(element);
    }

    /**
     * Get the head content as a string.
     *
     * @param  document the document.
     *
     * @return the head content.
     */
    private String getHeadContent(Document document) {
        Element element = null;

        element = selectSingleNode(document.getRootElement(), "/xhtml:html/xhtml:head");
        if (element == null) {
            return null;
        }

        return getElementContentsAsText(element);
    }

    /**
     * Get the element contents as a String.
     *
     * @param  element the element.
     *
     * @return the element contents.
     */
    private String getElementContentsAsText(Element element) {
        StringBuilder text     = new StringBuilder();
        HTMLOutputter writer   = new HTMLOutputter(Format.getPrettyFormat().setTextMode(Format.TextMode.TRIM_FULL_WHITE)
                                                       .setExpandEmptyElements(true));
        List<Element> children = getChildren(element);

        if (children.size() == 0) {
            return element.getText();
        }

        for (Element child : children) {
            StringWriter sw = new StringWriter();

            try {
                writer.output(child, sw);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            text.append(sw);
        }

        return text.toString();
    }

    /**
     * Wrapper around Element.getChildren() to suppress the type warning.
     *
     * @param  element the element whose children to get.
     *
     * @return a List of Elements representing the children of the specified
     *         element.
     */
    @SuppressWarnings("unchecked")
    private List<Element> getChildren(Element element) {
        return (List<Element>) element.getChildren();
    }

    /**
     * Select a single node using XPath.
     *
     * @param  element the element.
     * @param  path    the path to select.
     *
     * @return the element selected.
     */
    private Element selectSingleNode(Element element, String path) {
        try {
            XPath xpath = XPath.newInstance(path);

            xpath.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");
            return (Element) xpath.selectSingleNode(element);
        } catch (JDOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Simplify references to the attribute hash map.
     */
    private static class AttributeMap extends HashMap<Object, Object> {
        private static final long serialVersionUID = 1787343499009497124L;
    }
}
