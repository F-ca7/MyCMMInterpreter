package semantics;

/**
 * @description 整数操作数
 * @author FANG
 * @date 2019/10/28 9:37
 **/
public class IntOperand extends Operand {
    // 操作数的整形字面量
   public int intLiteral;

    public IntOperand(int intLiteral) {
        this.intLiteral = intLiteral;
    }
}
