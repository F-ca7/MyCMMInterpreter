package semantics;

import exception.SemanticException;
import gram.GramParser;
import gram.TreeNode;
import gram.TreeNodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @description 中间代码生成器
 * @author FANG
 * @date 2019/10/26 23:04
 **/
class InterGenerator {
    // 中间指令列表
    private List<Quadruple> codes = new ArrayList<>();
    // 语法分析器
    private GramParser parser;
    // 回填列表
    private Stack<Integer> backPatch = new Stack<>();
    // 临时变量序号
    // 保证中间变量名全局唯一
    private int tempSerialNum = 0;
    // 循环层次
    private int loopLevel = 0;
    // 记录break的位置回填
    private Stack<Integer> breakIndex = new Stack<>();

    InterGenerator(GramParser parser) {
        this.parser = parser;
    }

    public void start() throws SemanticException {
        generate(parser.getTreeNodes());
    }

    public List<Quadruple> getCodes() {
        return codes;
    }

    private void generate(List<TreeNode> nodes) throws SemanticException {
        // 对于每个语法树按顺序生成四元式表示
        for(TreeNode node:nodes) {
            if (!breakIndex.empty()) {
                throw new SemanticException("Unreachable statements after break!");
            }
            switch (node.getType()) {
                case INT_DECLARATION:
                case REAL_DECLARATION:
                case CHAR_DECLARATION:
                    genDeclaration(node);
                    break;
                case INT_ARRAY_DECLARATION:
                case REAL_ARRAY_DECLARATION:
                    genArrDeclaration(node);
                    break;
                case ASSIGN:
                    genAssign(node);
                    break;
                case IF:
                    genIf(node);
                    break;
                case WHILE:
                    genWhile(node);
                    break;
                case PRINT:
                    genPrint(node);
                    break;
                case SCAN  :
                    genScan(node);
                    break;
                case STATEMENT_BLOCK:
                    genStatementBlock(node);
                    break;
                case BREAK:
                    if(loopLevel==0) {
                        throw new SemanticException("Using break outside loop!");
                    } else {
                        genBreak();
                    }
                    break;
                case EMPTY:
                    break;
                default:
                    throw new SemanticException("Unknown statement!");
            }
        }
    }



    /**
     * 生成break的中间代码
     */
    private void genBreak() {
        Quadruple code1 = new Quadruple();
        // 要多加一次出语句块
        codes.add(CodeConstant.outCode);
        code1.operation = CodeConstant.JMP;
        codes.add(code1);
        // 具体跳出循环的位置等待回填
        breakIndex.push(codes.size()-1);
    }

    /**
     * 生成算术表达式的中间代码
     */
    private String genArithmetic(TreeNode node) throws SemanticException{
        // 返回算术表达式结果的变量名
        List<TreeNode> postTraversalResult = new ArrayList<>();
        postTraverseArithExp(node,postTraversalResult);
        Stack<TreeNode> stack = new Stack<>();
        for (TreeNode every: postTraversalResult) {
            switch (every.getType()) {
                case INT_LITERAL:
                case REAL_LITERAL:
                case IDENTIFIER:
                    stack.push(every);
                    break;
                case PLUS:
                    arithOpToCode(stack, TreeNodeType.PLUS);
                    break;
                case MINUS:
                    arithOpToCode(stack, TreeNodeType.MINUS);
                    break;
                case MULTIPLY:
                    arithOpToCode(stack, TreeNodeType.MULTIPLY);
                    break;
                case DIVIDE:
                    arithOpToCode(stack, TreeNodeType.DIVIDE);
                    break;
                case ARRAY_ACCESS:
                    genArrayAccess(stack);
                    break;

            }
        }
        return stack.peek().getSymbolName();
    }

    /**
     * 生成语句块的中间代码
     */
    private void genStatementBlock(TreeNode node) throws SemanticException {
        codes.add(CodeConstant.inCode);
        generate(node.getStatements());
        codes.add(CodeConstant.outCode);
    }

    /**
     * 生成关系表达式的中间代码
     */
    private String genRelationalExp(TreeNode node) throws SemanticException {
        Quadruple code = new Quadruple();
        switch (node.getType()) {
            case LESS:
                code.operation = CodeConstant.LE;
                break;
            case EQUAL:
                code.operation = CodeConstant.EQ;
                break;
            case GREATER:
                code.operation = CodeConstant.GR;
                break;
            case LESS_EQ:
                code.operation = CodeConstant.LE_EQ;
                break;
            case GREATER_EQ:
                code.operation = CodeConstant.GR_EQ;
                break;
            case NOT_EQUAL:
                code.operation = CodeConstant.NEQ;
                break;

        }
        handleOperandLeft(code, node.left);
        handleOperandRight(code, node.right);
        code.dest = getNextTempName();
        codes.add(code);
        return code.dest;
    }

    /**
     * 生成变量声明的中间代码
     */
    private void genDeclaration(TreeNode node) throws SemanticException {
        Quadruple code = new Quadruple();
        if(node.right != null) {
            // 声明的同时进行了赋值
            handleOperandLeft(code, node.right);
        } else {
            // 默认赋值为0
            switch (node.getType()) {
                case INT_DECLARATION:
                    code.firstOperandType = OperandType.INT_LITERAL;
                    code.firstOperand = new IntOperand(0);
                case REAL_DECLARATION:
                    code.firstOperandType = OperandType.REAL_LITERAL;
                    code.firstOperand = new RealOperand(0.0);
                case CHAR_DECLARATION:
                    code.firstOperandType = OperandType.INT_LITERAL;
                    code.firstOperand = new IntOperand(0);
            }
        }
        if(node.getType() == TreeNodeType.INT_DECLARATION) {
            code.operation = CodeConstant.INT;
        } else if(node.getType() == TreeNodeType.REAL_DECLARATION) {
            code.operation = CodeConstant.REAL;
        } else if(node.getType() == TreeNodeType.CHAR_DECLARATION) {
            code.operation = CodeConstant.CHAR;
        }
        code.dest = node.left.getSymbolName();
        codes.add(code);
    }

    /**
     * 生成数组声明的中间代码
     */
    private void genArrDeclaration(TreeNode node) throws SemanticException {
        Quadruple code = new Quadruple();
        if(node.getType() == TreeNodeType.INT_ARRAY_DECLARATION) {
            code.operation = CodeConstant.INT_ARR;
        } else if(node.getType() == TreeNodeType.REAL_ARRAY_DECLARATION) {
            code.operation = CodeConstant.REAL_ARR;
        }
        switch (node.right.getType()) {
            case INT_LITERAL:
                code.firstOperandType = OperandType.INT_LITERAL;
                code.firstOperand = new IntOperand(node.right.getIntValue());
                break;
            case IDENTIFIER:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = node.right.getSymbolName();
                break;
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVIDE:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = genArithmetic(node.right);
                break;
            case ARRAY_ACCESS:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = genArrayAccess(node.right);
                break;
        }
        code.dest = node.left.getSymbolName();
        codes.add(code);
    }

    /**
     * 生成赋值操作的中间代码
     */
    private void genAssign(TreeNode node) throws SemanticException {
        Quadruple code = new Quadruple();
        if(node.left.getType() == TreeNodeType.ARRAY_ACCESS) {
            Stack<TreeNode> stack = new Stack<>();
            stack.push(node.left.left);
            stack.push(node.left.right);
            code.dest = genArrayAccess(stack);
        } else {
            code.dest = node.left.getSymbolName();
        }
        code.operation = CodeConstant.ASSIGN;
        handleOperandLeft(code, node.right);
        codes.add(code);
    }

    /**
     * 生成if语句的中间代码
     */
    private void genIf(TreeNode node) throws SemanticException {
        // if语句块内部的待回填列表
        Stack<Integer> ifBackPatch = new Stack<>();
        genSelect(node, ifBackPatch);
        if(node.getStatements().size()!=0) {
            for (TreeNode node1: node.getStatements()) {
                genSelect(node1, ifBackPatch);
            }
        }
        if(node.right!=null) {
            codes.add(CodeConstant.inCode);
            generate(node.right.getStatements());
            Quadruple code1 = new Quadruple();
            if (!breakIndex.empty()) {
                // 如果最后一句是break
                // 把out放在jmp前
                codes.set(codes.size()-1, CodeConstant.outCode);
                Quadruple codeBreak = new Quadruple();
                codeBreak.operation = CodeConstant.JMP;
                codes.add(codeBreak);
                breakIndex.pop();
                // 具体跳出循环的位置等待回填
                breakIndex.push(codes.size()-1);
            }else {
                codes.add(CodeConstant.outCode);
            }
            code1.operation = CodeConstant.JMP;
            codes.add(code1);
            // 具体跳转位置等待回填
            ifBackPatch.push(codes.size()-1);
        }
        // 跳转位置为整个代码段的下一条
        int jumpLocation = codes.size();
        while (!ifBackPatch.empty()) {
            int backPatch = ifBackPatch.pop();
            codes.get(backPatch).jumpLocation = jumpLocation;
        }
    }

    /**
     * 生成while的中间代码
     */
    private void genWhile(TreeNode node) throws SemanticException {
        // 进入循环
        loopLevel++;
        String condition = genRelationalExp(node.getCondition());
        // while的开头位置入栈
        backPatch.push(codes.size()-1);
        Quadruple code = new Quadruple();
        code.operation = CodeConstant.JMP_WITH_CONDITION;
        code.firstOperandType = OperandType.IDENTIFIER;
        code.firstOperand.name = condition;
        codes.add(code);
        codes.add(CodeConstant.inCode);
        generate(node.left.getStatements());
        codes.add(CodeConstant.outCode);
        Quadruple code1 = new Quadruple();
        code1.operation = CodeConstant.JMP;
        // 跳到while的开头
        int jumpLocation = backPatch.pop();
        code1.jumpLocation = jumpLocation;
        codes.add(code1);
        codes.get(jumpLocation+1).jumpLocation = codes.size();
        if (!breakIndex.empty()) {
            // 回填break位置
            int breakLocation = breakIndex.pop();
            codes.get(breakLocation).jumpLocation = codes.size();
        }
        // 退出循环
        loopLevel--;
    }


    private void genSelect(TreeNode node, Stack<Integer> innerBackFills) throws SemanticException {
        boolean needBackFill = (node.getCondition() != null);
        if(needBackFill) {
            String condition = genRelationalExp(node.getCondition());
            Quadruple code = new Quadruple();
            code.operation = CodeConstant.JMP_WITH_CONDITION;
            code.firstOperandType = OperandType.IDENTIFIER;
            code.firstOperand.name = condition;
            codes.add(code);
            backPatch.push(codes.size()-1);
        }
        codes.add(CodeConstant.inCode);
        generate(node.left.getStatements());
        Quadruple code1 = new Quadruple();
        // codes.add(CodeConstant.outCode);
        if (!breakIndex.empty()) {
            // 如果最后一句是break
            // 把out放在jmp前
            codes.set(codes.size()-1, CodeConstant.outCode);
            Quadruple codeBreak = new Quadruple();
            codeBreak.operation = CodeConstant.JMP;
            codes.add(codeBreak);
            breakIndex.pop();
            // 具体跳出循环的位置等待回填
            breakIndex.push(codes.size()-1);
        }else {
            codes.add(CodeConstant.outCode);
        }
        code1.operation = CodeConstant.JMP;
        codes.add(code1);
        innerBackFills.push(codes.size()-1);
        if(needBackFill) {
            // 跳转目标地址为下一条指令
            int jumpLocation = codes.size();
            // jump指令的索引
            int backFillInstruction = backPatch.pop();
            // 回填目标地址
            codes.get(backFillInstruction).jumpLocation = jumpLocation;
        }
    }

    /**
     * 生成输出指令
     */
    private void genPrint(TreeNode node) {
        Quadruple code = new Quadruple();
        code.operation = CodeConstant.PRINT;
        code.dest = node.left.getSymbolName();
        codes.add(code);
    }

    /**
     * 生成输入指令
     */
    private void genScan(TreeNode node) {
        Quadruple code = new Quadruple();
        code.operation = CodeConstant.SCAN;
        code.dest = node.left.getSymbolName();
        codes.add(code);
    }

    /**
     * 获取下一个临时变量名，递增序列号
     */
    private String getNextTempName() {
        tempSerialNum++;
        return "temp"+tempSerialNum;
    }

    /**
     * 后序遍历算术表达式
     * 添加到遍历结果中
     */
    private void postTraverseArithExp(TreeNode arithmeticExpression, List<TreeNode> result) {
        if(arithmeticExpression == null) {
            return;
        }
        postTraverseArithExp(arithmeticExpression.left,result);
        postTraverseArithExp(arithmeticExpression.right,result);
        result.add(arithmeticExpression);
    }

    /**
     * 将算数操作转为中间代码
     */
    private void arithOpToCode(Stack<TreeNode> stack, TreeNodeType type) throws SemanticException {
        Quadruple code = new Quadruple();
        switch (type) {
            case PLUS:
                code.operation = CodeConstant.PLUS;
                break;
            case MINUS:
                code.operation = CodeConstant.MINUS;
                break;
            case MULTIPLY:
                code.operation = CodeConstant.MUL;
                break;
            case DIVIDE:
                code.operation = CodeConstant.DIV;
                break;
        }
        TreeNode operand1 = stack.pop();
        TreeNode operand2 = stack.pop();
        String tempName = getNextTempName();
        switch (operand1.getType()) {
            case INT_LITERAL:
                code.firstOperandType = OperandType.INT_LITERAL;
                code.firstOperand = new IntOperand(operand2.getIntValue());
                break;
            case REAL_LITERAL:
                code.firstOperandType = OperandType.REAL_LITERAL;
                code.firstOperand = new RealOperand(operand2.getRealValue());
                break;
            case IDENTIFIER:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = operand1.getSymbolName();
                break;
        }
        switch (operand2.getType()) {
            case INT_LITERAL:
                code.secondOperandType = OperandType.INT_LITERAL;
                code.secondOperand = new IntOperand(operand2.getIntValue());
                if(code.operation.equals(CodeConstant.DIV) &&
                        ((IntOperand)code.secondOperand).intLiteral == 0) {
                    divByZeroException();
                }
                break;
            case REAL_LITERAL:
                code.secondOperandType = OperandType.REAL_LITERAL;
                code.secondOperand = new RealOperand(operand2.getRealValue());
                if(code.operation.equals(CodeConstant.DIV) &&
                        Math.abs(((RealOperand)code.secondOperand).realLiteral) < 1e-10) {
                    divByZeroException();
                }
                break;
            case IDENTIFIER:
                code.secondOperandType = OperandType.IDENTIFIER;
                code.secondOperand.name = operand2.getSymbolName();
                break;
        }
        code.dest = tempName;
        TreeNode temp = new TreeNode();
        temp.setType(TreeNodeType.IDENTIFIER);
        temp.setSymbolName(tempName);
        stack.push(temp);
        codes.add(code);
    }

    /**
     * 生成访问数组中间代码
     */
    private String genArrayAccess(Stack<TreeNode> stack) throws SemanticException {
        Quadruple code = new Quadruple();
        code.operation = CodeConstant.ARR_ACC;
        // 索引值
        TreeNode operand1 = stack.pop();
        // 数组名
        TreeNode operand2 = stack.pop();

        handleOperandRight(code,operand1);
        if(operand2.getType() == TreeNodeType.IDENTIFIER) {
            code.firstOperandType = OperandType.IDENTIFIER;
            code.firstOperand.name = operand2.getSymbolName();
        }

        code.dest = getNextTempName();
        codes.add(code);
        return code.dest;
    }

    /**
     * 生成左操作数的中间代码
     */
    private void handleOperandLeft (Quadruple code, TreeNode node) throws SemanticException {
        switch (node.getType()) {
            case INT_LITERAL:
                code.firstOperandType = OperandType.INT_LITERAL;
                code.firstOperand = new IntOperand(node.getIntValue());
                break;
            case REAL_LITERAL:
                code.firstOperandType = OperandType.REAL_LITERAL;
                code.firstOperand = new RealOperand(node.getRealValue());
                break;
            case IDENTIFIER:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = node.getSymbolName();
                break;
            case ARRAY_ACCESS:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = genArrayAccess(node);
                break;
            default:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = genArithmetic(node);
        }
    }

    /**
     * 生成右操作数的中间代码
     */
    private void handleOperandRight(Quadruple code, TreeNode node) throws SemanticException {
        switch (node.getType()) {
            case INT_LITERAL:
                code.secondOperandType = OperandType.INT_LITERAL;
                code.secondOperand = new IntOperand(node.getIntValue());
                break;
            case REAL_LITERAL:
                code.secondOperandType = OperandType.REAL_LITERAL;
                code.secondOperand = new RealOperand(node.getRealValue());
                break;
            case IDENTIFIER:
                code.secondOperandType = OperandType.IDENTIFIER;
                code.secondOperand.name = node.getSymbolName();
                break;
            case ARRAY_ACCESS:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = genArrayAccess(node);
                break;
            default:
                code.secondOperandType = OperandType.IDENTIFIER;
                code.secondOperand.name = genArithmetic(node);
        }
    }


    private String genArrayAccess(TreeNode node) throws SemanticException {
        Stack<TreeNode> stack = new Stack<>();
        stack.push(node.left);
        stack.push(node.right);
        return genArrayAccess(stack);
    }

    /**
     * 抛出除以0异常
     */
    private void divByZeroException() throws SemanticException {
        throw new SemanticException("Illegal operation! Cannot divide by zero!");
    }

    /**
     * 返回格式化后的中间代码
     */
    public String getFormattedCodes() {
        StringBuilder stringBuilder = new StringBuilder(10*codes.size());
        for (int i = 0;i<codes.size();i++) {
            stringBuilder.append(i).append("  ").append(codes.get(i).toString()).append("\n");
        }
        return stringBuilder.toString();
    }
}
