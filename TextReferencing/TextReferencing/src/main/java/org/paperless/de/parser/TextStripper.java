package org.paperless.de.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

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
}
