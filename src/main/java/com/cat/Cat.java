package com.cat;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * The Cat class.
 *
 * @author Jojokes
 */
public class Cat {
	boolean nbLine;
	int nb = 0;

	/** Creates a new Cat reading from stdin. */
	public Cat() {
		Scanner scanner = new Scanner(System.in);

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (nbLine == true) {
				System.out.print("\t" + nb + "\t");
				nb++;
			}
			System.out.println(line);
		}

		scanner.close();
	}

	/**
	 * Creates a new Cat reading from a file or glob pattern.
	 *
	 * @param arg file path or glob pattern
	 */
	public Cat(String arg) {
		if (arg.equals("-n")) {
			nbLine = true;
		}

		if (arg.equals("-")) {
			new Cat();
			return;
		}

		if (arg.contains("*")) {
			File dir = new File(".");
			String regex = arg.replace(".", "\\.").replace("*", ".*");
			File[] matches = dir.listFiles((d, name) -> name.matches(regex));
			if (matches == null || matches.length == 0) {
				System.err.println("cat: " + arg + ": Aucun fichier ou dossier de ce motif");
				return;
			}
			for (File file : matches) {
				System.out.print(readFile(file.getPath()));
			}
		} else {
			File file = new File(arg);
			if (file.isDirectory()) {
				System.err.println("cat: " + arg + ": Est un dossier");
				return;
			}
			System.out.print(readFile(arg));
		}
	}

	/**
	 * Reads the content of a file.
	 *
	 * @param filename the file path
	 * @return the file content
	 */
	public static String readFile(String filename) {
		File file = new File(filename);
		StringBuilder text = new StringBuilder();

		try (Scanner myReader = new Scanner(file)) {
			while (myReader.hasNextLine()) {
				String data = myReader.nextLine();
				text.append(data).append("\n");
			}
		} catch (FileNotFoundException e) {
			System.err.println("cat: " + filename + ": Aucun fichier ou dossier de ce type");
			return "";
		}
		return text.toString();
	}
}
