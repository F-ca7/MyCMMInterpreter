package lex;

import exception.LexException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @description 词法分析器
 * @author FANG
 * @date 2019/10/22 15:30
 **/
public class Lexer {
    // 文件尾常量
    private static final char END_OF_TEXT = '\u0003';
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

        RESERVED_WORDS.put("if", TokenType.IF.ordinal());
        RESERVED_WORDS.put("else", TokenType.ELSE.ordinal());
        RESERVED_WORDS.put("while", TokenType.WHILE.ordinal());
        RESERVED_WORDS.put("for", TokenType.FOR.ordinal());
        RESERVED_WORDS.put("print", TokenType.PRINT.ordinal());
        RESERVED_WORDS.put("int", TokenType.INT.ordinal());
        RESERVED_WORDS.put("real", TokenType.REAL.ordinal());
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("E:\\desktop\\MyCMMInterpreter\\test_lex_err4.cmm");
        lexer.loadSourceCode();
        Token token;
        try {
            do {
                token = lexer.getNextToken();
                if (token.getType()==TokenType.SINGLE_LINE_COMMENT
                        ||token.getType()==TokenType.MULTIPLE_LINE_COMMENT){
                    continue;
                }
                System.out.println(token);
            } while (token.getType() != TokenType.EOF);
            System.out.println("词法分析成功");
        } catch (LexException e){
            System.out.println("词法分析错误！" + e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
        }


    }


    public Lexer(String path) {
        this.srcFilePath = path;
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
            // 判断小于还是不等于
            readChar();
            if(curCh =='>') {
                token.setType(TokenType.NOT_EQUAL);
            } else {
                token.setType(TokenType.LESS);
                pointer--;
            }
        } else if(curCh == ';') {
            token.setType(TokenType.SEMICOLON);
        } else {
            if(Character.isDigit(curCh)) {
                // 数字常量
                parseNum(token);
            } else if(Character.isLetter(curCh)) {
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
    private void parseLetter(Token token) {
        while (true) {
            if(Character.isLetter(curCh)
                    || Character.isDigit(curCh)
                    || curCh == '_') {
                stringBuilder.append(curCh);
                readChar();
            } else {
                pointer--;
                break;
            }
        }
        String value = stringBuilder.toString();
        stringBuilder.delete(0, stringBuilder.length());
        if(RESERVED_WORDS.containsKey(value)) {
            token.setType(values[RESERVED_WORDS.get(value)]);
        } else {
            token.setType(TokenType.IDENTIFIER);
            token.setStringValue(value);
        }
    }

    /**
     * 分析 数字常量
     * 判断是int 还是 real
     * @param token 待解析类型的token
     */
    private void parseNum(Token token) throws LexException {
        boolean isReal = false;
        while (true) {
            if((curCh >='0'&& curCh <='9')|| curCh =='.') {
                stringBuilder.append(curCh);
                if (curCh == '.') {
                    // 注意此处判断反复出现小数点
                    if (isReal) {
                        throw new LexException("Illegal number literal at line "+ token.getLineNum());
                    } else {
                        // 有小数点说明是实数
                        isReal = true;
                    }
                }
                readChar();
            } else if(Character.isLetter(curCh)) {
                // 数字后面不能跟字母作为标识符
                throw new LexException("IDENTIFIER cannot start with digit at line " + token.getLineNum());
            }
            else {
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
            token.setRealValue(Double.parseDouble(strVal));
        } else {
            token.setType(TokenType.INT_LITERAL);
            token.setIntValue(Integer.parseInt(strVal));
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


    public void setSrcCode(String srcCode) {
        this.srcCode = srcCode;
    }

}
