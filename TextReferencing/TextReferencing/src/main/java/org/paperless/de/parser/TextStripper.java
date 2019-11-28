package org.paperless.de.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.paperless.de.util.Attribute;

public class TextStripper extends PDFTextStripper {
	
	private List<PdfString> texts;

	public TextStripper() throws IOException {
		super();
		this.texts = new ArrayList<PdfString>();
	}
	
	public void writeString(String text, List<TextPosition> positions) {
		if (texts == null) {
			this.texts = new ArrayList<PdfString>();
		}
		if (!text.trim().isEmpty()) {
			texts.add(new PdfString(text, positions));
		}
	}

	public List<PdfString> getTexts() {
		return texts;
	}

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
	
	public HashMap<String, String> getAttrValues(List<Attribute> attributes, float xTol, float yTol) {
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
}
