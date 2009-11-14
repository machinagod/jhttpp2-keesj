================================================================

			 README

	jHTTPp2 - OpenSource HTTP Proxy Server

	This software requires the Java Runtime Environment (JRE)

================================================================

The PDF-File "htdocs/jp2-user-manual.pdf" contains the full user 
manual. Please read it first.

----------------------------------------------------------------
Starting the proxy-server
----------------------------------------------------------------
Use the file "jhttpp2.bat" to start the proxy.

If that doesn't work (or if you're using Linux or Max OS) than
you can type in a console window:

	java -jar Jhttpp2.jar

	OR

	java -classpath Jhttpp2.jar Jhttpp2Launcher

This should told the Java VM to start jHTTPp2 in the console
(text) mode with using the Jhttpp2.jar file as classpath 

Caution: "Jhttpp2Launcher" is case-sensitive (or you will get
"Class not found error" or something)
To use the "java" command anywhere, it must be in your PATH
variable.

Now you should see a message like:

	jHTTPp2 HTTP Proxy Server Release 0.4.50
	Copyright (c) 2001-2003 by Benjamin Kohl
	http://jhttp2.sourceforge.net/
	Running on port 8088

Then start your webbrowser and go to:

	http://localhost:8088/