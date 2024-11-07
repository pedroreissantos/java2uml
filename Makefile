.SUFFIXES: .class .java .g4
GRAM=Java
MAIN=java2yuml
FILE=*.java
JFILE=../java2uml.jar
CP=wget-1.3.0.jar:antlr-4.5-complete.jar:org.apache.commons-io-2.4.jar:.

all:
	antlr4 $(GRAM).g4
	javac -cp $(CP) *.java

.g4.java:
	antlr4 $<

.java.class:
	javac -cp $(CP) $<

jar: all
	jar -cfm $(JFILE) MANIFEST.MF .

yuml: all
	java -cp $(CP) $(MAIN) $(FILE)

umletino: all
	java -cp $(CP) java2umletino $(FILE)

clean:
	rm -f *.class $(GRAM)*.java *.interp *.tokens *~
