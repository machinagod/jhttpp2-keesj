/* Written and copyright 2001-2003 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 */

//import java.awt.Dimension;
//import java.awt.Toolkit;
//import javax.swing.UIManager;

//import Jhttpp2MainFrame;
/**
 * Title:        jHTTPp2: Java HTTP Filter Proxy
 * Description: starts thwe Swing GUI or the console-mode only proxy
 * Copyright:    Copyright (c) 2001-2003
 *
 * @author Benjamin Kohl
 */

public class Jhttpp2Launcher {

  static Jhttpp2Server server;

  public static void main(String[] args)
  {
		server = new Jhttpp2Server(true);
    	if (server.error) {
    		System.out.println("Error: " + server.error_msg);
		}
    	else {
    		new Thread(server).start();
    	   	System.out.println("Running on port " + server.port);
    	}
  }
}