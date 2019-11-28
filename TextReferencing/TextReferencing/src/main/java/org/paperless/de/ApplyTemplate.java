package org.paperless.de;

import java.io.File;

public class ApplyTemplate {
	
	File pdf, xml, output;

	public static void main(String[] args) {
		ApplyTemplate inst = new ApplyTemplate();
		inst.printUsage();
	}
	
	private void readArgs(String[] args) throws IllegalArgumentException {
		pdf = null;
		output = null;
		xml = null;
		
		if (args.length < 1) {
			printUsage();
			throw new IllegalArgumentException("ungültige Anzahl Argumente");
		}
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--pdfPath")) {
				if (++i < args.length) {
					pdf = new File(args[i]);
				} else {
					printUsage();
					throw new IllegalArgumentException("Nach --pdfPath muss ein gültiges Verzeichnis angegeben werden.");
				}
			} else if (args[i].equals("--attributes")) {
				if (++i < args.length) {
					xml = new File(args[i]);
				} else {
					printUsage();
					throw new IllegalArgumentException("Nach --attributes muss eine gültige Datei angegeben werden.");
				}
			} else if (args[i].equals("--output")) {
				if (++i < args.length) {
					output = new File(args[i]);
				} else {
					printUsage();
					throw new IllegalArgumentException("Nach --output muss eine gültige Datei angegeben werden.");
				}
			}
		}
		
		if (pdf == null) {
			printUsage();
			throw new IllegalArgumentException("Mit --pdfPath muss ein gültiges Verzeichnis angegeben werden.");
		}
		if (xml == null) {
			printUsage();
			throw new IllegalArgumentException("Mit --attributes muss eine gültige Datei angegeben werden.");
		}
		if (output == null) {
			printUsage();
			throw new IllegalArgumentException("Mit --output muss eine gültige Datei angegeben werden.");
		}
		if (!pdf.isDirectory()) {
			throw new IllegalArgumentException(pdf.getName() + " konnte nicht gelesen werden.");
		}
		if (!xml.canRead()) {
			throw new IllegalArgumentException(xml.getName() + " konnte nicht gelesen werden.");
		}
		if (!output.canWrite() && output.exists()) {
			throw new IllegalArgumentException("Nach " + output.getName() + " kann nicht geschrieben werden.");
		}
	}
	
	private void printUsage() {
		System.out.println("Nutzung: " + this.getClass().getSimpleName() + " --pdfPath PDF-Verzeichnis --attributes attr.xml"
				+ " [--output result.txt]");
		System.out.println();
		System.out.println("\t--pdfPath            \t\tPDF-Eingabeverzeichnis");
		System.out.println("\t--attributes         \t\tXML-Datei mit den Attributen");
		System.out.println("\t--output             \t\tAusgabedatei mit Liste der Attributwerte");
	}
}
