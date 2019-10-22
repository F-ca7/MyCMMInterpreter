package lex;

/**
 * @author FANG
 * @description Token类型枚举类
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
    L_BRACKET,          // (
    R_BRACKET,          // )
    L_ANGLE_BRACKET,    // {
    R_ANGLE_BRACKET,    // }
    L_SQUARE_BRACKET,   // [
    R_SQUARE_BRACKET,   // ]

    // 字面量
    INT_LITERAL,    // 整形
    REAL_LITERAL,   // 实数
    IDENTIFIER,     // 标识符

    // 注释
    SINGLE_LINE_COMMENT,    // 单行注释
    MULTIPLE_LINE_COMMENT,  // 多行注释

    // 其他
    EOF,            // 文件尾
}

