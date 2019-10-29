package semantics;

/**
 * @description 中间代码的指令常量
 *              具体指令格式参见文档
 * @author FANG
 * @date 2019/10/27 16:31
 **/
class CodeConstant {
    // 条件为假时跳转
    static final String JMP_WITH_CONDITION = "jne";
    // 无条件跳转
    static final String JMP = "jmp";
    // 命令行输出
    static final String PRINT = "print";
    // 进入语句块
    static final String IN = "in";
    // 退出语句块
    static final String OUT = "out";

    static final String INT = "int";
    static final String REAL = "real";
    static final String INT_ARR = "int[]";
    static final String REAL_ARR = "real[]";
    static final String ASSIGN = "assign";

    static final String PLUS = "+";
    static final String MINUS = "-";
    static final String MUL = "*";
    static final String DIV = "/";
    static final String LE = "<";
    static final String EQ = "==";
    static final String NEQ = "<>";
    // 数组访问
    static final String ARR_ACC = "arr";

    // 入口代码
    static final Quadruple inCode = new Quadruple(CodeConstant.IN);
    // 出口代码
    static final Quadruple outCode = new Quadruple(CodeConstant.OUT);;

}
