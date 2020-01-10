package org.paperless.de;

import java.io.File;
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
import org.paperless.de.util.ProgressListener;

/**
 * Klasse zur Erstellung einer XML-Templatedatei
 * @author nba
 */
public class CreateTemplate {
	
	public interface AttributeSelector {
		public String[] getAttributes(String[] values);
	};
	
	/**
	 * Eingabeverzeichnis mit den PDF-Dateien
	 */
	private File input;
	
	private AttributeSelector selector;
	
	private ProgressListener progress;
	
	/**
	 * XML-Templatedatei
	 */
	private File output;
	
	/**
	 * <p>enthält geparste Texte</p>
	 * <p>SCHLÜSSEL: PDF-Dateiname<br>
	 * WERT: Liste von PDFStrings</p>
	 */
	private Map<String, TextStripper> texts;
	
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
	 * <table summary="Kommandozeilenparameter">
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
		} catch (IllegalArgumentException e) {
			System.out.println('\n' + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Standardkonstruktor, liest Kommandozeilenargumente
	 * @param args  Kommandozeilenparameter
	 * <table summary="Kommandozeilenparameter">
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
		this.selector = new AttributeSelector() {			
			@Override
			public String[] getAttributes(String[] value) {
				String[] ret = new String[value.length];
				for (int i = 0; i < value.length; i++) {					
					Scanner sc = new Scanner(System.in);
					System.out.println("Bitte einen Namen für das Attribut mit dem Wert \"" +
							value[i] + "\" angeben (\"discard\" zum löschen).");
					ret[i] = sc.nextLine();
					if (ret[i].equalsIgnoreCase("discard")) {
						ret[i] = null;
					} else {
						ret[i] = ret[i].trim();
					}
				}
				return ret;
			}
		};
		readArgs(args);
	}
	
	public CreateTemplate(File input, AttributeSelector selector, ProgressListener progress,File output, float xTolerance, float yTolerance) {
		this.input = input;
		this.selector = selector;
		this.output = output;
		this.xTol = xTolerance;
		this.yTol = yTolerance;
		this.progress = progress;
		
		if (input == null || !input.isDirectory()) {
			throw new IllegalArgumentException("Eingabeverzeichnis nicht gefunden");
		}
		if (output == null) {
			throw new IllegalArgumentException("Ausgabedatei darf nicht null sein");
		}
	}
	
	public CreateTemplate(File input, AttributeSelector selector, ProgressListener progress, File output) {
		this(input, selector, progress, output, 5.0f, 5.0f);
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
		
		File[] pdfFileList = input.listFiles(filter);
		for (int i = 0; i < pdfFileList.length; i++) {
			File f = pdfFileList[i];
			if (progress != null) {
				progress.setProgress(i, pdfFileList.length, "Parsing " + f.getName());
			}
			docs.add(new DocWrapper(f));
		}
		if (progress != null) {
			progress.setProgress(pdfFileList.length, pdfFileList.length, "Done");
		}
		
		texts = new HashMap<String, TextStripper>();
		for (int i = 0; i < docs.size(); i++) {
			DocWrapper doc = docs.get(i);
			if (progress != null) {
				progress.setProgress(i, docs.size(), "Parsing " + doc.filename);
			}
			System.out.println("Verarbeite " + doc.filename + "...");
			
			TextStripper stripper = new TextStripper();
			stripper.parse(doc.doc);
			
            texts.put(doc.filename, stripper);
		}
		if (progress != null) {
			progress.setProgress(docs.size(), docs.size(), "Done");
		}
	}
	
	/**
	 * Vergleicht zwei der geparsten PDF-Texte und exportiert die Unterschiede
	 * als Attributliste. Der Nutzer wählt aus, aus welchen unterschiedlichen
	 * Datenfeldern Attribute erstellt werden. Dazu wird eine Liste mit allen
	 * gefundenen möglichen Attributen angezeigt. Aus diesen kann der Nutzer
	 * die gewünschten heraussuchen und sie nach Angabe eines Namens in die
	 * Attributliste einfügen.
	 * @throws Exception
	 * 		Es wurden noch keine 2 PDF-Dateien geparst oder Fehler beim XML-Export
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
	
	/**
	 * Lässt den Nutzer die gewünschten Attribute aus einer Liste mit
	 * unterschiedlichen Textwerten die Attributliste erstellen.
	 * @param attributes
	 * 			Liste mit möglichen Attributen, also unterschiedliche Texte
	 * @throws Exception
	 * 			Fehler beim XML-Export, s. {@link AttributeXMLExporter}
	 */
	private void exportAttributes(List<PdfString> attributes) throws Exception {		
		try (AttributeXMLExporter xml = new AttributeXMLExporter(output);){
			
			String[] attributeValues = new String[attributes.size()];
			for (int i = 0; i < attributes.size(); i++) {
				attributeValues[i] = attributes.get(i).getText();
			}
			String[] attNames = selector.getAttributes(attributeValues);
			for (int i = 0; i < attributes.size() && i < attNames.length; i++) {
				if (attNames[i] != null && !attNames[i].isEmpty()) {
					xml.writeAttribute(attNames[i], attributes.get(i));
				}
			}
			
//			System.out.println("Attribute:\n");
//			for (int i = 0; i < attributes.size(); i++) {
//				System.out.println("[" + (i+1) + "] - " + attributes.get(i).getText());
//			}
//			while(true) {
//				System.out.println("Bitte den Index des nächsten zu speichernden Attributs angeben oder STOP");
//				String next = scanner.nextLine();
//				if (next.equalsIgnoreCase("STOP")) {
//					break;
//				} else {
//					if (next.startsWith("[")) {
//						next = next.substring(1);
//					}
//					if (next.endsWith("]")) {
//						next = next.substring(0, next.length() - 1);
//					}
//					try {
//						int attIndex = Integer.parseInt(next);
//						if (attIndex < 1 || attIndex > attributes.size()) {
//							throw new NumberFormatException("Ungültiges Attribut: [" + attIndex + "] ist kein Index eines gelisteten Attributs.");
//						}
//						System.out.println("Bitte einen Namen für das Attribut eingeben: ");
//						String name = scanner.nextLine();
//						if (name.isEmpty()) {
//							throw new NumberFormatException("Der Attributname darf nicht leer sein.");
//						}
//						if (createdAttributes.contains(name)) {
//							throw new NumberFormatException("Es existiert bereits ein Attribut mit dem Namen " + name);
//						}
//						
//						PdfString att = attributes.get(attIndex - 1);						
//						createdAttributes.add(name);						
//						
//						xml.writeAttribute(name, att);
//					} catch (NumberFormatException e) {
//						System.out.println(e.getLocalizedMessage());
//					}
//				}
//			}
			
		}
	}
	
	/**
	 * Liest Kommandozeilenparameter ein.
	 * @param args
	 * 		Kommandozeilenparameter
	 * <table summary="Kommandozeilenparameter">
	 * <tr><td>--input</td><td>Pfad des PDF-Eingabeordners</td></tr>
	 * <tr><td>--output</td><td>Pfad der XML-Attributdatei, die erstellt werden soll</td></tr>
	 * <tr><td>--tolerance</td><td>Toleranzen in X- und Y-Richtung beim Vergleich der Textpositionen</td></tr>
	 * <tr><td>--xTolerance</td><td>Toleranz in X-Richtung beim Vergleich der Textpositionen</td></tr>
	 * <tr><td>--xTolerance</td><td>Toleranz in Y-Richtung beim Vergleich der Textpositionen</td></tr> 
	 * </table>
	 * @throws IllegalArgumentException
	 * 			ungültiger oder fehlerhaftes Argument
	 */
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
	
	/**
	 * Zeigt Hinweis über korrekte Benutzung in der Standardausgabe an
	 */
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
