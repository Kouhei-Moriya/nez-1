package nez.x.generator;

import java.util.ArrayList;
import java.util.HashMap;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.Typestate;
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
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.lang.expr.Xsymbol;
import nez.parser.GrammarWriter;
import nez.parser.ParserGrammar;
import nez.util.StringUtils;

public class JavaParserGenerator extends GrammarWriter {

	@Override
	protected String getFileExtension() {
		return "java";
	}

	@Override
	public void makeHeader(ParserGrammar gg) {
		W("/* The following is generated by the Nez Grammar Generator */");
		L("class P").Begin("{");
	}

	@Override
	public void makeFooter(ParserGrammar gg) {
		End("}");
	}

	String _func(Production rule) {
		return rule.getLocalName();
	}

	String _func(Expression e) {
		return unique(e);
	}

	String _ctx() {
		return "c";
	}

	String _call(Production p) {
		return _func(p) + "(" + _ctx() + ")";
	}

	String _call(Expression e) {
		ensureFunc(e);
		return unique(e) + "(" + _ctx() + ")";
	}

	String _not(String expr) {
		return "!(" + expr + ")";
	}

	String _true() {
		return "true";
	}

	String _false() {
		return "false";
	}

	String _left() {
		return "left";
	}

	String _log() {
		return "log";
	}

	String _cref(String n) {
		return _ctx() + "." + n + "()";
	}

	String _ccall(String n) {
		return _ctx() + "." + n + "()";
	}

	String _ccall(String n, String a) {
		return _ctx() + "." + n + "(" + a + ")";
	}

	String _ccall(String n, String a, String a2) {
		return _ctx() + "." + n + "(" + a + "," + a2 + ")";
	}

	String _cleft() {
		return _cref("left");
	}

	String _clog() {
		return _cref("log");
	}

	JavaParserGenerator VarNode(String n, String left) {
		L("Object " + n + " = " + left).Semi();
		return this;
	}

	JavaParserGenerator Commit(String log, String left) {
		Return(_ctx() + ".commit(" + log + ", " + left + ")");
		return this;
	}

	JavaParserGenerator Abort(String log, String left) {
		Return(_ctx() + ".abort(" + log + ", " + left + ")");
		return this;
	}

	String _match(int c) {
		return _ccall("byte", "" + c);
	}

	String _match(boolean[] b) {
		return _ccall("byte", "" + b);
	}

	String _match() {
		return _ccall("any");
	}

	String _result() {
		return "result";
	}

	String _pos() {
		return "pos";
	}

	String _cpos() {
		return _cref("pos");
	}

	String _eq(String v, String v2) {
		return "(" + v + " == " + v2 + ")";
	}

	protected JavaParserGenerator Save(Expression e) {
		VarInt(_pos(), _cpos());
		if (e.inferTypestate() != Typestate.Unit) {
			VarInt(_log(), _clog());
		}
		return this;
	}

	protected JavaParserGenerator Rollback(Expression e) {
		if (e.inferTypestate() != Typestate.Unit) {
			Statement(_ccall("abort", _log()));
		}
		Statement(_ccall("setpos", _pos()));
		return this;
	}

	@Override
	protected JavaParserGenerator W(String word) {
		file.write(word);
		return this;
	}

	@Override
	protected JavaParserGenerator L() {
		file.writeIndent();
		return this;
	}

	@Override
	protected JavaParserGenerator inc() {
		file.incIndent();
		return this;
	}

	@Override
	protected JavaParserGenerator dec() {
		file.decIndent();
		return this;
	}

	@Override
	protected JavaParserGenerator L(String line) {
		file.writeIndent(line);
		return this;
	}

	protected JavaParserGenerator DefPublicFunc(String name) {
		L("public static boolean ").W(name).W(" (NezContext ").W(_ctx()).W(") ");
		return this;
	}

	protected JavaParserGenerator DefFunc(String name) {
		L("private static boolean ").W(name).W(" (NezContext ").W(_ctx()).W(") ");
		return this;
	}

	protected JavaParserGenerator FuncName(Expression e) {
		W(unique(e));
		return this;
	}

	protected JavaParserGenerator IfThen(String c) {
		L("if (").W(c).W(") ");
		return this;
	}

	protected JavaParserGenerator IfNotThen(String c) {
		L("if (").W(_not(c)).W(") ");
		return this;
	}

	protected JavaParserGenerator Else() {
		L("else");
		return this;
	}

	protected JavaParserGenerator While(String c) {
		L("while (").W(c).W(") ");
		return this;
	}

	protected JavaParserGenerator Continue() {
		L("continue").Semi();
		return this;
	}

	protected JavaParserGenerator Break() {
		L("break").Semi();
		return this;
	}

	protected JavaParserGenerator VarInt(String n, String v) {
		L("int ").W(n).W(" = ").W(v).Semi();
		return this;
	}

	protected JavaParserGenerator VarBool(String n, String v) {
		L("boolean ").W(n).W(" = ").W(v).Semi();
		return this;
	}

	protected JavaParserGenerator Assign(String n, String v) {
		L(n).W(" = ").W(v).Semi();
		return this;
	}

	protected JavaParserGenerator Semi() {
		W(";");
		return this;
	}

	protected JavaParserGenerator Statement(String expr) {
		L(expr).Semi();
		return this;
	}

	protected JavaParserGenerator Return(String v) {
		L("return ").W(v).Semi();
		return this;
	}

	protected JavaParserGenerator Comment(Object s) {
		W("/* ").W(s.toString()).W(" */");
		return this;
	}

	protected JavaParserGenerator LComment(Object s) {
		L("// ").W(s.toString());
		return this;
	}

	public void writeLinkLogic(Tlink e) {
		VarNode(_left(), _cleft());
		VarInt(_log(), _clog());
		IfThen(_call(e.get(0))).Begin("{");
		Commit(_log(), _left()).End("}");
		Abort(_log(), _left());
	}

	HashMap<String, Object> funcMap = new HashMap<String, Object>();

	private void ensureFunc(Expression e) {
		String key = _func(e);
		if (!funcMap.containsKey(key)) {
			funcMap.put(key, e);
		}
	}

	private void makeFunc() {
		ArrayList<Expression> l = new ArrayList<Expression>(funcMap.size());
		for (String key : funcMap.keySet()) {
			Object o = funcMap.get(key);
			if (o instanceof Expression) {
				l.add((Expression) o);
				funcMap.put(key, key);
			}
		}
		for (Expression e : l) {
			writeFunc(e);
		}
	}

	private void writeFunc(Expression e) {
		DefFunc(_func(e));
		Begin("{");
		Comment(e);
		if (e instanceof Pchoice) {
			writeChoiceLogic(e);
		} else if (e instanceof Tlink) {
			writeLinkLogic((Tlink) e);
		} else if (e instanceof Poption) {
			writeOptionLogic(e);
		} else if (e instanceof Pzero || e instanceof Pone) {
			writeRepetitionLogic(e);
		} else if (e instanceof Pnot || e instanceof Pand) {
			writePredicateLogic(e);
		} else {
			visitExpression(e);
			Return(_true());
		}
		End("}");
		makeFunc();
	}

	private void writeOptionLogic(Expression e) {
		Save(e.get(0));
		IfThen(_call(e.get(0))).Begin("{");
		Return(_true());
		End("}");
		Rollback(e.get(0));
		Return(_true());
	}

	private void writeRepetitionLogic(Expression e) {
		While(_true()).Begin("{");
		{
			Save(e.get(0));
			ensureFunc(e.get(0));
			IfThen(_call(e.get(0))).Begin("{");
			{
				IfThen(_eq(_pos(), _cpos())).Begin("{");
				{
					Break();
				}
				End("}");
				Continue();
			}
			End("}");
			Rollback(e.get(0));
			Break();
		}
		End("}");
		Return(_true());
	}

	private void writeChoiceLogic(Expression e) {
		Save(e);
		for (Expression s : e) {
			IfThen(_call(s)).Begin("{");
			{
				Return(_true());
			}
			End("}");
			Rollback(e);
		}
		Return(_false());
	}

	private void writePredicateLogic(Expression e) {
		Save(e.get(0));
		VarBool(_result(), _call(e.get(0)));
		Rollback(e.get(0));
		Return(_result());
	}

	@Override
	public void visitProduction(ParserGrammar gg, Production rule) {
		DefPublicFunc(_func(rule));
		Begin("{");
		visitExpression(rule.getExpression());
		Return(_true());
		End("}");
		makeFunc();
	}

	@Override
	public void visitPempty(Expression e) {

	}

	@Override
	public void visitPfail(Expression e) {
		Return(_false());
	}

	@Override
	public void visitNonTerminal(NonTerminal e) {
		IfThen(_not(_call(e.getProduction()))).Begin("{");
		{
			Return(_false());
		}
		End("}");
	}

	@Override
	public void visitCbyte(Cbyte e) {
		IfNotThen(_match(e.byteChar)).Begin("{");
		{
			Return(_false());
		}
		End("}");
	}

	@Override
	public void visitCset(Cset e) {
		IfNotThen(_match(e.byteMap)).Begin("{");
		{
			Return(_false());
		}
		End("}");
	}

	@Override
	public void visitCany(Cany e) {
		IfNotThen(_match()).Begin("{");
		{
			Return(_false());
		}
		End("}");
	}

	@Override
	public void visitCmulti(Cmulti p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPoption(Poption e) {
		Statement(_call(e));
	}

	@Override
	public void visitPzero(Pzero e) {
		Statement(_call(e));
	}

	@Override
	public void visitPone(Pone e) {
		visitExpression(e.get(0));
		Statement(_call(e));
	}

	@Override
	public void visitPand(Pand e) {
		IfNotThen(_call(e)).Begin("{");
		{
			Return(_false());
		}
		End("}");
	}

	@Override
	public void visitPnot(Pnot e) {
		IfThen(_call(e)).Begin("{");
		{
			Return(_false());
		}
		End("}");
	}

	@Override
	public void visitPsequence(Psequence e) {
		for (Expression s : e) {
			visitExpression(s);
		}
	}

	@Override
	public void visitPchoice(Pchoice e) {
		IfNotThen(_call(e)).Begin("{");
		{
			Return(_false());
		}
		End("}");
	}

	@Override
	public void visitTnew(Tnew e) {
		Statement(_ccall("new"));
	}

	@Override
	public void visitTcapture(Tcapture e) {
		Statement(_ccall("capture"));
	}

	protected String _tag(Symbol tag) {
		return StringUtils.quoteString('"', tag.getSymbol(), '"');
	}

	@Override
	public void visitTtag(Ttag e) {
		Statement(_ccall("tag", _tag(e.tag)));
	}

	@Override
	public void visitTreplace(Treplace e) {
		Statement(_ccall("replace", StringUtils.quoteString('"', e.value, '"')));
	}

	@Override
	public void visitTlink(Tlink e) {
		IfNotThen(_call(e)).Begin("{");
		{
			Return(_false());
		}
		End("}");
	}

	@Override
	public void visitUndefined(Expression e) {
		LComment("undefined " + e);
	}

	@Override
	public void visitXblock(Xblock p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXdef(Xsymbol p) {
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
