package lex;

/**
 * @author FANG
 * @description Token
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
}
