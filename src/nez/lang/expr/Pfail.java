package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.ExpressionTransducer;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Pfail extends Term {
	Pfail(SourcePosition s) {
		super(s);
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		return (o instanceof Pfail);
	}

	@Override
	public final void format(StringBuilder sb) {
		sb.append("!''");
	}

	@Override
	public Expression reshape(ExpressionTransducer m) {
		return m.reshapePfailure(this);
	}

	@Override
	public boolean isConsumed() {
		return true;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Reject;
	}

	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodePfail(this);
	}

}