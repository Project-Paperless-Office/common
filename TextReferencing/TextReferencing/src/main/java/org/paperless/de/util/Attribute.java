package org.paperless.de.util;

import java.util.regex.Pattern;

/**
 * Speicherklasse für ein Attribut als POJO
 * 
 * @author nba
 */
public class Attribute {
	
	/**
	 * Koordinaten für Start und Ende des Texts relativ zum Dokument
	 */
	public float xStart, yStart, xEnd, yEnd;
	
	/**
	 * Seitenzahl (1-basiert)
	 */
	public int page;
	
	/**
	 * Name des Attributs
	 */
	public String name;
	
	/**
	 * Regex-Pattern zum Entfernen einer Zeichenkette. Die auf das Pattern
	 * matchende Zeichenkette wird aus dem Attributwert entfernt. Zur
	 * Verbesserung der Performance wird statt des Regex-Strings das bereits
	 * vorbereitete Pattern gespeichert, das dann lediglich auf die
	 * Attributwerte angewendet wird.
	 */
	public Pattern removePattern;
	
	/**
	 * Regex-Pattern zur Auswahl einer Zeichenkette. Die auf das Pattern
	 * matchende Zeichenkette wird als Attributwert benutzt. Dieses Pattern
	 * wird nach dem {@link #removePattern} benutzt.
	 */
	public Pattern selectPattern;
}
