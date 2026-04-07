package com.cat;

import java.util.Arrays;

/** Entry point for the cat command. */
public class Main {

  /** Parses arguments and runs cat. */
  public static void main(String[] args) {
    if (args.length == 0) {
      new Cat().processArg("-");
      return;
    }

    boolean nbLine = false;
    boolean nbNonBlank = false;
    boolean showEnds = false;
    boolean showTabs = false;
    boolean showNonPrinting = false;
    boolean squeezeBlank = false;

    for (String arg : args) {
      if ("--help".equals(arg)) {
        Cat.printHelp();
        return;
      }
      if (arg.startsWith("--")) {
        switch (arg) {
          case "--number" -> nbLine = true;
          case "--number-nonblank" -> nbNonBlank = true;
          case "--show-ends" -> showEnds = true;
          case "--show-tabs" -> showTabs = true;
          case "--show-nonprinting" -> showNonPrinting = true;
          case "--show-all" -> {
            showEnds = true;
            showTabs = true;
            showNonPrinting = true;
          }
          case "--squeeze-blank" -> squeezeBlank = true;
          default -> {
            System.err.println(
                "cat : option invalide -- '" + arg + "'");
            System.err.println(
                "Saisissez << cat --help >>"
                    + " pour plus d'informations.");
            return;
          }
        }
      } else if (arg.startsWith("-") && !"-".equals(arg)) {
        for (int i = 1; i < arg.length(); i++) {
          char c = arg.charAt(i);
          switch (c) {
            case 'n' -> nbLine = true;
            case 'b' -> nbNonBlank = true;
            case 'E' -> showEnds = true;
            case 'T' -> showTabs = true;
            case 'v' -> showNonPrinting = true;
            case 'A' -> {
              showEnds = true;
              showTabs = true;
              showNonPrinting = true;
            }
            case 'e' -> {
              showEnds = true;
              showNonPrinting = true;
            }
            case 't' -> {
              showTabs = true;
              showNonPrinting = true;
            }
            case 's' -> squeezeBlank = true;
            case 'u' -> {} // ignored (POSIX)
            default -> {
              System.err.println(
                  "cat : option invalide -- '" + c + "'");
              System.err.println(
                  "Saisissez << cat --help >>"
                      + " pour plus d'informations.");
              return;
            }
          }
        }
      }
    }

    Cat cat = new Cat(
        nbLine, nbNonBlank, showEnds,
        showTabs, showNonPrinting, squeezeBlank);
    String[] files = Arrays.stream(args)
        .filter(arg -> !arg.startsWith("-") || "-".equals(arg))
        .toArray(String[]::new);
    if (files.length == 0) {
      cat.processArg("-");
    } else {
      Arrays.stream(files).forEach(cat::processArg);
    }
  }
}
