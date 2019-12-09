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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.paperless.de.parser.TextStripper;
import org.paperless.de.util.Attribute;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Klasse zur Anwendung eines Templates auf eine Menge PDF-Dokumente
 * 
 * @author nba
 */
public class ApplyTemplate {
	
	/**
	 * PDF-Eingabeordner
	 */
	private File pdf;
	
	/**
	 * XML-Attributtemplate
	 */
	private File xml;
	
	/**
	 * CSV-Ausgabedatei
	 */
	private File output;
	
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
	 * <table>
	 * <tr><td>--pdfPath</td><td>Pfad des PDF-Eingabeordners</td></tr>
	 * <tr><td>--attributes</td><td>Pfad der XML-Attributdatei</td></tr>
	 * <tr><td>--output</td><td>Pfad der CSV-Ausgabedatei</td></tr>
	 * <tr><td>--tolerance</td><td>Toleranzen in X- und Y-Richtung bei der Anwendung der Attribute</td></tr>
	 * <tr><td>--xTolerance</td><td>Toleranz in X-Richtung bei der Anwendung der Attribute</td></tr>
	 * <tr><td>--yTolerance</td><td>Toleranz in Y-Richtung bei der Anwendung der Attribute</td></tr>	 * 
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
	 * <table>
	 * <tr><td>--pdfPath</td><td>Pfad des PDF-Eingabeordners</td></tr>
	 * <tr><td>--attributes</td><td>Pfad der XML-Attributdatei</td></tr>
	 * <tr><td>--output</td><td>Pfad der CSV-Ausgabedatei</td></tr>
	 * <tr><td>--tolerance</td><td>Toleranzen in X- und Y-Richtung bei der Anwendung der Attribute</td></tr>
	 * <tr><td>--xTolerance</td><td>Toleranz in X-Richtung bei der Anwendung der Attribute</td></tr>
	 * <tr><td>--yTolerance</td><td>Toleranz in Y-Richtung bei der Anwendung der Attribute</td></tr>	 * 
	 * </table>
	 */
	public ApplyTemplate(String[] args) {
		readArgs(args);
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
		attrList = new ArrayList<Attribute>();
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
		HashMap<String, HashMap<String, String>> fileValues = new HashMap<String, HashMap<String, String>>();
		
		//Filter, um nur PDF-Dateien auszuwählen
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".pdf") || name.endsWith(".PDF");
			}
		};
		for (File file : pdf.listFiles(filter)) {
			PDDocument doc = PDDocument.load(file);
			TextStripper stripper = new TextStripper();
			stripper.setSortByPosition( true );
            stripper.setStartPage( 1 );
            stripper.setEndPage( doc.getNumberOfPages() );
            Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            
            stripper.writeText(doc, dummy);
            doc.close();
            
            HashMap<String, String> values = stripper.getAttrValues(attrList, xTol, yTol);
            fileValues.put(file.getName(), values);
		}
		//Ausgabe der Werte
		outputValues(fileValues);
	}
	
	/**
	 * Methode zur Ausgabe der Attributwerte
	 * @param values
	 * 			Attributwerte<br>
	 * 			SCHLÜSSEL (äußere Map): Dateiname
	 * 			SCHLÜSSEL (innere Map): Attributname
	 * 			WERT (innere Map): Attributwert
	 */
	public void outputValues(HashMap<String, HashMap<String, String>> values) {
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
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			bw.write("Datei;");
			for (Attribute attr : attrList) {
				bw.write(attr.name + ';');
			}
			bw.newLine();
			for (String file : values.keySet()) {
				bw.write(file + ';');
				for (Attribute attr : attrList) {
					String value = values.get(file).get(attr.name);
					bw.write(value + ';');
				}
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			System.out.println("[ERROR] Error writing to output file " + output.getAbsolutePath());
			e.printStackTrace(System.out);
		}		
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
			if (n.getNodeName().equals("name")) {
				ret.name = n.getTextContent();
			} else if (n.getNodeName().equals("x-start")) {
				try {
					ret.xStart = Float.parseFloat(n.getTextContent());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (n.getNodeName().equals("y-start")) {
				ret.yStart = Float.parseFloat(n.getTextContent());
			} else if (n.getNodeName().equals("x-end")) {
				ret.xEnd = Float.parseFloat(n.getTextContent());
			} else if (n.getNodeName().equals("y-end")) {
				ret.yEnd = Float.parseFloat(n.getTextContent());
			}
		}
		return ret;
	}
	
	/**
	 * liest Kommandozeilenparameter
	 * @param args
	 * 		<table>
	 * 			<tr><td>--pdfPath</td><td>Pfad des PDF-Eingabeordners</td></tr>
	 * 			<tr><td>--attributes</td><td>Pfad der XML-Attributdatei</td></tr>
	 * 			<tr><td>--output</td><td>Pfad der CSV-Ausgabedatei</td></tr>
	 * 			<tr><td>--tolerance</td><td>Toleranzen in X- und Y-Richtung bei der Anwendung der Attribute</td></tr>
	 * 			<tr><td>--xTolerance</td><td>Toleranz in X-Richtung bei der Anwendung der Attribute</td></tr>
	 * 			<tr><td>--yTolerance</td><td>Toleranz in Y-Richtung bei der Anwendung der Attribute</td></tr>	 * 
	 * 		</table>
	 * @throws IllegalArgumentException
	 */
	private void readArgs(String[] args) throws IllegalArgumentException {
		pdf = null;
		output = null;
		xml = null;
		xTol = -1;
		yTol = -1;
		
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
