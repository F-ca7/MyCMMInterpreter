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

    // 第一个操作数类型
    OperandType firstOperandType = OperandType.NULL;
    // 第二个操作数类型
    OperandType secondOperandType = OperandType.NULL;
    // 第一个操作数的标识符
    String firstOperandName;
    // 第二个操作数的标识符
    String secondOperandName;
    // 目标的标识符
    String dest;
    // 第一个操作数的整型字面量
    int firstOperandIntLiteral;
    // 第一个操作数的实数字面量
    double firstOperandRealLiteral;
    // 第二个操作数的整型字面量
    int secondOperandIntLiteral;
    // 第二个操作数的实数字面量
    double secondOperandRealLiteral;
    // 跳转指令的语句位置
    int jumpLocation;


    Quadruple(){}
    Quadruple(String codeConstant) {
        operation = codeConstant;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(operation);
        appendOpToBuilder(builder, firstOperandType, firstOperandIntLiteral, firstOperandRealLiteral, firstOperandName);
        appendOpToBuilder(builder, secondOperandType, secondOperandIntLiteral, secondOperandRealLiteral, secondOperandName);
        builder.append(",");
        if(operation.equals(CodeConstant.JMP) ||
                operation.equals(CodeConstant.JMP_WITH_CONDITION)) {
            builder.append(jumpLocation);
        } else {
            builder.append(dest);
        }
        return builder.toString();

    }

    private void appendOpToBuilder(StringBuilder builder, OperandType operandType, int operandIntLiteral, double operandRealLiteral, String operandName) {
        builder.append(",");
        switch (operandType) {
            case INT_LITERAL:
                builder.append(operandIntLiteral);
                break;
            case REAL_LITERAL:
                builder.append(operandRealLiteral);
                break;
            case IDENTIFIER:
                builder.append(operandName);
                break;
            case NULL:
                builder.append("null");
                break;
        }
    }

}
