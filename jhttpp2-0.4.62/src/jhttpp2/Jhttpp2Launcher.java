package jhttpp2;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/* Written and copyright 2001-2003 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 */

//import java.awt.Dimension;
//import java.awt.Toolkit;
//import javax.swing.UIManager;

//import Jhttpp2MainFrame;
/**
 * Title: jHTTPp2: Java HTTP Filter Proxy Description: starts thwe Swing GUI or
 * the console-mode only proxy Copyright: Copyright (c) 2001-2003
 * 
 * @author Benjamin Kohl
 */

public class Jhttpp2Launcher implements Jhttpp2SettingsSaverInteface {
	private static Log log = LogFactory.getLog(Jhttpp2Launcher.class);
	private final String SERVER_PROPERTIES_FILE = "server.properties";
	private final String DATA_FILE = "server.data";
	Jhttpp2Server server;

	public Jhttpp2Launcher() {
		server = new Jhttpp2Server(true);
		server.setServerProperties(loadServerProperties());
		restoreSettings();
		server.setSettingsSaver(this);
		SortedMap<String, String> hostRedirects= new TreeMap<String, String>();
		hostRedirects.put("redirect.me.com", "localhost");
		server.setHostRedirects(hostRedirects);
		server.init();
		if (Jhttpp2Server.error) {
			System.out.println("Error: " + Jhttpp2Server.error_msg);
		} else {
			new Thread(server).start();
			log.info("Running on port " + server.port);
		}
	}

	public static void main(String[] args) {
		new Jhttpp2Launcher();
	}

	public Properties loadServerProperties() {
		Properties serverproperties = new Properties();
		try {
			serverproperties.load(new DataInputStream(new FileInputStream(
					SERVER_PROPERTIES_FILE)));
		} catch (IOException e) {
			log.warn("failed to load the server properties", e);
		}
		return serverproperties;
	}

	public void storeServerProperties() {
		if (server.getServerProperties() == null)
			return;
		try {
			server
					.getServerProperties()
					.store(
							new FileOutputStream(SERVER_PROPERTIES_FILE),
							"Jhttpp2Server main properties. Look at the README file for further documentation.");
		} catch (IOException e) {
			log.warn("storeServerProperties()", e);
		}
	}


	@SuppressWarnings("unchecked")
	public void restoreSettings()// throws Exception
	{
		if (server.getServerProperties() == null) {
			log.warn("server propertie where not set will not save them");
			return;
		}

		try {

			// Restore the WildcardDioctionary and the URLActions with the
			// ObjectInputStream (settings.dat)...
			ObjectInputStream obj_in;
			File file = new File(DATA_FILE);
			if (!file.exists()) {
				if (!file.createNewFile() || !file.canWrite()) {
					log
							.warn("Can't create or write to file "
									+ file.toString());
				} else
					saveServerSettings();
			}

			obj_in = new ObjectInputStream(new FileInputStream(file));
			server.setWildcardDictionary((WildcardDictionary) obj_in
					.readObject());
			server.setURLActions((List<OnURLAction>) obj_in.readObject());
			obj_in.close();
		} catch (IOException e) {
			log.warn("restoreSettings(): " + e.getMessage());
		} catch (ClassNotFoundException e_class_not_found) {
		}
	}

	public boolean saveServerSettings() {
		storeServerProperties();
		ObjectOutputStream file;
		try {
			file = new ObjectOutputStream(new FileOutputStream(
					DATA_FILE));
			file.writeObject(server.getWildcardDictionary());
			file.writeObject(server.getURLActions());
			file.close();
			return true;
		} catch (Exception e) {
			log.warn("Was not able to save the settings" , e);
		} 
		return false;

	}

}
