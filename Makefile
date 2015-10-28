.SUFFIXES: .class .java .g4
GRAM=Java
MAIN=java2yuml
FILE=*.java
JFILE=../java2uml.jar

all:
	antlr4 $(GRAM).g4
	javac *.java

.g4.java:
	antlr4 $<

.java.class:
	javac $<

jar: all
	jar -cfm $(JFILE) MANIFEST.MF .

yuml: all
	java $(MAIN) $(FILE)

umletino: all
	java java2umletino $(FILE)

clean:
	rm -f *.class $(GRAM)[A-Z]*.java *.tokens *~
