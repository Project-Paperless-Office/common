package org.paperless.de.util;

public interface ProgressListener {
	public void setProgress(int currentIndex, int totalNum, String msg);
}
