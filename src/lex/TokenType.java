package lex;

/**
 * @description Token类型枚举类
 * @author FANG
 * @date 2019/10/22 15:35
 **/
public enum TokenType {
    // 保留字
    IF,
    ELSE,
    WHILE,
    READ,
    WRITE,
    INT,
    REAL,
    FOR,

    // 算术运算符
    PLUS,       // +
    MINUS,      // -
    MULTIPLY,   // *
    DIVIDE,     // /
    ASSIGN,     // =

    // 关系运算符
    LESS,       // <
    EQUAL,      // ==
    NOT_EQUAL,  // <>


    // 分隔符
    SEMICOLON,          // ;

    // 括号
    L_PARENTHESIS,      // (
    R_PARENTHESIS,      // )
    L_BRACKET,          // [
    R_BRACKET,          // ]
    L_BRACE,            // {
    R_BRACE,            // }

    // 字面量
    INT_LITERAL,    // 整形
    REAL_LITERAL,   // 实数
    IDENTIFIER,     // 标识符

    // 注释
    SINGLE_LINE_COMMENT,    // 单行注释
    MULTIPLE_LINE_COMMENT,  // 多行注释

    // 其他
    EOF,            // 文件尾
    UNKNOWN,        // 未知
}

