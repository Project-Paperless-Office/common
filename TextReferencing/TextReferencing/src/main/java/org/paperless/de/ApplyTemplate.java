package org.paperless.de;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.paperless.de.parser.TextStripper;
import org.paperless.de.util.Attribute;
import org.paperless.de.util.ProgressListener;
import org.paperless.de.util.ValueCsvExporter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Klasse zur Anwendung eines Templates auf eine Menge PDF-Dokumente
 * 
 * @author nba
 */
public class ApplyTemplate {
	
	/**
	 * Interface, an das die gefundenen Werte aus den Dateien weitergegeben
	 * werden.
	 * 
	 * @author nba
	 */
	public interface OutputLister {

		/**
		 * Methode zum Erhalt von Werten aus der Datei
		 * @param fileName
		 * 			Name der Datei
		 * @param attValues
		 * 			Name und Wert der Attribute
		 */
		void getFileValues(String fileName, Map<String, String> attValues);

		void close() throws IOException;
	}

	/**
	 * PDF-Eingabeordner
	 */
	private File pdf;
	
	/**
	 * XML-Attributtemplate
	 */
	private File xml;
	
	/**
	 * Ausgabeklasse
	 */
	private OutputLister output;
	
	private ProgressListener progress;
	
	/**
	 * Toleranzen in X- und Y-Richtung. Derzeit werden nur X-Start und Y-Start verglichen.
	 */
	private float xTol, yTol;
	
	/**
	 * Liste der geparsten Attribute aus {@link #xml dem Template}.
	 */
	private List<Attribute> attrList;
	
	/**
	 * Hauptmethode
	 * @param args
	 * <table summary="Kommandozeilenparameter">
	 * <tr><td>--pdfPath</td><td>Pfad des PDF-Eingabeordners</td></tr>
	 * <tr><td>--attributes</td><td>Pfad der XML-Attributdatei</td></tr>
	 * <tr><td>--output</td><td>Pfad der CSV-Ausgabedatei</td></tr>
	 * <tr><td>--tolerance</td><td>Toleranzen in X- und Y-Richtung bei der Anwendung der Attribute</td></tr>
	 * <tr><td>--xTolerance</td><td>Toleranz in X-Richtung bei der Anwendung der Attribute</td></tr>
	 * <tr><td>--yTolerance</td><td>Toleranz in Y-Richtung bei der Anwendung der Attribute</td></tr>
	 * </table>
	 */
	public static void main(String[] args) {
		try {
			ApplyTemplate inst = new ApplyTemplate(args);
			inst.readAttributes();
			inst.applyAttributes();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Konstruktor, liest Kommadozeilenparameter
	 * @param args
	 * <table summary="Kommandozeilenparameter">
	 * 	<tr><td>--pdfPath</td><td>Pfad des PDF-Eingabeordners</td></tr>
	 * 	<tr><td>--attributes</td><td>Pfad der XML-Attributdatei</td></tr>
	 * 	<tr><td>--output</td><td>Pfad der CSV-Ausgabedatei</td></tr>
	 * 	<tr><td>--tolerance</td><td>Toleranzen in X- und Y-Richtung bei der Anwendung der Attribute</td></tr>
	 * 	<tr><td>--xTolerance</td><td>Toleranz in X-Richtung bei der Anwendung der Attribute</td></tr>
	 * 	<tr><td>--yTolerance</td><td>Toleranz in Y-Richtung bei der Anwendung der Attribute</td></tr>
	 * </table>
	 */
	public ApplyTemplate(String[] args) {
		readArgs(args);
	}
	
	public ApplyTemplate(File pdfDir, File xmlAttributes, OutputLister csvOutput,
			ProgressListener progress, float xTolerance, float yTolerance) {
		
		this.pdf = pdfDir;
		this.xml = xmlAttributes;
		this.output = csvOutput;
		this.xTol = xTolerance;
		this.yTol = yTolerance;
		this.progress = progress;
		
		if (pdf == null || !pdf.isDirectory()) {
			throw new IllegalArgumentException("Kein gültiges PDF-Verzeichnis angegeben.");
		}
		if (xml == null || !xml.isFile()) {
			throw new IllegalArgumentException("Keine gültige Attributdatei angegeben.");
		}
		if (xTol < 0 || yTol < 0) {
			throw new IllegalArgumentException("Die Toleranz darf nicht negativ sein.");
		}
	}
	
	public ApplyTemplate(File pdfDir, File xmlAttributes, OutputLister csvOutput, ProgressListener progress) {
		this(pdfDir, xmlAttributes, csvOutput, progress, 5.0f, 5.0f);
	}
	
	/**
	 * liest die Attribute aus der {@link #xml Templatedatei}
	 * @throws ParserConfigurationException
	 * 			Fehler beim Parsen der XML
	 * @throws SAXException
	 * 			Fehler beim Parsen der XML
	 * @throws IOException
	 * 			Fehler beim Lesen der XML-Datei
	 */
	public void readAttributes() throws ParserConfigurationException, SAXException, IOException {		
		attrList = new ArrayList<>();
		//Parsen der XML
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xml);
		//doc.getDocumentElement().normalize();
		
		NodeList attrNodeList = doc.getElementsByTagName("attribute");
		for (int i = 0; i < attrNodeList.getLength(); i++) {
			//Lesen aller Attribute
			attrList.add(readAttribute(attrNodeList.item(i)));
		}
	}
	
	/**
	 * Vergleicht Attribute der Dateien mit der Attributliste
	 * @throws IOException
	 * 			Fehler beim Lesen einer PDF
	 */
	public void applyAttributes() throws IOException {
		//Speichern der Attributwerte
		//Schlüssel der äußeren Map: Dateiname
		//Schlüssel der inneren Map: Attributname
		//Wert der inneren Map: Attributwert
		Map<String, Map<String, String>> fileValues = new HashMap<>();
		
		//Filter, um nur PDF-Dateien auszuwählen
		FilenameFilter filter = (dir, name) -> name.endsWith(".pdf") || name.endsWith(".PDF");
		File[] pdfFiles = pdf.listFiles(filter);
		if (pdfFiles == null) {
			throw new IOException("PDF input directory could not be opened or read");
		}
		for (int i = 0; i < pdfFiles.length; i++) {			
			File file = pdfFiles[i];
			if (progress != null) {
				progress.setProgress(i, pdfFiles.length, "Parsing " + file.getName());
			}
			PDDocument doc = PDDocument.load(file);
			TextStripper stripper = new TextStripper();
			stripper.parse(doc);
            doc.close();
            
            Map<String, String> values = stripper.getAttrValues(attrList, xTol, yTol);
            fileValues.put(file.getName(), values);
		}
		if (progress != null) {
			progress.setProgress(pdfFiles.length, pdfFiles.length, "Done");
		}
//		checkForValuePatterns(fileValues);
		//Ausgabe der Werte		
		outputValues(fileValues);
	}
	
	/**
	 * Methode zur Ausgabe der Attributwerte
	 * @param values
	 * 			Attributwerte:<br>
	 * 			SCHLÜSSEL (äußere Map): Dateiname<br>
	 * 			SCHLÜSSEL (innere Map): Attributname<br>
	 * 			WERT (innere Map): Attributwert<br>
	 * @throws IOException Fehler bei der Ausgabe
	 */
	public void outputValues(Map<String, Map<String, String>> values) throws IOException {
		//Ausgabe auf Konsole
		System.out.print("Dateiname           ");
		//Ausgabe der Attributnamen
		for (Attribute attr : attrList) {
			System.out.print(attr.name);
			for (int i = 0; i < 20 - attr.name.length(); i++) {
				System.out.print(' ');
			}
		}
		System.out.println();		
		
		//Ausgabe der Attributwerte
		for (String file : values.keySet()) {
			System.out.print(file);
			for (int i = 0; i < 20 - file.length(); i++) {
				System.out.print(' ');
			}
			for (Attribute attr : attrList) {
				String value = values.get(file).get(attr.name);
				System.out.print(value);
				for (int i = 0; i < 20 - value.length(); i++) {
					System.out.print(' ');
				}
			}
			System.out.println();
		}
		
		//Ausgabe in CSV-Datei
		for (String file : values.keySet()) {
			output.getFileValues(file, values.get(file));
		}
		output.close();
	}
	
	/**
	 * liest ein Attribut aus der XML-Attributtemplatedatei
	 * @param node
	 * 			XML-Node eines Attributs
	 * @return
	 * 			{@link Attribute Attributobjekt}
	 */
	private Attribute readAttribute(Node node) {
		System.out.println(node.getTextContent());
		NodeList attrProp = node.getChildNodes();
		Attribute ret = new Attribute();
		for (int i = 0; i < attrProp.getLength(); i++) {
			Node n = attrProp.item(i);
			switch (n.getNodeName()) {
				case "name":
					ret.name = n.getTextContent();
					break;
				case "page":
					ret.page = Integer.parseInt(n.getTextContent());
					break;
				case "x-start":
					ret.xStart = Float.parseFloat(n.getTextContent());
					break;
				case "y-start":
					ret.yStart = Float.parseFloat(n.getTextContent());
					break;
				case "x-end":
					ret.xEnd = Float.parseFloat(n.getTextContent());
					break;
				case "y-end":
					ret.yEnd = Float.parseFloat(n.getTextContent());
					break;
				case "remove":
					String remove = n.getTextContent();
					if (remove != null && !remove.isEmpty()) {
						ret.removePattern = Pattern.compile(remove);
					}
					break;
				case "select":
					String select = n.getTextContent();
					if (select != null && !select.isEmpty()) {
						ret.selectPattern = Pattern.compile(select);
					}
					break;
			}
		}
		return ret;
	}
	
//	private void checkForValuePatterns(Map<String, Map<String, String>> fileValues) {
//		Map<String, String> leadingPattern = new HashMap<String, String>();
//		Map<String, String> trailingPattern = new HashMap<String, String>();
//		
//		for (Map<String, String> values : fileValues.values()) {
//			for (String attribute : values.keySet()) {
//				String value = values.get(attribute);
//				
//				String valuePatternLeading = leadingPattern.get(attribute);				
//				if (valuePatternLeading == null) {
//					leadingPattern.put(attribute, value);
//				} else {
//					String newPattern = "";
//					for (int i = 0; i < valuePatternLeading.length(); i++) {
//						if (value.length() <= i) {
//							break;
//						}
//						char ch = valuePatternLeading.charAt(i);
//						if (value.charAt(i) == ch) {
//							newPattern += ch;
//						} else {
//							break;
//						}
//					}
//					leadingPattern.put(attribute, newPattern);
//				}
//				
//				String valuePatternTrailing = trailingPattern.get(attribute);				
//				if (valuePatternTrailing == null) {
//					trailingPattern.put(attribute, value);
//				} else {
//					String[] trailingParts = valuePatternTrailing.split("\\s");
//					
//				}
//			}
//		}
//		
//		for (Attribute attr : attrList) {
//			System.out.println("============ " + attr.name + " ===============");
//			System.out.println("Trailing Pattern: \"" + trailingPattern.get(attr.name) + '"');
//			System.out.println("Leading Pattern: \"" + leadingPattern.get(attr.name) + '"');
//		}
//	}
	
	/**
	 * liest Kommandozeilenparameter
	 * @param args
	 * 		<table summary="Kommandozeilenparameter">
	 *			<tr><td>--pdfPath</td><td>Pfad des PDF-Eingabeordners</td></tr>
	 *			<tr><td>--attributes</td><td>Pfad der XML-Attributdatei</td></tr>
	 *			<tr><td>--output</td><td>Pfad der CSV-Ausgabedatei</td></tr>
	 *			<tr><td>--tolerance</td><td>Toleranzen in X- und Y-Richtung bei der Anwendung der Attribute</td></tr>
	 *			<tr><td>--xTolerance</td><td>Toleranz in X-Richtung bei der Anwendung der Attribute</td></tr>
	 *			<tr><td>--yTolerance</td><td>Toleranz in Y-Richtung bei der Anwendung der Attribute</td></tr>
	 * 		</table>
	 * @throws IllegalArgumentException
	 * 		ungültiger Kommadozeilenparameter
	 */
	private void readArgs(String[] args) throws IllegalArgumentException {
		pdf = null;
		File output = null;
		xml = null;
		xTol = -1;
		yTol = -1;
		
		if (args.length < 1) {
			printUsage();
			throw new IllegalArgumentException("ungültige Anzahl Argumente");
		}
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "--pdfPath":
					if (++i < args.length) {
						pdf = new File(args[i]);
					} else {
						printUsage();
						throw new IllegalArgumentException("Nach --pdfPath muss ein gültiges Verzeichnis angegeben werden.");
					}
					break;
				case "--attributes":
					if (++i < args.length) {
						xml = new File(args[i]);
					} else {
						printUsage();
						throw new IllegalArgumentException("Nach --attributes muss eine gültige Datei angegeben werden.");
					}
					break;
				case "--output":
					if (++i < args.length) {
						output = new File(args[i]);
					} else {
						printUsage();
						throw new IllegalArgumentException("Nach --output muss eine gültige Datei angegeben werden.");
					}
					break;
				case "--tolerance":
					if (xTol > 0 || yTol > 0) {
						printUsage();
						throw new IllegalArgumentException("Bei Angabe von --tolerance darf keine Einzeltoleranz mit "
								+ "--xTolerance oder --yTolerance angegeben werden.");
					}
					if (++i < args.length) {
						try {
							xTol = Float.parseFloat(args[i]);
							//noinspection SuspiciousNameCombination
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
						throw new IllegalArgumentException("Nach --tolerance muss eine Gleitkommazahl angegeben werden.");
					}
					break;
				case "--xTolerance":
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
					break;
				case "--yTolerance":
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
					break;
			}
		}
		
		if (xTol < 0) {
			xTol = 3;
		}
		if (yTol < 0) {
			yTol = 3;
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
		
		try {
			this.output = new ValueCsvExporter(output);
		} catch (IOException e) {
			throw new IllegalArgumentException("Nach " + output.getName() + " kann nicht geschrieben werden.");
		}
	}
	
	/**
	 * Gibt Kommandozeilenparameter aus 
	 */
	private void printUsage() {
		System.out.println("Nutzung: " + this.getClass().getSimpleName() + " --pdfPath PDF-Verzeichnis --attributes attr.xml"
				+ " [--output result.csv] [--tolerance Toleranz | --xTolerance X-Toleranz --yTolerance Y-Toleranz]");
		System.out.println();
		System.out.println("\t--pdfPath            \t\tPDF-Eingabeverzeichnis");
		System.out.println("\t--attributes         \t\tXML-Datei mit den Attributen");
		System.out.println("\t--output             \t\tCSV-Ausgabedatei mit Liste der Attributwerte");
		System.out.println("\t--tolerance          \t\tSetzt Toleranz für den Vergleich der Textkoordinaten (float)");
		System.out.println("\t--xTolerance         \t\tSetzt Toleranz in X-Richtung (float)");
		System.out.println("\t--yTolerance         \t\tSetzt Toleranz in Y-Richtung (float)");
	}
}
