package gram;

/**
 * @description 语法树结点类型
 * @author FANG
 * @date 2019/10/23 16:43
 **/
public enum TreeNodeType {
    /**
     * 若变量声明同时赋值则
     * left存储变量名, right存储赋值表达式
     */
    // 整型变量声明
    INT_DECLARATION,
    // 实数变量声明,同上
    REAL_DECLARATION,
    // 字符型变量声明,同上
    CHAR_DECLARATION,
    // 空类型
    VOID,

    /**
     * 函数
     * left存储函数签名, right存储函数实现语句块
     *
     */
    FUNCTION,
    /**
     * 函数签名
     * left存储参数类型列表, right存储返回值
     */
    FUNC_SIGN,
    /**
     * 参数类型列表
     */
    ARGS,
    /**
     * 返回语句
     * left存储返回值/标识符
     */
    RETURN,

    /**
     * 函数调用
     * left存储参数, right存储函数实现语句块
     */
    FUNC_CALL,

    /**
     * left存储数组标识符, right存储数组大小
     */
    // 整型数组声明,
    INT_ARRAY_DECLARATION,
    // 实数数组声明
    REAL_ARRAY_DECLARATION,

    /**
     * 访问数组, left存储数组标识符, right存储索引
     */
    ARRAY_ACCESS,

    /**
     * 变量赋值, left存储变量名, right存储赋值表达式
     */
    ASSIGN,
    /**
     * 命令行输出，输入
     */
    PRINT,
    SCAN,

    /**
     * 算数运算, left存储第一个操作数, right存储第二个操作数
     */
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,

    /**
     * 关系运算, left存储第一个操作数, right存储第二个操作数
     */
    LESS,
    EQUAL,
    NOT_EQUAL,
    GREATER,
    LESS_EQ,
    GREATER_EQ,

    /**
     * left存储条件为真的语句块
     * 如果有else语句块, 存储在right
     * 所有的else if存储在statementBlock里
     */
    IF,
    ELSE_IF,


    /**
     * left存储while语句块
     */
    WHILE,
    /**
     * 块内容存储在statementBlock里
     */
    STATEMENT_BLOCK,

    /**
     * 控制
     */
    BREAK,
    CONTINUE,

    // 整型字面量
    INT_LITERAL,
    // 实数字面量
    REAL_LITERAL,
    //标识符
    IDENTIFIER,

    // 空语句
    EMPTY,
    NULL,
}