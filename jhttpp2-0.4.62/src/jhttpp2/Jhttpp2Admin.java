package jhttpp2;
/* Written and copyright 2001-2003 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 * Authors:
 *	Benjamin Kohl <bkohl@users.sf.net>
 *	Artem Melnik <mandara@mac.com>
 * Created:	10.2001
 **/
import java.util.Enumeration;
import java.util.Properties;
import java.io.IOException;


public class Jhttpp2Admin {
  /*****************************************
  Artem Melnik
  Known problems:
  1. no overflow checks
  2. argument parsing is loose (e.g. filter= can be equal to anotherfilter=)
  3. doesn't understand arguments without value
  ******************************************/

	private static Jhttpp2Server server;

	private final String JHTTP2ADMIN_VERSION = "0.2.4 20-05-2003";

	private String myfilename = "";
	private Properties p;

	private static String status = "";
	private static long stat = 0;
	static public String error_msg = "";


	/** perform admin functions */
	public Jhttpp2Admin( String infilename, Jhttpp2Server inserver ) {
		server = inserver;
		myfilename = infilename;
	}
	public void WebAdmin()  throws IOException {
		String filename = myfilename;
		ParseRequest( filename );
		String app = ( String ) p.get("requesturl");

		if ( app != null && app.equalsIgnoreCase( server.WEB_CONFIG_FILE ) ) {

			String command = ( String ) p.get( "command" );
			int sid = 0;
			try { sid = Integer.parseInt((String)p.get("sid")); }
			catch (NumberFormatException e_nfe) { sid = 0; }
			if ( command != null )			{
				if ( command.equals( "auth" ) )				{
					String user = (String)p.get("user");
					String passw = (String)p.get("p");
					server.AuthenticateUser(user,passw);
					if (server.config_auth != 0) {
						server.config_session_id = (long)(Math.random() * 100000);
						status = "Access granted";
						stat = 3;
					}
					else {
						status = "Acess forbidden";
						stat = 2;
					}
				}
				else if (server.config_auth != 0 && sid == server.config_session_id) {
					if (command.equals("logout")) {
						server.AuthenticateUser("","");
						server.config_session_id = 0;
						status = "Logged out.";
						stat = 1;
					}
					else if (command.equals("shutdown")) {
						server.shutdownServer();
					}
					else if (command.equals("update_password")) {
						String a = (String)p.get("old_pass");
						String b = (String)p.get("new_pass");
						String c = (String)p.get("new_pass2");

						if (!a.equals(server.config_password)) {
							status = "Enter the correct old password.";
							stat = 5;
						}
						else if (a.equals(b)) {
							status = "Old and new password must be different.";
							stat = 5;
						}
						else if (a == null || b == null) {
							status = "You must enter a password. (should be at least 8 characters long)";
							stat = 5;
						}
						else {
							if (b.equals(c)) {
								server.config_password = b;
								server.saveSettings();
								stat = 5;
								status = "Password sucessfully updated.";
							}
						}
					}
					else if ( command.equals( "savefilter" ) ){
						String filterurl =  ( String ) p.get( "filter" );
						String action =  ( String ) p.get( "action" );

						if ( filterurl != null && action != null ) {
							Jhttpp2URLMatch a = new Jhttpp2URLMatch( filterurl, false/*set to real*/, Integer.parseInt( action ), ""/*set to real!*/ ); // add new url match
							server.getWildcardDictionary().put( filterurl,a );
						}
					}
					else if ( command.equals( "global" ) ) {
						String a =  ( String ) p.get( "debug" );
						server.debug = (a != null);
						a =  ( String ) p.get( "use_proxy" );
						server.use_proxy = (a != null);
						a = (String) p.get("webconfig");
						server.webconfig = (a != null);
						a = (String) p.get("www_server");
						server.www_server = (a != null);
						a = (String) p.get("filter_http");
						server.filter_http = (a != null);
						a = (String) p.get("http_useragent");
						if (a != null) server.setUserAgent(a);
						a = (String) p.get("block_urls");
						server.block_urls = (a != null);
						a = (String) p.get("enable_cookies_by_default");
						server.enableCookiesByDefault(a != null);
						a = (String) p.get("port");
						if (a != null) {
							try {
								server.port = Integer.parseInt(a);
							} catch (NumberFormatException e_easx)
							{ server.port = server.DEFAULT_SERVER_PORT; }
						}
						a = (String) p.get("log_access");
						server.log_access = (a != null);
						a = (String) p.get("log_access_filename");
						if (a != null) server.log_access_filename = a;

						//a = (String) p.get("remote_proxy_hostname");
						//if (a != null) server.config_user = a;

						//server.http-proxy.hostname=127.0.0.1
						//server.http-proxy.port=8080

						server.saveSettings(); //save settings
						status = "Changes saved.";
						stat = 4;
					}
					else if ( command.equals( "deletefilter" ) ) {
						String filterid =  ( String ) p.get( "filterid" );
						if (filterid != null) server.getWildcardDictionary().remove( filterid );

					}
				} // end if (server.config_auth != 0)
				else {
					status = "You must log in to excute this command (" + command + ").";
					stat = 1;
					server.config_session_id = 0;
					server.config_auth = 0;
				}
			}// end if command != null
			else {
				server.config_auth = 0;
				server.config_session_id = 0;
			}
		}// if app
	}//end void

	public void ParseRequest( String filename )	{
		p = new Properties();
		int filenamelen = filename.length();

		int ixQmark = filename.indexOf( "?" );
		if ( ixQmark != -1 ) {
			String request = Jhttpp2Utils.urlDecoder( filename.substring( ixQmark + 1, filenamelen ) );
			if ( server.debug ) System.out.println( "\trequest: " + request );

			p.put( "requesturl", filename.substring( 0, ixQmark ) );
			if ( server.debug ) System.out.println( "\trequesturl: " + filename.substring( 0, ixQmark ) );


			GetParams( request );
		}
		else {
			p.put( "requesturl", filename );
			if ( server.debug ) System.out.println( "\trequesturl: " + filename );
		}
	}
	public void GetParams( String request ) {
		boolean d = true;
		int prev = 0;

		while ( d ) {
			int nextamp = request.indexOf( "&", prev );

			if ( nextamp != -1 )
			{
				String pair = request.substring( prev, nextamp );
				if ( server.debug ) System.out.println( "\tpair: " + pair );
				ParseParam( pair, p);

				prev = nextamp + 1;
				pair = null;
			}
			else {
				String pair = request.substring( prev, request.length() );
				if ( server.debug ) System.out.println( "\tpair: " + pair );
				ParseParam( pair, p);
				pair = null;
				d = false;
			}
		}

	}

	public boolean ParseParam( String pair, Properties p ) {
		boolean result = false;
		int ixequal = pair.indexOf( "=" );
		if ( ixequal != -1) {
			String param = pair.substring( 0, ixequal ).toLowerCase();
			if ( server.debug ) System.out.println( "\tparam: " + param );
			String val = pair.substring( ixequal + 1, pair.length() );
			if ( server.debug ) System.out.println( "\tval: " + val );
			p.put( param, val );
			result = true;
		}
		else
			p.put( pair, "" );

		return result;
	}

	/** html form */
	public String HTMLAdmin() {
		String msg = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\"><html>\r"
		+ "<!-- jHTTPp2 admin page --><HEAD>\r"
		+ "<TITLE>jHTTPp2 Admin</TITLE>\r"
		+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"/style.css\"></HEAD>\r"
		+"<body><h2 class=\"headline\">jHTTPp2 Configuration</h2>\r";


		if (server.config_auth == 0) {
			msg = msg +"\r"
			+"<form method=\"POST\" action=\"/" + server.WEB_CONFIG_FILE + "\">"
			+"<input type=\"hidden\" name=\"command\" value=\"auth\" />"
			+"<p>Please enter your password to access the administration form.</p>"
			+"<table border=\"0\" cellpadding=\"3\" cellspacing=\"0\">"
			+"<tr>"
			+"<td align=\"right\">Username:</td>"
			+"<td><input type=\"text\" class=\"Feld\" name=\"user\" size=\"14\" maxlength=\"40\"></td>"
			+"</tr>"
			+"<tr>"
			+"<td align=\"right\">Password:</td>"
			+"<td><input type=\"password\" class=\"Feld\" name=\"p\" size=\"17\" maxlength=\"40\"></td>"
			+"</tr>"
			+"<tr>"
			+"<td align=\"right\">"
			+"<input type=\"submit\" class=\"Button\" value=\"Summit\" name=\"B1\">"
			+"<input type=\"reset\" class=\"Button\" value=\"Reset\" name=\"B2\">"
			+"</td></tr>"
			+"</table>"
			+"</form>"
			+"<a href=\"/\">jHTTPp2 local homepage</a>";
		}
		else {
			msg = msg
			+"<form method=\"POST\" action=\"/" + server.WEB_CONFIG_FILE + "\">\r"
			+"<input type=\"hidden\" name=\"command\" value=\"global\"/>"
			+"<input type=\"hidden\" name=\"sid\" value=\"" + server.config_session_id + "\"/>"
			+"<b>Global Settings</b>"
			+"<table align=\"center\" border=\"0\" cellspacing=\"1\" style=\"border-collapse: collapse\" bordercolor=\"#111111\" width=\"500\" height=\"32\" id=\"AutoNumber1\">"
			+ chk_option	("server.webconfig <br><i>Enables the web-based configuration module</I>",		"webconfig",	server.webconfig)
			+ chk_option	("server.www <br><i> The built-in webserver </i>",			"www_server",		server.www_server)
			//+ chk_option	("server.debug-logging"	,		"debug",		server.debug)
			+ chk_option	("server.debug-logging"	,		"debug",		server.debug)
			+ chk_option	("server.http-proxy",			"use_proxy",	server.use_proxy)
			+ chk_option	("server.fiter.http",			"filter_http",	server.filter_http)
			+ txt_field		("","server.filter.http.useragent","http_useragent", server.getUserAgent())
			+ chk_option	("server.filter.url",			"block_urls",	server.block_urls)
			+ chk_option	("server.enable-cookies-by-default",	"enable_cookies_by_default",
			server.enableCookiesByDefault())
			+txt_field		("","server.port",	"port",	Integer.toString(server.port))
			+chk_option		("server.access.log",			"log_access",	server.log_access)
			+txt_field		("","server.access.log.filename",	"log_access_filename",server.log_access_filename)
			//+txt_field		("server.webconfig.username", 	"config_username", server.config_user)
			//+txt_field		("server.webconfig.password", 	"config_password", server.config_password,true)
			+"<tr>"
			+"<td width=\"52\" height=\"6\"></td>"
			+"<td width=\"449\" height=\"6\"><input type=\"submit\" value=\"Save Settings\" /></td>"
			+"</tr>"
			+"</table>"
			+"</form>"
			// Password update
			+"<form method=\"POST\" action=\"/" + server.WEB_CONFIG_FILE + "\">\r"
			+"<input type=\"hidden\" name=\"command\" value=\"update_password\"/>"
			+"<input type=\"hidden\" name=\"sid\" value=\"" + server.config_session_id + "\"/>"
			+"<b>Administration Password</b>"
			+"<table align=\"center\" border=\"0\" cellspacing=\"1\" style=\"border-collapse: collapse\" bordercolor=\"#111111\" width=\"500\" height=\"32\" id=\"AutoNumber1\">"
			+txt_field("Old Password:","","old_pass","",true)
			+txt_field("New Password:","","new_pass","",true)
			+txt_field("Repeat New Password:","","new_pass2","",true)
			+"<tr>"
			+"<td width=\"52\" height=\"6\"></td>"
			+"<td width=\"449\" height=\"6\"><input type=\"submit\" value=\"Update Password\" /></td>"
			+"</tr>"
			+"</table>"
			+"</form>"

			+ HTMLFiltersList()

			+"<a href=\"/" + server.WEB_CONFIG_FILE + "?command=logout&sid=" + server.config_session_id + "\">Logout</a>&nbsp;<a href=\"/" + server.WEB_CONFIG_FILE + "?command=shutdown&sid=" + server.config_session_id + "\">Shutdown Server</a>";
		}

		msg = msg
		//+ "<HR size=\"4\">\r"
		+ "<p class=\"i25\"><A HREF=\"http://jhttp2.sourceforge.net/\">jHTTPp2</A> HTTP Proxy, Version " + server.getServerVersion() + "\r"
		+ " webadmin v." + JHTTP2ADMIN_VERSION + "\r"
		+ "<br>Copyright &copy; 2001-2003 <A HREF=\"mailto:bkohl@users.sourceforge.net\">Benjamin Kohl</A></p>\r"
		+ "<p class=\"status\">" + status + "</p>\r"
		+ "</BODY></HTML>";

		if (stat >= 1) {
			stat = 0;
			status ="";
		}
		return msg.toString();
	}

	public static String chk_option(String o,String o2,boolean w) {
		return "<tr>\r"
		+"<td width=\"52\" height=\"6\">\r"
		+"<input name=\"" + o2 + "\" type=\"checkbox\"" + (w?" checked ":"") + "/></td>\r"
		+"<td width=\"449\" height=\"6\">" + o + "</td>\r"
		+"</tr>\r";
	}
	public static String txt_field(String a1,String o,String o2,String w) {
		return "<tr>\r"
		+"<td width=\"52\" height=\"6\">" + a1 + "</td>\r"
		+"<td width=\"449\" height=\"6\">" + o + " <input type=\"text\" name=\"" + o2 + "\" size=\"20\" value=\"" + w + "\"></td>\r"
		+"</tr>\r";
	}
	public static String txt_field(String a1, String o,String o2,String w,boolean b) {
			return "<tr>\r"
			+"<td width=\"52\" height=\"6\">"+ a1 + "</td>\r"
			+"<td width=\"449\" height=\"6\">" + o + " <input type=\"password\" name=\"" + o2 + "\" size=\"20\" value=\"" + w + "\"></td>\r"
			+"</tr>\r";
	}

	/** list of actions as html popup */
	public static String HTMLActionPopup() {
		StringBuffer out = new StringBuffer();

		Enumeration an=server.getURLActions().elements();
		int i = 0;

		while (an.hasMoreElements())
		{
			OnURLAction a = (OnURLAction) an.nextElement() ;
			String aname = a.getDescription() ;

			out.append( "<option value=\"" );
			out.append( i );
			out.append( "\">");
			out.append( aname );
			out.append( "</options>" );
			i++;
		}
		return out.toString();

	}

	public static String HTMLFiltersList () {

		StringBuffer out = new StringBuffer();

		Enumeration an=server.getWildcardDictionary().elements();

		int i = 0;

		out.append(
			 "<form method=\"POST\" action=\"/" + server.WEB_CONFIG_FILE + "\">\r"
			+"<input type=\"hidden\" name=\"command\" value=\"savefilter\"/>"
			+"<input type=\"hidden\" name=\"sid\" value=\"" + server.config_session_id + "\"/>"
			+"<b>URL Filter Settings</b><br>"
			+"New/Update Filter: <input type=\"text\" name=\"filter\" size=\"20\" value=\"\">"
			+"Action: <input type=\"text\" name=\"action\" size=\"10\" value=\"1\"> "
			+"<input type=\"submit\" value=\"Save\" />"
			+"<br>"
			+"<p style=\"font-size: 10px;\">");
		while (an.hasMoreElements())
		{
			Jhttpp2URLMatch a = ( Jhttpp2URLMatch ) an.nextElement() ;
			String aname = a.getMatch() ;

			out.append(
			a.getMatch()+ " [" + Integer.toString(a.getActionIndex())
			+"] <a href=\"/" + server.WEB_CONFIG_FILE + "?command=deletefilter&filterid=" + aname + "&sid=" + server.config_session_id + "\">Delete Filter</a>"
			+ "<br>");
		}
		out.append("</p></form>");

		return out.toString();
	}
}
