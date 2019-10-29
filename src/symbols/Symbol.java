package symbols;

/**
 * @description 需要记录的符号
 * @author FANG
 * @date 2019/10/25 22:46
 **/
public class Symbol {
    // 符号类型
    private SymValueType type;
    // 符号名
    private String name;
    // 所处层次
    private int level;
    // 比当前低一个层次的符号
    Symbol next;

    private int intValue;
    private double realValue;

    // 数组名
    private String arrName;
    private int[] intArray;
    private double[] realArray;

    // 原始数组的索引
    private int index;


    public Symbol (String name) {
        this.name = name;
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

    public int[] getIntArray () {
        return intArray;
    }

    public void setIntArray (int[] intArray) {
        this.intArray = intArray;
    }

    public double[] getRealArray () {
        return realArray;
    }

    public void setRealArray (double[] realArray) {
        this.realArray = realArray;
    }

    public SymValueType getType () {
        return type;
    }

    public void setType (SymValueType type) {
        this.type = type;
    }

    public String getName () {
        return name;
    }

    public int getLevel () {
        return level;
    }

    public void setLevel (int level) {
        this.level = level;
    }

    public String getArrName() {
        return arrName;
    }

    public void setArrName(String arrName) {
        this.arrName = arrName;
    }

    public int getIndex () {
        return index;
    }

    public void setIndex (int index) {
        this.index = index;
    }
}
