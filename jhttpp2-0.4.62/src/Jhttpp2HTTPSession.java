/* Written and copyright 2001-2003 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 */

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

import Jhttpp2Server;
import Jhttpp2Read;
import Jhttpp2ClientInputStream;
import Jhttpp2ServerInputStream;

import Jhttpp2Admin;

/**
	One HTTP connection
	@file Jhttpp2HTTPSession.java
	@author Benjamin Kohl
*/
public class Jhttpp2HTTPSession extends Thread {

	public static final int SC_OK=0;
	public static final int SC_CONNECTING_TO_HOST=1;
	public static final int SC_HOST_NOT_FOUND=2;
	public static final int SC_URL_BLOCKED=3;
	public static final int SC_CLIENT_ERROR=4;
	public static final int SC_INTERNAL_SERVER_ERROR=5;
	public static final int SC_NOT_SUPPORTED=6;
	public static final int SC_REMOTE_DEBUG_MODE=7;
	public static final int SC_CONNECTION_CLOSED=8;
	public static final int SC_HTTP_OPTIONS_THIS=9;
	public static final int SC_FILE_REQUEST=10;
	public static final int SC_MOVED_PERMANENTLY=11;
	public static final int SC_CONFIG_RQ = 12;

	private static Jhttpp2Server server;

	/** downstream connections */
	private Socket client;
	private BufferedOutputStream out;
	private Jhttpp2ClientInputStream in;

	/** upstream connections */
	private Socket HTTP_Socket;
	private BufferedOutputStream HTTP_out;
	private Jhttpp2ServerInputStream HTTP_in;

	public Jhttpp2HTTPSession(Jhttpp2Server server,Socket client) {
		try {
			in = new Jhttpp2ClientInputStream(server,this,client.getInputStream());//,true);
			out = new BufferedOutputStream(client.getOutputStream());
			this.server=server;
			this.client=client;
		}
		catch (IOException e_io) {
			try {
				client.close();
			}
			catch (IOException e_io2) {}
			server.writeLog("Error while creating IO-Streams: " + e_io);
			return;
		}
		start();
	}
	public Socket getLocalSocket() {
		return client;
	}
	public Socket getRemoteSocket() {
		return HTTP_Socket;
	}
	public boolean isTunnel() {
		return in.isTunnel();
	}
	public boolean notConnected() {
		return HTTP_Socket==null;
	}
	public void sendHeader(int a,boolean b)throws IOException {
		sendHeader(a);
		endHeader();
		out.flush();
	}
	public void sendHeader(int status, String content_type, long content_length) throws IOException {
		sendHeader(status);
		sendLine("Content-Length", String.valueOf(content_length));
		sendLine("Content-Type", content_type );
	}
	public void sendLine(String s) throws IOException {
		write(out,s + "\r\n");
	}
	public void sendLine(String header, String s) throws IOException {
		write(out,header + ": " + s + "\r\n");
	}
	public void endHeader() throws IOException {
		write(out,"\r\n");
	}
	public void run() {
		if (server.debug)server.writeLog("begin http session");
		server.increaseNumConnections();
		try {
			handleRequest();
		}
		catch (IOException e_handleRequest) {
			if (server.debug) System.out.println(e_handleRequest.toString());
		}
		catch (Exception e) {
			e.printStackTrace();
			server.writeLog("Jhttpp2HTTPSession.run(); " + e.getMessage());
		}
		try {
			// close downstream connections
			in.close(); // since 0.4.10b
			out.close();
			client.close();
			// close upstream connections (webserver or other proxy)
			if (!notConnected()) {
				HTTP_Socket.close();
				HTTP_out.close();
				HTTP_in.close();
			}
		}
		catch (IOException e_run) {
			System.out.println(e_run.getMessage());
		}
		server.decreaseNumConnections();
		if (server.debug)server.writeLog("end http session");
	}
	/** sends a message to the user */
	public void sendErrorMSG(int a,String info)throws IOException {
		String statuscode = sendHeader(a);
		String localhost = "localhost";
		try {
			localhost = InetAddress.getLocalHost().getHostName() + ":" + server.port;
		}
		catch(UnknownHostException e_unknown_host ) {}
		String msg = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"><html>\r"
		+ "<!-- jHTTPp2 error message --><HEAD>\r"
		+ "<TITLE>" + statuscode + "</TITLE>\r"
		+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://" + localhost + "/style.css\"></HEAD>\r"  // use css style sheet in htdocs
		+ "<BODY BGCOLOR=\"#FFFFFF\" TEXT=\"#000000\" LINK=\"#000080\" VLINK=\"#000080\" ALINK=\"#000080\">\r"
		+ "<h2 class=\"headline\">HTTP " + statuscode + " </h2>\r"
		+ "<HR size=\"4\">\r"
		+ "<p class=\"i30\">Your request for the following URL failed:</p>"
		+ "<p class=\"tiagtext\"><a href=\"" + in.getFullURL() + "\">" + in.getFullURL() + "</A> </p>\r"
		+ "<P class=\"i25\">Reason: " + info + "</P>"
		+ "<HR size=\"4\">\r"
		+ "<p class=\"i25\"><A HREF=\"http://jhttp2.sourceforge.net/\">jHTTPp2</A> HTTP Proxy, Version " + server.getServerVersion() + " at " + localhost
		+ "<br>Copyright &copy; 2001-2003 <A HREF=\"mailto:bkohl@users.sourceforge.net\">Benjamin Kohl</A></p>\r"
		+ "<p class=\"i25\"><A HREF=\"http://" + localhost + "/\">jHTTPp2 local website</A> <A HREF=\"http://" + localhost + "/" + server.WEB_CONFIG_FILE + "\">Configuration</A></p>"
		+ "</BODY></HTML>";
		sendLine("Content-Length",String.valueOf(msg.length()));
		sendLine("Content-Type","text/html; charset=iso-8859-1");
		endHeader();
		write(out,msg);
		out.flush();
	}

	public String sendHeader(int a)throws IOException	{
		String stat;
		switch(a) {
			case 200:stat="200 OK"; break;
			case 202:stat="202 Accepted"; break;
			case 300:stat="300 Ambiguous"; break;
			case 301:stat="301 Moved Permanently"; break;
			case 400:stat="400 Bad Request"; break;
			case 401:stat="401 Denied"; break;
			case 403:stat="403 Forbidden"; break;
			case 404:stat="404 Not Found"; break;
			case 405:stat="405 Bad Method"; break;
			case 413:stat="413 Request Entity Too Large"; break;
			case 415:stat="415 Unsupported Media"; break;
			case 501:stat="501 Not Implemented"; break;
			case 502:stat="502 Bad Gateway"; break;
			case 504:stat="504 Gateway Timeout"; break;
			case 505:stat="505 HTTP Version Not Supported"; break;
			default: stat="500 Internal Server Error";
		}
		sendLine(server.getHttpVersion() + " " + stat);
		sendLine("Server",server.getServerIdentification());
		if (a==501) sendLine("Allow","GET, HEAD, POST, PUT, DELETE, CONNECT");
		sendLine("Cache-Control", "no-cache, must-revalidate");
		sendLine("Connection","close");
		return stat;
	}

  /** the main routine, where it all happens */
	public void handleRequest() throws Exception {
		InetAddress remote_host;
		Jhttpp2Read remote_in=null;
		int remote_port;
		byte[] b=new byte[65536];
		int numread=in.read(b);

		while(true) { // with this loop we support persistent connections
			if (numread==-1) { // -1 signals an error
				if (in.getStatusCode()!=SC_CONNECTING_TO_HOST) {
					switch (in.getStatusCode()) {
						case SC_CONNECTION_CLOSED: break;
						case SC_CLIENT_ERROR: sendErrorMSG(400,"Your client sent a request that this proxy could not understand. (" + in.getErrorDescription() + ")"); break;
						case SC_HOST_NOT_FOUND: sendErrorMSG(504,"Host not found.<BR>jHTTPp2 was unable to resolve the hostname of this request. <BR>Perhaps the hostname was misspelled, the server is down or you have no connection to the internet."); break;
						case SC_INTERNAL_SERVER_ERROR: sendErrorMSG(500,"Server Error! (" + in.getErrorDescription() + ")"); break;
						case SC_NOT_SUPPORTED: sendErrorMSG(501,"Your client used a HTTP method that this proxy doesn't support: (" + in.getErrorDescription() + ")"); break;
						case SC_URL_BLOCKED: sendErrorMSG(403,(in.getErrorDescription()!=null && in.getErrorDescription().length()>0?in.getErrorDescription():"The request for this URL was denied by the jHTTPp2 URL-Filter.")); break;
						//case SC_REMOTE_DEBUG_MODE: remoteDebug(); break;
						case SC_HTTP_OPTIONS_THIS: sendHeader(200); endHeader(); break;
						case SC_FILE_REQUEST: file_handler(); break;
						case SC_CONFIG_RQ: admin_handler(b); break;
						//case SC_HTTP_TRACE:
						case SC_MOVED_PERMANENTLY:
							sendHeader(301);
							write(out,"Location: " + in.getErrorDescription() + "\r\n");
							endHeader();
							out.flush();
						default:
					}
					break; // return from main loop.
				}
				else { // also an error because we are not connected (or to the wrong host)
					// Creates a new connection to a remote host.
					if (!notConnected()) {
						try {
							HTTP_Socket.close();
						}
						catch (IOException e_close_socket) {}
					}
					numread=in.getHeaderLength(); // get the header length
					if (!server.use_proxy) {// sets up hostname and port
						remote_host=in.getRemoteHost();
						remote_port=in.remote_port;
					}
					else {
						remote_host=server.proxy;
						remote_port=server.proxy_port;
					}
					//if (server.debug)server.writeLog("Connect: " + remote_host + ":" + remote_port);
					try {
						connect(remote_host,remote_port);
					}
					catch (IOException e_connect) {
						if (server.debug) server.writeLog(e_connect.toString());
						sendErrorMSG(502,"Error while creating a TCP connecting to [" +remote_host.getHostName()+ ":" + remote_port + "] <BR>The proxy server cannot connect to the given address or port [" + e_connect.toString() + "]");
						break;
					}
					catch (Exception e) {
						server.writeLog(e.toString());
						sendErrorMSG(500,"Error: " + e.toString());
						break;
					}
					if (!in.isTunnel()  || (in.isTunnel() && server.use_proxy))
					{ // no SSL-Tunnel or SSL-Tunnel with another remote proxy: simply forward the request
						HTTP_out.write(b, 0, numread);
						HTTP_out.flush();
					}
					else
					{ //  SSL-Tunnel with "CONNECT": creates a tunnel connection with the server
						sendLine(server.getHttpVersion() + " 200 Connection established");
						sendLine("Proxy-Agent",server.getServerIdentification());
						endHeader(); out.flush();
					}
					remote_in = new Jhttpp2Read(server,this, HTTP_in, out); // reads data from the remote server
					server.addBytesWritten(numread);
				}
			}
			while(true) { // reads data from the client
				numread=in.read(b);
				//if (server.debug)server.writeLog("Jhttpp2HTTPSession: " + numread + " Bytes read.");
				if (numread!=-1) {
					HTTP_out.write(b, 0, numread);
					HTTP_out.flush();
					server.addBytesWritten(numread);
				} else break;
			} // end of inner loop
		}// end of main loop
		out.flush();
		if (!notConnected() && remote_in != null)
			remote_in.close(); // close Jhttpp2Read thread
		return;
	}
  /** connects to the given host and port */
  public void connect(InetAddress host,int port)throws IOException {
      HTTP_Socket = new Socket(host,port);
      HTTP_in = new Jhttpp2ServerInputStream(server,this,HTTP_Socket.getInputStream(),false);
      HTTP_out = new BufferedOutputStream(HTTP_Socket.getOutputStream());
  }
  /** converts an String into a Byte-Array to write it with the OutputStream */
  public void write(BufferedOutputStream o,String p)throws IOException {
    o.write(p.getBytes(),0,p.length());
  }

  /**
   * Small webserver for local files in {app}/htdocs
   * @since 0.4.04
   */
  public void file_handler() throws IOException {
	if (!server.www_server) {
		sendErrorMSG(500, "The jHTTPp2 built-in WWW server module is disabled.");
		return;
	}
    String filename=in.url;
    if (filename.equals("/")) filename="index.html"; // convert / to index.html
    else if (filename.startsWith("/")) filename=filename.substring(1);
    if (filename.endsWith("/")) filename+="index.html"; // add index.html, if ending with /
    File file = new File("htdocs/" + filename); // access only files in "htdocs"
    if ( !file.exists() || !file.canRead() // be sure that we can read the file
		|| filename.indexOf("..")!=-1 // don't allow ".." !!!
		|| file.isDirectory() ) { // dont't read if it's a directory
      sendErrorMSG(404,"The requested file /" + filename + " was not found or the path is invalid.");
      return;
    }
    int pos = filename.lastIndexOf("."); // MIME type of the specified file
    String content_type="text/plain"; // all unknown content types will be marked as text/plain
    if (pos != -1) {
    	String extension = filename.substring(pos+1);
    	if (extension.equalsIgnoreCase("htm") || (extension.equalsIgnoreCase("html"))) content_type="text/html; charset=iso-8859-1";
    	else if (extension.equalsIgnoreCase("jpg") || (extension.equalsIgnoreCase("jpeg"))) content_type="image/jpeg";
    	else if (extension.equalsIgnoreCase("gif")) content_type = "image/gif";
    	else if (extension.equalsIgnoreCase("png")) content_type = "image/png";
    	else if (extension.equalsIgnoreCase("css")) content_type = "text/css";
    	else if (extension.equalsIgnoreCase("pdf")) content_type = "application/pdf";
    	else if (extension.equalsIgnoreCase("ps") || extension.equalsIgnoreCase("eps")) content_type = "application/postscript";
    	else if (extension.equalsIgnoreCase("xml")) content_type = "text/xml";
	}
    sendHeader(200,content_type, file.length() );
    endHeader();
    BufferedInputStream file_in = new BufferedInputStream(new FileInputStream(file));
    byte[] buffer=new byte[4096];
    int a=file_in.read(buffer);
    while (a!=-1) { // read until EOF
      out.write(buffer,0,a);
      a = file_in.read(buffer);
    }
    out.flush();
    file_in.close(); // finished!
  }
  /**
   * @since 0.4.10b
   */
  public int getStatus()
  {
    return in.getStatusCode();
  }
  /**
     * @since 0.4.20a
     * admin webpage
   */
  public void admin_handler(byte[] b) throws IOException
  {
	  if (!server.webconfig) {
		  sendErrorMSG(500,"The web-based configuration module is disabled.");
		  return;
	  }
	  Jhttpp2Admin admin = null;
	  String filename = in.url;
	  if (in.post_data_len > 0) { // if the client used "POST" then append the data to the filename
		  filename = filename + "?" + new String(b,in.getHeaderLength()-in.post_data_len,in.post_data_len);
	  }
	  if (filename.startsWith("/")) filename = filename.substring(1);
      String adminpage = "";
      try {
        admin = new Jhttpp2Admin( filename, server ) ;
		admin.WebAdmin();
		adminpage = admin.HTMLAdmin();
      }
      catch( Exception e) {
		e.printStackTrace();
		server.writeLog("Jhttpp2Admin Exception: " + e.getMessage());
      }
    	int adminlen = adminpage.length();
    	if ( adminlen < 1 ) {
    	    sendErrorMSG(500,"Error Message from the Web-Admin modul: " + admin.error_msg);
    	}
    	else {
    	  	sendHeader(200,"text/html",adminlen);
    	    endHeader();
    	    write(out,adminpage);
    	}
    	out.flush();
	}
}
