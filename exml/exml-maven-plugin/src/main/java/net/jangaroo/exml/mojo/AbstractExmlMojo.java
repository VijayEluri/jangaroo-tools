package net.jangaroo.exml.mojo;

import net.jangaroo.exml.ExmlConstants;
import net.jangaroo.exml.ExmlcException;
import net.jangaroo.exml.compiler.Exmlc;
import net.jangaroo.exml.config.ExmlConfiguration;
import net.jangaroo.jooc.JangarooParser;
import net.jangaroo.jooc.mvnplugin.JangarooMojo;
import net.jangaroo.jooc.mvnplugin.util.MavenPluginHelper;
import net.jangaroo.utils.log.Log;
import net.jangaroo.utils.log.LogHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Mojo to invoke the EXML compiler.
 */
public abstract class AbstractExmlMojo extends JangarooMojo {
  /**
   * The Maven project object
   *
   * @parameter expression="${project}"
   */
  private MavenProject project;
  /**
   * The package into which config classes of EXML components are generated.
   *
   * @parameter
   */
  private String configClassPackage;
  /**
   * The XSD Schema that will be generated for this component suite
   *
   * @parameter
   */
  private File[] importedXsds;
  /**
   * @component
   */
  MavenProjectHelper projectHelper;
  /**
   * The XSD Schema that will be generated for this component suite
   *
   * @parameter expression="${project.artifactId}.xsd"
   */
  protected String xsd;
  /**
   * The folder where the XSD Schema for this component suite will be generated
   *
   * @parameter expression="${project.build.directory}/generated-resources"
   */
  protected File generatedResourcesDirectory;
  /**
   * A list of inclusion filters for the compiler.
   *
   * @parameter
   */
  private Set<String> includes = new HashSet<String>();
  /**
   * A list of exclusion filters for the compiler.
   *
   * @parameter
   */
  private Set<String> excludes = new HashSet<String>();
  /**
   * Sets the granularity in milliseconds of the last modification
   * date for testing whether a source needs recompilation.
   *
   * @parameter expression="${lastModGranularityMs}" default-value="0"
   */
  private int staleMillis;

  @Override
  protected MavenProject getProject() {
    return project;
  }

  public abstract String getNamespace();

  public abstract String getNamespacePrefix();

  public abstract String getXsd();

  public abstract File getSourceDirectory();

  public abstract File getGeneratedSourcesDirectory();

  public abstract File getGeneratedResourcesDirectory();

  public File[] getImportedXsds() {
    return importedXsds;
  }

  public void execute() throws MojoExecutionException, MojoFailureException {

    File generatedSourcesDirectory = getGeneratedSourcesDirectory();
    if (!generatedSourcesDirectory.exists()) {
      getLog().info("generating sources into: " + generatedSourcesDirectory.getPath());
      getLog().debug("created " + generatedSourcesDirectory.mkdirs());
    }
    File generatedResourcesDirectory = getGeneratedResourcesDirectory();
    if (!generatedResourcesDirectory.exists()) {
      getLog().info("generating resources into: " + generatedResourcesDirectory.getPath());
      getLog().debug("created " + generatedResourcesDirectory.mkdirs());
    }


    MavenLogHandler errorHandler = new MavenLogHandler();
    Log.setLogHandler(errorHandler);
    ExmlConfiguration exmlConfiguration = new ExmlConfiguration();
    exmlConfiguration.setConfigClassPackage(configClassPackage);
    MavenPluginHelper mavenPluginHelper = new MavenPluginHelper(getProject(), getLog());
    exmlConfiguration.setClassPath(mavenPluginHelper.getActionScriptClassPath());
    exmlConfiguration.setOutputDirectory(generatedSourcesDirectory);
    List<File> sourceRoots = Collections.singletonList(getSourceDirectory());
    try {
      exmlConfiguration.setSourcePath(sourceRoots);
    } catch (IOException e) {
      throw new MojoExecutionException("could not determine source directory", e);
    }
    exmlConfiguration.setSourceFiles(mavenPluginHelper.computeStaleSources(sourceRoots, includes, excludes, generatedSourcesDirectory, ExmlConstants.EXML_SUFFIX, JangarooParser.AS_SUFFIX, staleMillis));


    Exmlc exmlc;
    try {
      exmlc = new Exmlc(exmlConfiguration);
      // Generate all config classes from EXML files:
      exmlc.generateAllConfigClasses();
      exmlc.generateAllComponentClasses();
      // exmlc.generateXsd();
    } catch (ExmlcException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    if (errorHandler.lastException != null) {
      throw new MojoExecutionException(errorHandler.exceptionMsg, errorHandler.lastException);
    }

    StringBuffer errorsMsgs = new StringBuffer();
    for (String msg : errorHandler.errors) {
      errorsMsgs.append(msg);
      errorsMsgs.append("\n");
    }

    if (errorsMsgs.length() != 0) {
      throw new MojoFailureException(errorsMsgs.toString());
    }


    for (String msg : errorHandler.warnings) {
      getLog().warn(msg);
    }


    //generate the XSD for that
//    if (!suite.getComponentClasses().isEmpty()) {
//      Writer out = null;
//      try {
//        try {
//          //generate the XSD for that
//          File xsdFile = new File(generatedResourcesDirectory, getXsd());
//          out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xsdFile), "UTF-8"));
//          new XsdGenerator(suite).generateXsd(out);
//          out.close();
//          projectHelper.attachArtifact(project, "xsd", xsdFile );
//        } finally {
//          if (out != null) {
//            out.close();
//          }
//        }
//      } catch (IOException e) {
//        throw new MojoExecutionException("Error while generating XML schema", e);
//      }
//    }

    getProject().addCompileSourceRoot(generatedSourcesDirectory.getPath());
  }

  class MavenLogHandler implements LogHandler {
    ArrayList<String> errors = new ArrayList<String>();
    ArrayList<String> warnings = new ArrayList<String>();
    Exception lastException;
    String exceptionMsg;
    File currentFile;

    public void setCurrentFile(File file) {
      this.currentFile = file;
    }

    public void error(String message, int lineNumber, int columnNumber) {
      errors.add(String.format("ERROR in %s, line %s, column %s: %s", currentFile, lineNumber, columnNumber, message));
    }

    public void error(String message, Exception exception) {
      this.exceptionMsg = message;
      if (currentFile != null) {
        this.exceptionMsg += String.format(" in file: %s", currentFile);
      }
      this.lastException = exception;
    }

    public void error(String message) {
      errors.add(message);
    }

    public void warning(String message) {
      warnings.add(message);
    }

    public void warning(String message, int lineNumber, int columnNumber) {
      warnings.add(String.format("WARNING in %s, line %s, column %s: %s", currentFile, lineNumber, columnNumber, message));
    }

    public void info(String message) {
      getLog().info(message);
    }

    public void debug(String message) {
      getLog().debug(message);
    }
  }
}