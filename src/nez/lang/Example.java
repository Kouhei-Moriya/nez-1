package nez.lang;

import nez.NezOption;
import nez.SourceContext;
import nez.ast.AbstractTree;
import nez.main.Verbose;

public class Example {
	AbstractTree<?> nameNode;
	AbstractTree<?> textNode;
	boolean result;

	Example(AbstractTree<?> nameNode, AbstractTree<?> textNode, boolean result) {
		this.nameNode = nameNode;
		this.textNode = textNode;
		this.result = result;
	}

	boolean test(GrammarFile grammar, NezOption option) {
		Parser g = grammar.newGrammar(nameNode.toText(), option);
		if (g == null) {
			System.out.println(nameNode.formatSourceMessage("error", "undefined nonterminal"));
			return false;
		}
		SourceContext source = textNode.newSourceContext();
		String name = (this.result ? "" : "!") + nameNode.toText() + " (" + textNode.getSource().getResourceName() + ":" + textNode.getSource().linenum(textNode.getSourcePosition()) + ")";
		boolean matchingResult = g.match(source);
		boolean unConsumed = true;
		if (matchingResult) {
			while (source.hasUnconsumed()) {
				int ch = source.byteAt(source.getPosition());
				if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
					source.consume(1);
					continue;
				}
				break;
			}
			unConsumed = source.hasUnconsumed();
		}
		if (result) {
			if (!matchingResult) {
				Verbose.println("[FAIL] " + name);
				Verbose.println(source.getSyntaxErrorMessage());
				return false;
			}
			if (unConsumed) {
				Verbose.println("[FAIL] " + name);
				Verbose.println(source.getUnconsumedMessage());
				return false;
			}
			if (Verbose.Example) {
				Verbose.println("[PASS] " + name);
			}
			return true;
		} else {
			if (!matchingResult || unConsumed) {
				if (Verbose.Example) {
					Verbose.println("[PASS] " + name);
				}
				return true;
			}
			Verbose.println("[FAIL] " + name);
			return false;
		}
	}
}
