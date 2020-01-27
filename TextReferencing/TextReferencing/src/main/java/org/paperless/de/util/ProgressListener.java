package org.paperless.de.util;

public interface ProgressListener {
	void setProgress(int currentIndex, int totalNum, String msg);
}
