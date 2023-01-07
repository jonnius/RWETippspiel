/*
 *	©Copyright 2014 Jonatan Zeidler <jonatan_zeidler@gmx.de>
 *
 *	This program is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Dieses Programm ist Freie Software: Sie können es unter den Bedingungen
 * der GNU General Public License, wie von der Free Software Foundation,
 * Version 3 der Lizenz oder (nach Ihrer Wahl) jeder neueren
 * veröffentlichten Version, weiterverbreiten und/oder modifizieren.
 *
 * Dieses Programm wird in der Hoffnung, dass es nützlich sein wird, aber
 * OHNE JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 * Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
 * Siehe die GNU General Public License für weitere Details.
 *
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
 * Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 *
 * Verwendete Bibliothek: JFreeChart (C) Copyright 2000-2013, by Object Refinery Limited and Contributors.
 * Quelltextdateien:
 * ChartColor: (C) Copyright 2003-2011, by Cameron Riley and Contributors.
 * ChartFactory: (C) Copyright 2001-2013, by Object Refinery Limited and Contributors.
 * ChartPanel: (C) Copyright 2000-2013, by Object Refinery Limited and Contributors.
 * JFreeChart: (C) Copyright 2000-2013, by Object Refinery Limited and Contributors.
 * NumberAxis: (C) Copyright 2000-2013, by Object Refinery Limited and Contributors.
 * CategoryPlot: (C) Copyright 2000-2013, by Object Refinery Limited and Contributors.
 * DefaultDrawingSupplier: (C) Copyright 2003-2008, by Object Refinery Limited.
 * DefaultCategoryDataset: (C) Copyright 2003-2008, by Object Refinery Limited and Contributors.
 *
 * Änderungen (Changes): Keine (None)
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.data.category.DefaultCategoryDataset;

public class Tippspiel extends JFrame {
	private static final long serialVersionUID = 1L;
	static boolean hintergrund = false;
	static File d_konfig, d_wolke, d_autostart;
	static String kicker, jahre[], saison, name = null;

	static Spiel spiele[], tipps[][];
	static String namen[];
	static int erinnern, spt_aktuell, spt_anzeige;
	static Properties konfig;
	static Spieler rangliste[];
	static String pfad;
	static boolean jar, windows = false, autostart = false;
	static Color c_mark = new Color(93, 152, 233), c_mark_hg = new Color(180, 202, 233), c_pkt[] = {
			new Color(200, 200, 200), new Color(255, 255, 150), new Color(150, 255, 255), new Color(100, 255, 100) };
	static Font f_rangliste = new Font("SansSerif", Font.BOLD, 18), f_ergebnisse = new Font("SansSerif", Font.BOLD, 12);
	static String fehler = "";
	static TreeMap<GregorianCalendar, String> chat_eigen = new TreeMap<GregorianCalendar, String>(),
			chat_alle = new TreeMap<GregorianCalendar, String>();

	static int spieltage = 34;

	// Administrator darf auch im Nachhinein seine Tipps ändern
	static boolean admin = false;
	// Admin startet als dieser Spieler, falls null, wird Spieler aus der Konfig
	// benutzt
	static String admin_spieler = null;

	/**
	 * Programmstart
	 *
	 * @param args: -admin (darf auch im Nachhinein Tipps ändern)<br>
	 *              -leise (prüft, ob getippt werden muss und öffnet nur in diesem
	 *              Fall das Fenster)
	 */
	public static void main(String[] args) {

		hintergrund = args.length >= 1 && args[0].equals("leise") || autostart;
		admin = admin || args.length >= 1 && args[0].equals("admin");

		if (admin)
			System.out.println("Als Administrator gestartet");
		if (hintergrund)
			System.out.println("Als Hintergrunddienst gestartet");

		init();

		aktualisiereDateien();

		for (int i = 0; i < spiele.length; i++) {
			if (spiele[i].istAustehend() && spiele[i].tageVerbleibend() < erinnern && !tipps[0][i].mitErgebnis()) {
				if (hintergrund)
					hintergrund = JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(null,
							"Die Begegnung gegen " + spiele[i].gegner + " wurde noch nicht getippt. Jetzt öffnen?",
							"Erinnerung ans Tippen", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
				else
					JOptionPane.showMessageDialog(null,
							"Die Begegnung gegen " + spiele[i].gegner + " wurde noch nicht getippt!",
							"Erinnerung ans Tippen", JOptionPane.INFORMATION_MESSAGE);
				break;
			}
		}

		if (!hintergrund)
			new Tippspiel();
	}

	/**
	 * Initialisierung inkl. Auslesen bzw. Anlegen der Konfigurationsdatei
	 */
	static void init() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		windows = System.getProperty("os.name").contains("Windows");
		System.out.println(System.getProperty("os.name"));

		pfad = Tippspiel.class.getProtectionDomain().getCodeSource().getLocation().toString();
		pfad = pfad.replace("%20", " ");
		pfad = pfad.substring(5);
		pfad = new File(pfad).getAbsolutePath();
		jar = pfad.endsWith(".jar");

		if (windows) {
			d_autostart = new File(System.getProperty("user.home")
					+ "/AppData/Roaming/Microsoft/Windows/Start Menu/Programs/Startup/Tippspiel.jar");
			autostart = pfad.equals(d_autostart.getAbsolutePath());
		} else {
			d_autostart = new File("/home/" + System.getProperty("user.name") + "/.config/autostart/Tippspiel.desktop");
		}

		d_konfig = new File(System.getProperty("user.home") + "/.Tippspiel.conf");
		konfig = new Properties();

		// Konfiguration einlesen
		try {
			FileReader fr = new FileReader(d_konfig);
			konfig.load(fr);
			fr.close();
		} catch (FileNotFoundException e) {
			// Normal - Noch keine Datei vorhanden
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Fehler beim Lesen der Konfigurationsdatei " + d_konfig.getAbsolutePath());
		}

		saison = konfig.getProperty("jahr", "2022-23");
		kicker = konfig.getProperty("kicker",
				"https://www.kicker.de/rot-weiss-erfurt/spielplan/regionalliga-nordost/{jahr}");
		erinnern = Integer.parseInt(konfig.getProperty("erinnern", "3"));
		setzeAutostart(erinnern > 0, true);

		if (konfig.getProperty("wolke") == null) {
			d_wolke = erfragWolke();
			if (d_wolke == null)
				System.exit(0);
		} else
			d_wolke = new File(konfig.getProperty("wolke"));

		System.out.println("Admin: " + admin);
		if (admin && admin_spieler != null)
			name = admin_spieler;
		else if (konfig.getProperty("name") == null) {
			name = JOptionPane.showInputDialog("Wie heißt du?", System.getProperty("user.name"));
			if (name == null)
				System.exit(0);
		} else
			name = konfig.getProperty("name");

//		Comparator<GregorianCalendar> vergleich = new Comparator<GregorianCalendar>() {
//			@Override
//			public int compare(GregorianCalendar o1, GregorianCalendar o2) {
//				return o1.compareTo(o2);			}			
//		};

		speicherKonfig();
	}

	static void speicherKonfig() {
		// Konfiguration speichern
		try {
			konfig.setProperty("erinnern", Integer.toString(erinnern));
			konfig.setProperty("wolke", d_wolke.getAbsolutePath());
			konfig.setProperty("jahr", saison);
			konfig.setProperty("kicker", kicker);
			if (!admin || admin_spieler == null)
				konfig.setProperty("name", name);

			FileWriter fw = new FileWriter(d_konfig);
			konfig.store(fw, "Tippspielkonfiguration");
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
					"Fehler beim Schreiben der Konfigurationsdatei " + d_konfig.getAbsolutePath());
			System.exit(1);
		}
	}

	static boolean setzeAutostart(boolean an, boolean still) {
		if (windows && new File(pfad).getAbsolutePath().equals(d_autostart.getAbsolutePath())) {
			if (!still)
				JOptionPane.showMessageDialog(null,
						"Das Programm wurde vom Autostart geladen.\n"
								+ "Um diese Funktion umzuschalten, muss das Programm manuell gestartet worden sein.\n"
								+ "(Diese Unanehmlichkeit wird Ihnen präsentiert von Windows)",
						"Umständlichkeit aufgrund von Windows", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		if (an) {
			if (d_autostart.exists())
				d_autostart.delete();

			if (windows) {
				try {
					JarInputStream lesen = new JarInputStream(new FileInputStream(pfad));
					JarOutputStream schreiben = new JarOutputStream(new FileOutputStream(d_autostart),
							lesen.getManifest());
					JarEntry alt;

					while ((alt = lesen.getNextJarEntry()) != null) {
						JarEntry neu = new JarEntry(alt.getName());
						schreiben.putNextEntry(neu);

						byte[] puffer = new byte[1024];
						int laenge;
						while ((laenge = lesen.read(puffer)) > 0) {
							schreiben.write(puffer, 0, laenge);
						}
					}

					schreiben.close();
					lesen.close();
					return true;
				} catch (IOException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null,
							"Konnte Programm nicht zum Autostart hinzufügen. Sag Onkel Windows ganz lieb Danke!.\n"
									+ d_autostart.getAbsolutePath(),
							"Fehler", JOptionPane.ERROR_MESSAGE);
				}

			} else {
				if (d_autostart.exists())
					d_autostart.delete();

				try {
					String str = "[Desktop Entry]\n" + "Name=Tippspiel\n" + "Exec=java -jar \"" + pfad + "\" leise\n"
							+ "Terminal=false\n" + "Type=Application\n" + "StartupNotify=false\n"
							+ "X-GNOME-Autostart-enabled=true\n" + "Name[de_DE]=Tippspiel";
					FileWriter schreiben = new FileWriter(d_autostart);
					schreiben.write(str);
					schreiben.close();
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null,
							"Konnte Programm nicht zum Autostart hinzufügen. Ist dies vielleicht kein Linux?\n"
									+ d_autostart.getAbsolutePath(),
							"Fehler", JOptionPane.ERROR_MESSAGE);
				}
			}
		} else {
			d_autostart.delete();
			return true;
		}
		return false;
	}

	static File erfragWolke() {
		JFileChooser wahl = new JFileChooser("Tippspielverzeichnis wählen (in der Wolke)");

		FileFilter filter = new FileFilter() {
			@Override
			public String getDescription() {
				return "Wolkenordner";
			}

			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}
		};
		wahl.setFileFilter(filter);
		wahl.setAcceptAllFileFilterUsed(false);
		wahl.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		wahl.setApproveButtonText("Tippspielverzeichnis festlegen");

		wahl.showOpenDialog(null);
		return wahl.getSelectedFile();
	}

	/**
	 * Ergebnisse aus dem Internet herunterladen und Dateien aktualisieren
	 */
	static void aktualisiereDateien() {
		Spiel spiele_kicker[] = new Spiel[spieltage];
		boolean heruntergeladen = false;

		spiele = new Spiel[spieltage];
		for (int i = 0; i < spiele.length; i++)
			spiele[i] = new Spiel();

		String html;

		int ja = new GregorianCalendar().get(GregorianCalendar.YEAR);
		if (new GregorianCalendar().get(GregorianCalendar.MONTH) < 6)
			ja--;
		jahre = new String[2];
		jahre[0] = Integer.toString(ja) + "-" + Integer.toString((ja + 1) % 1000);
		jahre[1] = Integer.toString(ja - 1) + "-" + Integer.toString((ja) % 1000);

		html = readHTML(kicker.replace("{jahr}", saison));
		if (html.equals("")) {
			fehler = "<html>Konnte Daten nicht mit kicker.de abgleichen.<br>Möglicherweise besteht keine Internetverbindung.</html>";
			System.out.println("Adresse: " + kicker.replace("{jahr}", saison) + " nicht erreichbar.");
		} else {
			if (html.contains("Es sind zur Zeit keine Daten vorhanden.")) {
				if (!hintergrund)
					JOptionPane.showMessageDialog(null,
							"Für die gewählte Saison " + saison + " sind keine Daten auf kicker.de vorhanden.\n"
									+ "In der Konfigdatei kann eine andere Saison eingetragen werden:\n"
									+ d_konfig.getAbsolutePath());
				System.exit(1);
			}

			int i1 = 0, i2 = 0;
			for (int i = 1; i <= spiele_kicker.length; i++) {
				i1 = html.indexOf("RL Nordost, " + i + ". Spt");
				i2 = html.substring(i1).indexOf("class=\"kick__table--hide-mobile\"") + i1;
				spiele_kicker[i - 1] = new Spiel(html.substring(i1, i2));
			}
			heruntergeladen = true;
		}

		boolean aenderung = false;
		// Ergebnisdatei laden
		try {
			ObjectInputStream lesen = new ObjectInputStream(new FileInputStream(new File(d_wolke, "Ergebnisse")));
			spiele = new Spiel[spieltage];
			for (int i = 0; i < spiele.length; i++) {
				spiele[i] = (Spiel) lesen.readObject();
				aenderung |= heruntergeladen && !spiele[i].equals(spiele_kicker[i]);
			}
			lesen.close();
		} catch (FileNotFoundException e1) {
			aenderung = true;
		} catch (Exception e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null,
					"Fehler beim Lesen der Ergebnisdatei " + new File(d_wolke, "Ergebnisse").getAbsolutePath());
			aenderung = true;
		}

		if (heruntergeladen)
			spiele = spiele_kicker;

		// Ergebnisdatei beschreiben
		if (aenderung) {
			try {
				ObjectOutputStream schreiben = new ObjectOutputStream(
						new FileOutputStream(new File(d_wolke, "Ergebnisse")));
				for (Spiel sp : spiele)
					schreiben.writeObject(sp);
				schreiben.close();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null,
						"Fehler beim Schreiben der Ergebnisdatei " + new File(d_wolke, "Ergebnisse").getAbsolutePath());
			}
		}

		for (int i = 0; i < spieltage; i++) {
			spt_aktuell = i + 1;
			if (!spiele[i].mitErgebnis())
				break;
		}

		// Andere Tipps lesen
		File dat[] = d_wolke.listFiles();
		int z = 0;
		for (int i = 0; i < dat.length; i++) {
			if (dat[i].isFile() && dat[i].getName().endsWith(".tipps") && !dat[i].getName().equals(name + ".tipps"))
				z++;
		}

		tipps = new Spiel[z + 1][spieltage];
		namen = new String[z + 1];
		rangliste = new Spieler[0];
		namen[0] = name;

		z = 1;
		for (int i = 0; i < dat.length; i++) {
			if (dat[i].isFile() && dat[i].getName().endsWith(".tipps") && !dat[i].getName().equals(name + ".tipps")) {
				namen[z] = dat[i].getName().substring(0, dat[i].getName().indexOf(".tipps"));
				tipps[z] = ladeTipps(dat[i]);
				new Spieler(tipps[z], spiele, namen[z]);
				z++;
			}
		}

		// Eigene Tipps lesen
		tipps[0] = ladeTipps(new File(d_wolke, name + ".tipps"));
		if (!new File(d_wolke, name + ".tipps").exists())
			schreibeTipps();
		new Spieler(tipps[0], spiele, name);
	}

	static Spiel[] ladeTipps(File f) {
		Spiel t[] = new Spiel[spieltage];
		for (int i = 0; i < t.length; i++) {
			t[i] = spiele[i].gibLeerenTipp();
		}

		try {
			ObjectInputStream lesen = new ObjectInputStream(new FileInputStream(f));

			for (int i = 0; i < t.length; i++) {
				t[i] = (Spiel) lesen.readObject();
			}
			lesen.close();
		} catch (FileNotFoundException e1) {
			// normal
		} catch (Exception e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Fehler beim Lesen der Tippdatei " + f.getAbsolutePath());
		}

		return t;
	}

	/**
	 * Abgegebene Tipps speichern
	 */
	static void schreibeTipps() {
		try {
			ObjectOutputStream schreiben = new ObjectOutputStream(
					new FileOutputStream(new File(d_wolke, name + ".tipps")));
			for (Spiel sp : tipps[0])
				schreiben.writeObject(sp);
			schreiben.close();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"Fehler beim Schreiben der Tippdatei " + new File(d_wolke, name + ".tipps").getAbsolutePath());
		}
	}

	static void ladeChat() {
		GregorianCalendar kal;
		String kom;

		chat_eigen.clear();
		chat_alle.clear();
		for (String n : namen) {
			File d_chat = new File(d_wolke, n + ".chat");

			try {
				ObjectInputStream lesen = new ObjectInputStream(new FileInputStream(d_chat));
				int groesse = lesen.readInt();

				for (int i = 0; i < groesse; i++) {
					kal = (GregorianCalendar) lesen.readObject();
					kom = (String) lesen.readObject();
					chat_alle.put(kal, n + ": " + kom);
					if (n.equals(name)) {
						chat_eigen.put(kal, kom);
					}
				}

				lesen.close();
			} catch (FileNotFoundException e) {
				// Normal - Noch kein Kommentar abgegeben
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null,
						"Fehler beim Lesen von " + name + "s Kommentardatei " + d_chat.getAbsolutePath());
			}
		}
	}

	/**
	 * Abgegebene Tipps speichern
	 */
	static void schreibeChat() {
		File d_chat = new File(d_wolke, name + ".chat");

		try {
			ObjectOutputStream schreiben = new ObjectOutputStream(new FileOutputStream(d_chat));
			schreiben.writeInt(chat_eigen.size());

			Iterator<GregorianCalendar> zeiten = chat_eigen.keySet().iterator();
			while (zeiten.hasNext()) {
				GregorianCalendar kal = zeiten.next();
				schreiben.writeObject(kal);
				schreiben.writeObject(chat_eigen.get(kal));
			}
			schreiben.close();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null,
					"Fehler beim Schreiben von " + name + "s Kommentardatei " + d_chat.getAbsolutePath());
		}
	}

	/**
	 * Rangliste sortieren
	 */
	static void sortiereRangliste() {
		for (int i = 0; i < rangliste.length - 1; i++) {
			for (int j = 0; j < rangliste.length - 1 - i; j++) {
				if (rangliste[j + 1].istBesserAls(rangliste[j], spt_anzeige)) {
					Spieler sp = rangliste[j];
					rangliste[j] = rangliste[j + 1];
					rangliste[j + 1] = sp;
				}
			}
		}
	}

	/**
	 * Eine Webseite herunterladen
	 *
	 * @param urltext
	 * @return HTML-Quelltext
	 */

	public static String readHTML(String urltext) {
//		System.out.println("***    |Reading HTML '"+urltext+"' ....");
		InputStream in = null;
		StringWriter schreiber = new StringWriter();
		String output = "";
		int l = 0;
		try {
			URL url = new URL(urltext);
			in = url.openStream();
			byte puffer[] = new byte[8192];

			while ((l = in.read(puffer)) >= 0) {
				for (int i = 0; i < l; i++) {
					schreiber.write(puffer[i] & 0xff);
				}
			}
			output = schreiber.toString();
			schreiber.close();
			System.out.println("HTML fertig gelesen!");// +output);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return output;
	}

	private JPanel historie, verlauf, einstellungen;
	private JPanel einst_wolkenpfad, einst_intervall;
	private JPanel verl_spieltag, verl_tabelle, verl_graph, verl_komment, verl_unterh;
	private JSplitPane daten;

	private JSlider sl_intervall, sl_spieltag;
	private JLabel sl_info, sl_spt;
	private JCheckBox cb_erinnern;
	private JTextField tf_komm;
	private JComboBox<String> co_saison;

	private Aktualisierer aktualisierer;

	/**
	 * Konstruktor des Hauptfensters
	 */
	public Tippspiel() {
		super("Das Zeidertippspiel - " + name);

		Image symbol;
		if (jar)
			symbol = Toolkit.getDefaultToolkit().getImage(Tippspiel.class.getResource("RWE.png"));
		else
			symbol = Toolkit.getDefaultToolkit().getImage("RWE.png");
		symbol = symbol.getScaledInstance(20, 32, Image.SCALE_AREA_AVERAGING);
		setIconImage(symbol);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(Toolkit.getDefaultToolkit().getScreenSize());
		setExtendedState(JFrame.MAXIMIZED_BOTH);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(daten = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true), BorderLayout.CENTER);
		getContentPane().add(einstellungen = new JPanel(), BorderLayout.SOUTH);
		if (jar) {
			getContentPane().add(
					new JLabel(new ImageIcon(
							Toolkit.getDefaultToolkit().getImage(Tippspiel.class.getResource("RWE.png")))),
					BorderLayout.WEST);
			getContentPane().add(
					new JLabel(new ImageIcon(
							Toolkit.getDefaultToolkit().getImage(Tippspiel.class.getResource("RWE2.png")))),
					BorderLayout.EAST);
		} else {
			getContentPane().add(new JLabel(new ImageIcon(Toolkit.getDefaultToolkit().getImage("RWE.png"))),
					BorderLayout.WEST);
			getContentPane().add(new JLabel(new ImageIcon(Toolkit.getDefaultToolkit().getImage("RWE2.png"))),
					BorderLayout.EAST);
		}

		initErgebnisse();

		initEinstellungen();

		setVisible(true);

		aktualisierer = new Aktualisierer(this);
		aktualisierer.start();
	}

	private void initErgebnisse() {
		JScrollPane sc_daten, sc_verlauf;
		daten.setLeftComponent(sc_daten = new JScrollPane(historie = new JPanel()));
		sc_daten.setViewportBorder(null);
		daten.setRightComponent(sc_verlauf = new JScrollPane(verlauf = new JPanel()));
		sc_verlauf.setViewportBorder(null);

		spt_anzeige = spt_aktuell;

		verlauf.setLayout(new BorderLayout());
		verlauf.add(verl_spieltag = new JPanel(), BorderLayout.NORTH);
		verlauf.add(verl_tabelle = new JPanel(), BorderLayout.CENTER);
		JPanel verl_unten;
		verlauf.add(verl_unten = new JPanel(), BorderLayout.SOUTH);
		verl_unten.setLayout(new BorderLayout());
		verl_unten.add(verl_graph = new JPanel(), BorderLayout.NORTH);
		verl_unten.add(verl_komment = new JPanel(), BorderLayout.CENTER);

		verl_spieltag.setLayout(new BorderLayout());
		verl_spieltag.add(sl_spt = new JLabel(" Spieltag " + spt_anzeige), BorderLayout.WEST);
		sl_spt.setFont(f_rangliste);
		verl_spieltag.add(sl_spieltag = new JSlider(1, spieltage), BorderLayout.CENTER);
		sl_spieltag.setMinorTickSpacing(1);
//		sl_spieltag.setMajorTickSpacing(37);
//		sl_spieltag.setPaintLabels(true);
		sl_spieltag.setPaintTicks(true);
		sl_spieltag.setValue(spt_anzeige - 1);
		sl_spieltag.setForeground(c_mark);
		sl_spt.setForeground(c_mark);
		sl_spieltag.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				spt_anzeige = sl_spieltag.getValue();
				sl_spt.setText(" Spieltag " + spt_anzeige);
				if (spt_anzeige == spt_aktuell - 1) {
					sl_spieltag.setForeground(c_mark);
					sl_spt.setForeground(c_mark);
				} else {
					sl_spieltag.setForeground(Color.black);
					sl_spt.setForeground(Color.black);
				}
				aktualisiereTabelle();
			}
		});

		aktualisiereAnzeige();
	}

	// Alle Anzeigebereiche aktualisieren
	public void aktualisiereAnzeige() {
		aktualisiereGraph();
		aktualisiereErgebnisse();
		aktualisiereTabelle();
		aktualisiereKommentare();
		historie.revalidate();
	}

	private void aktualisiereKommentare() {
		ladeChat();

		if (tf_komm == null) {
			// Erste Ausführung
			verl_komment.setLayout(new BorderLayout());
//			JLabel text;
//			verl_komment.add(text = new JLabel("Kommentare"), BorderLayout.NORTH);
//			text.setFont(f_ergebnisse);

			tf_komm = new JTextField();
			tf_komm.setFont(f_ergebnisse);
			tf_komm.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						String k = tf_komm.getText().trim();
						if (!k.equals("")) {
							tf_komm.setText("");
							chat_eigen.put(new GregorianCalendar(), k);
							schreibeChat();
							aktualisiereKommentare();
							revalidate();
							tf_komm.requestFocus();
						}
					}
				}
			});

			verl_komment.add(tf_komm, BorderLayout.NORTH);
		}
		JLabel text;
//		verl_komment.add(text = new JLabel("Kommentare"), BorderLayout.NORTH);
//		
		JPanel unterhaltung = new JPanel();
		unterhaltung.setLayout(new GridLayout(chat_alle.size(), 1));

		Iterator<GregorianCalendar> zeiten = chat_alle.descendingKeySet().iterator();
		while (zeiten.hasNext()) {
			GregorianCalendar kal = zeiten.next();
			String kommentar = chat_alle.get(kal);

			// Zeilenumbrüche bei langen Kommentaren
			int i = 0, leerzeichen = 0;
			while (kommentar.length() - i > 80) {
				leerzeichen = kommentar.substring(i, i + 80).lastIndexOf(' ');
				if (leerzeichen > 0)
					kommentar = kommentar.substring(0, i + leerzeichen) + "<br>"
							+ kommentar.substring(i = i + leerzeichen + 1);
				else
					kommentar = kommentar.substring(0, i + 80) + "<br>" + kommentar.substring(i = i + 80);
				i += 3;
			}

			unterhaltung.add(text = new JLabel("<html>" + kommentar + "</html>"));
			text.setFont(f_ergebnisse);
		}

		if (verl_unterh != null)
			verl_komment.remove(verl_unterh);
		verl_komment.add(verl_unterh = unterhaltung);
	}

	private void aktualisiereErgebnisse() {
		JPanel hist = new JPanel();

		JLabel label;
		GridBagConstraints gbc = new GridBagConstraints();
		GridBagLayout gbl2 = new GridBagLayout();
		gbc = new GridBagConstraints();
		hist.setLayout(gbl2);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(2, 2, 2, 5);
		gbc.fill = GridBagConstraints.BOTH;

		gbc.gridx++;
		gbl2.setConstraints(label = new JLabel("Datum", JLabel.CENTER), gbc);
		label.setFont(f_ergebnisse);
		hist.add(label);

		gbc.gridx++;
		gbl2.setConstraints(label = new JLabel("Gegner", JLabel.CENTER), gbc);
		label.setFont(f_ergebnisse);
		hist.add(label);

		gbc.gridx++;
		gbl2.setConstraints(label = new JLabel("Ort", JLabel.CENTER), gbc);
		label.setFont(f_ergebnisse);
		hist.add(label);

		gbc.gridx++;
		gbl2.setConstraints(label = new JLabel("Ergebnis", JLabel.CENTER), gbc);
		label.setFont(f_ergebnisse);
		hist.add(label);

		for (int i = 0; i < tipps.length; i++) {
			gbc.gridx++;
			gbl2.setConstraints(label = new JLabel(namen[i], JLabel.CENTER), gbc);
			label.setFont(f_ergebnisse);
			hist.add(label);
		}

		gbc.gridx++;
		gbl2.setConstraints(label = new JLabel("  ", JLabel.CENTER), gbc);
		label.setFont(f_ergebnisse);
		hist.add(label);

		for (int i = 0; i < spiele.length; i++) {
			gbc.gridy++;
			JPanel bereich;

			gbc.gridx = 0;
			bereich = new JPanel();
			bereich.add(label = new JLabel(Integer.toString(i + 1)));
			label.setFont(f_ergebnisse);
			gbl2.setConstraints(bereich, gbc);
			hist.add(bereich);
			if (i + 1 == spt_aktuell)
				bereich.setBackground(c_mark_hg);

			gbc.gridx++;
			bereich = new JPanel();
			bereich.add(label = new JLabel(spiele[i].gibDatumText()));
			label.setFont(f_ergebnisse);
			gbl2.setConstraints(bereich, gbc);
			hist.add(bereich);
			if (i + 1 == spt_aktuell)
				bereich.setBackground(c_mark_hg);

			gbc.gridx++;
			bereich = new JPanel();
			bereich.add(label = new JLabel(spiele[i].gegner));
			label.setFont(f_ergebnisse);
			gbl2.setConstraints(bereich, gbc);
			hist.add(bereich);
			if (i + 1 == spt_aktuell)
				bereich.setBackground(c_mark_hg);

			gbc.gridx++;
			bereich = new JPanel();
			bereich.add(label = new JLabel(spiele[i].gibHeimText(), JLabel.CENTER));
			label.setFont(f_ergebnisse);
			gbl2.setConstraints(bereich, gbc);
			hist.add(bereich);
			if (i + 1 == spt_aktuell)
				bereich.setBackground(c_mark_hg);

			gbc.gridx++;
			bereich = new JPanel();
			bereich.add(label = new JLabel(spiele[i].gibErgebnisText(), JLabel.CENTER));
			gbl2.setConstraints(bereich, gbc);
			label.setFont(f_ergebnisse);
			hist.add(bereich);
			if (i + 1 == spt_aktuell)
				bereich.setBackground(c_mark_hg);

			ActionListener tippen = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					tippe(Integer.parseInt(e.getActionCommand()));
				}
			};
			boolean zeigen = tipps[0][i].mitErgebnis() || !tipps[0][i].istAustehend();
			for (int k = 0; k < tipps.length; k++) {
				gbc.gridx++;
				if (k == 0 && (tipps[k][i].istAustehend() && !tipps[k][i].mitErgebnis() || admin)) {
					JButton tipp = new JButton(tipps[k][i].gibErgebnisText());
					tipp.setFont(f_ergebnisse);
					tipp.setActionCommand(Integer.toString(i));
					tipp.addActionListener(tippen);
					if (i + 1 == spt_aktuell && !tipps[k][i].mitErgebnis()) {
						tipp.setForeground(Color.red);
						tipp.setText("Tippen!");
						tipp.setFont(new Font(tipp.getFont().getName(), Font.BOLD, tipp.getFont().getSize()));
					}
					gbl2.setConstraints(tipp, gbc);
					hist.add(tipp);
				} else {
					bereich = new JPanel();
					if (zeigen || k == 0 || !tipps[k][i].mitErgebnis())
						label = new JLabel(tipps[k][i].gibErgebnisText(), JLabel.CENTER);
					else
						label = new JLabel("?-?", JLabel.CENTER);
					label.setFont(f_ergebnisse);
					bereich.add(label);
					gbl2.setConstraints(bereich, gbc);
					hist.add(bereich);

					if (!tipps[k][i].istAustehend() && tipps[k][i].mitErgebnis()) {
						bereich.setBackground(c_pkt[spiele[i].gibPunkte(tipps[k][i])]);
					}
				}
			}
		}

		sl_spieltag.setValue(spt_anzeige);

		historie.removeAll();
		historie.add(hist);
	}

	private void aktualisiereGraph() {
		verl_graph.removeAll();

		// Daten zusammentragen
		DefaultCategoryDataset verlaeufe = new DefaultCategoryDataset();
		for (int i = 0; i < rangliste.length; i++) {
			Spieler spieler = rangliste[i];

			if (spieler.gibErstenTippSpieltag() > 0)
				for (int k = spieler.gibErstenTippSpieltag(); k < spt_aktuell; k++) {
					verlaeufe.addValue(spieler.gibPlatzierung(k), spieler.gibName(), Integer.toString(k));
				}
		}

		// Diagramm erstellen
		JFreeChart chart = ChartFactory.createLineChart("Tabellenfahrt", "Spieltag", "Platzierung", verlaeufe);
		chart.setBackgroundPaint(new Color(0, 0, 0, 0));

		// Aussehen bearbeiten
		CategoryPlot plot = chart.getCategoryPlot();
		plot.setDrawingSupplier(new DefaultDrawingSupplier(
				new Paint[] { ChartColor.BLUE, ChartColor.RED, ChartColor.DARK_GREEN, ChartColor.MAGENTA,
						ChartColor.CYAN, ChartColor.GRAY, ChartColor.YELLOW, ChartColor.PINK, ChartColor.ORANGE },
				DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE, DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
				DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE, DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
		plot.setBackgroundAlpha(0);
		plot.setOutlineVisible(false);
		plot.setDomainGridlinesVisible(true);
		plot.setRangeGridlinesVisible(true);
		plot.setDomainGridlinePaint(Color.GRAY);
		plot.setRangeGridlinePaint(Color.GRAY);

		// Achsenbeschriftung bearbeiten
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setAutoRangeIncludesZero(false);
		rangeAxis.setInverted(true);

		// Bereich erstellen und einbetten
		ChartPanel bereich = new ChartPanel(chart);
		bereich.setDefaultDirectoryForSaveAs(d_wolke);
		verl_graph.add(bereich);
	}

	private void aktualisiereTabelle() {
		sortiereRangliste();
		JPanel tabelle = new JPanel();

		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		tabelle.setLayout(gbl);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(2, 5, 2, 5);
		gbc.fill = GridBagConstraints.BOTH;

		JLabel label;
		JPanel bereich = new JPanel();
		gbl.setConstraints(label = new JLabel("Platz"), gbc);
		label.setFont(f_rangliste);
		tabelle.add(label);

		gbc.gridx++;
		gbl.setConstraints(label = new JLabel("Tipper"), gbc);
		label.setFont(f_rangliste);
		tabelle.add(label);

		gbc.gridx++;
		gbl.setConstraints(label = new JLabel("Spiele", JLabel.CENTER), gbc);
		label.setFont(f_rangliste);
		tabelle.add(label);

		gbc.gridx++;
		bereich = new JPanel();
		label = new JLabel("Exakt", JLabel.CENTER);
		label.setFont(f_rangliste);
		bereich.setBackground(c_pkt[3]);
		bereich.add(label);
		gbl.setConstraints(bereich, gbc);
		tabelle.add(bereich);

		gbc.gridx++;
		bereich = new JPanel();
		label = new JLabel("Differenz", JLabel.CENTER);
		label.setFont(f_rangliste);
		bereich.setBackground(c_pkt[2]);
		bereich.add(label);
		gbl.setConstraints(bereich, gbc);
		tabelle.add(bereich);

		gbc.gridx++;
		bereich = new JPanel();
		label = new JLabel("Tendenz", JLabel.CENTER);
		label.setFont(f_rangliste);
		bereich.setBackground(c_pkt[1]);
		bereich.add(label);
		gbl.setConstraints(bereich, gbc);
		tabelle.add(bereich);

		gbc.gridx++;
		gbl.setConstraints(label = new JLabel("Punkte", JLabel.CENTER), gbc);
		label.setFont(f_rangliste);
		tabelle.add(label);

		for (int i = 0; i < rangliste.length; i++) {
			gbc.gridy++;
			gbc.gridx = 0;

			int platz = rangliste[i].gibPlatzierung(spt_anzeige);
			if (platz == i + 1) {
				gbl.setConstraints(label = new JLabel(Integer.toString(platz), JLabel.CENTER), gbc);
				label.setFont(f_rangliste);
				tabelle.add(label);
			}

			gbc.gridx++;
			gbl.setConstraints(label = new JLabel(rangliste[i].gibName()), gbc);
			label.setFont(f_rangliste);
			tabelle.add(label);

			gbc.gridx++;
			gbl.setConstraints(label = new JLabel(
					Integer.toString(rangliste[i].gibtAnzahlTipps(Math.min(spt_anzeige, spt_aktuell - 1))),
					JLabel.CENTER), gbc);
			label.setFont(f_rangliste);
			tabelle.add(label);

			gbc.gridx++;
			gbl.setConstraints(
					label = new JLabel(Integer.toString(rangliste[i].gibAnzahlExakt(spt_anzeige, 3)), JLabel.CENTER),
					gbc);
			label.setFont(f_rangliste);
			tabelle.add(label);

			gbc.gridx++;
			gbl.setConstraints(
					label = new JLabel(Integer.toString(rangliste[i].gibAnzahlExakt(spt_anzeige, 2)), JLabel.CENTER),
					gbc);
			label.setFont(f_rangliste);
			tabelle.add(label);

			gbc.gridx++;
			gbl.setConstraints(
					label = new JLabel(Integer.toString(rangliste[i].gibAnzahlExakt(spt_anzeige, 1)), JLabel.CENTER),
					gbc);
			label.setFont(f_rangliste);
			tabelle.add(label);

			gbc.gridx++;
			gbl.setConstraints(label = new JLabel(Integer.toString(rangliste[i].gibPunkte(spt_anzeige)), JLabel.CENTER),
					gbc);
			label.setFont(f_rangliste);
			tabelle.add(label);
		}

		verl_tabelle.removeAll();
		verl_tabelle.add(tabelle);
	}

	private void initEinstellungen() {
		einstellungen.setLayout(new BorderLayout());
		einstellungen.add(einst_intervall = new JPanel(), BorderLayout.WEST);
		einstellungen.add(einst_wolkenpfad = new JPanel(), BorderLayout.EAST);

		JPanel infos;
		JScrollPane sc_infos;
		einstellungen.add(sc_infos = new JScrollPane(infos = new JPanel(), JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
		sc_infos.setViewportBorder(null);

		JLabel l_fehler = new JLabel(fehler);
		l_fehler.setFont(f_ergebnisse);
		l_fehler.setForeground(Color.RED);
		infos.add(l_fehler);

		Image symbol;
		JButton bu_lizenz;
		if (jar)
			symbol = Toolkit.getDefaultToolkit().getImage(Tippspiel.class.getResource("gpl.png"));
		else
			symbol = Toolkit.getDefaultToolkit().getImage("gpl.png");
		bu_lizenz = new JButton(new ImageIcon(symbol));
		infos.add(bu_lizenz);

		bu_lizenz.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI("http://www.gnu.de/documents/gpl.de.html"));
				} catch (Exception e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(Tippspiel.this, "Browser konnte nicht aufgerufen werden.");
				}
			}
		});

		JButton bu_copyright;
		bu_copyright = new JButton("<html>© Copyright 2014-2022 Jonatan Zeidler<br>jonatan_zeidler@gmx.de</html>");
		bu_copyright.setFont(f_ergebnisse);
		infos.add(bu_copyright);

		bu_copyright.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI("mailto:jonatan_zeidler@gmx.de"));
				} catch (Exception e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(Tippspiel.this, "Mailprogramm konnte nicht aufgerufen werden.");
				}
			}
		});

		JLabel l_quelle = new JLabel("Datenquelle:");
		l_quelle.setFont(f_ergebnisse);
		infos.add(l_quelle);

		JButton bu_quelle;
		if (jar)
			symbol = Toolkit.getDefaultToolkit().getImage(Tippspiel.class.getResource("kicker.png"));
		else
			symbol = Toolkit.getDefaultToolkit().getImage("kicker.png");
		bu_quelle = new JButton(new ImageIcon(symbol));
		bu_quelle.setFont(f_ergebnisse);
		infos.add(bu_quelle);

		bu_quelle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(kicker.replace("{jahr}", saison)));
				} catch (Exception e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(Tippspiel.this, "Browser konnte nicht aufgerufen werden.");
				}
			}
		});

		// Wolkenpfad
		einst_wolkenpfad.setLayout(new GridLayout(2, 1));
		JButton bu_wolke;
		einst_wolkenpfad.add(bu_wolke = new JButton("Tippspielverzeichnis ändern"));
		bu_wolke.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int a = JOptionPane.showConfirmDialog(null,
						"Sollen die Daten aus dem bisherigen Verzeichnis übernommen werden?\n"
								+ "Alternativ können die Daten auch manuell kopiert werden.",
						"Neues Tippspielverzeichnis", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (a == JOptionPane.CANCEL_OPTION)
					return;

				File f = erfragWolke();
				if (f != null) {
					File alt = d_wolke;
					d_wolke = f;
					speicherKonfig();
					File k;
					if (a == JOptionPane.YES_OPTION) {
						if ((k = new File(alt, "Ergebnisse")).exists())
							k.renameTo(new File(d_wolke, "Ergebnisse"));
						if ((k = new File(alt, name + ".tipps")).exists())
							k.renameTo(new File(d_wolke, name + ".tipps"));
					}
					aktualisiereDateien();
					aktualisiereAnzeige();
				}
			}
		});
		einst_wolkenpfad.add(co_saison = new JComboBox<String>(jahre));
		co_saison.setSelectedItem(saison);
		co_saison.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String neu_saison = (String) Tippspiel.this.co_saison.getSelectedItem();
				if (!neu_saison.equals(saison)) {
					// Alte Daten sichern
					File sichern = new File(d_wolke, saison);
					sichern.mkdirs();
					new File(d_wolke, "Ergebnisse").renameTo(new File(sichern, "Ergebnisse"));
					for (String s : namen) {
						new File(d_wolke, s + ".tipps").renameTo(new File(sichern, s + ".tipps"));
						new File(d_wolke, s + ".chat").renameTo(new File(sichern, s + ".chat"));
					}

					// Ggf. gesicherte Daten wiederherstellen
					File wiederherstellen = new File(d_wolke, neu_saison);
					if (wiederherstellen.isDirectory()) {
						for (File f : wiederherstellen.listFiles()) {
							if (f.isFile())
								f.renameTo(new File(d_wolke, f.getName()));
						}
					}

					saison = neu_saison;
					speicherKonfig();
					aktualisiereDateien();
					spt_anzeige = spt_aktuell;
					aktualisiereAnzeige();
				}
			}
		});

		// Intervall
		einst_intervall.add(cb_erinnern = new JCheckBox("Erinnern", erinnern > 0));
		cb_erinnern.setFont(f_ergebnisse);
		cb_erinnern.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (setzeAutostart(cb_erinnern.isSelected(), false)) {
					if (cb_erinnern.isSelected())
						erinnern = Math.abs(erinnern);
					else
						erinnern = -Math.abs(erinnern);
					sl_intervall.setEnabled(cb_erinnern.isSelected());
					sl_info.setEnabled(cb_erinnern.isSelected());
					speicherKonfig();
				} else
					cb_erinnern.setSelected(erinnern > 0);
			}
		});

		einst_intervall.add(sl_intervall = new JSlider(1, 7));
		sl_intervall.setMajorTickSpacing(1);
//		sl_intervall.setPaintLabels(true);
		sl_intervall.setPaintTicks(true);
		sl_intervall.setValue(Math.abs(erinnern));
		sl_intervall.setEnabled(cb_erinnern.isSelected());
		sl_intervall.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				erinnern = sl_intervall.getValue();
				speicherKonfig();
				if (sl_intervall.getValue() > 1)
					sl_info.setText(erinnern + " Tage vor dem Spiel");
				else
					sl_info.setText("Ein Tag vor dem Spiel");
			}
		});

		if (Math.abs(erinnern) == 1)
			einst_intervall.add(sl_info = new JLabel("Ein Tag vor dem Spiel"));
		else
			einst_intervall.add(sl_info = new JLabel(Math.abs(erinnern) + " Tage vor dem Spiel"));
		sl_info.setEnabled(cb_erinnern.isSelected());
		sl_intervall.setFont(f_ergebnisse);
		sl_info.setFont(f_ergebnisse);
	}

	/**
	 *
	 * @param spieltag von 0 an gezählt
	 */
	private void tippe(int spieltag) {
		if (tipps[0][spieltag].istAustehend() || admin) {
			new Tippen(this, tipps[0][spieltag]);
			aktualisiereErgebnisse();
		} else {
			aktualisiereErgebnisse();
			JOptionPane.showMessageDialog(this, "Spiele können nur vor der Anstoßzeit getippt werden",
					"Tipp nicht möglich", JOptionPane.INFORMATION_MESSAGE);
		}
	}
}

/**
 * Repräsentiert eine Ansetzung bzw. den Tipp dazu
 *
 * @author jonius
 */
class Spiel implements Serializable {
	private static final long serialVersionUID = 1L;

	String gegner, zeit_string;
	boolean heim;
	int tore_rwe, tore_gegner;
	GregorianCalendar zeit;

	static String verein_a = "<div class=\"kick__v100-gameCell__team__name\">",
			verein_e = "<span class=\"kick__v100-gameCell__team__info\"></span></div>", spt = "<td>Spt.",
			zeit_a = "<div class=\"kick__table--gamelist__date-mobile\">", zeit_e = "</div>",
			ergebnis_a = "<div class=\"kick__v100-scoreBoard__scoreHolder__score\">", ergebnis_e = "</div>";
	static int z = 1;

	static enum Ausgang {
		SIEG, UNENTSCHIEDEN, NIEDERLAGE, AUSSTEHEND
	};

	public Spiel() {
		gegner = "<leer>";
		zeit_string = "<leer>";
		tore_gegner = -1;
		tore_rwe = -1;
		zeit = new GregorianCalendar();
		heim = false;
	}

	/**
	 * Konstruktor, der aus HTML-Quelltext von kicker.de das Objekt erzeugt
	 *
	 * @param html
	 */
	public Spiel(String html) {
		String zeit_html;

		gegner = html;
		gegner = gegner.substring(gegner.indexOf(verein_a) + verein_a.length());
		gegner = gegner.substring(0, gegner.indexOf(verein_e));
		heim = gegner.contains("Erfurt");

		if (heim) {
			gegner = html.substring(html.indexOf(verein_e));
			gegner = gegner.substring(gegner.indexOf(verein_a) + verein_a.length());
			gegner = gegner.substring(0, gegner.indexOf(verein_e));
		}

		gegner = gegner.trim();

		html = html.substring(html.indexOf(zeit_a) + zeit_a.length());
		zeit_html = html.substring(0, html.indexOf(zeit_e));
		zeit_string = zeit_html;

		if (zeit_html.contains(":")) {
			zeit = new GregorianCalendar(Integer.parseInt(zeit_html.substring(10, 14)),
					Integer.parseInt(zeit_html.substring(7, 9)) - 1, Integer.parseInt(zeit_html.substring(4, 6)),
					Integer.parseInt(zeit_html.substring(13, 15)), Integer.parseInt(zeit_html.substring(16)));
		} else {
			zeit = new GregorianCalendar(Integer.parseInt(zeit_html.substring(10, 14)),
					Integer.parseInt(zeit_html.substring(7, 9)) - 1, Integer.parseInt(zeit_html.substring(4, 6)), 14,
					00);
		}

		if (html.contains(ergebnis_a)) {
			html = html.substring(html.indexOf(ergebnis_a) + ergebnis_a.length());
			int t1 = Integer.parseInt(html.substring(0, html.indexOf(ergebnis_e)));

			html = html.substring(html.indexOf(ergebnis_a) + ergebnis_a.length());
			int t2 = Integer.parseInt(html.substring(0, html.indexOf(ergebnis_e)));

			if (heim) {
				tore_rwe = t1;
				tore_gegner = t2;
			} else {
				tore_rwe = t2;
				tore_gegner = t1;
			}
		} else {
			tore_rwe = -1;
			tore_gegner = -1;
		}

		// System.out.println("\nGegner: "+gegner);
		// System.out.println("Zeit:
		// "+zeit.get(GregorianCalendar.DATE)+"."+(zeit.get(GregorianCalendar.MONTH)+1)+"."+zeit.get(GregorianCalendar.YEAR)+
		// "
		// "+zeit.get(GregorianCalendar.HOUR_OF_DAY)+":"+zeit.get(GregorianCalendar.MINUTE)+"
		// am "+zeit.get(GregorianCalendar.DAY_OF_WEEK));
		// System.out.println("Heim: "+heim);
		// System.out.println("Ergebnis: "+tore_rwe+":"+tore_gegner);
		// System.out.println("In "+tageVerbleibend()+" Tagen.");
	}

	/**
	 * Erstellt ein Spiel mit den angegebenen Werten
	 *
	 * @param gegner
	 * @param zeit
	 * @param heim
	 */
	public Spiel(String gegner, GregorianCalendar zeit, boolean heim, String zeit_string) {
		this.gegner = gegner;
		this.zeit = zeit;
		this.heim = heim;
		this.tore_gegner = -1;
		this.tore_rwe = -1;
		this.zeit_string = zeit_string;
	}

	/**
	 * Prüft, ob die Objekte identisch sind
	 */
	@Override
	public boolean equals(Object obj) {
		Spiel spiel = (Spiel) obj;
		return this.gleiches(spiel) && tore_gegner == spiel.tore_gegner && tore_rwe == spiel.tore_rwe;
	}

	/**
	 * Prüft, ob es sich um die selbe Ansetzung handelt. Das Ergebnis muss nicht
	 * übereinstimmen.
	 *
	 * @param spiel
	 * @return
	 */
	public boolean gleiches(Spiel spiel) {
		return zeit.equals(spiel.zeit) && gegner.equals(spiel.gegner) && heim == spiel.heim;
	}

	/**
	 * @return true, wenn ein Ergebnis gespeichert ist
	 */
	public boolean mitErgebnis() {
		return tore_rwe >= 0 && tore_gegner >= 0;
	}

	/**
	 * gibt Ausgang zurück (S,U,N) oder Ausstehend
	 *
	 * @return
	 */
	public Ausgang gibAusgang() {
		if (!mitErgebnis())
			return Ausgang.AUSSTEHEND;
		if (tore_gegner > tore_rwe)
			return Ausgang.NIEDERLAGE;
		if (tore_rwe > tore_gegner)
			return Ausgang.SIEG;
		return Ausgang.UNENTSCHIEDEN;
	}

	/**
	 * Berechnet die gewonnenen Punkte anhand des Tipps
	 *
	 * @param tipp
	 * @return
	 */
	public int gibPunkte(Spiel tipp) {
		if (gibAusgang() == Ausgang.AUSSTEHEND || tipp.gibAusgang() == Ausgang.AUSSTEHEND)
			return 0;
		if (tore_rwe == tipp.tore_rwe && tore_gegner == tipp.tore_gegner)
			return 3;
		if (tore_rwe - tore_gegner == tipp.tore_rwe - tipp.tore_gegner)
			return 2;
		if (gibAusgang() == tipp.gibAusgang())
			return 1;
		return 0;
	}

	/**
	 * @return true, wenn der Spielbeginn in der Zukunft liegt
	 */
	public boolean istAustehend() {
		return zeit.after(new GregorianCalendar());
	}

	/**
	 * @return Zahl der verbleibenden Tage
	 */
	public long tageVerbleibend() {
		return (zeit.getTimeInMillis() - new GregorianCalendar().getTimeInMillis()) / 86400000;
	}

	/**
	 * Gibt eine Kopie des Spiels ohne eingetragenem Ergebnis zurück
	 *
	 * @return leerer Tipp (Spiel)
	 */
	public Spiel gibLeerenTipp() {
		return new Spiel(gegner, zeit, heim, zeit_string);
	}

	public String gibErgebnisText() {
		if (mitErgebnis())
			return tore_rwe + ":" + tore_gegner;
		return "-:-";
	}

	public String gibHeimText() {
		if (heim)
			return "H";
		return "A";
	}

	public String gibDatumText() {
		return zeit_string;
	}
}

class Spieler {
	private Spiel tipps[], ergebnisse[];
	private String name;

	/**
	 * @param tipps      - Tipps
	 * @param ergebnisse - Ergebnisse
	 * @param name       - Name des Spielers
	 */
	public Spieler(Spiel tipps[], Spiel ergebnisse[], String name) {
		this.tipps = tipps;
		this.ergebnisse = ergebnisse;
		this.name = name;

		Spieler rl[] = new Spieler[Tippspiel.rangliste.length + 1];
		for (int i = 0; i < Tippspiel.rangliste.length; i++) {
			rl[i] = Tippspiel.rangliste[i];
		}
		rl[rl.length - 1] = this;
		Tippspiel.rangliste = rl;
	}

	/**
	 * Vergleicht die Punktzahlen zweier Spieler. Gibt false zurück, falls gleich
	 * gut.
	 *
	 * @param sp Spieler, mit dem verglichen wird
	 * @return wahr, falls dieser Spieler besser ist
	 */
	public boolean istBesserAls(Spieler sp) {
		return istBesserAls(sp, Tippspiel.spt_aktuell);
	}

	/**
	 * Vergleicht die Punktzahlen zweier Spieler bis zum angegebenen Spieltag. Gibt
	 * false zurück, falls gleich gut.
	 *
	 * @param sp       Spieler, mit dem verglichen wird
	 * @param spieltag Spieltag, bis zu dem verglichen wird
	 * @return wahr, falls dieser Spieler besser ist
	 */
	public boolean istBesserAls(Spieler sp, int spieltag) {
		if (sp.gibPunkte(spieltag) < this.gibPunkte(spieltag))
			return true;
		if (sp.gibPunkte(spieltag) > this.gibPunkte(spieltag))
			return false;
		if (sp.gibAnzahlMin(spieltag, 3) < this.gibAnzahlMin(spieltag, 3))
			return true;
		if (sp.gibAnzahlMin(spieltag, 3) > this.gibAnzahlMin(spieltag, 3))
			return false;
		if (sp.gibAnzahlMin(spieltag, 2) < this.gibAnzahlMin(spieltag, 2))
			return true;
		if (sp.gibAnzahlMin(spieltag, 2) > this.gibAnzahlMin(spieltag, 2))
			return false;
		return false; // gleich gut
	}

	public boolean istGleichGut(Spieler sp) {
		return sp.gibAnzahlMin(Tippspiel.spt_aktuell, 1) == gibAnzahlMin(Tippspiel.spt_aktuell, 1)
				&& sp.gibAnzahlMin(Tippspiel.spt_aktuell, 2) == gibAnzahlMin(Tippspiel.spt_aktuell, 2)
				&& sp.gibAnzahlMin(Tippspiel.spt_aktuell, 3) == gibAnzahlMin(Tippspiel.spt_aktuell, 3);
	}

	public int gibPlatzierung(int spieltag) {
		int p = 1;
		for (Spieler s : Tippspiel.rangliste)
			if (s.istBesserAls(this, spieltag))
				p++;
		return p;
	}

	/**
	 * Gibt die Anzahl der Punkte, die der Spieler bis zum angegebenen Spieltag
	 * erreicht hat.
	 *
	 * @param spieltag
	 * @return Punktezahl
	 */
	public int gibPunkte(int spieltag) {
		return gibAnzahlMin(spieltag, 1) + gibAnzahlMin(spieltag, 2) + gibAnzahlMin(spieltag, 3);
	}

	/**
	 * Gibt die Anzahl der Spiele bis zum angegebenen Spieltag, bei denen die Tipps
	 * mindestens die angegebene Punktzahl eingebracht haben.
	 *
	 * @param spieltag
	 * @param punkte   - 1 liefert die Anzahl der korrekten Tendenzen, 2 die
	 *                 korrekten Tordifferenzen und 3 die exakten Ergebnisse
	 * @return Anzahl
	 */
	public int gibAnzahlMin(int spieltag, int punkte) {
		int p = 0;
		for (int i = 0; i < spieltag; i++) {
			if (ergebnisse[i].gibPunkte(tipps[i]) >= punkte)
				p++;
		}
		return p;
	}

	/**
	 * Gibt die Anzahl der Spiele bis zum angegebenen Spieltag, bei denen die Tipps
	 * genau die angegebene Punktzahl eingebracht haben.
	 *
	 * @param spieltag
	 * @param punkte   - 1 liefert die Anzahl der korrekten Tendenzen, 2 die
	 *                 korrekten Tordifferenzen und 3 die exakten Ergebnisse
	 * @return Anzahl
	 */
	public int gibAnzahlExakt(int spieltag, int punkte) {
		int p = 0;
		for (int i = 0; i < spieltag; i++) {
			if (ergebnisse[i].gibPunkte(tipps[i]) == punkte)
				p++;
		}
		return p;
	}

	/**
	 * Gibt die Anzahl der getippten Spiele bis zum angegebenen Spieltag.
	 *
	 * @param spieltag
	 * @return Anzahl
	 */
	public int gibtAnzahlTipps(int spieltag) {
		int p = 0;
		for (int i = 0; i < spieltag; i++) {
			if (tipps[i].mitErgebnis())
				p++;
		}
		return p;
	}

	/**
	 * @return Gibt ersten Spieltag, für den ein Tipp vorliegt.
	 */
	public int gibErstenTippSpieltag() {
		for (int i = 0; i < tipps.length; i++)
			if (tipps[i].mitErgebnis())
				return i + 1;
		return -1;
	}

	/**
	 * @return Name des Spielers
	 */
	public String gibName() {
		return name;
	}
}

class Tippen extends JDialog {
	private static final long serialVersionUID = 1L;
	private Spiel spieltipp;
	private JSpinner tore_rwe, tore_gegner;

	public Tippen(JFrame fenster, Spiel tipp) {
		super(fenster, true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new GridLayout(2, 1));
		this.spieltipp = tipp;

		JPanel tipp_panel = new JPanel(), knopf_panel = new JPanel();
		JButton ok = new JButton("Tippen"), abbruch = new JButton("Abbrechen");
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				spieltipp.tore_gegner = (int) tore_gegner.getValue();
				spieltipp.tore_rwe = (int) tore_rwe.getValue();
				Tippspiel.schreibeTipps();
				dispose();
			}
		});
		abbruch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		getContentPane().add(tipp_panel);
		getContentPane().add(knopf_panel);

		tipp_panel.add(new JLabel("Rot Weiß Erfurt "));
		tipp_panel.add(tore_rwe = new JSpinner(new SpinnerNumberModel(Math.max(0, tipp.tore_rwe), 0, 20, 1)));
		tipp_panel.add(new JLabel(":"));
		tipp_panel.add(tore_gegner = new JSpinner(new SpinnerNumberModel(Math.max(0, tipp.tore_gegner), 0, 20, 1)));
		tipp_panel.add(new JLabel(" " + tipp.gegner));
		knopf_panel.add(ok);
		knopf_panel.add(abbruch);

		pack();
		setLocation((int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() - this.getWidth()) / 2,
				(int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() - this.getHeight()) / 2);
		setVisible(true);
	}
}

class Aktualisierer extends Thread {
	private Tippspiel tippspiel;

	public Aktualisierer(Tippspiel tippspiel) {
		this.tippspiel = tippspiel;
		this.setDaemon(true);
		this.setPriority(1);
	}

	@Override
	public void run() {
		int i = 0;
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			tippspiel.aktualisiereAnzeige();
			if (i++ >= 60) {
				Tippspiel.aktualisiereDateien();
				i = 0;
			}
		}
	}
}