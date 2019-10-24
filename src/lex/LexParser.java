package lex;

import exception.LexException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author FANG
 * @description 词法分析器
 * @date 2019/10/22 15:30
 **/
public class LexParser {
    // 文件尾常量
    private static final char END_OF_TEXT = '\u0003';
    // 常量构造缓冲区
    private StringBuilder stringBuilder = new StringBuilder();
    // 当前字符
    private char curCh;
    // 可直接识别的Token表, 不需要后看
    private static final Map<Character,Integer> directRecognized = new HashMap<>();
    // 保留字表
    private static final HashMap<String,Integer> reserveWords = new HashMap<>();

    private static TokenType[] values = TokenType.values();
    // 当前行号
    private static int lineNum = 1;
    // 源文件路径
    private String srcFilePath;
    // 源代码
    private String sourceCode;

    // 指向正在读取字符的位置的指针
    private int pointer = 0;


    static {
        directRecognized.put('+', TokenType.PLUS.ordinal());
        directRecognized.put('-', TokenType.MINUS.ordinal());
        directRecognized.put('*', TokenType.MULTIPLY.ordinal());
        directRecognized.put(';', TokenType.SEMICOLON.ordinal());
        directRecognized.put('(', TokenType.L_BRACKET.ordinal());
        directRecognized.put(')', TokenType.R_BRACKET.ordinal());
        directRecognized.put('{', TokenType.L_ANGLE_BRACKET.ordinal());
        directRecognized.put('}', TokenType.R_ANGLE_BRACKET.ordinal());
        directRecognized.put('[', TokenType.L_SQUARE_BRACKET.ordinal());
        directRecognized.put(']', TokenType.R_SQUARE_BRACKET.ordinal());

        reserveWords.put("if", TokenType.IF.ordinal());
        reserveWords.put("else", TokenType.ELSE.ordinal());
        reserveWords.put("while", TokenType.WHILE.ordinal());
        reserveWords.put("for", TokenType.FOR.ordinal());
        reserveWords.put("read", TokenType.READ.ordinal());
        reserveWords.put("write", TokenType.WRITE.ordinal());
        reserveWords.put("int", TokenType.INT.ordinal());
        reserveWords.put("real", TokenType.REAL.ordinal());
    }


    public LexParser (String path) {
        this.srcFilePath = path;
    }

    public LexParser() {}

    public void getSourceCode() {
        File file = new File(srcFilePath);
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder builder = new StringBuilder();
            while (bufferedReader.ready()) {
                builder.append(bufferedReader.readLine());
                builder.append('\n');
            }
            // 把源代码当做字符串直接保存
            sourceCode = builder.toString();
            bufferedReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    /**
     * 返回下一个token
     */
    public Token getNextToken() throws LexException{
        Token token = new Token();
        token.setType(TokenType.UNKNOWN);
        token.setLineNum(lineNum);
        readCharSkip();
        if(curCh == END_OF_TEXT) {
            token.setType(TokenType.EOF);
            return token;
        }
        // 先判断出可以直接识别的token
        if(directRecognized.containsKey(curCh)) {
            token.setType(values[directRecognized.get(curCh)]);
            return token;
        } else if(curCh == '/') {
            readCharSkip();
            // 判断是单行注释还是多行注释
            parseSplash(token);

        }  else if(curCh == '=') {
            // 判断赋值还是相等
            readChar();
            if(curCh == '=') {
                token.setType(TokenType.EQUAL);
            } else {
                token.setType(TokenType.ASSIGN);
                pointer--;
            }
        } else if(curCh == '<') {//判断小于还是不等于
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
            if(isCharDigit(curCh)) {
                // 数字常量
                boolean isReal = false;
                while (true) {
                    if((curCh >='0'&& curCh <='9')|| curCh =='.') {
                        stringBuilder.append(curCh);
                        if (curCh == '.') {
                            // 注意此处判断反复出现小数点
                            // 有小数点说明是实数
                            isReal = true;
                        }
                        readChar();
                    } else if(Character.isLetter(curCh)) {
                        // 数字后面不能跟字母作为标识符
                        throw new LexException("IDENTIFIER cannot start with digit at line "+lineNum);
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
            else if(Character.isLetter(curCh)) {
                // 字母开头
                // 说明接下来是一个标识符或者关键字
                while (true) {
                    if(Character.isLetter(curCh)
                            || isCharDigit(curCh)|| curCh == '_') {
                        stringBuilder.append(curCh);
                        readChar();
                    } else {
                        pointer--;
                        break;
                    }
                }
                String value = stringBuilder.toString();
                stringBuilder.delete(0, stringBuilder.length());
                if(reserveWords.containsKey(value)) {
                    token.setType(values[reserveWords.get(value)]);
                }
                else {
                    token.setType(TokenType.IDENTIFIER);
                    token.setStringValue(value);
                }
            }
        }
        return token;
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
            // 单行行注释
            readLineEnd();
            token.setType(TokenType.SINGLE_LINE_COMMENT);
        } else {
            // 除号
            token.setType(TokenType.DIVIDE);
        }
    }

    /**
     * 跳过空白符
     */
    private void readCharSkip () {
        do {
            if (pointer < sourceCode.length()) {
                curCh = sourceCode.charAt(pointer);
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

    private void readLineEnd() {
        while (sourceCode.charAt(pointer)!='\n') {
            pointer++;
        }
    }

    private void readChar() {
        if (pointer < sourceCode.length()) {
            curCh = sourceCode.charAt(pointer);
            pointer++;
        } else {
            curCh = END_OF_TEXT;
        }
    }

    /**
     * 判断字符是否为数字
     * @param ch 待判断字符
     */
    private boolean isCharDigit(char ch) {
        return ch>='0' && ch<='9';
    }

}
