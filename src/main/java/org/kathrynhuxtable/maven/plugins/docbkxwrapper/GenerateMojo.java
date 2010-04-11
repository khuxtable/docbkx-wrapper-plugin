package org.kathrynhuxtable.maven.plugins.docbkxwrapper;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Field;

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.i18n.DefaultI18N;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReaderFactory;

import com.agilejava.docbkx.maven.DocbkxXhtmlMojo;

/**
 * Goal which wraps the
 *
 * @description Runs docbkx htmlfilter-site plugins.
 * @goal        generate
 * @phase       pre-site
 */
public class GenerateMojo extends AbstractMojo {

    /**
     * Specifies the input encoding.
     *
     * @parameter expression="${encoding}"
     *            default-value="${project.build.sourceEncoding}"
     */
    private String inputEncoding;

    /**
     * Specifies the output encoding.
     *
     * @parameter expression="${outputEncoding}"
     *            default-value="${project.reporting.outputEncoding}"
     */
    private String outputEncoding;

    /**
     * Match pattern for the files to be processed.
     *
     * @parameter expression="${htmlfiltersite.filePattern}"
     *            default-value="**\/*.xml,**\/*.xml.vm"
     */
    private String filePattern;

    /**
     * Match pattern for the files to be filtered.
     *
     * @parameter expression="${htmlfiltersite.filterExtension}"
     *            default-value=".xml.vm"
     */
    private String filterExtension;

    /**
     * Directory containing the site.xml file and the source for apt, fml and
     * xdoc docs, e.g. ${basedir}/src/site.
     *
     * @parameter expression="${htmlfiltersite.siteDirectory}"
     *            default-value="${basedir}/src/site"
     */
    private File siteDirectory;

    /**
     * Location of the source directory.
     *
     * @parameter expression="${htmlfiltersite.docbookOutputDirectory}"
     *            default-value="${project.build.directory}/docbook-xhtml"
     */
    private File docbookOutputDirectory;

    /**
     * Location of the output directory.
     *
     * @parameter expression="${htmlfiltersite.targetDirectory}"
     *            default-value="${project.build.directory}/site"
     */
    private File targetDirectory;

    /**
     * DOCUMENT ME!
     *
     * @parameter expression="${templateFile}"
     *            default-value="org/kathrynhuxtable/maven/plugins/docbkxwrapper/site.vm"
     */
    private File templateFile;

    /**
     * Project base.
     *
     * @parameter expression="${basedir}"
     * @required
     */
    private File basedir;

    /**
     * Location of generated files.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File projectBuildDirectory;

    /**
     * Remote repositories used for the project.
     *
     * @todo      this is used for site descriptor resolution - it should relate
     *            to the actual project but for some reason they are not always
     *            filled in
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    protected List<?> repositories;

    /**
     * The local repository.
     *
     * @parameter expression="${localRepository}"
     */
    protected ArtifactRepository localRepository;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The reactor projects.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List<?> reactorProjects;

    /**
     * The Doxia SiteTool object.
     *
     * @component
     */
    private SiteTool siteTool;

    /** Plexis internationalization element. */
    private I18N i18n = new DefaultI18N();

    /**
     * Gets the input files encoding.
     *
     * @return The input files encoding, never <code>null</code>.
     */
    protected String getInputEncoding() {
        return (inputEncoding == null) ? ReaderFactory.ISO_8859_1 : inputEncoding;
    }

    /**
     * Set the input encoding.
     *
     * @param inputEncoding the inputEncoding to set
     */
    public void setInputEncoding(String inputEncoding) {
        this.inputEncoding = inputEncoding;
    }

    /**
     * Set the output encoding.
     *
     * @param outputEncoding the outputEncoding to set
     */
    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    /**
     * Gets the effective reporting output files encoding.
     *
     * @return The effective reporting output file encoding, never <code>
     *         null</code>.
     */
    protected String getOutputEncoding() {
        return (outputEncoding == null) ? ReaderFactory.UTF_8 : outputEncoding;
    }

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        generateXhtml();
        mergeFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private void generateXhtml() throws MojoFailureException, MojoExecutionException {
        DocbkxXhtmlMojo docbkxMojo = new DocbkxXhtmlMojo();

        docbkxMojo.setLog(getLog());

        setValue(docbkxMojo, "sourceDirectory", new File(basedir, "src/site/docbook"));
        setValue(docbkxMojo, "includes", filePattern);
        setValue(docbkxMojo, "targetDirectory", docbookOutputDirectory);
        setValue(docbkxMojo, "xhtmlCustomization", "/org/kathrynhuxtable/maven/plugins/docbkxwrapper/xsl/html.xsl");

        setValue(docbkxMojo, "targetFileExtension", "html");
        setValue(docbkxMojo, "imgSrcPath", "./");
        // setValue(docbkxMojo, "chunkedOutput", false);
        setValue(docbkxMojo, "generateMetaAbstract", "false");
        setValue(docbkxMojo, "generateToc", "false");
        setValue(docbkxMojo, "highlightSource", "true");
        setValue(docbkxMojo, "highlightDefaultLanguage", null);
        // setValue(docbkxMojo, "htmlCellSpacing", 2);
        // setValue(docbkxMojo, "htmlCellPadding", 2);
        setValue(docbkxMojo, "suppressHeaderNavigation", "true");
        setValue(docbkxMojo, "suppressFooterNavigation", "true");
        setValue(docbkxMojo, "tableBordersWithCss", "true");
        setValue(docbkxMojo, "tableFrameBorderThickness", "0");
        setValue(docbkxMojo, "tableCellBorderThickness", "0");
        setValue(docbkxMojo, "useExtensions", "true");
        setValue(docbkxMojo, "calloutsExtension", "true");

        docbkxMojo.execute();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  o         DOCUMENT ME!
     * @param  fieldName DOCUMENT ME!
     * @param  value     DOCUMENT ME!
     *
     * @throws MojoFailureException DOCUMENT ME!
     */
    private void setValue(Object o, String fieldName, Object value) throws MojoFailureException {
        Class<?> c     = o.getClass();
        Field    field;

        try {
            field = c.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(o, value);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoFailureException(e.getMessage());
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private void mergeFiles() throws MojoFailureException, MojoExecutionException {
        Merger merger = new Merger();

        merger.initialize(templateFile, localRepository, repositories, i18n, getInputEncoding(), getOutputEncoding(), siteDirectory,
                          project, siteTool, reactorProjects);

        // Convert filter extension into what Docbkx will produce, e.g. foo.xml.vm becomes foo.xm.html
        String localExtension = makeDocbookFilename(filterExtension);

        String localPattern = makeDocbookPattern(filePattern);

        for (String file : getFileList(new File(projectBuildDirectory, "generated-site/docbook-xhtml"), localPattern)) {
            boolean doFiltering = (localExtension != null
                        && localExtension.equals(file.substring(file.length() - localExtension.length())));

            File targetFile = new File(targetDirectory, makeFinalFilename(file, doFiltering, filterExtension));

            merger.mergeFile(file, doFiltering, localExtension, docbookOutputDirectory, targetFile);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  filename TODO
     *
     * @return
     */
    private String makeDocbookFilename(String filename) {
        return filename.replaceFirst("....$", ".html");
    }

    /**
     * DOCUMENT ME!
     *
     * @param  filePattern DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    private String makeDocbookPattern(String filePattern) {
        String[] splits = filePattern.replaceFirst("^ *", "").replaceFirst(" *$", "").split(" *, *");

        StringBuilder result = new StringBuilder();

        for (String element : splits) {
            if (result.length() > 0) {
                result.append(',');
            }

            result.append(makeDocbookFilename(element));
        }

        return result.toString();
    }

    /**
     * Get the list of filenames to merge.
     *
     * @param  sourceDirectory intermediateDirectory DOCUMENT ME!
     * @param  filePattern     DOCUMENT ME!
     *
     * @return a list of filenames to merge.
     */
    @SuppressWarnings("unchecked")
    private List<String> getFileList(File sourceDirectory, String filePattern) {
        List<String> fileList = null;

        try {
            fileList = FileUtils.getFileNames(sourceDirectory, filePattern, "", false, true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return fileList;
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
}
