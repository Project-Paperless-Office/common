package org.paperless.de.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.*;
import org.paperless.de.ApplyTemplate;
import org.paperless.de.ApplyTemplate.OutputLister;
import org.paperless.de.CreateTemplate;
import org.paperless.de.CreateTemplate.AttributeProperty;
import org.paperless.de.CreateTemplate.AttributeSelector;
import org.paperless.de.util.ProgressListener;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class AppStart extends Composite implements OutputLister, AttributeSelector, ProgressListener {
	private Table table;
	private Table attrTable;
	private Text textPdf;
	private Text textXml;
	private ProgressBar currentProgressBar;
	private Label currentProgressMsg;
	
	private String inputPath;
	
	private String xmlPath;
	
	private ArrayList<String> attributes;
	
	private AppStart thisA;

	private Button addRegex;

	private String selectedAttribute;
	
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		//shell.setLayout(new GridLayout(1, false));
		shell.setLayout(new FillLayout());

		new AppStart(shell);
		shell.setText("Paperless");

		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	public AppStart(Composite parent) {
		super(parent, SWT.BORDER);
		thisA = this;
		setLayout(new FormLayout());
		
		table = new Table(this, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL
		        | SWT.H_SCROLL);
		FormData fd_table = new FormData();
		fd_table.bottom = new FormAttachment(0, 597);
		fd_table.right = new FormAttachment(0, 690);
		fd_table.top = new FormAttachment(0, 109);
		fd_table.left = new FormAttachment(0, 10);
		table.setLayoutData(fd_table);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
//		final Menu tableMenu = new Menu(table);
//		table.setMenu(tableMenu);
//		final MenuItem miRemove = new MenuItem(tableMenu, SWT.PUSH);
//		miRemove.setText("Remove-Regex hinzufügen");
		
		
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (table.getSelectionCount() > 0) {
					TableItem item = table.getSelection()[0];
					File file = new File(inputPath + "\\" + item.getText());
					try {
						Desktop.getDesktop().open(file);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		Label lblPdfverzeichnis = new Label(this, SWT.NONE);
		FormData fd_lblPdfverzeichnis = new FormData();
		fd_lblPdfverzeichnis.right = new FormAttachment(0, 117);
		fd_lblPdfverzeichnis.top = new FormAttachment(0, 12);
		fd_lblPdfverzeichnis.left = new FormAttachment(0, 10);
		lblPdfverzeichnis.setLayoutData(fd_lblPdfverzeichnis);
		lblPdfverzeichnis.setText("PDF-Verzeichnis:");
		
		textPdf = new Text(this, SWT.BORDER);
		FormData fd_textPdf = new FormData();
		fd_textPdf.bottom = new FormAttachment(0, 32);
		fd_textPdf.right = new FormAttachment(0, 514);
		fd_textPdf.top = new FormAttachment(0, 7);
		fd_textPdf.left = new FormAttachment(0, 123);
		textPdf.setLayoutData(fd_textPdf);
		textPdf.setEditable(false);
		
		Button btnDurchsuchenPdf = new Button(this, SWT.NONE);
		FormData fd_btnDurchsuchenPdf = new FormData();
		fd_btnDurchsuchenPdf.top = new FormAttachment(0, 7);
		fd_btnDurchsuchenPdf.left = new FormAttachment(0, 520);
		btnDurchsuchenPdf.setLayoutData(fd_btnDurchsuchenPdf);
		btnDurchsuchenPdf.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell());
				dd.setMessage("PDF-Verzeichnis auswählen");
				dd.setText("PDF-Verzeichnis auswählen");
				dd.setFilterPath("D:\\nba\\Projekte\\SWE\\daen_1-1\\");
				
				inputPath = dd.open();
				if (inputPath != null) {
					File input = new File(inputPath);
					if (!input.isDirectory()) {
						MessageBox mb = new MessageBox(getShell(), SWT.OK);
						mb.setMessage("Das angegebene Verzeichnis konnte nicht gefunden oder geöffnet werden.");
						mb.setText("Verzeichnis nicht gefunden");
						mb.open();
					} else {
						System.out.println("\"" + inputPath + "\"");
						textPdf.setText(inputPath);
					}
				}
			}
		});
		btnDurchsuchenPdf.setText("Durchsuchen");
		
		textXml = new Text(this, SWT.BORDER);
		FormData fd_textXml = new FormData();
		fd_textXml.bottom = new FormAttachment(0, 63);
		fd_textXml.right = new FormAttachment(0, 514);
		fd_textXml.top = new FormAttachment(0, 38);
		fd_textXml.left = new FormAttachment(0, 123);
		textXml.setLayoutData(fd_textXml);
		textXml.setEditable(false);
		
		Label lblXmlattributdatei = new Label(this, SWT.NONE);
		FormData fd_lblXmlattributdatei = new FormData();
		fd_lblXmlattributdatei.right = new FormAttachment(0, 115);
		fd_lblXmlattributdatei.top = new FormAttachment(0, 43);
		fd_lblXmlattributdatei.left = new FormAttachment(0, 10);
		lblXmlattributdatei.setLayoutData(fd_lblXmlattributdatei);
		lblXmlattributdatei.setText("XML-Attributdatei:");
		
		Button btnDurchsuchenXml = new Button(this, SWT.NONE);
		FormData fd_btnDurchsuchenXml = new FormData();
		fd_btnDurchsuchenXml.top = new FormAttachment(0, 38);
		fd_btnDurchsuchenXml.left = new FormAttachment(0, 520);
		btnDurchsuchenXml.setLayoutData(fd_btnDurchsuchenXml);
		btnDurchsuchenXml.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setText("Attribut-Datei auswählen");
				fd.setFilterPath("D:\\nba\\Projekte\\SWE\\github\\common\\TextReferencing\\TextReferencing\\");
				fd.setFileName("out.xml");
				
				xmlPath = fd.open();
				if (xmlPath != null) {
					File input = new File(xmlPath);
					if (!input.isFile()) {
						outputError("Datei nicht gefunden", "Die angegebene Attributdatei konnte nicht gefunden oder geöffnet werden.");
					} else {
						System.out.println("\"" + xmlPath + "\"");
						textXml.setText(xmlPath);
					}
				}
			}
		});
		btnDurchsuchenXml.setText("Durchsuchen");
		
		Button btnNeuErstellen = new Button(this, SWT.NONE);
		FormData fd_btnNeuErstellen = new FormData();
		fd_btnNeuErstellen.right = new FormAttachment(100, -5);
		fd_btnNeuErstellen.top = new FormAttachment(0, 38);
		fd_btnNeuErstellen.left = new FormAttachment(0, 608);
		btnNeuErstellen.setLayoutData(fd_btnNeuErstellen);
		btnNeuErstellen.setText("Neu erstellen");		
		btnNeuErstellen.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (inputPath == null || inputPath.isEmpty()) {
					outputError("PDF-Verzeichnis angeben", "Bitte ein PDF-Verzeichnis für die Templateerstellung angeben.");
					return;
				}
				FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
				fd.setOverwrite(true);
				fd.setText("Speicherort für XML-Attribut-Datei auswählen");
				fd.setFileName("attribute.xml");
				String attrFile = fd.open();
				if (attrFile == null || attrFile.isEmpty()) {
					outputError("Datei angeben", "Die angegebene XML-Attributdatei konnte nicht erstellt werden.");
					return;
				}
				CreateTemplate crea = new CreateTemplate(new File(inputPath), thisA, thisA, new File(attrFile));
				try {
					crea.parse();
					crea.lookForSimilarities();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				textXml.setText(attrFile);
				xmlPath = attrFile;
			}
		});
		
		Button btnAnwenden = new Button(this, SWT.NONE);
		FormData fd_btnAnwenden = new FormData();
		fd_btnAnwenden.right = new FormAttachment(0, 198);
		fd_btnAnwenden.top = new FormAttachment(0, 78);
		fd_btnAnwenden.left = new FormAttachment(0, 123);
		btnAnwenden.setLayoutData(fd_btnAnwenden);
		btnAnwenden.addMouseListener(new MouseAdapter() {			
			@Override
			public void mouseDown(MouseEvent e) {
				if (inputPath == null || inputPath.isEmpty()) {
					outputError("PDF-Verzeichnis angeben", "Bitte ein PDF-Verzeichnis zum Auslesen der Attribute angeben.");
					return;
				}
				if (xmlPath == null || xmlPath.isEmpty()) {
					outputError("Attributdatei angeben", "Bitte eine XML-Attributdatei zum Auslesen der Attribute angeben"
							+ " oder neu erstellen.");
					return;
				}
				
				ApplyTemplate appl = new ApplyTemplate(new File(inputPath), new File(xmlPath), thisA, thisA);
				try {
					appl.readAttributes();
				} catch (ParserConfigurationException | SAXException | IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				try {
					appl.applyAttributes();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnAnwenden.setText("Anwenden");
		
		ProgressBar progressBar = new ProgressBar(this, SWT.NONE);
		FormData fd_progressBar = new FormData();
		fd_progressBar.left = new FormAttachment(textPdf, -199);
		fd_progressBar.top = new FormAttachment(btnAnwenden, 0, SWT.TOP);
		fd_progressBar.right = new FormAttachment(textPdf, 0, SWT.RIGHT);
		progressBar.setLayoutData(fd_progressBar);
		
		currentProgressBar = progressBar;
		
		Label lblProgress = new Label(this, SWT.NONE);
		FormData fd_lblProgress = new FormData();
		fd_lblProgress.width = 500;
		fd_lblProgress.top = new FormAttachment(table, 2);
		fd_lblProgress.left = new FormAttachment(table, 0, SWT.LEFT);
		lblProgress.setLayoutData(fd_lblProgress);
		lblProgress.setText("");
		
		currentProgressMsg = lblProgress;
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	@Override
	public void getFileValues(String fileName, Map<String, String> attValues) {
		if (attributes == null) {
			TableColumn fileCol = new TableColumn(table, SWT.LEFT);
			fileCol.setText("Datei");
			fileCol.setWidth(200);
			attributes = new ArrayList<>();
			for (String att : attValues.keySet()) {
				attributes.add(att);
				TableColumn col = new TableColumn(table, SWT.NULL);
				col.setText(att);
				col.setWidth(200);
			}
		}
		TableItem item = new TableItem(table, SWT.NULL);
		item.setText(fileName);
		item.setText(0, fileName);
		for (int i = 0; i < attributes.size(); i++) {
			item.setText(i + 1, attValues.get(attributes.get(i)));
		}
		
	}
	
	private void outputError(String title, String message) {
		MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
		mb.setMessage(message);
		mb.setText(title);
		mb.open();
	}

	@Override
	public AttributeProperty[] getAttributes(String[] values) {
		AttributeProperty[] ret = new AttributeProperty[values.length];
		
		final Shell dialog = new Shell(getShell(), SWT.ON_TOP | SWT.DIALOG_TRIM);
		dialog.setText("Attribute auswählen");
		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth = 10;
		formLayout.marginHeight = 10;
		formLayout.spacing = 10;
		dialog.setLayout (formLayout);
		
		Label label = new Label (dialog, SWT.NONE);
		label.setText ("Bitte die zu übernehmenden Attribute auswählen:");
		FormData data = new FormData ();
		label.setLayoutData (data);
		
		Button submit = new Button(dialog, SWT.NONE);
		FormData fdButton = new FormData();
		fdButton.top = new FormAttachment(0, 20);
		submit.setLayoutData(fdButton);
		submit.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				for (int i = 0; i < attrTable.getItemCount(); i++) {
					TableItem item = attrTable.getItem(i);
					if (item.getChecked()) {
						String name = item.getText(1);
						if (name == null || name.isEmpty()) {
							outputError("Leerer Attributname", "Der Name von Attribut " + item.getText() + " darf nicht leer sein.");
							return;
						}
						System.out.println(i + ":" + name);
						if (ret[i] == null) {
							ret[i] = new AttributeProperty();
						}
						ret[i].name = name;
						String regexRemove = item.getText(2);
						if (regexRemove != null && !regexRemove.isEmpty()) {
							ret[i].removeRegex = regexRemove;
						}
						String regexSelect = item.getText(3);
						if (regexSelect != null && !regexSelect.isEmpty()) {
							ret[i].selectRegex = regexSelect;
						}
					}
				}
				dialog.dispose();
			}
		});
		submit.setText("Übernehmen");

		addRegex = new Button(dialog, SWT.NONE);
		FormData btnRegex = new FormData();
		btnRegex.top = new FormAttachment(0, 20);
		btnRegex.right = new FormAttachment(100, 0);
		addRegex.setLayoutData(btnRegex);
		addRegex.setText("Regulären Ausdruck hinzufügen");
		addRegex.setEnabled(false);
		addRegex.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				final Shell regexDialog = new Shell(getShell(), SWT.ON_TOP | SWT.DIALOG_TRIM);
				regexDialog.setText("Regulären Ausdruck für Attribut " + selectedAttribute + " angeben");
				FormLayout regexLayout = new FormLayout ();
				regexLayout.marginWidth = 10;
				regexLayout.marginHeight = 10;
				regexLayout.spacing = 10;
				regexDialog.setLayout (regexLayout);

				Button removeRegex = new Button(regexDialog, SWT.RADIO);
				FormData removeRegexLayout = new FormData();
				removeRegexLayout.top = new FormAttachment(0, 0);
				removeRegex.setLayoutData(removeRegexLayout);
				removeRegex.setText("Ausdruck zum Entfernen einer Zeichenkette");

				Button selectRegex = new Button(regexDialog, SWT.RADIO);
				FormData selectRegexLayout = new FormData();
				selectRegexLayout.top = new FormAttachment(0, 20);
				selectRegex.setLayoutData(selectRegexLayout);
				selectRegex.setText("Ausdruck zum Auswählen einer Zeichenkette");

				Text regexHelper = new Text(regexDialog, SWT.BORDER);
				FormData regexHelperLayout = new FormData();
				regexHelperLayout.top = new FormAttachment(0, 40);
				regexHelper.setLayoutData(regexHelperLayout);
				regexHelper.setText("Regulären Ausdruck eingeben");
				regexHelper.addFocusListener(new FocusAdapter() {
					@Override
					public void focusGained(FocusEvent e) {
						super.focusGained(e);
						if (regexHelper.getText().equals("Regulären Ausdruck eingeben")) {
							regexHelper.setText("");
						}
					}

					@Override
					public void focusLost(FocusEvent e) {
						super.focusLost(e);
						if (regexHelper.getText().equals("")) {
							regexHelper.setText("Regulären Ausdruck eingeben");
						}
					}
				});

				Button cancel = new Button(regexDialog, SWT.NONE);
				FormData cancelLayout = new FormData();
				cancelLayout.bottom = new FormAttachment(100, 0);
				cancelLayout.top = new FormAttachment(0, 65);
				cancelLayout.right = new FormAttachment(100, 0);
				cancel.setLayoutData(cancelLayout);
				cancel.setText("Abbrechen");
				cancel.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						super.mouseDown(e);
						regexDialog.dispose();
					}
				});

				Button confirm = new Button(regexDialog, SWT.NONE);
				FormData confirmLayout = new FormData();
				confirmLayout.bottom = new FormAttachment(100, 0);
				confirmLayout.top = new FormAttachment(0, 65);
				confirm.setLayoutData(confirmLayout);
				confirm.setText("Übernehmen");
				confirm.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						super.mouseDown(e);

						if (removeRegex.getSelection()) {
							if (regexHelper.getText().isEmpty() || regexHelper.getText().equals("Regulären Ausdruck eingeben")) {
								outputError("Kein gültiger Regulärer Ausdruck", "Bitte einen gültigen regulären Ausdruck angeben.");
							} else {
								for (TableItem item : attrTable.getSelection()) {
									item.setText(2, regexHelper.getText());
								}
							}
							regexDialog.dispose();
						} else if (selectRegex.getSelection()) {
							if (regexHelper.getText().isEmpty() || regexHelper.getText().equals("Regulären Ausdruck eingeben")) {
								outputError("Kein gültiger Regulärer Ausdruck", "Bitte einen gültigen regulären Ausdruck angeben.");
							} else {
								for (TableItem item : attrTable.getSelection()) {
									item.setText(3, regexHelper.getText());
								}
							}
							regexDialog.dispose();
						} else {
							outputError("Art des regulären Ausdrucks angeben", "Bitte angeben, ob es sich um einen regulären Ausdruck zum Entfernen oder zur Auswahl einer Teilzeichenkette handelt.");
						}
					}
				});

				regexDialog.pack();
				regexDialog.open();

				while (!regexDialog.isDisposed()) {
					if (!regexDialog.getDisplay().readAndDispatch())
						regexDialog.getDisplay().sleep();
				}
			}
		});

		attrTable = new Table(dialog, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL
		        | SWT.H_SCROLL | SWT.FULL_SELECTION);
		FormData fdTable = new FormData();
		fdTable.bottom = new FormAttachment(0, 500);
		fdTable.right = new FormAttachment(0, 645);
		fdTable.top = new FormAttachment(0, 70);
		fdTable.left = new FormAttachment(0, 0);
		attrTable.setLayoutData(fdTable);
		attrTable.setHeaderVisible(true);
		attrTable.setLinesVisible(true);
		TableColumn valCol = new TableColumn(attrTable, SWT.LEFT);
		valCol.setText("Beispielwert");
		valCol.setWidth(130);
		TableColumn nameCol = new TableColumn(attrTable, SWT.LEFT);
		nameCol.setText("Bitte Name eingeben");
		nameCol.setWidth(150);
		TableColumn regexRemoveCol = new TableColumn(attrTable, SWT.LEFT);
		regexRemoveCol.setText("Regulärer Ausdruck [REMOVE]");
		regexRemoveCol.setWidth(180);
		TableColumn regexSelectCol = new TableColumn(attrTable, SWT.LEFT);
		regexSelectCol.setText("Regulärer Ausdruck [SELECT]");
		regexSelectCol.setWidth(180);
		final TableEditor editor = new TableEditor (table);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.minimumWidth = 50;
		attrTable.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent selectionEvent) {
				if (attrTable.getSelectionCount() < 1) {
					return;
				}
				addRegex.setEnabled(true);
				selectedAttribute = attrTable.getSelection()[0].getText();
				System.out.println("Selected: " + selectedAttribute);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent selectionEvent) {
				widgetSelected(selectionEvent);
			}
		});
		attrTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				Point pt = new Point(e.x, e.y);
				TableItem item = attrTable.getItem(pt);
				System.out.println(item);
				if (item != null && item.getBounds(1).contains(pt)) {
					item.setChecked(true);
					final Text text = new Text (table, SWT.NONE);
					Listener textListener = ex -> {
						switch (ex.type) {
							case SWT.FocusOut:
								item.setText (1, text.getText ());
								text.dispose ();
								break;
							case SWT.Traverse:
								switch (ex.detail) {
									case SWT.TRAVERSE_RETURN:
										item.setText (1, text.getText ());
										//FALL THROUGH
									case SWT.TRAVERSE_ESCAPE:
										text.dispose ();
										ex.doit = false;
								}
								break;
						}
					};
					text.addListener (SWT.FocusOut, textListener);
					text.addListener (SWT.Traverse, textListener);
					text.addModifyListener(e1 -> {
						Text text1 = (Text) editor.getEditor();
						item.setText(1, text1.getText());
					});
					if (editor.getEditor() != null) {
						editor.getEditor().dispose();
					}
					editor.setEditor (text, item, 1);
					text.setText (item.getText (1));
					text.selectAll ();
					text.setFocus ();
				}
			}
		});
		
		for (String val : values) {
			TableItem item = new TableItem(attrTable, SWT.NONE);
			item.setText(val);
		}
		
		dialog.pack();
		dialog.open();
		
		while (!dialog.isDisposed()) {
			if (!dialog.getDisplay().readAndDispatch())
				dialog.getDisplay().sleep();
		}

		return ret;
	}

	@Override
	public void setProgress(int currentIndex, int totalNum, String msg) {
		if (currentProgressBar != null) {
			currentProgressBar.setMinimum(0);
			currentProgressBar.setMaximum(totalNum);
			currentProgressBar.setSelection(currentIndex);
		}
		
		if (currentProgressMsg != null) {
			currentProgressMsg.setText(msg);
		}
	}

	@Override
	public void close() {
		//wird am Ende des Outputs gerufen
	}
}
