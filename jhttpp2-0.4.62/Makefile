jar:compile Jhttpp2.jar

Jhttpp2.jar:
	jar cmf src/Console.mf Jhttpp2.jar -C bin .
compile:
	rm -rf Jhttpp2.jar
	rm -rf bin
	mkdir bin
	javac -d bin src/jhttpp2/*.java

clean:
	 rm -rf bin
