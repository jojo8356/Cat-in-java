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
			for (String arg : args) {
				new Cat(arg);
			}
		}
	}
}
