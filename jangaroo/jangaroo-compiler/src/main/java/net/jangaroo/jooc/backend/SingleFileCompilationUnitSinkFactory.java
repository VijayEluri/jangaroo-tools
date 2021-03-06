package net.jangaroo.jooc.backend;

import net.jangaroo.jooc.Jooc;
import net.jangaroo.jooc.JsWriter;
import net.jangaroo.jooc.ast.CompilationUnit;
import net.jangaroo.jooc.ast.IdeDeclaration;
import net.jangaroo.jooc.ast.PackageDeclaration;
import net.jangaroo.jooc.config.JoocOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Compilation unit sink factory for one compilation unit per output file.
 */
public class SingleFileCompilationUnitSinkFactory extends AbstractCompilationUnitSinkFactory {

  private String suffix;
  private boolean generateApi;

  public SingleFileCompilationUnitSinkFactory(JoocOptions options, File destinationDir, boolean generateApi, String suffix) {
    super(options, destinationDir);
    this.suffix = suffix;
    this.generateApi = generateApi;
  }

  protected File getOutputFile(File sourceFile, String[] packageName) {
    return new File(getOutputFileName(sourceFile, packageName));
  }

  protected String getOutputFileName(File sourceFile, String[] packageName) {
    String result;
    if (getOutputDir() == null) {
      result = sourceFile.getAbsoluteFile().getParentFile().getAbsolutePath();
    } else {
      result = getOutputDir().getAbsolutePath();
      StringBuilder buffy = new StringBuilder(result);
      for (String aPackageName : packageName) {
        buffy.append(File.separator);
        buffy.append(aPackageName);
      }
      result = buffy.toString();
    }
    result += File.separator;
    result += sourceFile.getName();
    int dotpos = result.lastIndexOf('.');
    if (dotpos >= 0) {
      result = result.substring(0, dotpos);
    }
    result += suffix;
    return result;
  }

  public CompilationUnitSink createSink(PackageDeclaration packageDeclaration,
                                        IdeDeclaration primaryDeclaration, File sourceFile,
                                        final boolean verbose) {
    final File outFile = getOutputFile(sourceFile, packageDeclaration.getQualifiedName());
    String fileName = outFile.getName();
    String classPart = fileName.substring(0, fileName.lastIndexOf('.'));

    String className = primaryDeclaration.getName();
    if (!classPart.equals(className)) {
      Jooc.warning(primaryDeclaration.getSymbol(),
              "class name should be equal to file name: expected " + classPart + ", found " + className);
    }
    createOutputDirs(outFile);

    return new CompilationUnitSink() {
      public File writeOutput(CompilationUnit compilationUnit) {
        if (verbose) {
          System.out.println("writing file: '" + outFile.getAbsolutePath() + "'"); // NOSONAR this is a cmd line tool
        }

        try {
          OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
          try {
            if (generateApi) {
              ApiModelGenerator apiModelGenerator = new ApiModelGenerator(isExcludeClassByDefault(getOptions()));
              apiModelGenerator.generateModel(compilationUnit).visit(new ActionScriptCodeGeneratingModelVisitor(writer));
            } else {
              JsWriter out = new JsWriter(writer);
              try {
                out.setOptions(getOptions());
                compilationUnit.visit(new JsCodeGenerator(out));
              } finally {
                out.close();
              }
            }
          } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            outFile.delete(); // NOSONAR
            throw Jooc.error("error writing file: '" + outFile.getAbsolutePath() + "'", outFile, e);
          }
        } catch (IOException e) {
          throw Jooc.error("cannot open output file for writing: '" + outFile.getAbsolutePath() + "'", outFile, e);
        }

        return outFile;
      }
    };
  }

  private static boolean isExcludeClassByDefault(JoocOptions options) {
    try {
      return options.isExcludeClassByDefault();
    } catch (IncompatibleClassChangeError e) {
      // ignore, old front ends did not know that you can exclude classes by default
      return false;
    }
    
  }
}
