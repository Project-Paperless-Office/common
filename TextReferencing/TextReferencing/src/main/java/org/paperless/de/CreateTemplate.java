package org.paperless.de;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.paperless.de.parser.PdfString;
import org.paperless.de.parser.TextStripper;

public class CreateTemplate {
	
	private File input, output;
	
	private BufferedWriter out;
	
	private Map<String, TextStripper> texts;
	
	private float xTol, yTol;
	
	private class DocWrapper {
		public PDDocument doc;
		public String filename;

		public DocWrapper(File file) throws IOException {
			this.doc = PDDocument.load(file);
			this.filename = file.getName();
		}
	};
	
	public static void main(String[] args) throws Exception {
		CreateTemplate inst;
		try {
			inst = new CreateTemplate(args);
		} catch (IllegalArgumentException e) {
			System.out.println('\n' + e.getMessage());
			return;
		}
		
		inst.parse();
		
		inst.lookForSimilarities();
		
		inst.finish();
	}

	public CreateTemplate(String[] args) throws IOException, IllegalArgumentException {
		readArgs(args);
		out = new BufferedWriter(new FileWriter(output, true));
	}
	
	public void parse() throws IOException {		
		List <DocWrapper> docs = new ArrayList<DocWrapper>();
		if (input.isDirectory()) {			
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith("pdf");
				}
			};
			
			for (File f : input.listFiles(filter)) {
				docs.add(new DocWrapper(f));
			}
		} else {
			docs.add(new DocWrapper(input));
		}
		
		texts = new HashMap<String, TextStripper>();
		for (DocWrapper doc : docs) {
			System.out.println("Verarbeite " + doc.filename + "...");
			
			TextStripper stripper = new TextStripper();
			stripper.setSortByPosition( true );
            stripper.setStartPage( 1 );
            stripper.setEndPage( doc.doc.getNumberOfPages() );

            Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            stripper.writeText(doc.doc, dummy);
            texts.put(doc.filename, stripper);
		}
	}
	
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
 		BufferedWriter fileWriter = new BufferedWriter(new FileWriter(output));
		XMLStreamWriter xml = XMLOutputFactory.newInstance().createXMLStreamWriter(fileWriter);
		Scanner scanner = new Scanner(System.in);
		
		try {			
			xml.writeStartDocument();
			xml.writeStartElement("template");
			
			for (PdfString att : attributes) {
				System.out.print("Bitte Name für das Attribut mit dem Namen " + att.getText() + " eingeben: ");
				String name = scanner.nextLine();
				
				xml.writeStartElement("attribute");
				
				xml.writeStartElement("name");
				xml.writeCharacters(name);
				xml.writeEndElement();
				
				xml.writeStartElement("x-start");
				xml.writeCharacters("" + att.getFirstX());
				xml.writeEndElement();
				
				xml.writeStartElement("y-start");
				xml.writeCharacters("" + att.getFirstY());
				xml.writeEndElement();
				
				xml.writeStartElement("x-end");
				xml.writeCharacters("" + att.getLastX());
				xml.writeEndElement();
				
				xml.writeStartElement("y-end");
				xml.writeCharacters("" + att.getLastY());
				xml.writeEndElement();
				
				xml.writeEndElement();
			}
			
			xml.writeEndDocument();
		} catch (Exception e) {
			throw new Exception("Fehler beim Schreiben der XML");
		} finally {
			try {
				xml.close();
			} catch (Exception e) {
			}
			try {
				fileWriter.close();
			} catch (Exception e) {
			}
			scanner.close();
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
			}
		}
		
		if (xTol < 0) {
			xTol = 5;
		}
		if (yTol < 0) {
			yTol = 5;
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
				+ "--output result.txt [--tolerance Toleranz | --xTolerance X-Toleranz --yTolerance Y-Toleranz]");
		System.out.println();
		System.out.println("\t--input              \t\tPDF-Eingabeverzeichnis");
		System.out.println("\t--output             \t\tAusgabedatei mit Auswertung");
		System.out.println("\t--tolerance          \t\tSetzt Toleranz für den Vergleich der Textkoordinaten (float)");
		System.out.println("\t--xTolerance         \t\tSetzt Toleranz in X-Richtung (float)");
		System.out.println("\t--yTolerance         \t\tSetzt Toleranz in Y-Richtung (float)");
	}
}
