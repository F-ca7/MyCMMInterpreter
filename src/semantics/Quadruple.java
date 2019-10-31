package semantics;

/**
 * @description 四元式表示
 *              用于中间代码
 * @author FANG
 * @date 2019/10/26 18:47
 **/
public class Quadruple {
    // 操作类型
    String operation;

    // 第一个操作数
    Operand firstOperand;
    // 第二个操作数
    Operand secondOperand;

    // 第一个操作数类型
    OperandType firstOperandType = OperandType.NULL;
    // 第二个操作数类型
    OperandType secondOperandType = OperandType.NULL;

    // 目标位置的标识符
    String dest;

    // 跳转指令的语句位置
    int jumpLocation;


    Quadruple(){
        firstOperand = new Operand();
        secondOperand = new Operand();
    }
    Quadruple(String codeConstant) {
        operation = codeConstant;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(operation);
        appendOpToBuilder(builder, firstOperandType, firstOperand);
        appendOpToBuilder(builder, secondOperandType, secondOperand);
        builder.append(",");
        if(operation.equals(CodeConstant.JMP) ||
                operation.equals(CodeConstant.JMP_WITH_CONDITION)) {
            builder.append(jumpLocation);
        } else {
            builder.append(dest);
        }
        return builder.toString();

    }

    private void appendOpToBuilder(StringBuilder builder, OperandType operandType, Operand operand) {
        builder.append(",");
        switch (operandType) {
            case INT_LITERAL:
                builder.append(((IntOperand)operand).intLiteral);
                break;
            case REAL_LITERAL:
                builder.append(((RealOperand)operand).realLiteral);
                break;
            case IDENTIFIER:
                builder.append(operand.name);
                break;
            case NULL:
                builder.append("null");
                break;
        }
    }

}
