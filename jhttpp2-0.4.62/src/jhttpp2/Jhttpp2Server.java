package jhttpp2;

/* Written and copyright 2001-2003 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 * More Information and documentation: HTTP://jhttp2.sourceforge.net/
 */

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Jhttpp2Server implements Runnable {
	private static Log log = LogFactory.getLog(Jhttpp2Server.class);
	private static Log accessLog = LogFactory.getLog("Jhttpp2Server.accesslog");

	@SuppressWarnings("unused")
	private static final String CRLF = "\r\n";
	private final String VERSION = "0.4.62";
	private final String V_SPECIAL = " 2003-05-20";
	private final String HTTP_VERSION = "HTTP/1.1";

	private String http_useragent = "Mozilla/4.0 (compatible; MSIE 4.0; WindowsNT 5.0)";
	private ServerSocket listen;
	private Properties serverproperties = null;

	private long bytesread;
	private long byteswritten;
	private int numconnections;

	private boolean enable_cookies_by_default = true;
	private WildcardDictionary dic = new WildcardDictionary();
	private List<OnURLAction> urlactions = new ArrayList<OnURLAction>();
	
	private SortedMap<String,URL> hostRedirects;

	public SortedMap<String, URL> getHostRedirects() {
		return hostRedirects;
	}

	public void setHostRedirects(SortedMap<String, URL> hostRedirects) {
		this.hostRedirects = hostRedirects;
	}

	public final int DEFAULT_SERVER_PORT = 8088;
	public final String WEB_CONFIG_FILE = "admin/jp2-config";

	public int port = DEFAULT_SERVER_PORT;
	public InetAddress proxy;
	public int proxy_port = 0;

	public long config_auth = 0;
	public long config_session_id = 0;
	public String config_user = "root";
	public String config_password = "geheim";

	public static boolean error;
	public static String error_msg;

	public boolean use_proxy = false;
	public static boolean block_urls = false;
	public boolean filter_http = false;
	public boolean debug = false;
	public boolean log_access = false;
	public String log_access_filename = "paccess.log";
	public boolean webconfig = true;
	public boolean www_server = true;

	
	private Jhttpp2SettingsSaverInteface settingsSaver;

	public void init() {
		writeLog("server startup...");
		if (serverproperties == null) {
			log.warn("Server properties should be set prior to init");
		}
		if (dic == null) {
			log.warn("Server dic should be set prior to calling init");
		}
		if (urlactions == null) {
			log.warn("url actions should be set prior to init");
		}
		try {
			listen = new ServerSocket(port);
		} catch (BindException e_bind_socket) {
			setErrorMsg("Socket " + port
					+ " is already in use (Another jHTTPp2 proxy running?) "
					+ e_bind_socket.getMessage());
		} catch (IOException e_io_socket) {
			setErrorMsg("IO Exception while creating server socket on port "
					+ port + ". " + e_io_socket.getMessage());
		}

		if (error) {
			writeLog(error_msg);
			return;
		}
	}

	public Jhttpp2Server() {
	}

	public Jhttpp2Server(boolean b) {
		System.out
				.println("jHTTPp2 HTTP Proxy Server Release "
						+ getServerVersion()
						+ "\r\n"
						+ "Copyright (c) 2001-2003 Benjamin Kohl <bkohl@users.sourceforge.net>\r\n"
						+ "This software comes with ABSOLUTELY NO WARRANTY OF ANY KIND.\r\n"
						+ "http://jhttp2.sourceforge.net/");
	}

	/**
	 * sets up the serverport and starts for each connection new
	 * Jhttpp2Connection
	 */
	void serve() {
		writeLog("Server running.");
		try {
			while (true) {
				Socket client = listen.accept();
				new Jhttpp2HTTPSession(this, client);
			}
		} catch (Exception e) {
			log.warn("Exception in Jhttpp2Server.serve(): " , e);
		}
	}

	public void run() {
		serve();
	}

	public void setErrorMsg(String a) {
		error = true;
		error_msg = a;
	}

	/**
	 * Tests what method is used with the request
	 * 
	 * @return -1 if the server doesn't support the method
	 */
	public int getHttpMethod(String d) {
		if (startsWith(d, "GET") || startsWith(d, "HEAD"))
			return 0;
		if (startsWith(d, "POST") || startsWith(d, "PUT"))
			return 1;
		if (startsWith(d, "CONNECT"))
			return 2;
		if (startsWith(d, "OPTIONS"))
			return 3;

		return -1;/*
				 * No match...
				 * 
				 * Following methods are not implemented: ||
				 * startsWith(d,"TRACE")
				 */
	}

	public boolean startsWith(String a, String what) {
		int l = what.length();
		int l2 = a.length();
		return l2 >= l ? a.substring(0, l).equals(what) : false;
	}

	/**
	 *@return the Server response-header field
	 */
	public String getServerIdentification() {
		return "jHTTPp2/" + getServerVersion();
	}

	public String getServerVersion() {
		return VERSION + V_SPECIAL;
	}

	/**
	 * @return the HTTP version used by jHTTPp2
	 */
	public String getHttpVersion() {
		return HTTP_VERSION;
	}

	/**
	 * the User-Agent header field
	 * 
	 * @since 0.2.17
	 * @return User-Agent String
	 */
	public String getUserAgent() {
		return http_useragent;
	}

	public void setUserAgent(String ua) {
		http_useragent = ua;
	}

	/**
	 * writes into the server log file.
	 * 
	 * @since 0.2.21
	 */
	public void writeLog(String s) {
		writeLog(s, true);
	}

	/**
	 * writes to the server log file
	 * 
	 * @since 0.2.21
	 */
	public void writeLog(String s, boolean b) {
		log.debug(s);
	}

	public void closeLog() {
		writeLog("Server shutdown.");
	}

	public void addBytesRead(long read) {
		bytesread += read;
	}

	/**
	 * Functions for the jHTTPp2 statistics: How many connections Bytes
	 * read/written
	 * 
	 * @since 0.3.0
	 */
	public void addBytesWritten(int written) {
		byteswritten += written;
	}

	public int getServerConnections() {
		return numconnections;
	}

	public long getBytesRead() {
		return bytesread;
	}

	public long getBytesWritten() {
		return byteswritten;
	}

	public void increaseNumConnections() {
		numconnections++;
	}

	public void decreaseNumConnections() {
		numconnections--;
	}

	public void AuthenticateUser(String u, String p) {
		if (config_user.equals(u) && config_password.equals(p)) {
			config_auth = 1;
		} else
			config_auth = 0;
	}

	public String getGMTString() {
		return new Date().toString();
	}

	public Jhttpp2URLMatch findMatch(String url) {
		return (Jhttpp2URLMatch) dic.get(url);
	}

	public boolean enableCookiesByDefault() {
		return this.enable_cookies_by_default;
	}

	public void enableCookiesByDefault(boolean a) {
		enable_cookies_by_default = a;
	}

	public void resetStat() {
		bytesread = 0;
		byteswritten = 0;
	}

	/**
	 * @since 0.4.10a
	 */
	public void setServerProperties(Properties p) {
		serverproperties = p;
		use_proxy = new Boolean(serverproperties.getProperty(
				"server.http-proxy", "false")).booleanValue();
		try {
			proxy = InetAddress.getByName(serverproperties.getProperty(
					"server.http-proxy.hostname", "127.0.0.1"));
		} catch (UnknownHostException e) {
		}
		proxy_port = new Integer(serverproperties.getProperty(
				"server.http-proxy.port", "8080")).intValue();
		block_urls = new Boolean(serverproperties.getProperty(
				"server.filter.url", "false")).booleanValue();
		http_useragent = serverproperties.getProperty(
				"server.filter.http.useragent",
				"Mozilla/4.0 (compatible; MSIE 4.0; WindowsNT 5.0)");
		filter_http = new Boolean(serverproperties.getProperty(
				"server.filter.http", "false")).booleanValue();
		enable_cookies_by_default = new Boolean(serverproperties.getProperty(
				"server.enable-cookies-by-default", "true")).booleanValue();
		debug = new Boolean(serverproperties.getProperty(
				"server.debug-logging", "false")).booleanValue();
		port = new Integer(serverproperties.getProperty("server.port", "8088"))
				.intValue();
		log_access = new Boolean(serverproperties.getProperty(
				"server.access.log", "false")).booleanValue();
		log_access_filename = serverproperties.getProperty(
				"server.access.log.filename", "paccess.log");
		webconfig = new Boolean(serverproperties.getProperty(
				"server.webconfig", "true")).booleanValue();
		www_server = new Boolean(serverproperties.getProperty("server.www",
				"true")).booleanValue();
		config_user = serverproperties.getProperty("server.webconfig.username",
				"root");
		config_password = serverproperties.getProperty(
				"server.webconfig.password", "geheim");
	}

	public Properties getServerProperties() {
		serverproperties.setProperty("server.http-proxy",
				new Boolean(use_proxy).toString());
		serverproperties.setProperty("server.http-proxy.hostname", proxy
				.getHostAddress());
		serverproperties.setProperty("server.http-proxy.port", new Integer(
				proxy_port).toString());
		serverproperties.setProperty("server.filter.http", new Boolean(
				filter_http).toString());
		serverproperties.setProperty("server.filter.url", new Boolean(
				block_urls).toString());
		serverproperties.setProperty("server.filter.http.useragent",
				http_useragent);
		serverproperties.setProperty("server.enable-cookies-by-default",
				new Boolean(enable_cookies_by_default).toString());
		serverproperties.setProperty("server.debug-logging", new Boolean(debug)
				.toString());
		serverproperties.setProperty("server.port", new Integer(port)
				.toString());
		serverproperties.setProperty("server.access.log", new Boolean(
				log_access).toString());
		serverproperties.setProperty("server.access.log.filename",
				log_access_filename);
		serverproperties.setProperty("server.webconfig", new Boolean(webconfig)
				.toString());
		serverproperties.setProperty("server.www", new Boolean(www_server)
				.toString());
		serverproperties.setProperty("server.webconfig.username", config_user);
		serverproperties.setProperty("server.webconfig.password",
				config_password);
		return serverproperties;
	}

	/**
	 * @since 0.4.10a
	 */
	public void logAccess(String s) {
		accessLog.info(s);
	}

	public void shutdownServer() {
		closeLog();
		System.exit(0);
	}

	public void setWildcardDictionary(WildcardDictionary dic) {
		this.dic = dic;
	}

	public WildcardDictionary getWildcardDictionary() {
		return dic;
	}

	public void setURLActions(List<OnURLAction> urlactions) {
		this.urlactions = urlactions;
	}

	public List<OnURLAction> getURLActions() {
		return urlactions;
	}

	public Jhttpp2SettingsSaverInteface getSettingsSaver() {
		return settingsSaver;
	}

	public void setSettingsSaver(Jhttpp2SettingsSaverInteface settingsSaver) {
		this.settingsSaver = settingsSaver;
	}

}
