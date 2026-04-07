package com.cat;

/**
 * The Main class.
 *
 * @author Jojokes
 */
public class Main {
	/**
	 * main.
	 *
	 * @param args the args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			new Cat();
		} else {
			boolean nbLine = false;
			boolean nbNonBlank = false;
			boolean showEnds = false;
			boolean showTabs = false;
			boolean showNonPrinting = false;
			boolean squeezeBlank = false;

			for (String arg : args) {
				if (arg.equals("--help")) {
					Cat.printHelp();
					return;
				}
				if (arg.startsWith("--")) {
					switch (arg) {
						case "--number": nbLine = true; break;
						case "--number-nonblank": nbNonBlank = true; break;
						case "--show-ends": showEnds = true; break;
						case "--show-tabs": showTabs = true; break;
						case "--show-nonprinting": showNonPrinting = true; break;
						case "--show-all": showEnds = true; showTabs = true; showNonPrinting = true; break;
						case "--squeeze-blank": squeezeBlank = true; break;
						default:
							System.err.println("cat : option invalide -- '" + arg + "'");
							System.err.println("Saisissez « cat --help » pour plus d'informations.");
							return;
					}
				} else if (arg.startsWith("-") && !arg.equals("-")) {
					for (int i = 1; i < arg.length(); i++) {
						char c = arg.charAt(i);
						switch (c) {
							case 'n': nbLine = true; break;
							case 'b': nbNonBlank = true; break;
							case 'E': showEnds = true; break;
							case 'T': showTabs = true; break;
							case 'v': showNonPrinting = true; break;
							case 'A': showEnds = true; showTabs = true; showNonPrinting = true; break;
							case 'e': showEnds = true; showNonPrinting = true; break;
							case 't': showTabs = true; showNonPrinting = true; break;
							case 's': squeezeBlank = true; break;
							case 'u': break; // ignored (POSIX)
							default:
								System.err.println("cat : option invalide -- '" + c + "'");
								System.err.println("Saisissez « cat --help » pour plus d'informations.");
								return;
						}
					}
				}
			}

			Cat cat = new Cat(nbLine, nbNonBlank, showEnds, showTabs, showNonPrinting, squeezeBlank);
			boolean hasFiles = false;
			for (String arg : args) {
				if (!arg.startsWith("-") || arg.equals("-")) {
					hasFiles = true;
					cat.processArg(arg);
				}
			}
			if (!hasFiles) {
				cat.processArg("-");
			}
		}
	}
}
