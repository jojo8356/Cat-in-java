package com.cat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * The Cat class.
 *
 * @author Jojokes
 */
public class Cat {
  private boolean nbLine;
  private boolean nbNonBlank;
  private boolean showEnds;
  private boolean showTabs;
  private boolean showNonPrinting;
  private boolean squeezeBlank;
  private int line = 0;
  private int consecutiveBlanks = 0;

  /** Creates a new Cat with default options. */
  public Cat() {}

  /** Creates a new Cat with options. */
  public Cat(boolean nbLine, boolean nbNonBlank, boolean showEnds,
      boolean showTabs, boolean showNonPrinting, boolean squeezeBlank) {
    this.nbLine = nbLine;
    this.nbNonBlank = nbNonBlank;
    this.showEnds = showEnds;
    this.showTabs = showTabs;
    this.showNonPrinting = showNonPrinting;
    this.squeezeBlank = squeezeBlank;
    // -b overrides -n
    if (nbNonBlank) {
      this.nbLine = false;
    }
  }

  /**
   * Processes a single argument (file path, glob, or stdin).
   *
   * @param arg file path, glob pattern, or "-" for stdin
   */
  public void processArg(String arg) {
    if ("-".equals(arg)) {
      readStream(System.in);
      return;
    }

    if (arg.contains("*")) {
      findFilesRegex(arg).stream()
          .map(File::getPath)
          .forEach(this::readFile);
    } else {
      readFile(arg);
    }
  }

  /** Finds files matching a glob pattern in the current directory. */
  public List<File> findFilesRegex(String initRegex) {
    File dir = new File(".");
    String regex = initRegex.replace(".", "\\.")
        .replace("*", ".*");
    File[] matches = dir.listFiles(
        (d, name) -> name.matches(regex));
    if (matches == null || matches.length == 0) {
      System.err.println(
          "cat: " + initRegex
              + ": Aucun fichier ou dossier de ce motif");
      return List.of();
    }
    return Arrays.asList(matches);
  }

  /** Reads and outputs the contents of a file. */
  public void readFile(String filename) {
    File file = new File(filename);
    if (file.isDirectory()) {
      System.err.println("cat: " + filename + ": Is a directory");
      return;
    }
    try (FileInputStream fis = new FileInputStream(file)) {
      readStream(fis);
    } catch (FileNotFoundException e) {
      System.err.println(
          "cat: " + filename + ": No such file or directory");
    } catch (IOException e) {
      System.err.println(
          "cat: " + filename + ": " + e.getMessage());
    }
  }

  /** Prints usage help to stdout. */
  public static void printHelp() {
    try (var reader = new BufferedReader(new InputStreamReader(
        Cat.class.getResourceAsStream("/help.txt")))) {
      reader.lines().forEach(System.out::println);
    } catch (IOException e) {
      System.err.println(
          "cat: cannot read help: " + e.getMessage());
    }
  }

  private void readStream(InputStream input) {
    try {
      OutputStream out = System.out;
      ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
      int b;
      while ((b = input.read()) != -1) {
        if (b == '\n') {
          boolean isBlank = (lineBuffer.size() == 0);

          // Squeeze blank lines
          if (squeezeBlank && isBlank) {
            consecutiveBlanks++;
            if (consecutiveBlanks > 1) {
              lineBuffer.reset();
              continue;
            }
          } else {
            consecutiveBlanks = 0;
          }

          // Line numbering
          if (nbLine || (nbNonBlank && !isBlank)) {
            line++;
            out.write(
                String.format("%6d\t", line).getBytes());
          }

          // Write buffered line content
          out.write(lineBuffer.toByteArray());
          lineBuffer.reset();

          if (showEnds) {
            out.write('$');
          }
          out.write('\n');
        } else {
          // Process the byte and add to line buffer
          if (b == '\t') {
            if (showTabs) {
              lineBuffer.write('^');
              lineBuffer.write('I');
            } else {
              lineBuffer.write(b);
            }
          } else if (b == '\r') {
            if (showNonPrinting) {
              lineBuffer.write('^');
              lineBuffer.write('M');
            } else {
              lineBuffer.write(b);
            }
          } else if (showNonPrinting
              && b != '\n' && b != '\t') {
            int unsigned = b & 0xFF;
            if (unsigned < 32) {
              lineBuffer.write('^');
              lineBuffer.write(unsigned + 64);
            } else if (unsigned == 127) {
              lineBuffer.write('^');
              lineBuffer.write('?');
            } else if (unsigned > 127 && unsigned < 160) {
              lineBuffer.write('M');
              lineBuffer.write('-');
              lineBuffer.write('^');
              lineBuffer.write(unsigned - 128 + 64);
            } else if (unsigned >= 160 && unsigned < 255) {
              lineBuffer.write('M');
              lineBuffer.write('-');
              lineBuffer.write(unsigned - 128);
            } else if (unsigned == 255) {
              lineBuffer.write('M');
              lineBuffer.write('-');
              lineBuffer.write('^');
              lineBuffer.write('?');
            } else {
              lineBuffer.write(b);
            }
          } else {
            lineBuffer.write(b);
          }
        }
      }
      // Flush remaining content (no trailing newline)
      if (lineBuffer.size() > 0) {
        if (nbLine || nbNonBlank) {
          line++;
          out.write(
              String.format("%6d\t", line).getBytes());
        }
        out.write(lineBuffer.toByteArray());
        lineBuffer.reset();
      }
      out.flush();
    } catch (IOException e) {
      System.err.println("cat: " + e.getMessage());
    }
  }
}
