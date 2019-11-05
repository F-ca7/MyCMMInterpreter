package lex;

import exception.LexException;

import javax.xml.bind.PrintConversionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

/**
 * @description 词法分析器
 * @author FANG
 * @date 2019/10/22 15:30
 **/
public class Lexer {
    // 文件尾常量
    private static final char END_OF_TEXT = '\u0003';
    // 变量名长度限制
    private static final int VAR_NAME_LIMIT = 64;
    // 字面量构造缓冲区
    private StringBuilder stringBuilder = new StringBuilder();
    // 当前字符
    private char curCh;
    // 可直接识别的Token表, 不需要后看
    private static final Map<Character,Integer> DIRECT_RECOGNIZED = new HashMap<>();
    // 保留字表
    private static final HashMap<String,Integer> RESERVED_WORDS = new HashMap<>();
    // 词法单元类型集合
    private static TokenType[] values = TokenType.values();
    // 当前行号
    private static int lineNum = 1;
    // 源文件路径
    private String srcFilePath;
    // 源代码
    private String srcCode;
    // 错误信息
    private StringBuffer errInfoBuffer = new StringBuffer();
    // 是否成功
    private boolean ifSuccess = true;
    // token流
    public List<Token> tokenList = new ArrayList<>();
    // 指向正在读取字符的位置的指针
    private int pointer = 0;
    // 静态初始化保留字表
    static {
        DIRECT_RECOGNIZED.put('+', TokenType.PLUS.ordinal());
        DIRECT_RECOGNIZED.put('-', TokenType.MINUS.ordinal());
        DIRECT_RECOGNIZED.put('*', TokenType.MULTIPLY.ordinal());
        DIRECT_RECOGNIZED.put(';', TokenType.SEMICOLON.ordinal());
        DIRECT_RECOGNIZED.put('(', TokenType.L_PARENTHESIS.ordinal());
        DIRECT_RECOGNIZED.put(')', TokenType.R_PARENTHESIS.ordinal());
        DIRECT_RECOGNIZED.put('{', TokenType.L_BRACE.ordinal());
        DIRECT_RECOGNIZED.put('}', TokenType.R_BRACE.ordinal());
        DIRECT_RECOGNIZED.put('[', TokenType.L_BRACKET.ordinal());
        DIRECT_RECOGNIZED.put(']', TokenType.R_BRACKET.ordinal());
        DIRECT_RECOGNIZED.put(',', TokenType.COMMA.ordinal());

        RESERVED_WORDS.put("if", TokenType.IF.ordinal());
        RESERVED_WORDS.put("else", TokenType.ELSE.ordinal());
        RESERVED_WORDS.put("while", TokenType.WHILE.ordinal());
        RESERVED_WORDS.put("for", TokenType.FOR.ordinal());
        RESERVED_WORDS.put("print", TokenType.PRINT.ordinal());
        RESERVED_WORDS.put("scan", TokenType.SCAN.ordinal());
        RESERVED_WORDS.put("int", TokenType.INT.ordinal());
        RESERVED_WORDS.put("real", TokenType.REAL.ordinal());
        RESERVED_WORDS.put("char", TokenType.CHAR.ordinal());
        RESERVED_WORDS.put("continue", TokenType.CONTINUE.ordinal());
        RESERVED_WORDS.put("break", TokenType.BREAK.ordinal());
        RESERVED_WORDS.put("void", TokenType.VOID.ordinal());
        RESERVED_WORDS.put("func", TokenType.FUNC.ordinal());
        RESERVED_WORDS.put("return", TokenType.RETURN.ordinal());
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("Y:\\desktop\\MyCMMInterpreter\\test_func_call1.cmm");
        lexer.loadSourceCode();
        Token token = new Token();
        do {
            try {
                token = lexer.getNextToken();
                if (token.getType()==TokenType.SINGLE_LINE_COMMENT
                        ||token.getType()==TokenType.MULTIPLE_LINE_COMMENT){
                    continue;
                }
                System.out.println(token);
            }catch (LexException e) {
                lexer.errInfoBuffer.append("词法分析错误！").append(e.getMessage()).append('\n');
                lexer.ifSuccess = false;
            }

        } while (token.getType() != TokenType.EOF);
        System.out.println("词法分析结束");

        if (!lexer.ifSuccess) {
            // 报告错误
            System.out.println(lexer.errInfoBuffer.toString());
        }else {
            System.out.println("无错误发生！");
        }

    }


    public Lexer(String path) {
        this.srcFilePath = path;
    }

    /**
     * 进行词法分析获取token列表
     */
    public void loadTokenList() {
        Token token = new Token();
        do {
            try {
                token = getNextToken();
                if (token.getType()==TokenType.SINGLE_LINE_COMMENT
                        ||token.getType()==TokenType.MULTIPLE_LINE_COMMENT){
                    continue;
                }
                tokenList.add(token);
            }catch (LexException e) {
                errInfoBuffer.append("词法分析错误！").append(e.getMessage()).append('\n');
                ifSuccess = false;
            }

        } while (token.getType() != TokenType.EOF);
        System.out.println("词法分析结束");
        if (!ifSuccess) {
            // 报告错误
            System.out.println(errInfoBuffer.toString());
        }else {
            System.out.println("无词法错误发生！");
        }
    }


    /**
     * 加载源代码
     */
    public void loadSourceCode() {
        File file = new File(srcFilePath);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder builder = new StringBuilder();
            while (bufferedReader.ready()) {
                builder.append(bufferedReader.readLine());
                builder.append('\n');
            }
            // 把源代码当做字符串直接保存
            srcCode = builder.toString();
            bufferedReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * 返回下一个token
     */
    public Token getNextToken() throws LexException{
        Token token = new Token();
        token.setLineNum(lineNum);
        token.setType(TokenType.UNKNOWN);
        readCharSkip();
        if(curCh == END_OF_TEXT) {
            token.setLineNum(lineNum);
            token.setType(TokenType.EOF);
            return token;
        }
        // 是否可以直接识别
        if(DIRECT_RECOGNIZED.containsKey(curCh)) {
            token.setType(values[DIRECT_RECOGNIZED.get(curCh)]);
        } else if(curCh == '/') {
            readCharSkip();
            parseSplash(token);
        } else if(curCh == '=') {
            // 判断赋值还是相等
            readChar();
            if(curCh == '=') {
                token.setType(TokenType.EQUAL);
            } else {
                token.setType(TokenType.ASSIGN);
                pointer--;
            }
        } else if(curCh == '<') {
            // 判断小于还是小于等于还是不等于
            readChar();
            if(curCh =='>') {
                token.setType(TokenType.NOT_EQUAL);
            } else if(curCh =='=') {
                token.setType(TokenType.LESS_EQ);
            } else {
                token.setType(TokenType.LESS);
                pointer--;
            }
        } else if(curCh == ';') {
            token.setType(TokenType.SEMICOLON);
        } else if(curCh == '>') {
            // 判断大于还是大于等于
            readChar();
            if (curCh == '=') {
                token.setType(TokenType.GREATER_EQ);
            } else {
                token.setType(TokenType.GREATER);
                pointer--;
            }

        } else if (curCh == '\'') {
            // 单引号中间只能包含一个字符
            readChar();
            // 将其视为整形常量
            token.setType(TokenType.INT_LITERAL);
            token.setIntValue(curCh);
            readChar();
            if (curCh != '\'') {
                throw new LexException("Illegal character at line " + token.getLineNum());
            }
        } else {
            if(Character.isDigit(curCh) || curCh=='.') {
                // 数字常量
                parseNum(token);
            } else if(Character.isLetter(curCh) || curCh=='_') {
                // 字母开头
                // 说明接下来是一个标识符或者关键字
                parseLetter(token);
            }
        }
        // 中途可能会换行，如注释
        token.setLineNum(lineNum);
        if (token.getType()== TokenType.UNKNOWN){
            // 无法识别该token
            throw new LexException("Unknown token at line " + token.getLineNum());
        }
        return token;
    }

    /**
     * 分析 字母开头
     * 判断是标识符 还是 关键字
     * @param token 待解析类型的token
     */
    private void parseLetter(Token token) throws LexException {
        // 是否出现了字母
        boolean hasLetter = false;
        while (true) {
            if(curCh == '_') {
                stringBuilder.append(curCh);
                readChar();
            } else if(Character.isLetter(curCh)) {
                hasLetter = true;
                stringBuilder.append(curCh);
                readChar();
            }
            else if(Character.isDigit(curCh)) {
                // 没有字母的时候不能有数字
                if(!hasLetter) {
                    illegalIdException(token);
                } else{
                    stringBuilder.append(curCh);
                    readChar();
                }
            }
            else {
                pointer--;
                break;
            }
        }

        if (stringBuilder.charAt(stringBuilder.length()-1) == '_' ) {
            // 标识符不能以下划线结尾
            token.setLineNum(lineNum);
            throw new LexException("IDENTIFIER cannot end with _ at line "+ token.getLineNum());
        }

        String value = stringBuilder.toString();
        stringBuilder.delete(0, stringBuilder.length());
        if(RESERVED_WORDS.containsKey(value)) {
            // 是关键字
            token.setType(values[RESERVED_WORDS.get(value)]);
        } else {
            if (value.length() > VAR_NAME_LIMIT) {
                // 变量名过长
                varNameTooLongException(token);
            }
            // 往后看一个判断是不是函数调用
            readChar();
            if (curCh == '(') {
                // 后面紧跟左括号, 是函数调用
                token.setType(TokenType.FUNC_CALL);
                token.setStringValue(value);
            } else {
                token.setType(TokenType.IDENTIFIER);
                token.setStringValue(value);
            }
            pointer--;
        }
    }

    /**
     * 分析 数字常量
     * 判断是int 还是 real
     * @param token 待解析类型的token
     */
    private void parseNum(Token token) throws LexException {
        boolean isReal = false; // 实数
        boolean isHex = false;  // 16进制
        boolean isExp = false;  // 指数形式
        while (true) {
            if(Character.isDigit(curCh) || curCh =='.') {
                if (curCh == '.' && isReal) {
                    // 不能有多个小数点
                    illegalNumException(token);
                }
                stringBuilder.append(curCh);
                if (curCh == '.') {
                    if (isReal || isHex) {
                        // 反复出现小数点
                        // 或已经是十六进制

                        illegalNumException(token);
                    } else {
                        // 有小数点说明是实数
                        isReal = true;
                        readChar();
                        if (!Character.isDigit(curCh)) {
                            // 小数点后不是数字
                            illegalNumException(token);
                        }
                        // 回退指针
                        pointer--;
                    }
                }
                readChar();
            } else if(Character.isLetter(curCh)) {
                if (!isHex && stringBuilder.toString().equals("0") && curCh=='x') {
                    // 是16进制表示
                    isHex = true;
                    // 清除 0x前缀
                    stringBuilder.delete(0, stringBuilder.length());
                    readChar();
                } else if(!isExp && !isHex && !isReal && curCh=='e') {
                    isExp = true;
                    isReal = true;
                    stringBuilder.append(curCh);
                    readChar();
                    if(curCh!='+' && curCh!='-') {
                        illegalIdException(token);
                    } else {
                        stringBuilder.append(curCh);
                    }
                    readChar();
                } else {
                    // 数字后面不能跟字母作为标识符
                    illegalIdException(token);
                }

            } else if(curCh=='_'){
                // 忽略下划线
                readChar();
            } else {
                pointer--;
                break;
            }
        }
        // 先把缓冲区转为字符串，并清空缓冲区
        String strVal = stringBuilder.toString();
        stringBuilder.delete(0, stringBuilder.length());
        // 再转为对应数值
        if(isReal) {
            token.setType(TokenType.REAL_LITERAL);
            if(isExp) {
                token.setRealValue(expStrToInt(strVal));
            }else {
                token.setRealValue(Double.parseDouble(strVal));
            }
        } else {
            token.setType(TokenType.INT_LITERAL);
            if (isHex) {
                token.setIntValue(Integer.parseInt(strVal, 16));
            } else {
                token.setIntValue(Integer.parseInt(strVal));
            }
        }
    }

    /**
     * 分析 以斜杠/ 开头
     * 判断是多行注释, 单行注释, 还是除号
     * @param token 待解析类型的token
     */
    private void parseSplash(Token token) {
        if(curCh == '*') {
            // 多行注释
            while (true) {
                readCharSkip();
                if(curCh =='*') {
                    readCharSkip();
                    if(curCh == '/') {
                        token.setType(TokenType.MULTIPLE_LINE_COMMENT);
                        break;
                    }
                }
            }
        } else if(curCh == '/') {
            // 单行行注释, 直接读到行尾
            readToLineEnd();
            token.setType(TokenType.SINGLE_LINE_COMMENT);
        } else {
            // 除号
            token.setType(TokenType.DIVIDE);
            // 提前预读的指针退回去
            pointer--;
        }
    }

    /**
     * 跳过空白符
     */
    private void readCharSkip () {
        do {
            if (pointer < srcCode.length()) {
                curCh = srcCode.charAt(pointer);
                // 字符指针后移
                pointer++;
                if(curCh == '\n') {
                    lineNum++;
                }
            } else {
                // 直到文件尾
                curCh = END_OF_TEXT;
                break;
            }
        }while (curCh == '\n'|| curCh == '\r'|| curCh == '\t'|| curCh ==' ');
    }

    /**
     * 一直读取到行尾
     */
    private void readToLineEnd() {
        while (srcCode.charAt(pointer)!='\n') {
            pointer++;
        }
    }

    private void readChar() {
        if (pointer < srcCode.length()) {
            curCh = srcCode.charAt(pointer);
            pointer++;
        } else {
            curCh = END_OF_TEXT;
        }
    }

    /**
     * 指数形式字符串转整数
     */
    private double expStrToInt(String str) {
        String[] strArr = str.split("e");
        int base = Integer.parseInt(strArr[0]);
        int exp = Integer.parseInt(strArr[1]);
        return (base*Math.pow(10, exp));
    }

    private void illegalNumException(Token token) throws LexException {
        // 中途可能会换行，如注释
        token.setLineNum(lineNum);
        clearStrBuilder();
        throw new LexException("Illegal number literal at line "+ token.getLineNum());
    }


    private void illegalIdException(Token token) throws LexException {
        // 中途可能会换行，如注释
        token.setLineNum(lineNum);
        clearStrBuilder();
        throw new LexException("IDENTIFIER cannot start with digit at line "+ token.getLineNum());
    }

    private void varNameTooLongException(Token token) throws LexException {
        // 中途可能会换行，如注释
        token.setLineNum(lineNum);
        clearStrBuilder();
        throw new LexException("Variable name too long at line "+ token.getLineNum());
    }

    /**
     * 清空构造中的字符串缓冲区
     */
    private void clearStrBuilder() {
        stringBuilder.delete(0, stringBuilder.length());
    }

    public void setSrcCode(String srcCode) {
        this.srcCode = srcCode;
    }

}
