package nez.parser.generator;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import nez.Strategy;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.parser.GenerativeGrammar;
import nez.parser.ParserGenerator;
import nez.util.StringUtils;

public class CParserGenerator extends ParserGenerator {

	// GrammarOptimizer optimizer = null;
	int predictionCount = 0;
	private boolean enableOpt = false;

	private CParserGenerator N() {
		this.file.writeIndent();
		return this;
	}

	@Override
	protected String getFileExtension() {
		return "c";
	}

	@Override
	public void makeHeader(GenerativeGrammar grammar) {
		final String __FILE__ = new Throwable().getStackTrace()[1].getFileName();
		L("// This file is auto generated by nez.jar");
		L("// If you want to fix something, you must edit " + __FILE__);
		N();
		L("#include \"cnez.h\"");
		for (Production r : grammar.getProductionList()) {
			if (!r.getLocalName().startsWith("\"")) {
				L("int p" + r.getLocalName() + "(ParsingContext ctx);");
			}
		}
		N();
	}

	@Override
	public void makeFooter(GenerativeGrammar grammar) {
		// String urn =
		// grammar.getProductionList().get(0).getGrammarFile().getURN();
		int flagTableSize = this.flagTable.size();
		int prodSize = grammar.getProductionList().size();
		L("#define CNEZ_FLAG_TABLE_SIZE " + flagTableSize);
		L("#define CNEZ_MEMO_SIZE       " + this.memoId);
		// L("#define CNEZ_GRAMMAR_URN \"" + urn + "\"");
		L("#define CNEZ_PRODUCTION_SIZE " + prodSize);
		if (this.strategyASTConstruction) {
			L("#define CNEZ_ENABLE_AST_CONSTRUCTION 1");
		}
		L("#include \"cnez_main.c\"");
	}

	int fid = 0;

	class FailurePoint {
		int id;
		FailurePoint prev;

		public FailurePoint(int label, FailurePoint prev) {
			this.id = label;
			this.prev = prev;
		}
	}

	FailurePoint fLabel;

	private void initFalureJumpPoint() {
		this.fid = 0;
		this.fLabel = null;
	}

	private void pushFailureJumpPoint() {
		this.fLabel = new FailurePoint(this.fid++, this.fLabel);
	}

	private void popFailureJumpPoint(Production r) {
		Label("CATCH_FAILURE" + this.fLabel.id);
		this.fLabel = this.fLabel.prev;
	}

	private void popFailureJumpPoint(Expression e) {
		Label("CATCH_FAILURE" + this.fLabel.id);
		this.fLabel = this.fLabel.prev;
	}

	private void jumpFailureJump() {
		L("goto CATCH_FAILURE" + this.fLabel.id + ";");
	}

	private void jumpPrevFailureJump() {
		L("goto CATCH_FAILURE" + this.fLabel.prev.id + ";");
	}

	private void gotoLabel(String label) {
		L("goto " + label + ";");
	}

	private void Label(String label) {
		Begin("");
		L(label + ": ;");
		End("");
	}

	private void let(String type, String var, String expr) {
		String space = " ";
		if (type.endsWith("*")) {
			space = "";
		}
		L(type + space + var + " = " + expr + ";");
	}

	private void assign(String var, String expr) {
		L(var + " = " + expr + ";");
	}

	private void memoize(Production rule, int id, String pos) {
		String node = "ast_get_last_linked_node(ctx->ast)";
		L("memo_set(ctx->memo, " + pos + ", " + id + ", " + node + ", ctx->cur - " + pos + ", 0);");
	}

	private void memoizeFail(Production rule, int id, String pos) {
		L("memo_fail(ctx->memo, " + pos + ", " + id + ");");
	}

	private void lookup(Production rule, int id) {
		L("MemoEntry_t *entry = memo_get(ctx->memo, ctx->cur, " + id + ", 0);");
		L("if(entry != NULL)");
		Begin("{");
		{
			L("if(entry->failed == MEMO_ENTRY_FAILED)");
			Begin("{");
			{
				L("return 1;");
			}
			End("}");
			L("else ");
			Begin("{");
			{
				if (this.strategyASTConstruction) {
					String tag = rule.getLocalName();
					tag = StringUtils.quoteString('"', tag, '"');
					L("ast_log_link(ctx->ast, " + tag + ", entry->result);");
				}
				L("ctx->cur += entry->consumed;");
				L("return 0;");
			}
			End("}");
		}
		End("}");
	}

	private void consume() {
		L("ctx->cur++;");
	}

	private void choiceCount() {
		// L("ctx->choiceCount++;");
	}

	int memoId = 0;

	private Expression getNonTerminalRule(Expression e) {
		while (e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	public int specializeString(Psequence e, int start) {
		int count = 0;
		for (int i = start; i < e.size(); i++) {
			Expression inner = e.get(i);
			if (inner instanceof Cbyte) {
				count++;
			}
		}
		if (count > 1) {
			// for(int i = start; )
		}
		return 0;
	}

	public boolean checkByteMap(Pchoice e) {
		for (int i = 0; i < e.size(); i++) {
			Expression inner = e.get(i);
			if (!(inner instanceof Cbyte || inner instanceof Cset)) {
				return false;
			}
		}
		return true;
	}

	private boolean checkString(Psequence e) {
		for (int i = 0; i < e.size(); i++) {
			if (!(e.get(i) instanceof Cbyte)) {
				return false;
			}
		}
		return true;
	}

	public void specializeByteMap(Pchoice e) {
		if (!this.enableOpt) {
			return;
		}
		int fid = this.fid++;
		constructBmap(e, fid);
		L("if(!bmap" + fid + "[(uint8_t)*ctx->cur])");
		Begin("{");
		this.jumpFailureJump();
		End("}");
		L("ctx->cur++;");
	}

	public void specializeNotByteMap(Pchoice e) {
		if (!this.enableOpt) {
			return;
		}

		int fid = this.fid++;
		constructBmap(e, fid);
		L("if(bmap" + fid + "[(uint8_t)*ctx->cur])");
		Begin("{");
		this.jumpFailureJump();
		End("}");
	}

	private boolean[] constructBmap(Pchoice e) {
		boolean[] map = new boolean[256];
		for (int i = 0; i < e.size(); i++) {
			Expression inner = e.get(i);
			if (inner instanceof Cbyte) {
				map[((Cbyte) inner).byteChar] = true;
			} else if (inner instanceof Cset) {
				boolean[] bmap = ((Cset) inner).byteMap;
				for (int j = 0; j < bmap.length; j++) {
					if (bmap[j]) {
						map[j] = true;
					}
				}
			}
		}
		return map;
	}

	private void constructBmap(Pchoice e, int fid) {
		boolean[] map = constructBmap(e);
		constructBmap(map, fid);
	}

	private void constructBmap(boolean[] map, int fid) {
		L("unsigned long bmap" + fid + "[] = {");
		for (int i = 0; i < map.length - 1; i++) {
			if (map[i]) {
				W("1, ");
			} else {
				W("0, ");
			}
		}
		if (map[map.length - 1]) {
			W("1");
		} else {
			W("0");
		}
		W("};");
	}

	public void specializeNotString(Psequence e) {
		if (!this.enableOpt) {
			return;
		}

		for (int i = 0; i < e.size(); i++) {
			Expression inner = e.get(i);
			if (inner instanceof Cbyte) {
				Cbyte b = (Cbyte) inner;
				L("if((int)*(ctx->cur + " + i + ") == " + b.byteChar + ")");
				Begin("{");
			}
		}
		this.jumpFailureJump();
		for (int i = 0; i < e.size(); i++) {
			Expression inner = e.get(i);
			if (inner instanceof Cbyte) {
				End("}");
			}
		}
	}

	public boolean specializeNot(Pnot e) {
		if (!this.enableOpt) {
			return false;
		}

		Expression inner = e.get(0);
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof Cbyte) {
			L("if((int)*ctx->cur == " + ((Cbyte) inner).byteChar + ")");
			Begin("{");
			this.jumpFailureJump();
			End("}");
			return true;
		}
		if (inner instanceof Cset) {
			int fid = this.fid++;
			boolean[] map = ((Cset) inner).byteMap;
			constructBmap(map, fid);
			L("if(bmap" + fid + "[(uint8_t)*ctx->cur])");
			Begin("{");
			this.jumpFailureJump();
			End("}");
			return true;
		}
		if (inner instanceof Pchoice) {
			if (checkByteMap((Pchoice) inner)) {
				specializeNotByteMap((Pchoice) inner);
				return true;
			}
		}
		if (inner instanceof Psequence) {
			if (checkString((Psequence) inner)) {
				L("// Specialize not string");
				specializeNotString((Psequence) inner);
				return true;
			}
		}
		return false;
	}

	public void specializeOptionByteMap(Pchoice e) {
		if (!this.enableOpt) {
			return;
		}

		int fid = this.fid++;
		constructBmap(e, fid);
		L("if(bmap" + fid + "[(uint8_t)*ctx->cur])");
		Begin("{");
		L("ctx->cur++;");
		End("}");
	}

	public void specializeOptionString(Psequence e) {
		if (!this.enableOpt) {
			return;
		}

		int fid = ++this.fid;
		String label = "EXIT_OPTION" + fid;
		String backtrack = "c" + fid;
		this.let("char *", backtrack, "ctx->cur");
		for (int i = 0; i < e.size(); i++) {
			Expression inner = e.get(i);
			if (inner instanceof Cbyte) {
				L("if((int)*(ctx->cur++) == " + ((Cbyte) inner).byteChar + ")");
				Begin("{");
			}
		}
		this.gotoLabel(label);
		for (int i = 0; i < e.size(); i++) {
			Expression inner = e.get(i);
			if (inner instanceof Cbyte) {
				End("}");
			}
		}
		this.assign("ctx->cur", backtrack);
		Label(label);
	}

	public boolean specializeOption(Poption e) {
		if (!this.enableOpt) {
			return false;
		}

		Expression inner = e.get(0);
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof Cbyte) {
			L("if((int)*ctx->cur == " + ((Cbyte) inner).byteChar + ")");
			Begin("{");
			L("ctx->cur++;");
			End("}");
			return true;
		}
		if (inner instanceof Cset) {
			int fid = this.fid++;
			boolean[] map = ((Cset) inner).byteMap;
			constructBmap(map, fid);
			L("if(bmap" + fid + "[(uint8_t)*ctx->cur])");
			Begin("{");
			L("ctx->cur++;");
			End("}");
			return true;
		}
		if (inner instanceof Pchoice) {
			if (checkByteMap((Pchoice) inner)) {
				specializeOptionByteMap((Pchoice) inner);
				return true;
			}
		}
		if (inner instanceof Psequence) {
			if (checkString((Psequence) inner)) {
				L("// specialize option string");
				specializeOptionString((Psequence) inner);
				return true;
			}
		}
		return false;
	}

	public void specializeZeroMoreByteMap(Pchoice e) {
		if (!this.enableOpt) {
			return;
		}

		boolean[] b = constructBmap(e);
		constructByteMapRep(b);
	}

	public boolean specializeRepetition(Pzero e) {
		if (!this.enableOpt) {
			return false;
		}

		Expression inner = e.get(0);
		if (inner instanceof NonTerminal) {
			inner = getNonTerminalRule(inner);
		}
		if (inner instanceof Cbyte) {
			L("while(1)");
			Begin("{");
			L("if((int)*ctx->cur != " + ((Cbyte) inner).byteChar + ")");
			Begin("{");
			L("break;");
			End("}");
			L("ctx->cur++;");
			End("}");
			return true;
		}
		if (inner instanceof Cset) {
			boolean[] b = ((Cset) inner).byteMap;
			constructByteMapRep(b);
			return true;
		}
		if (inner instanceof Pchoice) {
			if (checkByteMap((Pchoice) inner)) {
				L("// specialize repeat choice");
				specializeZeroMoreByteMap((Pchoice) inner);
				return true;
			}
		}
		return false;
	}

	private void constructByteMapRep(boolean[] b) {
		L("while(1)");
		Begin("{");
		for (int start = 0; start < 256; start++) {
			if (b[start]) {
				int end = searchEndChar(b, start + 1);
				if (start == end) {
					L("if((int)*ctx->cur == " + start + ")");
					Begin("{");
					this.consume();
					L("continue;");
					End("}");
				} else {
					L("if(" + start + "<= (int)*ctx->cur" + " && (int)*ctx->cur <= " + end + ")");
					Begin("{");
					this.consume();
					L("continue;");
					End("}");
					start = end;
				}
			}
		}
		L("break;");
		End("}");
	}

	@Override
	public void visitProduction(GenerativeGrammar gg, Production rule) {
		this.initFalureJumpPoint();
		L("int p" + name(rule.getLocalName()) + "(ParsingContext ctx)");
		Begin("{");
		this.pushFailureJumpPoint();
		if (this.enabledPackratParsing) {
			lookup(rule, this.memoId);
		}
		String pos = "c" + this.fid;
		this.let("char *", pos, "ctx->cur");
		Expression e = rule.getExpression();
		visitExpression(e);
		if (this.enabledPackratParsing) {
			memoize(rule, this.memoId, pos);
		}
		L("return 0;");
		this.popFailureJumpPoint(rule);
		if (this.enabledPackratParsing) {
			memoizeFail(rule, this.memoId, pos);
		}
		L("return 1;");
		End("}");
		N();
		if (this.enabledPackratParsing) {
			this.memoId++;
		}
	}

	@Override
	public void visitPempty(Expression e) {
	}

	@Override
	public void visitPfail(Expression e) {
		this.jumpFailureJump();
	}

	boolean inlining = true;
	int dephth = 0;

	@Override
	public void visitNonTerminal(NonTerminal e) {
		// if(!e.getProduction().isRecursive() && dephth < 3 && inlining) {
		// Expression ne = this.getNonTerminalRule(e);
		// dephth++;
		// visit(ne);
		// dephth--;
		// return;
		// }
		L("if(p" + e.getLocalName() + "(ctx))");
		Begin("{");
		this.jumpFailureJump();
		End("}");
	}

	public String stringfyByte(int byteChar) {
		char c = (char) byteChar;
		switch (c) {
		case '\n':
			return ("'\\n'");
		case '\t':
			return ("'\\t'");
		case '\r':
			return ("'\\r'");
		case '\'':
			return ("\'\\\'\'");
		case '\\':
			return ("'\\\\'");
		}
		return "\'" + c + "\'";
	}

	@Override
	public void visitCbyte(Cbyte e) {
		L("if((int)*ctx->cur != " + e.byteChar + ")");
		Begin("{");
		this.jumpFailureJump();
		End("}");
		this.consume();
	}

	private int searchEndChar(boolean[] b, int s) {
		for (; s < 256; s++) {
			if (!b[s]) {
				return s - 1;
			}
		}
		return 255;
	}

	@Override
	public void visitCset(Cset e) {
		int fid = this.fid++;
		String label = "EXIT_BYTEMAP" + fid;
		boolean b[] = e.byteMap;
		for (int start = 0; start < 256; start++) {
			if (b[start]) {
				int end = searchEndChar(b, start + 1);
				if (start == end) {
					L("if((int)*ctx->cur == " + start + ")");
					Begin("{");
					this.consume();
					this.gotoLabel(label);
					End("}");
				} else {
					L("if(" + start + "<= (int)*ctx->cur" + " && (int)*ctx->cur <= " + end + ")");
					Begin("{");
					this.consume();
					this.gotoLabel(label);
					End("}");
					start = end;
				}
			}
		}
		this.jumpFailureJump();
		Label(label);
	}

	@Override
	public void visitCany(Cany e) {
		L("if(*ctx->cur == 0)");
		Begin("{");
		this.jumpFailureJump();
		End("}");
		this.consume();
	}

	@Override
	public void visitCmulti(Cmulti p) {
		int len = p.byteSeq.length;
		L("if (TAIL(ctx) - ctx->cur >= " + len + ")");
		Begin("{");
		try {
			String str = new String(p.byteSeq, StringUtils.DefaultEncoding);
			str = StringUtils.quoteString('"', str, '"');
			L("if (strncmp(ctx->cur, " + str + ", " + len + ") != 0)");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		Begin("{");
		this.jumpFailureJump();
		End("}");
		L("ctx->cur += " + len + ";");
		End("}");
	}

	@Override
	public void visitPoption(Poption e) {
		if (!specializeOption(e)) {
			this.pushFailureJumpPoint();
			String label = "EXIT_OPTION" + this.fid;
			String backtrack = "c" + this.fid;
			this.let("char *", backtrack, "ctx->cur");
			visitExpression(e.get(0));
			this.gotoLabel(label);
			this.popFailureJumpPoint(e);
			this.assign("ctx->cur", backtrack);
			Label(label);
		}
	}

	@Override
	public void visitPzero(Pzero e) {
		if (!specializeRepetition(e)) {
			this.pushFailureJumpPoint();
			String backtrack = "c" + this.fid;
			this.let("char *", backtrack, "ctx->cur");
			L("while(1)");
			Begin("{");
			visitExpression(e.get(0));
			this.assign(backtrack, "ctx->cur");
			End("}");
			this.popFailureJumpPoint(e);
			this.assign("ctx->cur", backtrack);
		}
	}

	@Override
	public void visitPone(Pone e) {
		visitExpression(e.get(0));
		this.pushFailureJumpPoint();
		String backtrack = "c" + this.fid;
		this.let("char *", backtrack, "ctx->cur");
		L("while(1)");
		Begin("{");
		visitExpression(e.get(0));
		this.assign(backtrack, "ctx->cur");
		End("}");
		this.popFailureJumpPoint(e);
		this.assign("ctx->cur", backtrack);
	}

	@Override
	public void visitPand(Pand e) {
		this.pushFailureJumpPoint();
		String label = "EXIT_AND" + this.fid;
		String backtrack = "c" + this.fid;
		this.let("char *", backtrack, "ctx->cur");
		visitExpression(e.get(0));
		this.assign("ctx->cur", backtrack);
		this.gotoLabel(label);
		this.popFailureJumpPoint(e);
		this.assign("ctx->cur", backtrack);
		this.jumpFailureJump();
		Label(label);
	}

	@Override
	public void visitPnot(Pnot e) {
		if (!specializeNot(e)) {
			this.pushFailureJumpPoint();
			String backtrack = "c" + this.fid;
			this.let("char *", backtrack, "ctx->cur");
			visitExpression(e.get(0));
			this.assign("ctx->cur", backtrack);
			this.jumpPrevFailureJump();
			this.popFailureJumpPoint(e);
			this.assign("ctx->cur", backtrack);
		}
	}

	@Override
	public void visitPsequence(Psequence e) {
		for (int i = 0; i < e.size(); i++) {
			visitExpression(e.get(i));
		}
	}

	boolean isPrediction = true;
	int justPredictionCount = 0;

	public String formatId(int id) {
		String idStr = Integer.toString(id);
		int len = idStr.length();
		StringBuilder sb = new StringBuilder();
		while (len < 9) {
			sb.append("0");
			len++;
		}
		sb.append(idStr);
		return sb.toString();
	}

	private void showChoiceInfo(Pchoice e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.toString() + ",").append(e.size() + ",");
		if (e.predictedCase != null) {
			int notNullSize = 0;
			int notChoiceSize = 0;
			int containsEmpty = 0;
			int subChoiceSize = 0;
			for (int i = 0; i < e.predictedCase.length; i++) {
				if (e.predictedCase[i] != null) {
					notNullSize++;
					if (e.predictedCase[i] instanceof Pchoice) {
						subChoiceSize += e.predictedCase[i].size();
					} else {
						notChoiceSize++;
						if (e.predictedCase[i].isEmpty()) {
							containsEmpty = 1;
						}
					}
				}
			}
			double evaluationValue = (double) (notChoiceSize + subChoiceSize) / (double) notNullSize;
			NumberFormat format = NumberFormat.getInstance();
			format.setMaximumFractionDigits(3);
			sb.append(notNullSize + ",");
			sb.append(notChoiceSize + ",");
			sb.append(containsEmpty + ",");
			sb.append(subChoiceSize + ",");
			sb.append(format.format(evaluationValue));
		}
		System.out.println(sb.toString());
	}

	@Override
	public void visitPchoice(Pchoice e) {
		// showChoiceInfo(e);
		if ((e.predictedCase != null && this.isPrediction && this.strategy.isEnabled("Ofirst", Strategy.Ofirst))) {
			this.predictionCount++;
			this.justPredictionCount++;
			int fid = this.fid++;
			String label = "EXIT_CHOICE" + fid;
			HashMap<Integer, Expression> m = new HashMap<Integer, Expression>();
			ArrayList<Expression> l = new ArrayList<Expression>();
			L("void* jump_table" + formatId(fid) + "[] = {");
			for (int ch = 0; ch < e.predictedCase.length; ch++) {
				Expression pCase = e.predictedCase[ch];
				if (pCase != null) {
					Expression me = m.get(pCase.getId());
					if (me == null) {
						m.put(pCase.getId(), pCase);
						l.add(pCase);
					}
					W("&&PREDICATE_JUMP" + formatId(fid) + "" + pCase.getId());
				} else {
					W("&&PREDICATE_JUMP" + formatId(fid) + "" + 0);
				}
				if (ch < e.predictedCase.length - 1) {
					W(", ");
				}
			}
			W("};");
			L("goto *jump_table" + formatId(fid) + "[(uint8_t)*ctx->cur];");
			for (int i = 0; i < l.size(); i++) {
				Expression pe = l.get(i);
				Label("PREDICATE_JUMP" + formatId(fid) + "" + pe.getId());
				if (!(pe instanceof Pchoice)) {
					this.choiceCount();
				} else {
					this.isPrediction = false;
				}
				visitExpression(pe);
				this.isPrediction = true;
				this.gotoLabel(label);
			}
			Label("PREDICATE_JUMP" + formatId(fid) + "" + 0);
			this.jumpFailureJump();
			Label(label);
			this.justPredictionCount--;
		} else {
			this.fid++;
			String label = "EXIT_CHOICE" + this.fid;
			String backtrack = "c" + this.fid;
			this.let("char *", backtrack, "ctx->cur");
			for (int i = 0; i < e.size(); i++) {
				this.pushFailureJumpPoint();
				this.choiceCount();
				visitExpression(e.get(i));
				this.gotoLabel(label);
				this.popFailureJumpPoint(e.get(i));
				this.assign("ctx->cur", backtrack);
			}
			this.jumpFailureJump();
			Label(label);
		}
	}

	Stack<String> markStack = new Stack<String>();

	@Override
	public void visitTnew(Tnew e) {
		if (this.strategyASTConstruction) {
			// this.pushFailureJumpPoint();
			String mark = "mark" + this.fid++;
			// this.markStack.push(mark);
			L("int " + mark + " = ast_save_tx(ctx->ast);");
			L("ast_log_new(ctx->ast, ctx->cur);");
		}
	}

	@Override
	public void visitTcapture(Tcapture e) {
		if (this.strategyASTConstruction) {
			String label = "EXIT_CAPTURE" + this.fid++;
			L("ast_log_capture(ctx->ast, ctx->cur);");
			this.gotoLabel(label);
			// this.popFailureJumpPoint(e);
			// L("ast_rollback_tx(ctx->ast, " + this.markStack.pop() + ");");
			// this.jumpFailureJump();
			Label(label);
		}
	}

	@Override
	public void visitTtag(Ttag e) {
		if (this.strategyASTConstruction) {
			L("ast_log_tag(ctx->ast, \"" + e.tag.getSymbol() + "\");");
		}
	}

	@Override
	public void visitTreplace(Treplace e) {
		if (this.strategyASTConstruction) {
			L("ast_log_replace(ctx->ast, \"" + e.value + "\");");
		}
	}

	@Override
	public void visitTlink(Tlink e) {
		this.pushFailureJumpPoint();
		String mark = "mark" + this.fid;

		if (this.strategyASTConstruction) {
			L("int " + mark + " = ast_save_tx(ctx->ast);");
		}
		visitExpression(e.get(0));
		if (this.strategyASTConstruction) {
			String po = "ctx->left";
			String label = "EXIT_LINK" + this.fid;
			Symbol sym = e.getLabel();
			if (sym == null) {
				sym = Symbol.NullSymbol;
			}
			String tag = sym.getSymbol();
			L("ast_commit_tx(ctx->ast, \"" + tag + "\", " + mark + ");");
			this.assign(po, "ast_get_last_linked_node(ctx->ast)");
			this.gotoLabel(label);
			this.popFailureJumpPoint(e);
			L("ast_rollback_tx(ctx->ast, " + mark + ");");
			this.jumpFailureJump();
			Label(label);
		}
	}

	ArrayList<String> flagTable = new ArrayList<String>();

	public void visitIfFlag(Xif e) {
		if (!this.flagTable.contains(e.getFlagName())) {
			this.flagTable.add(e.getFlagName());
		}
		String isPred = e.isPredicate() ? "!" : "";
		L("if(" + isPred + "ctx->flags[" + this.flagTable.indexOf(e.getFlagName()) + "])");
		Begin("{");
		this.jumpFailureJump();
		End("}");
	}

	public void visitOnFlag(Xon p) {
		if (!this.flagTable.contains(p.getFlagName())) {
			this.flagTable.add(p.getFlagName());
		}
		visitExpression(p.get(0));
		String isPositive = p.isPositive() ? "1" : "0";
		L("ctx->flags[" + this.flagTable.indexOf(p.getFlagName()) + "] = " + isPositive + ";");
	}

	@Override
	public void visitXblock(Xblock p) {
		String mark = "mark" + this.fid;
		L("int " + mark + " = symtable_savepoint(ctx->table);");
		visitExpression(p.get(0));
		L("symtable_rollback(ctx->table, " + mark + ";");
	}

	@Override
	public void visitXdef(Xdef p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXis(Xis p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXmatch(Xmatch p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXdefindent(Xdefindent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXindent(Xindent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXexists(Xexists p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXlocal(Xlocal p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTdetree(Tdetree p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXif(Xif p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXon(Xon p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTlfold(Tlfold p) {
		// TODO Auto-generated method stub

	}

}
