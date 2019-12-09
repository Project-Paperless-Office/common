package org.paperless.de.parser;

import java.util.List;

import org.apache.pdfbox.text.TextPosition;

public class PdfString {
	
	private String text;	
	private List<TextPosition> positions;
	
	private float firstX, firstY, lastX, lastY;
	
	private int pageNum;
	
	public PdfString (String text, List<TextPosition> positions, int pageNum) {
		this.text = text;
		this.positions = positions;
		this.pageNum = pageNum;
		
		calcTextBox();
	}
	
	private void calcTextBox() {
		this.firstX = positions.get(0).getX();
		this.firstY = positions.get(0).getY();
		this.lastX = positions.get(0).getEndX();
		this.lastY = positions.get(0).getEndY();
		
		for (TextPosition pos : positions) {
			if (pos.getUnicode().trim().isEmpty()) {
				continue;
			}
			if (pos.getX() < this.firstX) {
				this.firstX = pos.getX();
			}
			if (pos.getY() < this.firstY) {
				this.firstY = pos.getY();
			}
			if (pos.getEndX() < this.lastX) {
				this.lastX = pos.getEndX();
			}
			if (pos.getEndY() < this.lastY) {
				this.lastY = pos.getEndY();
			}
		}
	}

	public float getFirstX() {
		return firstX;
	}

	public float getFirstY() {
		return firstY;
	}

	public float getLastX() {
		return lastX;
	}

	public float getLastY() {
		return lastY;
	}

	public String getText() {
		return text;
	}
	
	public int getPageNum() {
		return pageNum;
	}
}
