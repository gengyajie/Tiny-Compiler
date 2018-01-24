import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.CharStreams;

import java.util.*;

public class Micro {
	public static void main(String[] args) throws Exception {
		CharStream input = CharStreams.fromFileName(args[0]);
		MicroLexer lexer = new MicroLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		MicroParser parser = new MicroParser(tokens);
		ANTLRErrorStrategy es = new CustomErrorStrategy();
		parser.setErrorHandler(es);
		ParseTree tree = null;

		try {
			tree = parser.program();
		} catch (Exception e) {
			System.out.println (";Not Accepted");
			System.exit(1);
		}

		SemanticListener L = new SemanticListener();
		IterativeParseTreeWalker.DEFAULT.walk(L, tree);
		ArrayList<IRCode> allIRcodes = L.getIR().generateAllIRCode(0);
		L.getIR().idLoop(allIRcodes);

		System.out.println(";IR code");
		for (IRCode c : allIRcodes) {
			System.out.print(";");
			c.printIRcode();
			System.out.println();
		}

		loopOpt lpopt = new loopOpt(allIRcodes, L.getIR().loopMap, L.getIR().symtab);
		lpopt.init();
		lpopt.defAnalysis();
		allIRcodes = lpopt.codeMotion();

		System.out.println(";Opted IR code");
		for (IRCode c : allIRcodes) {
			System.out.print(";");
			c.printIRcode();
			System.out.println();
		}

		regAlloc reg = new regAlloc(allIRcodes);
		reg.init(L.getIR().symtab);

		ArrayList<tinyCode> tiny;
		tinyGenerator tinyGen = new tinyGenerator(allIRcodes, L.getIR().symtab);

		tiny = tinyGen.generateTiny();
		System.out.println(";tiny code");
		for (tinyCode t : tiny) {
			t.printtinycode();
		}


		/*
			ArrayList<IRCode> allIRcodes = L.getIR().generateAllIRCode(0);
			L.getIR().idLoop(allIRcodes);

		*//*Iterator it = L.getIR().loopMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			System.out.println(pair.getKey() + " = " + pair.getValue());
			it.remove(); // avoids a ConcurrentModificationException
		}*//*

			System.out.println(";IR code");
			for (IRCode c : allIRcodes) {
				System.out.print(";");
				c.printIRcode();
				System.out.println();
			}

			loopOpt lpopt = new loopOpt(allIRcodes, L.getIR().loopMap, L.getIR().symtab);
			lpopt.init();
			lpopt.defAnalysis();
			allIRcodes = lpopt.codeMotion();

			System.out.println(";Opted IR code");
			for (IRCode c : allIRcodes) {
				System.out.print(";");
				c.printIRcode();
				System.out.println();
			}

			regAlloc reg = new regAlloc(allIRcodes);
			reg.init(L.getIR().symtab);

			ArrayList<tinyCode> tiny;
			tinyGenerator tinyGen = new tinyGenerator(allIRcodes, L.getIR().symtab);

			tiny = tinyGen.generateTiny();
			System.out.println(";tiny code");
			for (tinyCode t : tiny) {
				t.printtinycode();
			}*/
	}
}
