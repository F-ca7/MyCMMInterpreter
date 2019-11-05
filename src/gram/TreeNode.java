package gram;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @description 语法树结点
 * @author FANG
 * @date 2019/10/22 19:50
 **/
public class TreeNode {
    public TreeNode left;
    public TreeNode right;
    // 结点类型
    private TreeNodeType type;
    // 标识符的名字
    private String symbolName;
    // 整型值
    private int intValue;
    // 实数值
    private double realValue;
    // 是否为负
    private boolean isNegative = false;

    // 语句块
    private LinkedList<TreeNode> statementBlock = new LinkedList<>();
    // bool条件表达式
    private TreeNode condition;
    // 参数列表，可用于定义 或 调用
    // type为参数类型, symbolName为形参名
    private List<TreeNode> argList;


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

    public List<TreeNode> getArgList() {
        return argList;
    }

    public void setArgList(List<TreeNode> argList) {
        this.argList = argList;
    }


    /**
     * 层次遍历语法树的结果
     */
    public static String getLevelOrderString(TreeNode root) {
        StringBuilder stringBuilder = new StringBuilder();
        // 每一层的结果
        StringBuilder levelResult = new StringBuilder();
        // 辅助队列
        Queue<TreeNode> queue= new LinkedList<>();
        int nextLevel = 0;
        // 这层还需要加入结果的结点个数
        int toBeList = 1;
        // 当前层
        int curLevel= 0;
        if (root!=null) {
            queue.offer(root);
        }else {
            return "null";
        }
        while (!queue.isEmpty()) {
            // 层次遍历
            TreeNode node = queue.poll();
            toBeList--;
            levelResult.append(node.toStringBuilder(curLevel)).append("\n");
            if (node.left != null) {
                nextLevel++;
                queue.offer(node.left);
            }
            if(node.right != null){
                nextLevel++;
                queue.offer(node.right);
            }
            if (toBeList == 0) {
                curLevel++;
                // 该层添加完毕，可附加到总结果中
                stringBuilder.append(levelResult);
                stringBuilder.append("\n");

                levelResult.delete(0, levelResult.length());
                toBeList = nextLevel;
                nextLevel = 0;
            }
        }
        return stringBuilder.toString();
    }



    private StringBuilder toStringBuilder(int level) {
        StringBuilder stringBuilder = new StringBuilder();
        appendTabs(stringBuilder, level);
        stringBuilder.append("TreeNode{");
        stringBuilder.append("type=").append(type);
        switch (type) {
            case IF:
            case ELSE_IF:
            case WHILE:
                stringBuilder.append(", condition=").append(condition);
                break;
            case STATEMENT_BLOCK:
                stringBuilder.append(", stmtBlock={").append("\n");
                for (int i = 0; i<statementBlock.size(); i++){
                    appendTabs(stringBuilder, level);
                    stringBuilder.append(i+1).append(". ");
                    stringBuilder.append(statementBlock.get(i));
                    stringBuilder.append("\n");
                }
                appendTabs(stringBuilder, level);
                stringBuilder.append("}\n");
                appendTabs(stringBuilder, level);
                stringBuilder.append("}");
                return stringBuilder;
            case INT_LITERAL:
                stringBuilder.append(", value=").append(intValue);
                if (isNegative) {
                    stringBuilder.append(", negative");
                }
                break;
            case REAL_LITERAL:
                stringBuilder.append(", value=").append(realValue);
                if (isNegative) {
                    stringBuilder.append(", negative");
                }
                break;
            case IDENTIFIER:
                stringBuilder.append(", name=").append(symbolName);
                break;
            case FUNCTION:
                stringBuilder.append(", func=").append(symbolName);
                break;
            case DEF_ARGS:
                // 打印定义函数的参数
                stringBuilder.append(", args={");
                for (TreeNode node:argList) {
                    stringBuilder.append(node.type).append(':').append(node.symbolName).append(' ');
                }
                stringBuilder.append("}");
                break;
            case RETURN:
                stringBuilder.append(", ret=");
                switch (left.getType()) {
                    case IDENTIFIER:
                        stringBuilder.append(left.getSymbolName());
                        break;
                    case INT_LITERAL:
                    case CHAR_DECLARATION:
                        stringBuilder.append(left.getIntValue());
                        break;
                    case REAL_LITERAL:
                        stringBuilder.append(left.getRealValue());
                        break;
                }
                break;
            case FUNC_CALL:
                stringBuilder.append(", func_call=").append(symbolName).append("; args=");
                stringBuilder.append(left);
                break;
            case CALL_ARGS:
                // 打印调用函数的参数
                stringBuilder.append(", call_args={");
                for (TreeNode node:argList) {
                    stringBuilder.append(node.type).append(' ');
                }
                stringBuilder.append("}");
                break;
        }
        stringBuilder.append("}");
        return stringBuilder;
    }

    @Override
    public String toString() {
        return toStringBuilder(0).toString();
    }

    private static void appendTabs(StringBuilder stringBuilder, int count) {
        for (int j=0; j<count; j++) {
            stringBuilder.append("\t");
        }
    }

    public boolean isNegative() {
        return isNegative;
    }

    public void setNegative(boolean negative) {
        isNegative = negative;
    }
}
