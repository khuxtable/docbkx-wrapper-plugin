package org.kathrynhuxtable.maven.plugins.docbkxwrapper;

import java.io.File;
import java.io.IOException;

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

/**
 * Goal which wraps the
 *
 * @description                  Runs docbkx htmlfilter-site plugins.
 * @goal                         generate
 * @phase                        pre-site
 * @requiresDependencyResolution runtime
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
     * @parameter expression="${docbkxwrapper.filePattern}"
     *            default-value="**\/*.xml,**\/*.xml.vm"
     */
    private String filePattern;

    /**
     * Match pattern for the files to be filtered.
     *
     * @parameter expression="${docbkxwrapper.filterExtension}"
     *            default-value=".xml.vm"
     */
    private String filterExtension;

    /**
     * Directory containing the site.xml file and the source for apt, fml and
     * xdoc docs, e.g. ${basedir}/src/site.
     *
     * @parameter expression="${docbkxwrapper.siteDirectory}"
     *            default-value="${basedir}/src/site"
     */
    private File siteDirectory;

    /**
     * Location of the source directory.
     *
     * @parameter expression="${docbkxwrapper.docbookOutputDirectory}"
     *            default-value="${project.build.directory}/docbook-xhtml"
     */
    private File docbookOutputDirectory;

    /**
     * Location of the output directory.
     *
     * @parameter expression="${docbkxwrapper.targetDirectory}"
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

    private RunDocbkxPlugin docbkxPlugin = new RunDocbkxPlugin();
    private Merger          merger       = new Merger();

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
        docbkxPlugin.generateXhtml(getLog(), new File(siteDirectory, "docbook"), filePattern, docbookOutputDirectory);
        mergeFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private void mergeFiles() throws MojoFailureException, MojoExecutionException {
        merger.initialize(templateFile, localRepository, repositories, i18n, getInputEncoding(), getOutputEncoding(), siteDirectory,
                          project, siteTool, reactorProjects);

        // Convert filter extension into what Docbkx will produce, e.g. .xml.vm becomes .xm.html
        String filterExtension = makeDocbookFilename(this.filterExtension);

        for (String file : getFileList(docbookOutputDirectory, makeDocbookPattern(filePattern))) {
            boolean doFiltering = (filterExtension != null
                        && filterExtension.equals(file.substring(file.length() - filterExtension.length())));

            File targetFile = new File(targetDirectory, makeFinalFilename(file, doFiltering, filterExtension));

            merger.mergeFile(file, doFiltering, filterExtension, docbookOutputDirectory, targetFile);
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
