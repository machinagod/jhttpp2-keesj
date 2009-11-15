jar:compile Jhttpp2.jar

Jhttpp2.jar:
	( cd bin ; jar xvf ../lib/commons-logging.jar )
	jar cmf src/Console.mf Jhttpp2.jar -C bin .
compile:
	rm -rf Jhttpp2.jar
	rm -rf bin
	mkdir bin
	javac -d bin -cp lib/commons-logging.jar:bin src/jhttpp2/*.java  src/jhttpp2/admin/*.java

clean:
	 rm -rf bin
