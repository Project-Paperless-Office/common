package org.paperless.de.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Hilfsklasse zum Schreiben einer XML-Struktur. Am Ende des Schreibvorgangs muss
 * {@link #endXml()} aufgerufen werden.
 * 
 * @author Nils Bauer
 */
public class XmlPacker {
	
	private XMLStreamWriter xmlOutput;
	
	private BufferedWriter writer;
	
	/**
	 * Testmethode
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			XmlPacker pa = new XmlPacker(new File("out.xml"));
			
			HashMap<String, String> args1 = new HashMap<String, String>();
			HashMap<String, String> args2 = new HashMap<String, String>();
			HashMap<String, String> args3 = new HashMap<String, String>();
			
			args2.put("arg1", "123");
			args2.put("arg2", "");
			args2.put("arg3", "test");
			
			pa.writeElement("file1", null);
			pa.writeElement("file2", args2);
			pa.writeElement("file3", args3);
			
			pa.endXml();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Konstruktor für den XML-Output. Initialisiert das Schreiben in die Datei
	 * und beginnt den XML-Baum.
	 * 
	 * @param outputFile
	 * 				Datei, in die der XML-Baum geschrieben wird
	 * @throws IOException
	 * 				Fehler bei der Initialisierung des Schreibvorgangs in die Datei
	 * @throws XMLStreamException
	 * 				Fehler beim Schreiben des XML-Baums
	 * @throws FactoryConfigurationError
	 * 				interner Fehler bei der Initialisierung des XML-Streams
	 */
	public XmlPacker(File outputFile) throws IOException, XMLStreamException, FactoryConfigurationError {
		this.writer = new BufferedWriter(new FileWriter(outputFile));
		xmlOutput = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
		
		xmlOutput.writeStartDocument();
		xmlOutput.writeStartElement("template");
	}
	
	/**
	 * <p>Schreibt einen XML-Knoten in den Baum. Der Knoten besteht aus einem
	 * Dateinamen und verschiedenen Argumenten mit Namen und Wert(String).</p>
	 * 
	 * <p>Struktur des geschriebenen Knotens:
	 * <pre>{@code
	 * <filename>
	 * 	<argname1>wert1</argname1>
	 * 	<argname2>wert2</argname2>
	 * 	<argname3>wert3</argname3>
	 * </filename> 
	 * }</pre>
	 * @param fileName
	 * 			Dateiname der gespeicherten PDF. Wird zum Tagnamen des
	 * 			übergeordneten XML-Knotens.
	 * @param args
	 * 			Liste mit Argumenten. Die Keys werden zu den Tagnamen,
	 * 			die Werte zum Inhalt der XML-Knoten.
	 * @throws XMLStreamException
	 * 			Fehler beim Schreiben der XML-Struktur in die Datei
	 */
	public void writeElement(String fileName, HashMap<String, String> args) throws XMLStreamException {
		if (args == null) {
			//bei null wird eine leere HashMap verwendet
			args = new HashMap<String, String>();
		}
		
		xmlOutput.writeStartElement(fileName);
		for(String arg : args.keySet()) {
			xmlOutput.writeStartElement(arg);
			xmlOutput.writeCharacters(args.get(arg));
			xmlOutput.writeEndElement();
		}
		xmlOutput.writeEndElement();
	}
	
	/**
	 * Funktion zum Schließen des XML-Baums und Beenden des Streams.
	 * @throws XMLStreamException
	 * 			Fehler beim Beenden des XML-Baums
	 * @throws IOException
	 * 			Fehler beim Schließen des Dateistreams
	 */
	public void endXml() throws XMLStreamException, IOException {
		xmlOutput.writeEndDocument();
		writer.close();
	}
}
