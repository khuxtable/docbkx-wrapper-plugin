package org.kathrynhuxtable.maven.plugins.docbkxwrapper;

import java.io.File;

import java.lang.reflect.Field;

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.agilejava.docbkx.maven.DocbkxXhtmlMojo;

import org.kathrynhuxtable.maven.plugins.htmlfiltersite.MergeMojo;

/**
 * Goal which wraps the
 *
 * @description Runs docbkx htmlfilter-site plugins.
 * @goal        generate
 * @phase       pre-site
 */
public class GenerateMojo extends AbstractMojo {

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
        setValue(docbkxMojo, "includes", "**/*.xml,**/*.xml.vm");
        setValue(docbkxMojo, "targetDirectory", new File(projectBuildDirectory, "generated-site/docbook-xhtml"));
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
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private void mergeFiles() throws MojoFailureException, MojoExecutionException {
        MergeMojo mergeMojo = new MergeMojo();

        mergeMojo.setLog(getLog());

        setValue(mergeMojo, "sourceDirectory", new File(projectBuildDirectory, "generated-site/docbook-xhtml"));
        setValue(mergeMojo, "filePattern", "**/*.html,**/*.xm.html");
        setValue(mergeMojo, "filterExtension", ".xm.html");
        setValue(mergeMojo, "targetDirectory", new File(projectBuildDirectory, "generated-site/resources"));
        setValue(mergeMojo, "templateFile", templateFile);
        setValue(mergeMojo, "siteDirectory", new File(basedir, "src/site"));

        setValue(mergeMojo, "repositories", repositories);
        setValue(mergeMojo, "localRepository", localRepository);
        setValue(mergeMojo, "project", project);
        setValue(mergeMojo, "reactorProjects", reactorProjects);
        setValue(mergeMojo, "siteTool", siteTool);

        mergeMojo.execute();
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
}
