package org.paperless.de.parser;

import org.apache.pdfbox.text.TextPosition;

import java.util.List;

/**
 * Speicherstruktur für einen PDF-Text. Stellt eine Zeile dar, sollte durch
 * {@link TextStripper} spaltenweise getrennt sein.
 * 
 * @author nba
 */
public class PdfString {
	
	/**
	 * Text, der durch die Glyphen repräsentiert wird
	 */
	private String text;
	
	/**
	 * Glyphenliste
	 */
	private List<TextPosition> positions;
	
	/**
	 * Koordinatenrahmen des Texts
	 */
	private float firstX, firstY, lastX, lastY;
	
	/**
	 * Seitenzahl
	 */
	private int pageNum;
	
	/**
	 * Konstruktor, wenn der Text bekannt ist
	 * @param text
	 * 			Text, der durch die Glyphen repräsentiert wird
	 * @param positions
	 * 			Glyphenliste
	 * @param pageNum
	 * 			Seitenzahl
	 */
	public PdfString (String text, List<TextPosition> positions, int pageNum) {
		this.text = text;
		this.positions = positions;
		this.pageNum = pageNum;
		
		calcTextBox();
	}
	
	/**
	 * Konstruktor für unbekannten Textinhalt.
	 * 
	 * @param positions
	 * 			Glyphenliste
	 * @param pageNum
	 * 			Seitenzahl
	 */
	
	public PdfString(List<TextPosition> positions, int pageNum) {
		this.positions = positions;
		this.pageNum = pageNum;
		
		//Text wird aus den einzelnen Glyphen gebildet
		this.text = "";
		for (TextPosition p : positions) {
			//noinspection StringConcatenationInLoop
			this.text += p.getUnicode();
		}
		
		calcTextBox();
	}
	
	/**
	 * Methode zur Berechnung der Rahmenkoordinaten des Texts. Dazu werden die
	 * Extremwerte aller Glyphen genutzt.
	 */
	private void calcTextBox() {
		//Initialisierung mit den Werten des ersten Glyphen
		this.firstX = positions.get(0).getX();
		this.firstY = positions.get(0).getY();
		this.lastX = positions.get(0).getEndX();
		this.lastY = positions.get(0).getEndY();
		
		for (TextPosition pos : positions) {
			//Leere Glyphen an Anfang oder Ende ignorieren
			if (pos.getUnicode().trim().isEmpty()) {
				continue;
			}
			if (pos.getX() < this.firstX) {
				this.firstX = pos.getX();
			}
			if (pos.getY() < this.firstY) {
				this.firstY = pos.getY();
			}
			if (pos.getEndX() > this.lastX) {
				this.lastX = pos.getEndX();
			}
			if (pos.getEndY() > this.lastY) {
				this.lastY = pos.getEndY();
			}
		}
	}

	/**
	 * @return Anfang des Koordinatenrahmens um den Text in X-Richtung.
	 * Entspricht dem Abstand vom linken Seitenrand.
	 */
	public float getFirstX() {
		return firstX;
	}

	/**
	 * @return Anfang des Koordinatenrahmens um den Text in Y-Richtung.
	 * Entspricht dem Abstand vom oberen Seitenrand.
	 */
	public float getFirstY() {
		return firstY;
	}

	/**
	 * @return Ende des Koordinatenrahmens um den Text in X-Richtung.
	 * Entspricht dem Abstand vom linken Seitenrand.
	 */
	public float getLastX() {
		return lastX;
	}

	/**
	 * @return Ende des Koordinatenrahmens um den Text in Y-Richtung.
	 * Entspricht dem Abstand vom linken Seitenrand.
	 */
	public float getLastY() {
		return lastY;
	}

	/**
	 * @return Text, der durch die Glyphen repräsentiert wird
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * @return Seitenzahl
	 */
	public int getPageNum() {
		return pageNum;
	}
}
