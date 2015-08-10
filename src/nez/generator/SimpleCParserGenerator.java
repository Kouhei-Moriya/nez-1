package nez.generator;

import java.util.ArrayList;

import nez.NezOption;
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
import nez.lang.Grammar;
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
	private int fid = 0;
	private int memoSize = 0;
	private ArrayList<String> memorizedTerminalList = new ArrayList<String>();
	private int failureOpStackPoint = 0;
	private final ArrayList<Runnable> failureOpStack = new ArrayList<Runnable>();

	@Override
	public String getDesc() {
		return "C Parser Generator";
	}

	@Override
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
	}

	@Override
	public void makeHeader(Grammar g) {
		this.file.write("#include \"libnez/c/libnez.h\"");
		this.file.writeIndent("#include <stdio.h>");
		//this.file.writeIndent("#include <string.h>");
		this.file.writeNewLine();
		for(Production r : g.getProductionList()) {
			if(!r.getLocalName().startsWith("\"")) {
				this.file.writeIndent("int production_" + r.getLocalName() + "(ParsingContext ctx);");
			}
		}
		this.file.writeNewLine();
	}

	@Override
	public void makeFooter(Grammar g) {
		this.startBlock("int main(int argc, char* const argv[]) {");
			/*
			this.file.writeIndent("ParsingContext ctx = (ParsingContext)malloc(sizeof(struct ParsingContext));");
			this.startBlock("if(argv[1] != 0) {");
			this.startLoop("do {", null);
			this.file.writeIndent("size_t len = strlen(argv[1]);");
			this.file.writeIndent("char *source = (char *)malloc(len + 1);");
			this.file.writeIndent("strcpy(source, argv[1]);");
			this.file.writeIndent("ctx->input_size = len;");
			this.file.writeIndent("ctx->inputs = source;");
			this.endLoop("} while(0);");
			this.endAndStartBlock("} else {");
			this.file.writeIndent("ctx->input_size = 0;");
			this.file.writeIndent("ctx->inputs = (char *)malloc(1);");
			this.file.writeIndent("ctx->inputs[0] = '\\0';");
			this.endBlock("}");
			this.file.writeIndent("ctx->pos = 0;");
			this.file.writeIndent("ctx->cur = ctx->inputs;");
			this.file.writeIndent("ctx->choiceCount = 0;");
			 */
		this.file.writeIndent("ParsingContext ctx = nez_CreateParsingContext(argv[1]);");
		this.file.writeIndent("ctx->cur = ctx->inputs;");
		if(this.option.enabledPackratParsing && memoSize > 0) {
			this.file.writeIndent("createMemoTable(ctx, " + memoSize + ");");
		}

		this.startBlock("if(production_File(ctx)) {");
		this.file.writeIndent("nez_PrintErrorInfo(\"parse error\");");
		this.endAndStartBlock("} else if((ctx->cur - ctx->inputs) != ctx->input_size) {");
		this.file.writeIndent("nez_PrintErrorInfo(\"unconsume\");");
		this.endAndStartBlock("} else {");
		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("ParsingObject po = nez_commitLog(ctx,0);");
			this.file.writeIndent("dump_pego(&po, ctx->inputs, 0);");
		}
		else {
			this.file.writeIndent("fprintf(stderr, \"consumed\\n\");");
		}
		this.endBlock("}");

		this.file.writeIndent("return 0;");
		this.endBlock("}");

	}

	private int getMemoId(String name) {
		if(this.memorizedTerminalList.contains(name)) {
			return this.memorizedTerminalList.indexOf(name);
		}
		else {
			this.memorizedTerminalList.add(name);
			return this.memoSize++;
		}
	}

	private void pushOpFailure(Runnable op) {
		this.failureOpStack.add(op);
		++this.failureOpStackPoint;
	}

	private Runnable peekOpFailure() {
		return this.failureOpStack.get(this.failureOpStackPoint-1);
	}

	private Runnable popOpFailure() {
		return this.failureOpStack.remove(--this.failureOpStackPoint);
	}

	private void resetOpFailure(Runnable op) {
		this.popOpFailure();
		this.pushOpFailure(op);
	}

	private void appendOpFailure(Runnable op) {
		if(op != null) {
			Runnable current = this.popOpFailure();
			if(current != null) {
				this.pushOpFailure(() -> {
					op.run();
					current.run();
				});
			}
			else {
				this.pushOpFailure(op);
			}
		}
	}

	private void failure() {
		Runnable op = this.peekOpFailure();
		if(op != null) {
			op.run();
		}
	}

	private void startBlock(String text) {
		this.file.writeIndent(text);
		this.file.incIndent();
	}

	private void endBlock(String text) {
		this.file.decIndent();
		this.file.writeIndent(text);
	}

	private void endAndStartBlock(String text) {
		this.file.decIndent();
		this.file.writeIndent(text);
		this.file.incIndent();
	}

	private void startLoop(String text, Runnable failureOp) {
		this.pushOpFailure(failureOp);
		this.startBlock(text);
	}

	private void endLoop(String text) {
		this.endBlock(text);
		this.popOpFailure();
	}

	@Override
	public void visitEmpty(Expression p) {
	}

	@Override
	public void visitFailure(Expression p) {
		throw new RuntimeException("Failure Expression is not implemented");
	}

	@Override
	public void visitAnyChar(AnyChar p) {
		this.startBlock("if(*ctx->cur == 0) {");
		this.failure();
		this.endBlock("}");
		this.file.writeIndent("ctx->cur++;");
	}

	@Override
	public void visitByteChar(ByteChar p) {
		this.startBlock("if((int)*ctx->cur != " + p.byteChar + ") {");
		this.failure();
		this.endBlock("}");
		this.file.writeIndent("ctx->cur++;");
	}

	@Override
	public void visitByteMap(ByteMap p) {
		StringBuilder cond = new StringBuilder();
		boolean b[] = p.byteMap;
		for(int start = 0; start < 256; ++start) {
			if(b[start]) {
				if(cond.length() > 0) {
					cond.append(" && ");
				}
				int end;
				for(end = start; end < 255; ++end) {
					if(!b[end+1]){
						break;
					}
				}
				if(end - start <= 1) {
					cond.append("(int)*ctx->cur != " + start);
				}
				else {
					cond.append("((int)*ctx->cur < " + start + " || " + end + " < (int)*ctx->cur)");
					start = end;
				}
			}
		}
		this.startBlock("if(" + cond.toString() + ") {");
		this.failure();
		this.endBlock("}");
		this.file.writeIndent("ctx->cur++;");
	}

	@Override
	public void visitOption(Option p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		this.startLoop("do {", () -> this.file.writeIndent("break;"));
		visitExpression(p.get(0));
		this.file.writeIndent("c" + id + " = ctx->cur;");
		this.endLoop("} while(0);");
		this.file.writeIndent("ctx->cur = c" + id + ";");
	}

	@Override
	public void visitRepetition(Repetition p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		this.startLoop("do {", () -> this.file.writeIndent("break;"));
		visitExpression(p.get(0));
		this.file.writeIndent("c" + id + " = ctx->cur;");
		this.endLoop("} while(1);");
		this.file.writeIndent("ctx->cur = c" + id + ";");
	}

	@Override
	public void visitRepetition1(Repetition1 p) {
		visitExpression(p.get(0));
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		this.startLoop("do {", () -> this.file.writeIndent("break;"));
		visitExpression(p.get(0));
		this.file.writeIndent("c" + id + " = ctx->cur;");
		this.endLoop("} while(1);");
		this.file.writeIndent("ctx->cur = c" + id + ";");
	}

	@Override
	public void visitAnd(And p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		visitExpression(p.get(0));
		this.file.writeIndent("ctx->cur = c" + id + ";");
	}

	@Override
	public void visitNot(Not p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");
		this.file.writeIndent("int f" + id + " = 0;");
		this.startLoop("do {", () -> this.file.writeIndent("break;"));
		visitExpression(p.get(0));
		this.file.writeIndent("f" + id + " = 1;");
		this.endLoop("} while(0);");
		this.startBlock("if(f" + id + ") {");
		this.failure();
		this.endAndStartBlock("} else {");
		this.file.writeIndent("ctx->cur = c" + id + ";");
		this.endBlock("}");
	}

	@Override
	public void visitSequence(Sequence p) {
		for(int i = 0; i < p.size(); ++i) {
			visitExpression(p.get(i));
		}
	}

	@Override
	public void visitChoice(Choice p) {
		int id = this.fid++;
		this.file.writeIndent("char *c" + id + " = ctx->cur;");

		this.file.writeIndent("int i" + id + ";");
		this.startLoop("for(i" + id + " = 0; i" + id + " < " + p.size() + "; ++i" + id + ") {", null);
		this.file.writeIndent("ctx->cur = c" + id + ";");

		this.startBlock("switch(i" + id + ") {");
		for(int i = 0; i < p.size(); ++i) {
			this.resetOpFailure(() -> this.file.writeIndent("continue;"));
			this.endAndStartBlock("case " + i + ":");
			this.file.writeIndent(";");
			//this.file.writeIndent("ctx->choiceCount++;");
			visitExpression(p.get(i));
			this.file.writeIndent("break;"); //switch
		}
		this.endBlock("}");

		this.file.writeIndent("break;"); //for
		this.endLoop("}");

		this.startBlock("if(i" + id + " == " + p.size() + ") {");
		this.failure();
		this.endBlock("}");
	}

	@Override
	public void visitNonTerminal(NonTerminal p) {
		this.startBlock("if(production_" + p.getLocalName() + "(ctx)) {");
		this.failure();
		this.endBlock("}");
	}

	@Override
	public void visitCharMultiByte(CharMultiByte p) {
		throw new RuntimeException("CharMultiByte Expression is not implemented");
	}

	@Override
	public void visitLink(Link p) {
		if(this.option.enabledASTConstruction) {
			int id = this.fid++;
			this.file.writeIndent("int mark" + id + " = nez_markLogStack(ctx);");
			if(this.option.enabledPackratParsing && p.get(0) instanceof NonTerminal) {
				NonTerminal n = (NonTerminal)p.get(0);
				int memoId = this.getMemoId(n.getLocalName());

				this.file.writeIndent("memo = nez_getMemo(ctx, ctx->cur, " + memoId + ");");
				this.startBlock("if(memo != NULL) {");
				this.startBlock("if(memo->r) {");
				this.failure();
				this.endAndStartBlock("} else {");
				this.file.writeIndent("nez_pushDataLog(ctx, LazyLink_T, 0, -1, NULL, memo->left);");
				this.file.writeIndent("ctx->cur = memo->consumed;");
				//Succeed
				this.endBlock("}");

				this.endAndStartBlock("} else {");
				this.appendOpFailure(() -> this.file.writeIndent("nez_abortLog(ctx, mark" + id + ");"));
				this.file.writeIndent("char *c" + id + " = ctx->cur;");
				this.startBlock("if(production_" + n.getLocalName() + "(ctx)) {");
				this.file.writeIndent("nez_setMemo(ctx, c" + id + ", " + memoId + ", 1);");
				this.failure();
				this.endBlock("}");
				this.file.writeIndent("ctx->left = nez_commitLog(ctx, mark" + id + ");");
				this.file.writeIndent("nez_pushDataLog(ctx, LazyLink_T, 0, " + p.index + ", NULL, ctx->left);");
				this.file.writeIndent("nez_setMemo(ctx, c" + id + ", " + memoId + ", 0);");

				this.endBlock("}");
			}
			else {
				this.appendOpFailure(() -> this.file.writeIndent("nez_abortLog(ctx, mark" + id + ");"));
				visitExpression(p.get(0));
				this.file.writeIndent("ctx->left = nez_commitLog(ctx, mark" + id + ");");
				this.file.writeIndent("nez_pushDataLog(ctx, LazyLink_T, 0, " + p.index + ", NULL, ctx->left);");
			}
		}
		else {
			visitExpression(p.get(0));
		}
	}

	@Override
	public void visitNew(New p) {
		if(this.option.enabledASTConstruction) {
			int id = this.fid++;
			this.appendOpFailure(() -> this.file.writeIndent("nez_abortLog(ctx, mark" + id + ");"));
			this.file.writeIndent("int mark" + id + " = nez_markLogStack(ctx);");
			if(p.lefted) {
				this.file.writeIndent("nez_pushDataLog(ctx, LazyLeftJoin_T, ctx->cur - ctx->inputs, -1, NULL, NULL);");
			}
			else {
				this.file.writeIndent("nez_pushDataLog(ctx, LazyNew_T, ctx->cur - ctx->inputs, -1, NULL, NULL);");
			}
		}
	}

	@Override
	public void visitCapture(Capture p) {
		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("nez_pushDataLog(ctx, LazyCapture_T, ctx->cur - ctx->inputs, 0, NULL, NULL);");
		}
	}

	@Override
	public void visitTagging(Tagging p) {
		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("nez_pushDataLog(ctx, LazyTag_T, 0, 0, \"" + p.tag.getName() + "\", NULL);");
		}
	}

	@Override
	public void visitReplace(Replace p) {
		if(this.option.enabledASTConstruction) {
			this.file.writeIndent("nez_pushDataLog(ctx, LazyValue_T, 0, 0, \"" + p.value + "\", NULL);");
		}
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
		this.fid = 0;
		if(this.option.enabledPackratParsing && r.isNoNTreeConstruction()) {
			int id = this.fid++;
			int memoId = this.getMemoId(r.getLocalName());
			this.pushOpFailure(() -> {
				this.file.writeIndent("nez_setMemo(ctx, c" + id + ", " + memoId + ", 1);");
				this.file.writeIndent("return 1;");
			});

			this.startBlock("int production_" + r.getLocalName() + "(ParsingContext ctx) {");

			this.file.writeIndent("MemoEntry memo = nez_getMemo(ctx, ctx->cur, " + memoId + ");");
			this.startBlock("if(memo != NULL) {");
			this.startBlock("if(memo->r) {");
			this.file.writeIndent("return 1;"); //Failed
			this.endAndStartBlock("} else {");
			this.file.writeIndent("ctx->cur = memo->consumed;");
			this.file.writeIndent("return 0;"); //Succeed
			this.endBlock("}");
			this.endBlock("}");

			this.file.writeIndent("char *c" + id + " = ctx->cur;");
			this.visitExpression(r.getExpression());
			this.file.writeIndent("nez_setMemo(ctx, c" + id + ", " + memoId + ", 0);");
			this.file.writeIndent("return 0;");
			this.endBlock("}");

			this.popOpFailure();
		}
		else {
			this.pushOpFailure(() -> this.file.writeIndent("return 1;"));
			this.startBlock("int production_" + r.getLocalName() + "(ParsingContext ctx) {");
			if(this.option.enabledPackratParsing) {
				this.file.writeIndent("MemoEntry memo = NULL;");
			}
			this.visitExpression(r.getExpression());
			this.file.writeIndent("return 0;");
			this.endBlock("}");
			this.popOpFailure();
		}
		this.file.writeNewLine();
	}

}
