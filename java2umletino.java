/*
 * Convert .java files into umletino (umletino.me) format
 *
 * compile: antlr4 java.g4; javac -cp antlr-4.5-complete.jar:. *.java
 * run: java -cp antlr-4.5-complete.jar:. java2umletino *.java
 * jar: jar -cfm java2umletino.jar MANIFEST.MF *.class java2umletino.java java.g4 antlr-4.5-complete.jar
 * reis.santos@tecnico.ulisboa.pt (C)28oct2015
 */

import java.io.*;
import java.util.*;
import org.antlr.v4.runtime.*;

public class java2umletino {
    private static boolean param = true, atrib = true, meth = true;
    private static String prot = "";
    private static PrintWriter out = new PrintWriter(System.out);
    private static final String USAGE = "USAGE: java2umletino [-a] [-o outfile] [-p] files.java ...";

    public static void main(String[] args) throws Exception {
	int argc = 0;
	if (args.length == 0) {
	    System.err.println(USAGE + "\n"
	    	+ "\t-a: omit attributes in classes\n"
	    	+ "\t-o outfile: redirect output to file\n"
	    	+ "\t-p: omit parameters in methods");
	    return;
	}
	if (args.length > argc && args[argc].equals("-a")) { atrib = false; argc++; }
	if (args.length > argc && args[argc].equals("-o")) {
	    argc++;
	    if (args.length > argc) {
		out = new PrintWriter(args[argc], "UTF-8");
		argc++;
	    } else 
		System.err.println(USAGE + "\n\t-o: outfile missing.");
	}
	if (args.length > argc && args[argc].equals("-p")) { param = false; argc++; }
	while (argc < args.length) parse(args[argc++]);
	out.close();
    }

    public static void parse(String file) throws Exception {
	InputStream in = new FileInputStream(file);
        JavaLexer l = new JavaLexer(new ANTLRInputStream(in));
        JavaParser p = new JavaParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });

        p.addParseListener(new JavaBaseListener() {

            @Override
            public void exitClassHeader(JavaParser.ClassHeaderContext ctx) {
		out.print("\n"+ctx.Identifier().getText());
		if (ctx.type() != null)
		    out.print(" extends " + ctx.type().getText());

		if (ctx.typeList() != null) {
		    out.print(" implements");
		    for (JavaParser.TypeContext t: ctx.typeList().type())
			out.print(" " + t.getText());
		}
		out.println("\n--");
            }

            @Override
            public void exitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
		String id = ctx.Identifier().getText();
		String t = "void", p = "";
		JavaParser.FormalParameterListContext f =
		    ctx.formalParameters().formalParameterList();

		if (ctx.type() != null) t = ctx.type().getText();

		if (!param) f = null;
		if (f != null) {
		    for (int i = 0; i < f.formalParameter().size(); i++) {
			if (i > 0) p += " , ";
			p += f.formalParameter(i).variableDeclaratorId().getText()
			    + ": " + f.formalParameter(i).type().getText();
		    }
		    if (f.lastFormalParameter() != null)
			p += f.lastFormalParameter().variableDeclaratorId().getText()
			    + ": " + f.lastFormalParameter().type().getText();
		}
		if (!meth) { meth = true; out.println("--"); }
                out.println(prot + " " + id+"("+p+") : "+t);
            }

            @Override
            public void exitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
		if (!atrib) return;
		String t = ctx.type().getText();
		for (JavaParser.VariableDeclaratorContext v: ctx.variableDeclarators().variableDeclarator())
		    out.println(prot + " " + v.variableDeclaratorId().getText() + ": " + t);
            }

            @Override
            public void exitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
		String id = ctx.Identifier().getText();
		String p = "";
		JavaParser.FormalParameterListContext f =
		    ctx.formalParameters().formalParameterList();

		if (!param) f = null;
		if (f != null) {
		    for (int i = 0; i < f.formalParameter().size(); i++) {
			if (i > 0) p += " , ";
			p += f.formalParameter(i).variableDeclaratorId().getText()
			    + ": " + f.formalParameter(i).type().getText();
		    }
		    if (f.lastFormalParameter() != null)
			p += f.lastFormalParameter().variableDeclaratorId().getText()
			    + ": " + f.lastFormalParameter().type().getText();
		}
		if (!meth) { meth = true; out.println("--"); }
                out.println(prot + " " + id+"("+p+")");
            }

            @Override
            public void exitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
		meth = false;
            }

            @Override
            public void enterClassOrInterfaceModifier(JavaParser.ClassOrInterfaceModifierContext ctx) {
	    	prot = "~";
            }

            @Override
            public void exitPublicModif(JavaParser.PublicModifContext ctx) {
	    	prot = "+";
            }

            @Override
            public void exitProtectedModif(JavaParser.ProtectedModifContext ctx) {
	    	prot = "#";
            }

            @Override
            public void exitPrivateModif(JavaParser.PrivateModifContext ctx) {
	    	prot = "-";
            }

        });
        p.compilationUnit();
    }
}
