/*
 * Convert .java files into yuml (yuml.me) format
 *
 * compile: antlr4 java.g4; javac -cp antlr-4.5-complete.jar:. *.java
 * run: java -cp antlr-4.5-complete.jar:. java2yuml *.java
 * jar: jar -cfm java2yuml.jar MANIFEST.MF *.class java2yuml.java java.g4 antlr-4.5-complete.jar
 * reis.santos@tecnico.ulisboa.pt (C)28oct2015
 */

import java.io.*;
import java.util.*;
import org.antlr.v4.runtime.*;

public class java2yuml {
    private static boolean param = true, atrib = true, func = true, meth = false;
    private static String decl, locals = "", prot = "", cl, m1 = "]1-1>[", m2 = "]1-*>[";
    private static Set<String> ids = new HashSet<String>();
    private static Map<String,String> link = new HashMap<String,String>();
    private static PrintWriter out = new PrintWriter(System.out);
    private static final String USAGE = "USAGE: java2yuml [-a] [-c] [-m] [-o outfile] [-p] files.java ...";

    public static void main(String[] args) throws Exception {
	int argc = 0;
	if (args.length == 0) {
	    System.err.println(USAGE + "\n"
	    	+ "\t-a: omit attributes in classes\n"
	    	+ "\t-c: omit cardinality in associations\n"
	    	+ "\t-m: omit methods in classes\n"
	    	+ "\t-o outfile: redirect output to file\n"
	    	+ "\t-p: omit parameters in methods\n");
	    return;
	}
	if (args.length > argc && args[argc].equals("-a")) { atrib = false; argc++; }
	if (args.length > argc && args[argc].equals("-c")) { m1 = "]->["; m2 = "]->["; argc++; }
	if (args.length > argc && args[argc].equals("-m")) { func = false; argc++; }
	if (args.length > argc && args[argc].equals("-o")) {
	    argc++;
	    if (args.length > argc) {
		out = new PrintWriter(args[argc], "UTF-8");
		argc++;
	    } else 
		System.err.println(USAGE + "\n\t-o: outfile missing.");
	}
	if (args.length > argc && args[argc].equals("-p")) { param = false; argc++; }
	for (int k, j, i = argc; i < args.length; i++) {
	    if ((j = args[i].lastIndexOf('/')) == 0) j = -1;
	    if ((k = args[i].lastIndexOf('.')) == 0) k = args[i].length();
	    locals += args[i].substring(j+1,k) + " ";
	    ids.add(args[i].substring(j+1,k));
	}
	out.println("// " + locals); // simpler than ids.toString()
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
		String id = ctx.Identifier().getText();
		String ext = null;

                cl = id;
		decl = "";
		if (ctx.type() != null)
		    decl += "[" + ctx.type().getText() + "]^-[" + id + "]\n";

		if (ctx.typeList() != null)
		    for (JavaParser.TypeContext t: ctx.typeList().type())
			decl += "[" + t.getText() + "]^-.-[" + id + "]\n";
		out.print("["+id+"|");
            }

            @Override
            public void exitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
		if (!func) return;
		String id = ctx.Identifier().getText();
		String t = "void", p = "";
		JavaParser.FormalParameterListContext f =
		    ctx.formalParameters().formalParameterList();

		if (ctx.type() != null) t = type(ctx.type(), false);

		if (!param) f = null;
		if (f != null) {
		    for (int i = 0; i < f.formalParameter().size(); i++) {
			if (i > 0) p += " . "; // yuml does not support ','
			p += f.formalParameter(i).variableDeclaratorId().getText()
			    + ": " + type(f.formalParameter(i).type(), false);
		    }
		    if (f.lastFormalParameter() != null)
			p += f.lastFormalParameter().variableDeclaratorId().getText()
			    + ": " + type(f.lastFormalParameter().type(), false);
		}
		if (!meth) { meth = true; out.print("| "); }
                out.print(prot + " " + id+"("+p+") : "+t+"; ");
                // System.err.println("MT: "+ctx.getText());
            }

            @Override
            public void exitFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
		if (!atrib) return;
		String t = type(ctx.type(), true);
		for (JavaParser.VariableDeclaratorContext v: ctx.variableDeclarators().variableDeclarator())
		    out.print(prot + " " + v.variableDeclaratorId().getText() + ": " + t + "; ");
            }

            @Override
            public void exitConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
		if (!func) return;
		String id = ctx.Identifier().getText();
		String p = "";
		JavaParser.FormalParameterListContext f =
		    ctx.formalParameters().formalParameterList();

		if (!param) f = null;
		if (f != null) {
		    for (int i = 0; i < f.formalParameter().size(); i++) {
			if (i > 0) p += " . "; // yuml does not support ','
			p += f.formalParameter(i).variableDeclaratorId().getText()
			    + ": " + type(f.formalParameter(i).type(), false);
		    }
		    if (f.lastFormalParameter() != null)
			p += f.lastFormalParameter().variableDeclaratorId().getText()
			    + ": " + type(f.lastFormalParameter().type(), false);
		}
		if (!meth) { meth = true; out.print("| "); }
                out.print(prot + " " + id+"("+p+"); ");
                // System.err.println("CT: "+ctx.getText());
            }

            @Override
            public void exitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
		out.print("]\n"+decl);
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

	    private String type(JavaParser.TypeContext ctx, boolean assoc) {
		String id = "";
		if (ctx.primitiveType() != null) {
		    int i;
		    id = ctx.getText();
		    if ((i = id.indexOf('[')) > 0) id = id.substring(0,i)+"...";
		} else {
		    int i = ctx.classOrInterfaceType().Identifier().size(), j = 0;
		    id = ctx.classOrInterfaceType().Identifier(i-1).getText();
		    if (!id.equals(ctx.getText())) id += "...";
		    if (assoc && ids.contains(id)) 
		        decl += "[" + cl+"]1-1>["+id+"]\n";
		    if (ctx.classOrInterfaceType().typeArguments() != null) {
			j = ctx.classOrInterfaceType().typeArguments().size();
			if (ctx.classOrInterfaceType().typeArguments(j-1) != null) {
			    id += " ...";
			    // System.err.println(ctx.classOrInterfaceType().typeArguments(j-1).getText());
			    for (JavaParser.TypeArgumentContext t: ctx.classOrInterfaceType().typeArguments(j-1).typeArgument())
				if (t.type().primitiveType() == null)
				    if (assoc && ids.contains(id = t.type().classOrInterfaceType().Identifier(i-1).getText()))
					decl += "[" + cl+"]1-*>["+id+"]\n";
			}
		    }
		}
	    	return id;
	    }

        });
        p.compilationUnit();
    }
}
