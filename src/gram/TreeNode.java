package gram;

import java.util.LinkedList;
import java.util.List;

/**
 * @description 语法树结点
 * @author FANG
 * @date 2019/10/22 19:50
 **/
public class TreeNode {
    public TreeNode left;
    public TreeNode right;

    // 标识符的名字
    private String symbolName;
    // 整型值
    private int intValue;
    // 实数值
    private double realValue;


    // 语句块
    private LinkedList<TreeNode> statementBlock = new LinkedList<>();
    // bool条件表达式
    private TreeNode condition;
    // 结点类型
    private TreeNodeType type;

    public String getSymbolName () {
        return symbolName;
    }

    public void setSymbolName (String symbolName) {
        this.symbolName = symbolName;
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

    public TreeNode getCondition () {
        return condition;
    }

    public void setCondition (TreeNode condition) {
        this.condition = condition;
    }

    public TreeNodeType getType () {
        return type;
    }

    public void setType (TreeNodeType type) {
        this.type = type;
    }

    public void addStatement(TreeNode node) {
        statementBlock.add(node);
    }

    public List<TreeNode> getStatements() {
        return statementBlock;
    }
}
