import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Diese Klasse ähnelt in ihrer Funktion der Klasse java.io.File,
 * jedoch können auch Dateien und Ordner in Archiven behandelt werden.
 * URLs beschränken sich auf lokale Pfade (file:) und Dateien in einem 
 * JAR-Archiv (jar:file:)
 * @header JonFile
 * @author Jonius
 * @since 06.07.2015
 */

public class JonFile {
	private static Hashtable<JonFile,JonFile> move_later = new Hashtable<JonFile,JonFile>();
	private static ArrayList<JonFile> delete_later = new ArrayList<JonFile>();
	private static Hashtable<JonFile,JonFile> copy_later = new Hashtable<JonFile,JonFile>(); //TODO umgekehrte Reihenfolge beachten!
	
	private boolean inArchive; //TODO
	/** Repräsentiert das Archiv, in dem sich die Datei befindet. Ist null, falls sich die Datei nicht in einem Archiv befindet */
	private JonFile archive = null;
	private String path;
	private Class<?> program_class = JonFile.class;
	
	public synchronized static void doWaitingFileOperations() {
		//TODO delete_later & copy later
//		Hashtable<JonFile,JonFile> laterArchive = new Hashtable<JonFile,JonFile>();
		Vector<JonFile> laterArch1 = new Vector<JonFile>();
		Vector<JonFile> laterArch2 = new Vector<JonFile>();
		Vector<String> ersetzen = new Vector<String>();
		JonFile programm = JonFile.getSourceLocation();
		
		Enumeration<JonFile> files = move_later.keys();
		while(files.hasMoreElements()) {
			JonFile f1 = files.nextElement(), f2 = move_later.get(f1);
			if(!f2.inArchive() || !f2.getArchive().getPath().equals(programm.getPath())) {
				f1.move(f2);
			} else {
				laterArch1.add(f1);
				laterArch2.add(f2);
				ersetzen.add(f2.getArchivePath());
			}
		}
		move_later.clear();
				
		if(JonFile.isSourceInJar() && laterArch1.size() > 0) {
			ZipInputStream zis;
			ZipOutputStream zos;
			try {
				JonFile tmp = new JonFile(File.createTempFile("JonFileJar", ".tmp"));
				zis = new ZipInputStream(new FileInputStream(programm.toFile()));
				zos = new ZipOutputStream(new FileOutputStream(tmp.toFile()));
				
				byte[] buffer = new byte[4096];
				ZipEntry entry;
		    	while((entry = zis.getNextEntry()) != null) {
		    		if(!ersetzen.contains(entry.getName())) {
		    			System.out.println("Kopieren: "+entry.getName());
		    			zos.putNextEntry(new ZipEntry(entry.getName())); //Erzeuge neuen Eintrag
						int len;
						while ((len = zis.read(buffer)) > 0) { //fülle puffer
							zos.write(buffer, 0, len); //schreibe puffer
						}
		    		} else {
		    			int i = ersetzen.indexOf(entry.getName());
		    			JonFile f1 = laterArch1.get(i), f2 = laterArch2.get(i);
						zos.putNextEntry(new ZipEntry(f2.getArchivePath()));
						System.out.println("Speichern: "+f2.getArchivePath());
						
						FileInputStream lesen = new FileInputStream(f1.toFile());
						int len;
						while ((len = lesen.read(buffer)) > 0) { //fülle puffer
							zos.write(buffer, 0, len); //schreibe puffer
						}
						lesen.close();
		    		}
		    	}
		    	zis.close();
		    	
//		    	files = laterArchive.keys();
//				while(files.hasMoreElements()) {
//					
//				}
		    	zos.close();
		    	
		    	programm.delete();
		    	tmp.copy(programm);
		    	tmp.toFile().deleteOnExit();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Dieser Konstruktor dient relativen Pfadangaben.
	 * Falls sich das Programm in einem Archiv befindet, dann handelt es sich
	 * um den Pfad im Archiv.
	 *  @param pfad -> enthält Pfad zur Datei (String)
	 */
	public JonFile(String pfad) {
		init(pfad,this.getClass(),true);
	}
	
	/**
	 * Dieser Konstruktor dient Dateien, die sich nicht in einem Archiv befinden
	 * @param pfad -> Pfad zur Datei, die sich nicht in einem Archiv befindet (File)
	 */
	public JonFile(File pfad) {
		inArchive = false;
		init(pfad.getPath(),JonFile.class,false);
	}
	
	/**
	 * Dieser Konstruktor dient relativen Pfadangaben, bezogen auf den Ort, 
	 * an dem sich die angegebene Klasse befindet. 
	 * Dies kann, muss aber nicht in einem Archiv sein.
	 * @param pfad -> enthält relativen Pfad zur Datei (String)
	 * @param klasse -> enthält Klasse, in deren Umgebung sich die Datei befindet (Class<?>)
	 */
	public JonFile(String pfad,Class<?> klasse) {
		init(pfad,JonFile.class,true);
	}
	
	/**
	 * Dieser Konstruktor dient Dateien, die sich in einem Archiv befinden
	 * @param archiv -> Pfad zum Archiv (File)
	 * @param pfad -> relativer Pfad zur Datei im Archiv (String)
	 */
	public JonFile(File archiv, String pfad) {
		this(new JonFile(archiv),pfad);
	}
	
	/**
	 * Dieser Konstruktor dient Dateien, die sich in dem angegebenen Ordner
	 * bzw. Archiv befinden. Dabei darf der Ordner sich auch selbst in einem
	 * Archiv befinden.
	 * @param ordner -> Pfad zum Ordner bzw. Archiv (JonFile)
	 * @param pfad -> relativer Pfad zur Datei (String)
	 */
	public JonFile(JonFile ordner, String pfad) {
		if(ordner.inArchive) {
			inArchive = true;
			archive = ordner.archive;
			init(ordner.path+"/"+pfad,ordner.program_class,false);
		} else if(ordner.isArchive()) {
			archive = ordner;
			inArchive = true;
			init(pfad,ordner.program_class,false);
		} else {
			inArchive = false;
			init(ordner.path+"/"+pfad,JonFile.class,false);
		}
	}
	
	/**
	 * Dieser Konstruktor dient Dateien, egal ob sie sich in einem Archiv befinden oder nicht.
	 * @param url -> Pfad zur Datei (URL)
	 */
	public JonFile(URL url) throws IOException {
		init(url);
	}
	
	private void init(String pfad,Class<?> klasse,boolean pruefen) {
		this.path = pfad;
		this.program_class = klasse;
		if(pruefen) {
		   	try {
				inArchive = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).isFile();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}			
			inArchive = inArchive && !new File(pfad).getAbsolutePath().equals(new File(pfad).getPath());
			if(inArchive) archive = getSourceLocation(klasse);
		}		
	}
		
	private void init(URL url) throws IOException {		
		String new_path = url.toString();
    	new_path = correctURLString(new_path);
    	if(new_path.startsWith("jar:file:")) {
    		inArchive = true;
    		String file = new_path.substring(new_path.indexOf('!')+2);
        	new_path = new_path.substring("jar:file:".length(), new_path.indexOf('!'));
        	archive = new JonFile(new_path);
        	path = file;
    	} else if(new_path.startsWith("file:")) {
    		inArchive = false;
    		new_path = new_path.substring("file:".length());
        	new_path = correctURLString(new_path);
        	archive = new JonFile(new File(new_path).getParentFile());
        	path = new File(new_path).getName();
    	} else {
    		throw new IOException();
    	}
	}
	
	/**
	 * Diese Funktion korrigiert URL-Ersatzzeichen
	 * @param pfad -> Zeichenkette, die korrigiert werden soll (String)
	 */
	public static String correctURLString(String pfad) {
		 try {
			 return URLDecoder.decode(pfad,"UTF-8");
		} catch (UnsupportedEncodingException ex) {};
		
		return pfad;
	}
	
	/**
	 * Liefert true, wenn es sich um eine Datei handelt,
	 * d.h. wenn es sich nicht um einen Ordner handelt und die Datei existiert.
	 */
	public boolean isFile() {
		if(inArchive) {
			ZipInputStream zis = getInputStream();
			if(zis == null) return false;
			try {
				for(ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
					if(entry.getName().startsWith(path) && entry.getName().length() == path.length()) {
						zis.close();
						return true;
					}
				}
				zis.close();
			} catch(IOException e) {}
			return false;
		} else {
			return toFile().isFile();
		}
	}
	/**
	 * Liefert true, wenn es sich um einen Ordner handelt,
	 * d.h. wenn es sich nicht um eine Datei handelt und der Ordner existiert.
	 */
	public boolean isDirectory() {
		if(inArchive) {
			ZipInputStream zis = getInputStream();
			if(zis == null) return false;
			try {
				for(ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
					if(entry.getName().startsWith(path) && entry.getName().length() > path.length()) {
						zis.close();
						return true;
					}
				}
				zis.close();
			} catch(IOException e) {}		
			return false;
		} else {
			return toFile().isDirectory();
		}
	}
	/**
	 * Liefert true, wenn die Datei bzw. der Ordner existiert.
	 */
	public boolean exists() {
		if(inArchive) {
			ZipInputStream zis = getInputStream();
			if(zis == null) return false;
			try {
				for(ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
					if(entry.getName().startsWith(path)) {
						zis.close();
						return true;
					}
				}
				zis.close();
			} catch(IOException e) {}		
			return false;	
		} else {
			return toFile().exists();
		}
	}
	/**
	 * Liefert true, wenn die Datei bzw. der Ordner gelöscht wurde.
	 * ACHTUNG: Löscht auch Ordner, die Dateien enthalten!
	 */
	public boolean delete() {
		if(inArchive) {
			//TODO
			return false;
		} else {
			if(isDirectory()) {
				for(JonFile f:this.listFiles()) if(!f.delete()) return false;
			}
			return toFile().delete();
		}
	}
	/**
	 * Liefert true, wenn die Datei bzw. der Ordner gelöscht wurde.
	 * ACHTUNG: Löscht auch Ordner, die Dateien enthalten!
	 */
	public boolean delete(boolean later) {
		if(later) {
			delete_later.add(this);
			return true;
		} else {
			return delete();
		}
	}
	/**
	 * Verschiebt die angegebene Datei oder Ordner. 
	 * ACHTUNG: Falls die Datei oder Ziel im Archiv liegt, könnte diese Operation
	 * zeitaufwendig sein.
	 * @param destination -> gibt an, wohin die Datei später verschoben wird
	 */
	public boolean move(JonFile destination) {
		if(inArchive || destination.inArchive) {
			//TODO
			return false;
		} else {
			return this.toFile().renameTo(destination.toFile());
		}
	}
	/**
	 * Merkt Datei für der spätere Verschieben vor. Dies ist vor allem
	 * nützlich, wenn Daten einem Archiv hinzugefügt werden sollen.
	 * Ein Aufruf von moveWaitingFiles verschiebt die wartenden Dateien.
	 * @param destination -> gibt an, wohin die Datei später verschoben wird
	 */
	public boolean move(JonFile destination, boolean later) {
		if(later) {
			move_later.put(this, destination);
			return true;
		} else {
			return move(destination);
		}
	}
	/**
	 * Liefert true, wenn das Verzeichnis erfolgreich angelegt wurde.
	 * Wenn übergeordnete Verzeichnisse noch nicht existieren, werden
	 * diese ebenfalls angelegt (s. File.mkdirs()).
	 * HINWEIS: Falls der Pfad sich in einem Archiv befindet, ist das Anlegen eines
	 * leeren Ordners nicht möglich. Die Methode liefert in diesem Fall false zurück.
	 * Dateien können im Archiv angelegt werden, 
	 * ohne dass der beinhaltende Ordner vorher existieren muss.
	 */
	public boolean mkdir() {
		if(exists()) return false; //Falls die Datei bereits existiert
		if(inArchive) {
			return false;
		} else {
			return this.toFile().mkdirs();
		}
	}
	/**
	 * Liefert die Dateien und Unterordner in diesem Ordner als Stringarray.
	 * Falls es sich nicht um einen Ordner handelt, wird null zurückgegeben.
	 */
	public String[] list() {
		if(inArchive) {
			if(!isDirectory()) return null;
			ZipInputStream zis = getInputStream();
			if(zis == null) return null;
			Vector<String> d = new Vector<String>();
			String dat;
			try {
				for(ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
					if(entry.getName().startsWith(path)) {
						dat = entry.getName().substring(path.length()+1);
						if(dat.contains("/")) 
							dat = dat.substring(0,dat.indexOf('/'));
						if(!d.contains(dat)) d.add(dat);
					}
				}
				String dateien[] = new String[d.size()];
				d.toArray(dateien);
				zis.close();
				return dateien;
			} catch(IOException e) {}			
		} else {
			return toFile().list();
		}
		return null;
	}
	/**
	 * Liefert die Dateien und Unterordner in diesem Ordner als Stringarray.
	 * Falls es sich nicht um einen Ordner handelt, wird null zurückgegeben.
	 */
	public JonFile[] listFiles() {
		if(!isDirectory()) return null;
		
		if(inArchive) {
			ZipInputStream zis = getInputStream();
			if(zis == null) return null;
			Vector<JonFile> d = new Vector<JonFile>();
			String dat;
			try {
				for(ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
					if(entry.getName().startsWith(path)) {
						dat = entry.getName().substring(path.length()+1);
						if(dat.contains("/")) 
							dat = dat.substring(0,dat.indexOf('/'));
						if(!d.contains(dat)) d.add(new JonFile(this,dat));
					}
				}
				JonFile dateien[] = new JonFile[d.size()];
				d.toArray(dateien);
				zis.close();
				return dateien;
			} catch(IOException e) {}			
		} else {
			File liste[] = toFile().listFiles();
			JonFile jf_liste[] = new JonFile[liste.length];
			for(int i = 0; i<liste.length; i++) jf_liste[i] = new JonFile(liste[i]);
			return jf_liste;
		}
		return null;
	}
	/**
	 * Liefert true, wenn sich die Datei bzw. der Ordner in einem Archiv befindet.
	 */
	public boolean inArchive() {
		return inArchive;
	}
	
	/**
	 * Liefert das Archiv, in dem sich die Datei bzw. der Ordner befindet.
	 * Falls sich die Datei bzw. der Ordner nicht in einem Archiv befindet,
	 * wird null zurückgegeben.
	 */
	public JonFile getArchive() {
		if(inArchive) {
			return JonFile.getSourceLocation(program_class);
		} else {
			return null;
		}
	}
	
	/**
	 * Liefert das File-Objekt zu dieser Datei bzw. diesem Ordner.
	 * Falls diese(r) sich in einem Archiv befindet, wird ein File-Objekt
	 * zurückgegeben, welches das Archiv repräsentiert.
	 */
	public File toFile() {
		if(inArchive) {
			return new File(archive.path);
		} else {
			return new File(path);
		}
	}
	
	/**
	 * Kopiert die Datei bzw. den Ordner an das angegebene Ziel. Mit dieser Methode kann
	 * auch eine Datei aus einem Archiv entpackt werden, das Hinzufügen einer Datei
	 * zu einem Archiv ist noch nicht implementiert. Vorhandene Dateien werden ohne
	 * Rückfrage überschrieben.
	 * @param destination -> Ziel, wohin die Datei kopiert werden soll (JonFile)
	 * @return liefert true, wenn das Kopieren erfolgreich war
	 * @throws IOException 
	 */
	public boolean copy(JonFile destination, boolean later) throws IOException {
		if(later) {
			copy_later.put(destination, this);
			return true;
		} else {
			return copy(destination);
		}
	}
	
	/**
	 * Kopiert die Datei bzw. den Ordner an das angegebene Ziel. Mit dieser Methode kann
	 * auch eine Datei aus einem Archiv entpackt werden, das Hinzufügen einer Datei
	 * zu einem Archiv ist noch nicht implementiert. Vorhandene Dateien werden ohne
	 * Rückfrage überschrieben.
	 * @param destination -> Ziel, wohin die Datei kopiert werden soll (JonFile)
	 * @return liefert true, wenn das Kopieren erfolgreich war
	 * @throws IOException 
	 */
	public boolean copy(JonFile destination) throws IOException {
		if(destination.inArchive) {
			return false;
//			throw new NotImplementedException(); TODO
		}
		if(!this.exists()) return false;
		if(this.isDirectory()) {
			if(!destination.mkdir()) return false;
			boolean success = true;
			for(JonFile f:this.listFiles()) success &= f.copy(new JonFile(destination,f.getName()));
			return success;
		}
		
		InputStream fis;
		if(inArchive) {
			fis = getInputStream();
		} else {
			fis = new FileInputStream(this.toFile());
		}
		
		FileOutputStream fos = new FileOutputStream(destination.toFile());
		byte puffer[] = new byte[4048];
		int len = 0;
		while((len = fis.read(puffer)) > 0) {
			fos.write(puffer, 0, len);
		}
		fis.close();
		fos.close();
		return true;		
	}
	
	
	private boolean isArchive() {
		return path.toLowerCase().endsWith(".jar") || path.toLowerCase().endsWith(".zip");
	}
	
	private ZipInputStream getInputStream() {
		if(!inArchive) return null;
		try {
			return new ZipInputStream(new FileInputStream(archive.toFile()));
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	@SuppressWarnings("unused")
	private ZipOutputStream getOutputStream(File tmp) {
		if(!inArchive) return null;
		try {
			return new ZipOutputStream(new FileOutputStream(tmp));
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	/**
	 * Liefert das Verzeichnis bzw. das Archiv, in dem sich die Klasse befindet
	 */
	public static JonFile getSourceLocation(Class<?> klasse) {
		try {
			File f = new File(klasse.getProtectionDomain().getCodeSource().getLocation().toURI());
			if(f.isDirectory() && f.getName().equals("bin")) f = f.getParentFile();
			return new JonFile(f);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Liefert die URL zur Datei.
	 */
	public URL getURL() {
		try {
			if(inArchive) {			
				return new URL("jar:file:"+archive.toFile().getAbsolutePath()+"!/"+path);				
			} else {
				return toFile().toURI().toURL();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Liefert die Größe der Datei in Bytes.
	 */			
	public long length() {
		if(inArchive) {
			ZipInputStream zis = getInputStream();
			if(zis == null) return -1;
			try {
				for(ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
					if(entry.getName().equals(path)) {
						long l = 0;
						while(zis.available() > 0) {
							l++;
							zis.read();
						}
						return l;
					}
				}
			} catch(IOException e) {}
			return -1;
		} else {
			return toFile().length();
		}
	}
	/**
	 * Liefert das Elternverzeichnis. 
	 * ACHTUNG: Falls sich die Datei direkt im Wurzelverzeichnis eines Archivs befindet,
	 * wird null zurückgegeben.
	 */
	public JonFile getParent() {
		if(inArchive) {
			if(path.contains("/")) {
				return new JonFile(archive,path.substring(0,path.indexOf("/")));
			} else {
				return null;
			}
		} else {
			return new JonFile(toFile().getParentFile());
		}
	}
	/**
	 * Liefert den Pfad als String.
	 */
	public String getPath() {
		if(inArchive) {
			String help = archive.toFile().getAbsolutePath();
			help = help.substring(help.lastIndexOf("\\")+1);
			return help+"/"+path;				
		} else {
			return path;
		}
	}
	/**
	 * Liefert den Pfad im Archiv.
	 */
	public String getArchivePath() {
		if(inArchive) {
			return path;
		} else {
			return null;
		}
	}
	/**
	 * Liefert den Namen.
	 */
	public String getName() {
		if(inArchive) {
			if(!path.contains("/")) return path;
			return path.substring(path.lastIndexOf('/')+1);
		} else {
			return toFile().getName();
		}
	}
	
	/**
	 * Liefert den Namen.
	 */
	@Override
	public String toString() {
		return getName();
	}
	/**
	 * Liefert das Verzeichnis bzw. das Archiv, in dem sich diese Klasse (JonFile) befindet
	 */
	public static JonFile getSourceLocation() {
		return getSourceLocation(JonFile.class);
	}
	
	/**
	 * Liefert true, falls sich die Klasse in einem Jar-Archiv befindet.
	 */
	public static boolean isSourceInJar(Class<?> klasse) {
		return getSourceLocation(klasse).path.toLowerCase().endsWith(".jar");
	}
	
	/**
	 * Liefert true, falls sich diese Klasse (JonFile) in einem Jar-Archiv befindet.
	 */
	public static boolean isSourceInJar() {
		return isSourceInJar(JonFile.class);
	}
	
	/**
	 * Liefert den Pfad zum Desktop
	 * @return JonFile, das den Desktop repräsentiert
	 */
	public static JonFile getDesktop() {
		if(System.getProperty("os.name").equals("Linux")) {
			Properties paths = new Properties();
			try {
				paths.load(new FileInputStream(System.getProperty("user.home")+"/.config/user-dirs.dirs"));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return new JonFile(paths.getProperty("XDG_DESKTOP_DIR").replace("\"", "").replace("$HOME", System.getProperty("user.home")));
		} else {
			return new JonFile(new File(System.getProperty("user.dir")+"/Desktop"));
		}
	}
}

/** 
 * Zeigt eine IOException an, die auftritt, wenn die Operation nicht ausgeführt werden kann,
 * weil sich die Datei nicht in einem JAR-Archiv befindet, aber die Operation genau dafür bestimmt ist. 
 * @author Jonius *
 */
class NotInJarException extends IOException {
	private static final long serialVersionUID = -3126891810053819624L;
	
    public NotInJarException() {
        super();
    }
    public NotInJarException(String message) {
        super(message);
    }
    public NotInJarException(String message, Throwable cause) {
        super(message, cause);
    }
    public NotInJarException(Throwable cause) {
        super(cause);
    }
}