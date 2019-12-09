package org.paperless.de.util;

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
}
