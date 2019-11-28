package org.paperless.de;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

public class ApplyTemplate {
	
	private File pdf, xml, output;
	
	private float xTol, yTol;
	
	private List<Attribute> attrList;

	public static void main(String[] args) {
		try {
			ApplyTemplate inst = new ApplyTemplate(args);
			inst.readAttributes();
			inst.applyAttributes();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ApplyTemplate(String[] args) {
		readArgs(args);
	}
	
	public void readAttributes() throws ParserConfigurationException, SAXException, IOException {
		attrList = new ArrayList<Attribute>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xml);
		//doc.getDocumentElement().normalize();
		
		NodeList attrNodeList = doc.getElementsByTagName("attribute");
		for (int i = 0; i < attrNodeList.getLength(); i++) {
			attrList.add(readAttribute(attrNodeList.item(i)));
		}
	}
	
	public void applyAttributes() throws IOException {
		HashMap<String, HashMap<String, String>> fileValues = new HashMap<String, HashMap<String, String>>();
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith("pdf");
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
		
		outputValues(fileValues);
	}
	
	public void outputValues(HashMap<String, HashMap<String, String>> values) {
		System.out.print("Dateiname           ");
		for (Attribute attr : attrList) {
			System.out.print(attr.name);
			for (int i = 0; i < 20 - attr.name.length(); i++) {
				System.out.print(' ');
			}
		}
		System.out.println();		
		
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
	}
	
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
	
	private void printUsage() {
		System.out.println("Nutzung: " + this.getClass().getSimpleName() + " --pdfPath PDF-Verzeichnis --attributes attr.xml"
				+ " [--output result.txt] [--tolerance Toleranz | --xTolerance X-Toleranz --yTolerance Y-Toleranz]");
		System.out.println();
		System.out.println("\t--pdfPath            \t\tPDF-Eingabeverzeichnis");
		System.out.println("\t--attributes         \t\tXML-Datei mit den Attributen");
		System.out.println("\t--output             \t\tAusgabedatei mit Liste der Attributwerte");
		System.out.println("\t--tolerance          \t\tSetzt Toleranz für den Vergleich der Textkoordinaten (float)");
		System.out.println("\t--xTolerance         \t\tSetzt Toleranz in X-Richtung (float)");
		System.out.println("\t--yTolerance         \t\tSetzt Toleranz in Y-Richtung (float)");
	}
}
