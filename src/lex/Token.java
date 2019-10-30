package lex;

/**
 * @description Token词法单元
 * @author FANG
 * @date 2019/10/22 15:34
 **/
public class Token {
    // token类型
    private TokenType type;
    // 字符串值
    private String stringValue;
    // 整型值
    private int intValue;
    // 实数值
    private double realValue;
    // 行号
    private int lineNum;

    public Token() {
    }

    public Token(TokenType type) {
        this.type = type;
    }

    public TokenType getType () {
        return type;
    }

    public void setType (TokenType type) {
        this.type = type;
    }

    public String getStringValue () {
        return stringValue;
    }

    public void setStringValue (String stringValue) {
        this.stringValue = stringValue;
    }

    public int getIntValue () {
        return intValue;
    }

    public void setIntValue (int intValue) {
        this.intValue = intValue;
    }

    public double getRealValue () {
        return realValue;
    }

    public void setRealValue (double realValue) {
        this.realValue = realValue;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Token{type=").append(type);
        switch (type) {
            case INT_LITERAL:
                stringBuilder.append(", intValue=").append(intValue);
                break;
            case REAL_LITERAL:
                stringBuilder.append(", realValue=").append(realValue);
                break;
            case IDENTIFIER:
                stringBuilder.append(", stringValue='").append(stringValue).append('\'');
                break;
        }
        stringBuilder.append(", lineNum=").append(lineNum);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
