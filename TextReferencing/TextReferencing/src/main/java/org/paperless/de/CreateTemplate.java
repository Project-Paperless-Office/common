package org.paperless.de;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.paperless.de.parser.PdfString;
import org.paperless.de.parser.TextStripper;
import org.paperless.de.util.AttributeXMLExporter;

/**
 * Klasse zur Erstellung einer XML-Templatedatei
 * @author nba
 */
public class CreateTemplate {
	
	/**
	 * Eingabeverzeichnis mit den PDF-Dateien
	 */
	private File input;
	
	/**
	 * XML-Templatedatei
	 */
	private File output;
	
	/**
	 * Writer in die Templatedatei
	 */
	private BufferedWriter out;
	
	/**
	 * <p>enthält geparste Texte</p>
	 * <p>SCHLÜSSEL: PDF-Dateiname<br>
	 * WERT: Liste von PDFStrings</p>
	 */
	private Map<String, TextStripper> texts;
	
	/**
	 * Liste mit bereits verwendeten Attributnamen, damit keiner doppelt
	 * verwendet wird
	 */
	private List<String> createdAttributes = new ArrayList<String>();
	
	/**
	 * Toleranz beim Textvergleich in X-Richtung
	 */
	private float xTol;
	
	/**
	 * Toleranz beim Textvergleich in Y-Richtung
	 */
	private float yTol;
	
	/**
	 * Wrapper-Klasse für ein PDF-Dokument. Speichert Dateiname und
	 * geparstes Dokument
	 */
	private class DocWrapper {
		/**
		 * mit PDFBox geparstes Dokument
		 */
		public PDDocument doc;
		
		/**
		 * Dateiname des Dokuments
		 */
		public String filename;

		/**
		 * Konstruktor zum Parsen einer PDF-Datei
		 * @param file
		 * 			PDF-Datei
		 * @throws IOException
		 * 			Fehler beim Lesen der Datei
		 */
		public DocWrapper(File file) throws IOException {
			this.doc = PDDocument.load(file);
			this.filename = file.getName();
		}
	};
	
	/**
	 * Hauptmethode
	 * @param args Kommandozeilenparameter
	 * <table>
	 * <tr><td>--input</td><td>Pfad des PDF-Eingabeordners</td></tr>
	 * <tr><td>--output</td><td>Pfad der XML-Attributdatei, die erstellt werden soll</td></tr>
	 * <tr><td>--tolerance</td><td>Toleranzen in X- und Y-Richtung beim Vergleich der Textpositionen</td></tr>
	 * <tr><td>--xTolerance</td><td>Toleranz in X-Richtung beim Vergleich der Textpositionen</td></tr>
	 * <tr><td>--xTolerance</td><td>Toleranz in Y-Richtung beim Vergleich der Textpositionen</td></tr> 
	 * </table>
	 */
	public static void main(String[] args) {
		CreateTemplate inst;
		try {
			inst = new CreateTemplate(args);
			
			inst.parse();
			
			inst.lookForSimilarities();
			
			inst.finish();
		} catch (IllegalArgumentException e) {
			System.out.println('\n' + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Standardkonstruktor, liest Kommandozeilenargumente
	 * @param args  Kommandozeilenparameter
	 * <table>
	 * <tr><td>--input</td><td>Pfad des PDF-Eingabeordners</td></tr>
	 * <tr><td>--output</td><td>Pfad der XML-Attributdatei, die erstellt werden soll</td></tr>
	 * <tr><td>--tolerance</td><td>Toleranzen in X- und Y-Richtung beim Vergleich der Textpositionen</td></tr>
	 * <tr><td>--xTolerance</td><td>Toleranz in X-Richtung beim Vergleich der Textpositionen</td></tr>
	 * <tr><td>--xTolerance</td><td>Toleranz in Y-Richtung beim Vergleich der Textpositionen</td></tr> 
	 * </table>
	 * @throws IOException
	 * 			Fehler beim Erstellen der Ausgabedatei
	 * @throws IllegalArgumentException
	 * 			Ungültige Kommandozeilenparameter
	 */
	public CreateTemplate(String[] args) throws IOException, IllegalArgumentException {
		readArgs(args);
		out = new BufferedWriter(new FileWriter(output, true));
	}
	
	/**
	 * Parst alle PDF-Dateien im Eingabeordner
	 * @throws IOException
	 * 			Fehler beim Lesen der Datei
	 */
	public void parse() throws IOException {		
		List <DocWrapper> docs = new ArrayList<DocWrapper>();
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith("pdf");
			}
		};
		
		for (File f : input.listFiles(filter)) {
			docs.add(new DocWrapper(f));
		}
		
		texts = new HashMap<String, TextStripper>();
		for (DocWrapper doc : docs) {
			System.out.println("Verarbeite " + doc.filename + "...");
			
			TextStripper stripper = new TextStripper();
			stripper.parse(doc.doc);
			
            texts.put(doc.filename, stripper);
		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void lookForSimilarities() throws Exception {
		if (texts.size() < 2) {
			throw new IllegalArgumentException("Benötigt min. 2 Dateien zum Vergleich");
		}
		
		List<PdfString> compare = null;
		TextStripper first = null;
		String firstName = null;
		for (String file : texts.keySet()) {
			if (first == null) {
				first = texts.get(file);
				firstName = file;
			} else {
				System.out.println("Vergleiche " + firstName + " und " + file + "...");
				compare = first.compare(texts.get(file), xTol, yTol);
				break;
			}
		}
		for (PdfString att : compare) {
			System.out.println();
			System.out.println(att.getText() + ":\t" + att.getFirstX() + ',' + att.getFirstY() + ',' + att.getLastX() + ',' + att.getLastY());
		}
		
		exportAttributes(compare);
	}
	
	public void finish() throws IOException {
		out.close();
	}
	
	private void exportAttributes(List<PdfString> attributes) throws Exception {		
		try (AttributeXMLExporter xml = new AttributeXMLExporter(output);
				Scanner scanner = new Scanner(System.in);){
			
			System.out.println("Attribute:\n");
			for (int i = 0; i < attributes.size(); i++) {
				System.out.println("[" + (i+1) + "] - " + attributes.get(i).getText());
			}
			while(true) {
				System.out.println("Bitte den Index des nächsten zu speichernden Attributs angeben oder STOP");
				String next = scanner.nextLine();
				if (next.equalsIgnoreCase("STOP")) {
					break;
				} else {
					if (next.startsWith("[")) {
						next = next.substring(1);
					}
					if (next.endsWith("]")) {
						next = next.substring(0, next.length() - 1);
					}
					try {
						int attIndex = Integer.parseInt(next);
						if (attIndex < 1 || attIndex > attributes.size()) {
							throw new NumberFormatException("Ungültiges Attribut: [" + attIndex + "] ist kein Index eines gelisteten Attributs.");
						}
						System.out.println("Bitte einen Namen für das Attribut eingeben: ");
						String name = scanner.nextLine();
						if (name.isEmpty()) {
							throw new NumberFormatException("Der Attributname darf nicht leer sein.");
						}
						if (createdAttributes.contains(name)) {
							throw new NumberFormatException("Es existiert bereits ein Attribut mit dem Namen " + name);
						}
						
						PdfString att = attributes.get(attIndex - 1);						
						createdAttributes.add(name);						
						
						xml.writeAttribute(name, att);
					} catch (NumberFormatException e) {
						System.out.println(e.getLocalizedMessage());
					}
				}
			}
			
		}
	}
	
	private void readArgs(String[] args) throws IllegalArgumentException {
		input = null;
		output = null;
		xTol = -1;
		yTol = -1;
		
		if (args.length < 1) {
			printUsage();
			throw new IllegalArgumentException("ungültige Anzahl Argumente");
		}
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--input")) {
				if (++i < args.length) {
					input = new File(args[i]);
				} else {
					printUsage();
					throw new IllegalArgumentException("Nach --input muss ein gültiges Verzeichnis angegeben werden.");
				}
			} else if (args[i].equals("--output")) {
				if (++i < args.length) {
					output = new File(args[i]);
				} else {
					printUsage();
					throw new IllegalArgumentException("Nach --output muss eine gültige Datei angegeben werden.");
				}
			} else if (args[i].equals("--tolerance")) {
				if (xTol > 0 || yTol > 0) {
					printUsage();
					throw new IllegalArgumentException("Bei Angabe von --tolerance darf keine Einzeltoleranz mit "
							+ "--xTolerance oder --yTolerance angegeben werden.");
				}
				if (++i < args.length) {
					try {
						xTol = Float.parseFloat(args[i]);
						yTol = xTol;
					} catch (NumberFormatException e) {
						printUsage();
						throw new IllegalArgumentException("Nach --tolerance muss eine gültige Gleitkommazahl angegeben werden.");
					}
					if (xTol < 0) {
						printUsage();
						throw new IllegalArgumentException("Die Toleranz darf nicht negativ sein.");
					}
				} else {
					printUsage();
					throw new IllegalArgumentException("Nach --tolerance muss eine Gleitkommazahl Datei angegeben werden.");
				}
			} else if (args[i].equals("--xTolerance")) {
				if (xTol > 0) {
					printUsage();
					throw new IllegalArgumentException("Bei Angabe von --tolerance darf keine Einzeltoleranz mit "
							+ "--xTolerance oder --yTolerance angegeben werden.");
				}
				if (++i < args.length) {
					try {
						xTol = Float.parseFloat(args[i]);
					} catch (NumberFormatException e) {
						printUsage();
						throw new IllegalArgumentException("Nach --xTolerance muss eine gültige Gleitkommazahl angegeben werden.");
					}
					if (xTol < 0) {
						printUsage();
						throw new IllegalArgumentException("Die X-Toleranz darf nicht negativ sein.");
					}
				} else {
					printUsage();
					throw new IllegalArgumentException("Nach --xTolerance muss eine Gleitkommazahl Datei angegeben werden.");
				}
			} else if (args[i].equals("--yTolerance")) {
				if (yTol > 0) {
					printUsage();
					throw new IllegalArgumentException("Bei Angabe von --tolerance darf keine Einzeltoleranz mit "
							+ "--xTolerance oder --yTolerance angegeben werden.");
				}
				if (++i < args.length) {
					try {
						yTol = Float.parseFloat(args[i]);
					} catch (NumberFormatException e) {
						printUsage();
						throw new IllegalArgumentException("Nach --yTolerance muss eine gültige Gleitkommazahl angegeben werden.");
					}
					if (yTol < 0) {
						printUsage();
						throw new IllegalArgumentException("Die Y-Toleranz darf nicht negativ sein.");
					}
				} else {
					printUsage();
					throw new IllegalArgumentException("Nach --yTolerance muss eine Gleitkommazahl Datei angegeben werden.");
				}
			}
		}
		
		if (xTol < 0) {
			xTol = 3;
		}
		if (yTol < 0) {
			yTol = 3;
		}
		
		if (input == null) {
			printUsage();
			throw new IllegalArgumentException("Mit --input muss ein gültiges Verzeichnis angegeben werden.");
		}
		if (output == null) {
			printUsage();
			throw new IllegalArgumentException("Mit --output muss eine gültige Datei angegeben werden.");
		}
		if (!input.isDirectory()) {
			throw new IllegalArgumentException(input.getName() + " konnte nicht gelesen werden.");
		}
	}
	
	private void printUsage() {
		System.out.println("Nutzung: " + this.getClass().getSimpleName() + " --input dateipfad"
				+ "--output attr.xml [--tolerance Toleranz | --xTolerance X-Toleranz --yTolerance Y-Toleranz]");
		System.out.println();
		System.out.println("\t--input              \t\tPDF-Eingabeverzeichnis");
		System.out.println("\t--output             \t\tXML-Attributdate");
		System.out.println("\t--tolerance          \t\tSetzt Toleranz für den Vergleich der Textkoordinaten (float)");
		System.out.println("\t--xTolerance         \t\tSetzt Toleranz in X-Richtung (float)");
		System.out.println("\t--yTolerance         \t\tSetzt Toleranz in Y-Richtung (float)");
	}
}
