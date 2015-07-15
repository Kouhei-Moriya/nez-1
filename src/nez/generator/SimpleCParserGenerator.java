package nez.generator;

import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.CharMultiByte;
import nez.lang.Choice;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;

public class SimpleCParserGenerator extends ParserGenerator {

	@Override
	public String getDesc() {
		return "C Parser Generator";
	}

	/* @Override
	public void generate(Grammar grammar, NezOption option, String fileName) {
		this.setOption(option);
		this.setOutputFile(fileName);
		makeHeader(grammar);
		for(Production p : grammar.getProductionList()) {
			visitProduction(p);
		}
		makeFooter(grammar);
		file.writeNewLine();
		file.flush();
	} */

	/* @Override
	public void makeHeader(Grammar g) {
	} */

	/* @Override
	public void makeFooter(Grammar g) {
	} */

	@Override
	public void visitEmpty(Expression p) {
		throw new RuntimeException("Empty Expression is not implemented");
	}

	@Override
	public void visitFailure(Expression p) {
		throw new RuntimeException("Failure Expression is not implemented");
	}

	@Override
	public void visitAnyChar(AnyChar p) {
		throw new RuntimeException("AnyChar Expression is not implemented");
	}

	@Override
	public void visitByteChar(ByteChar p) {
		throw new RuntimeException("ByteChar Expression is not implemented");
	}

	@Override
	public void visitByteMap(ByteMap p) {
		throw new RuntimeException("ByteMap Expression is not implemented");
	}

	@Override
	public void visitOption(Option p) {
		throw new RuntimeException("Option Expression is not implemented");
	}

	@Override
	public void visitRepetition(Repetition p) {
		throw new RuntimeException("Repetition Expression is not implemented");
	}

	@Override
	public void visitRepetition1(Repetition1 p) {
		throw new RuntimeException("Repetition1 Expression is not implemented");
	}

	@Override
	public void visitAnd(And p) {
		throw new RuntimeException("And Expression is not implemented");
	}

	@Override
	public void visitNot(Not p) {
		throw new RuntimeException("Not Expression is not implemented");
	}

	@Override
	public void visitSequence(Sequence p) {
		throw new RuntimeException("Sequence Expression is not implemented");
	}

	@Override
	public void visitChoice(Choice p) {
		throw new RuntimeException("Choice Expression is not implemented");
	}

	@Override
	public void visitNonTerminal(NonTerminal p) {
		throw new RuntimeException("NonTerminal Expression is not implemented");
	}

	@Override
	public void visitCharMultiByte(CharMultiByte p) {
		throw new RuntimeException("CharMultiByte Expression is not implemented");
	}

	@Override
	public void visitLink(Link p) {
		throw new RuntimeException("Link Expression is not implemented");
	}

	@Override
	public void visitNew(New p) {
		throw new RuntimeException("New Expression is not implemented");
	}

	@Override
	public void visitCapture(Capture p) {
		throw new RuntimeException("Capture Expression is not implemented");
	}

	@Override
	public void visitTagging(Tagging p) {
		throw new RuntimeException("Tagging Expression is not implemented");
	}

	@Override
	public void visitReplace(Replace p) {
		throw new RuntimeException("Replace Expression is not implemented");
	}

	@Override
	public void visitBlock(Block p) {
		throw new RuntimeException("Block Expression is not implemented");
	}

	@Override
	public void visitDefSymbol(DefSymbol p) {
		throw new RuntimeException("DefSymbol Expression is not implemented");
	}

	@Override
	public void visitIsSymbol(IsSymbol p) {
		throw new RuntimeException("IsSymbol Expression is not implemented");
	}

	@Override
	public void visitDefIndent(DefIndent p) {
		throw new RuntimeException("DefIndent Expression is not implemented");
	}

	@Override
	public void visitIsIndent(IsIndent p) {
		throw new RuntimeException("IsIndent Expression is not implemented");
	}

	@Override
	public void visitExistsSymbol(ExistsSymbol p) {
		throw new RuntimeException("ExistsSymbol Expression is not implemented");
	}

	@Override
	public void visitLocalTable(LocalTable p) {
		throw new RuntimeException("LocalTable Expression is not implemented");
	}

	@Override
	public void visitProduction(Production r) {
		throw new RuntimeException("Production Expression is not implemented");
	}

}
