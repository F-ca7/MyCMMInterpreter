package semantics;

/**
 * @description 中间代码的指令常量
 *              具体指令格式参见文档
 * @author FANG
 * @date 2019/10/27 16:31
 **/
public class CodeConstant {
    // 条件为假时跳转
    public static final String JMP_WITH_CONDITION = "jne";
    // 无条件跳转
    public static final String JMP = "jmp";
    // 命令行输出
    public static final String PRINT = "print";
    // 命令行输入
    public static final String SCAN = "scan";
    // 进入语句块
    public static final String IN = "in";
    // 退出语句块
    public static final String OUT = "out";

    // 参数入栈
    public static final String ARG = "arg";
    // 调用函数
    public static final String CALL = "call";
    // 函数返回
    public static final String RETURN = "ret";

    public static final String INT = "int";
    public static final String REAL = "real";
    public static final String INT_ARR = "int[]";
    public static final String REAL_ARR = "real[]";
    public static final String CHAR = "char";
    public static final String VOID = "void";

    public static final String ASSIGN = "assign";
    public static final String PLUS = "+";
    public static final String MINUS = "-";
    public static final String MUL = "*";
    public static final String DIV = "/";
    public static final String LE = "<";
    public static final String EQ = "==";
    public static final String NEQ = "<>";
    public static final String GR = ">";
    public static final String LE_EQ = "<=";
    public static final String GR_EQ = ">=";
    // 数组访问
    public static final String ARR_ACC = "arr_acc";

    // 进入语句块代码
    public static final Quadruple inCode = new Quadruple(CodeConstant.IN);
    // 退出语句块代码
    public static final Quadruple outCode = new Quadruple(CodeConstant.OUT);;

    // 参数名前缀
    public static final String ARG_PREFIX = "%arg";
    // 返回值
    public static final String RETURN_VALUE = "%rax";
    // TRUE
    public static final String TRUE = "true";
    // FALSE
    public  static final String FALSE = "false";
    // 缺省操作
    public static final String DEFAULT = "default";
}
