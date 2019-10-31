package semantics;

/**
 * @description 实数操作数
 * @author FANG
 * @date 2019/10/28 9:38
 **/
public class RealOperand extends Operand {
    // 操作数的实数字面量
    public double realLiteral;

    public RealOperand(double realLiteral) {
        this.realLiteral = realLiteral;
    }
}
