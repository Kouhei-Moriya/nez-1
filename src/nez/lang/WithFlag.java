package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.UList;
import nez.util.UMap;

public class WithFlag extends Unary {
	String flagName;
	WithFlag(SourcePosition s, String flagName, Expression inner) {
		super(s, inner);
		this.flagName = flagName;
		this.optimized = inner.optimized;
	}
	@Override
	public String getPredicate() {
		return "with " + this.flagName;
	}
	@Override
	Expression dupUnary(Expression e) {
		return (this.inner != e) ? Factory.newWithFlag(this.s, this.flagName, e) : this;
	}
	@Override
	public boolean checkAlwaysConsumed(GrammarChecker checker, String startNonTerminal, UList<String> stack) {
		return inner.checkAlwaysConsumed(checker, startNonTerminal, stack);
	}
	@Override
	public int inferTypestate(UMap<String> visited) {
		return this.inner.inferTypestate(visited);
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		this.inner = this.inner.checkTypestate(checker, c);
		return this;
	}
	@Override
	public Expression removeFlag(TreeMap<String,String> undefedFlags) {
		boolean removeWithout = false;
		if(undefedFlags != null && undefedFlags.containsKey(flagName)) {
			undefedFlags.remove(flagName);
			removeWithout = true;
		}
		Expression e = inner.removeFlag(undefedFlags);
		if(removeWithout) {
			undefedFlags.put(flagName, flagName);
		}
		return e;
	}
	@Override
	public short acceptByte(int ch, int option) {
		return this.inner.acceptByte(ch, option);
	}
	
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return this.inner.encode(bc, next);
	}

	@Override
	protected int pattern(GEP gep) {
		return inner.pattern(gep);
	}

	@Override
	protected void examplfy(GEP gep, StringBuilder sb, int p) {
		this.inner.examplfy(gep, sb, p);
	}

	
}