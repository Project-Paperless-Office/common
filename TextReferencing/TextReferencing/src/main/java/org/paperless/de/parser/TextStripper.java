package org.paperless.de.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.paperless.de.util.Attribute;

/**
 * Klasse zum Parsen und Speichern der Texte mit PDFBox. Stellt immer ein
 * Dokument dar, sollte also für jedes neue Dokument neu erstellt werden.
 * 
 * @author nba
 */
public class TextStripper extends PDFTextStripper {
	
	/**
	 * Liste der gefundenen Texte
	 */
	private List<PdfString> texts;
	
	/**
	 * gerade geparste Seitennummer - aus irgendeinem Grund nicht in
	 * {@link TextPosition} enthalten.
	 */
	private int currentPageNum;

	/**
	 * Standard-Konstruktor
	 * 
	 * @throws IOException
	 * 			weitergereicht von {@link PDFTextStripper#PDFTextStripper()}
	 */
	public TextStripper() throws IOException {
		super();
		this.texts = new ArrayList<PdfString>();
	}
	
	/**
	 * Liest texte aus einem PDF-Dokument. Jede Seite des Dokuments wird
	 * einzeln bearbeitet, damit die Seitenzahlen im Attribut mit gespeichert
	 * werden können.
	 * 
	 * @param doc
	 * 			zu bearbeitendes PDF-Dokument
	 * @throws IOException
	 * 			Fehler im Dokument
	 */
	public void parse(PDDocument doc) throws IOException {
		setSortByPosition(true);
		Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
		
		for (currentPageNum = 1; currentPageNum <= doc.getNumberOfPages(); currentPageNum++) {
			setStartPage(currentPageNum);
            setEndPage(currentPageNum);            
            writeText(doc, dummy);
		}
	}
	
	/**
	 * Überschreibt die entsprechende Methode in {@link PDFTextStripper}.
	 * Statt den String irgendwohin zu schreiben, wird er in dieser Methode nur
	 * gespeichert.
	 */
	@Override
	public void writeString(String text, List<TextPosition> positions) {
		if (texts == null) {
			this.texts = new ArrayList<PdfString>();
		}
		
		if (!text.trim().isEmpty()) {
			List<List<TextPosition>> splitPos = splitText(positions);
			if (splitPos.size() == 1) {
				texts.add(new PdfString(text, positions, currentPageNum));
			} else {
				for (List<TextPosition> pos : splitPos) {
					texts.add(new PdfString(pos, currentPageNum));
				}
			}
		}
	}

	/**
	 * gibt die gefundenen Texte zurück
	 * @return alle Textobjekte der PDF
	 */
	public List<PdfString> getTexts() {
		return texts;
	}

	/**
	 * <p>Vergleicht die eigenen Texte mit denen des anderen TextStrippers und
	 * gibt unterschiedliche Texte, die an derselben Position stehen, zurück.</p>
	 * <p>TODO/BUG: es wird nach dem ersten Text an derselben Position gesucht,
	 * nicht nach dem am besten passendsten. Das köönte zu Problemen führen.</p>
	 * @param te
	 * 			anderer TextStripper zum Vergleich
	 * @param xTol
	 * 			Toleranz beim Vergleich in X-Richtung
	 * @param yTol
	 * 			Toleranz beim Vergleich in Y-Richtung
	 * @return
	 * 			Liste der unterschiedlichen Texte
	 */
	public List<PdfString> compare(TextStripper te, float xTol, float yTol) {
		List<PdfString> ret = new ArrayList<PdfString>();
		
		for (PdfString text : texts) {
			float xStart = text.getFirstX();
			float yStart = text.getFirstY();
			String textString = text.getText();
			
			for (PdfString otherText : te.getTexts()) {
				if (Math.abs(xStart - otherText.getFirstX()) > xTol) {
					continue;
				}
				if (Math.abs(yStart - otherText.getFirstY()) > yTol) {
					continue;
				}
				//sind an gleicher Position -> Texte vergleichen, unterschiedliche zurückgeben
				if (!textString.equals(otherText.getText())) {
					ret.add(text);
					break;
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Gibt eine Map der Attributwerte aus diesem Dokument zu einer gegebenen
	 * Attributliste zurück. Wird ein Attribut nicht gefunden, wird als
	 * Attributwert "N/A" eingetragen.
	 * 
	 * @param attributes
	 * 			Liste der Attribute, die mit dem Dokument abgeglichen werden
	 * 			sollen.
	 * @param xTol
	 * 			Toleranz beim Abgleich in X-Richtung
	 * @param yTol
	 * 			Toleranz beim Abgleich in Y-Richtung
	 * @return  <p>Mapping der Attributwerte</p>
	 * 			KEY: Attributname aus der Attributliste, alle Attribute werden
	 * 				belegt.<br>
	 * 			WERT: Attributwert aus dem Dokument oder "N/A"
	 */
	public Map<String, String> getAttrValues(List<Attribute> attributes, float xTol, float yTol) {
		HashMap<String, String> ret = new HashMap<String, String>();
		ATTRLOOP:for (Attribute attr : attributes) {
			for (PdfString text : texts) {
				if (Math.abs(attr.xStart - text.getFirstX()) <= xTol) {
					if (Math.abs(attr.yStart - text.getFirstY()) <= yTol) {
						ret.put(attr.name, text.getText());
						continue ATTRLOOP;
					}
				}
			}
			ret.put(attr.name, "N/A");
		}
		
		return ret;
	}
	
	/**
	 * Trennt den Text aus der Glyphliste in mehrere Glyphlisten, wenn der
	 * Abstand zwischen zwei aufeinanderfolgenden Zeichen zu groß ist.
	 * 
	 * @param position
	 * 			Glyphliste aus dem Text - sollte geordnet sein
	 * @return
	 * 			eine Liste von Unterglyphlisten, darin sollten alle Glyphen aus
	 * 			der Originalliste vorkommen. 
	 */
	private List<List<TextPosition>> splitText(List<TextPosition> position) {
		List<List<TextPosition>> ret = new ArrayList<List<TextPosition>>();
		List<TextPosition> currentList = new ArrayList<TextPosition>();
		ret.add(currentList);
		
		float lastXEnd = -1f;
		for (TextPosition pos : position) {
			if (!(lastXEnd < 0)) {
				if (Math.abs(pos.getX() - lastXEnd) > 1) {
					currentList = new ArrayList<TextPosition>();
					ret.add(currentList);
				}
			}
			currentList.add(pos);
			lastXEnd = pos.getEndX();
		}
		
		return ret;
	}
}
