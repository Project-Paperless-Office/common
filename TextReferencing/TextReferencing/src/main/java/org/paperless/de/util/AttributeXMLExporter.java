package org.paperless.de.util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.paperless.de.parser.PdfString;

/**
 * Exporter für Attribute im XML-Format. Nutzung: 
 * <ol>
 * <li>Angabe der Zieldatei im Konstruktor. Für jede Zieldatei muss eine neue
 * Instanz erstellt werden.</li>
 * <li>für jedes Attribut wird {@link #writeAttribute} gerufen.</li>
 * <li>am Ende der Ausgabe muss {@link #close()} aufegerufen werden, damit alle
 * XML-Tags beendet und die I/O-Streams geschlossen werden können.</li>
 * </ol>
 * @author nba
 */
public class AttributeXMLExporter implements Closeable {
	
	/**
	 * benutzter XML-Writer
	 */
	private XMLStreamWriter xml;
	
	/**
	 * darunterliegender BufferedWriter. Muss gespeichert werden, damit er am
	 * Ende der Benutzung ordentlich geschlossen werden kann.
	 */
	private BufferedWriter bw;
	
	/**
	 * darunterliegender FileWriter. Muss gespeichert werden, damit er am
	 * Ende der Benutzung ordentlich geschlossen werden kann.
	 */
	private FileWriter fw;
	
	/**
	 * gibt an, ob die dazugehörigen I/O-Schreibklassen bereits geschlossen
	 * wurden.
	 */
	private boolean closed = false;

	/**
	 * Konstruktor für die XML-Ausgabeklasse. Öffnet die Ausgabestreams und beginnt den XML-Baum
	 * @param file
	 * 			Datei, in die der XML-Baum geschrieben wird.
	 * @throws XMLStreamException
	 * 			Fehler beim Schreiben des XML-Baumbeginns
	 * @throws FactoryConfigurationError
	 * 			Fehler beim Starten des XML-Exports
	 * @throws IOException
	 * 			Fehler beim Starten des Schreibvorgangs
	 */
	public AttributeXMLExporter(File file) throws XMLStreamException, FactoryConfigurationError, IOException {
		closed = false;
		fw = new FileWriter(file);
		bw = new BufferedWriter(fw);
		xml = XMLOutputFactory.newInstance().createXMLStreamWriter(bw);
		
		xml.writeStartDocument();
		xml.writeStartElement("template");
	}
	
	/**
	 * Schreiben eines Attributs in den XML-Baum
	 * @param name
	 * 			Name des Attributs
	 * @param attribute
	 * 			Beispielwert für das Attribut, wird genutzt, um die
	 * 			Seitenzahl und die Koordinatenbox zu erhalten.
	 * @throws XMLStreamException
	 * 			Fehler beim Schreiben des Attributs in die XML-Datei
	 * @throws IOException
	 * 			Fehler beim Schreiben in die Datei oder der Stream wurde
	 * 			bereits geschlossen
	 */
	public void writeAttribute (String name, PdfString attribute) throws XMLStreamException, IOException {
		if (closed) {
			throw new IOException("This XML-Exporter has already been closed");
		}
		
		xml.writeStartElement("attribute");		
		
		writeElement("name", name);
		writeElement("page", attribute.getPageNum());
		writeElement("x-start", attribute.getFirstX());
		writeElement("y-start", attribute.getFirstY());
		writeElement("x-end", attribute.getLastX());
		writeElement("y-end", attribute.getLastY());
		
		xml.writeEndElement();
	}
	
	/**
	 * Beendet den XML-Baum und schließt die Datei-Streams
	 */
	public void close() {
		try {
			xml.writeEndDocument();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		} finally {
			try {
				xml.close();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			}
		}
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Komfort-Methode, schreibt einen XML-Knoten mit dem gegebenen Inhalt.
	 * @param name
	 * 			Name des Knotens
	 * @param value
	 * 			Inhalt des Knotens
	 * @throws XMLStreamException
	 * 			Fehler beim Schreiben des XMl-Knotens
	 * @throws IOException
	 * 			Der Stream wurde bereits geschlossen
	 */
	private void writeElement(String name, String value) throws XMLStreamException, IOException {
		if (closed) {
			throw new IOException("This XML-Exporter has already been closed");
		}
		
		xml.writeStartElement(name);
		xml.writeCharacters(value);
		xml.writeEndElement();
	}
	
	/**
	 * Komfort-Methode, schreibt einen XML-Knoten mit dem gegebenen Inhalt.
	 * @param name
	 * 			Name des Knotens
	 * @param value
	 * 			Inhalt des Knotens
	 * @throws XMLStreamException
	 * 			Fehler beim Schreiben des XMl-Knotens
	 * @throws IOException
	 * 			Der Stream wurde bereits geschlossen
	 */
	private void writeElement(String name, int value) throws XMLStreamException, IOException {
		if (closed) {
			throw new IOException("This XML-Exporter has already been closed");
		}
		
		xml.writeStartElement(name);
		xml.writeCharacters("" + value);
		xml.writeEndElement();
	}
	
	/**
	 * Komfort-Methode, schreibt einen XML-Knoten mit dem gegebenen Inhalt.
	 * @param name
	 * 			Name des Knotens
	 * @param value
	 * 			Inhalt des Knotens
	 * @throws XMLStreamException
	 * 			Fehler beim Schreiben des XMl-Knotens
	 * @throws IOException
	 * 			Der Stream wurde bereits geschlossen
	 */
	private void writeElement(String name, float value) throws XMLStreamException, IOException {
		if (closed) {
			throw new IOException("This XML-Exporter has already been closed");
		}
		
		xml.writeStartElement(name);
		xml.writeCharacters("" + value);
		xml.writeEndElement();
	}
}
