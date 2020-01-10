package org.paperless.de.util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.paperless.de.ApplyTemplate.OutputLister;

public class ValueCsvExporter implements OutputLister, Closeable {
	
	private BufferedWriter bw;
	
	private FileWriter fw;
	
	private ArrayList<String> attributes = new ArrayList<String>();
	
	private boolean headerWritten = false;
	
	public ValueCsvExporter(File csv) throws IOException {
		this.fw = new FileWriter(csv);
		this.bw = new BufferedWriter(fw);
	}

	@Override
	public void getFileValues(String fileName, Map<String, String> attValues) {
		if (!headerWritten) {
			attributes = new ArrayList<String>();
			try {
				bw.write("file;");
				for (String attr : attValues.keySet()) {
					attributes.add(attr);
					bw.write(attr + ';');
				}
				bw.newLine();
				headerWritten = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			bw.write(fileName + ';');
			for (String attr : attributes) {
				bw.write(attValues.get(attr) + ';');
			}
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException {
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

}
