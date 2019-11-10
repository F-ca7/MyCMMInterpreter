package semantics;

import exception.GramException;
import exception.SemanticException;
import gram.GramParser;
import gram.TreeNode;
import gram.TreeNodeType;
import lex.Lexer;

import java.util.*;

/**
 * @description 中间代码生成器
 * @author FANG
 * @date 2019/10/26 23:04
 **/
public class InterGenerator {
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
    // 返回值类型
    // 由于函数不能嵌套, 可以作为全局的变量
    private TreeNodeType returnType;

    // 根据函数名找到入口地址
    public Map<String, Integer> funcInstrMap = new HashMap<>();
    // 根据函数名找到参数类型列表, 可供调用时比对
    public Map<String, List<TreeNode>> funcArgTypeMap = new HashMap<>();



    // 是否开启优化
    private boolean optimEnabled = true;

    public static void main(String[] args) {
        Lexer lexer = new Lexer("E:\\desktop\\MyCMMInterpreter\\test_func_call2.cmm");
        lexer.loadSourceCode();
        lexer.loadTokenList();
        GramParser parser = new GramParser(lexer);
        try {
            parser.startParse();
        } catch (GramException e) {
            System.out.println("语法分析错误！" + e.getMessage());
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        InterGenerator generator = new InterGenerator(parser);
        try {
            generator.start();
            // 输出函数入口地址
            System.out.println("函数入口地址");
            System.out.println(generator.funcInstrMap);
            // 输出函数参数类型
            System.out.println("函数参数类型");
            System.out.println(generator.funcArgTypeMap);
            // 输出中间代码-四元式表示
            System.out.println(generator.getFormattedCodes());
        }catch (SemanticException e){
            System.out.println("语义分析错误！" + e.getMessage());
        }

    }


    public InterGenerator(GramParser parser) {
        this.parser = parser;
    }

    public void start() throws SemanticException {
        generate(parser.getTreeNodes());
        // 进行未使用变量优化
        if (optimEnabled) {
            optimizeUnusedVariables();
        }
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
                case FUNCTION:
                    genFunction(node);
                    break;
                case INT_DECLARATION:
                case REAL_DECLARATION:
                case CHAR_DECLARATION:
                    genDeclaration(node, Collections.emptyMap());
                    break;
                case INT_ARRAY_DECLARATION:
                case REAL_ARRAY_DECLARATION:
                    genArrDeclaration(node, Collections.emptyMap());
                    break;
                case ASSIGN:
                    genAssign(node, Collections.emptyMap());
                    break;
                case IF:
                    genIf(node, Collections.emptyMap());
                    break;
                case WHILE:
                    genWhile(node, Collections.emptyMap());
                    break;
                case PRINT:
                    genPrint(node);
                    break;
                case SCAN  :
                    genScan(node);
                    break;
                case STATEMENT_BLOCK:
                    genStatementBlock(node, Collections.emptyMap());
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
                case RETURN:
                    genReturn(node, Collections.emptyMap());
                    break;
                default:
                    throw new SemanticException("Unknown statement!");
            }
        }
    }

    /**
     * 对于每个语法树按顺序生成四元式表示
     * @param argMap 函数参数的替换
     */
    private void generate(List<TreeNode> nodes, Map<String, String>  argMap) throws SemanticException {
        for(TreeNode node:nodes) {
            if (!breakIndex.empty()) {
                throw new SemanticException("Unreachable statements after break!");
            }
            switch (node.getType()) {
                case INT_DECLARATION:
                case REAL_DECLARATION:
                case CHAR_DECLARATION:
                    genDeclaration(node, argMap);
                    break;
                case INT_ARRAY_DECLARATION:
                case REAL_ARRAY_DECLARATION:
                    genArrDeclaration(node, argMap);
                    break;
                case ASSIGN:
                    genAssign(node, argMap);
                    break;
                case IF:
                    genIf(node, argMap);
                    break;
                case WHILE:
                    genWhile(node, argMap);
                    break;
                case PRINT:
                    genPrint(node);
                    break;
                case SCAN  :
                    genScan(node);
                    break;
                case STATEMENT_BLOCK:
                    genStatementBlock(node, argMap);
                    break;
                case BREAK:
                    if(loopLevel==0) {
                        // 不能在非循环内break
                        throw new SemanticException("Using break outside loop!");
                    } else {
                        genBreak();
                    }
                    break;
                case EMPTY:
                    break;
                case RETURN:
                    genReturn(node, argMap);
                    break;
                case FUNC_CALL:
                    genFunctionCall(node, argMap);
                    break;
                default:
                    throw new SemanticException("Unknown statement!");
            }
        }
    }


    /**
     * 生成函数的中间代码
     */
    private void genFunction(TreeNode node) throws SemanticException {
        String funcName = node.getSymbolName();
        // 保存函数入口地址
        funcInstrMap.put(funcName, codes.size());
        // 左结点：函数签名
        TreeNode signNode = node.left;
        TreeNode argNode = signNode.left;
        // 设置全局的返回参数变量, 用于返回值判断
        returnType = signNode.right.getType();
        // 获取参数
        List<TreeNode> argList = argNode.getArgList();
        // 保存函数参数类型
        funcArgTypeMap.put(funcName, argList);
        Map<String, String> argMap = new HashMap<>();
        // 如 x->arg0; y->arg1
        for (int i=0; i<argList.size(); i++) {
            argMap.put(argList.get(i).getSymbolName(), CodeConstant.ARG_PREFIX + i);
        }

        // 右结点: 实现语句块
        genFuncStatementBlock(node.right, argMap);
    }

    /**
     * 生成函数语句块
     * ret语句在执行期间会自动跳出语句块层次
     * 不需要加入out中间代码
     */
    private void genFuncStatementBlock(TreeNode node, Map<String, String> argMap) throws SemanticException {
        codes.add(CodeConstant.inCode);
        generate(node.getStatements(), argMap);
    }

    /**
     * 生成返回语句的中间代码
     */
    private void genReturn(TreeNode node, Map<String, String> argMap) throws SemanticException {
        Quadruple code = new Quadruple();
        code.operation = CodeConstant.RETURN;

        TreeNode left = node.left;
        switch (left.getType()) {
            case IDENTIFIER:
                // 标识符在运行期再判断类型匹配
                code.firstOperandType = OperandType.IDENTIFIER;
                if (argMap.containsKey(left.getSymbolName())) {
                    // 是参数，替换为参数名
                    code.firstOperand.name = argMap.get(left.getSymbolName());
                } else {
                    code.firstOperand.name = left.getSymbolName();
                }
                break;
            case VOID:
                if (returnType != TreeNodeType.VOID){
                    returnTypeException(TreeNodeType.VOID);
                }
                break;
            case INT_LITERAL:
                if (returnType != TreeNodeType.INT_DECLARATION){
                    returnTypeException(TreeNodeType.INT_DECLARATION);
                }
                code.firstOperandType = OperandType.INT_LITERAL;
                code.firstOperand = new IntOperand(left.getIntValue());
                break;
            case REAL_LITERAL:
                if (returnType != TreeNodeType.REAL_DECLARATION){
                    returnTypeException(TreeNodeType.REAL_DECLARATION);
                }
                code.firstOperandType = OperandType.REAL_LITERAL;
                code.firstOperand = new RealOperand(left.getRealValue());
                break;
            case CHAR_DECLARATION:
                if (returnType != TreeNodeType.CHAR_DECLARATION){
                    returnTypeException(TreeNodeType.CHAR_DECLARATION);
                }
                code.firstOperandType = OperandType.INT_LITERAL;
                code.firstOperand = new IntOperand(left.getIntValue());
                break;

        }

        codes.add(code);
    }



    /**
     * 生成函数调用的中间代码
     */
    private void genFunctionCall(TreeNode node, Map<String, String> argMap) throws SemanticException {
        // 先生成入参
        // 左结点：参数列表
        TreeNode argNode = node.left;
        List<TreeNode> argList = argNode.getArgList();
        Quadruple paramCode;
        for (TreeNode arg:argList) {
            paramCode = new Quadruple();
            paramCode.operation = CodeConstant.ARG;
            handleOperandLeft(paramCode, arg, argMap);
            codes.add(paramCode);
        }

        // 再生成call
        Quadruple callCode = new Quadruple();
        callCode.operation = CodeConstant.CALL;
        callCode.firstOperandType = OperandType.IDENTIFIER;
        callCode.firstOperand.name = node.getSymbolName();
        codes.add(callCode);
    }


    /**
     * 生成break的中间代码
     */
    private void genBreak() {
        Quadruple code = new Quadruple();
        // 要多加一次出语句块
        codes.add(CodeConstant.outCode);
        code.operation = CodeConstant.JMP;
        codes.add(code);
        // 具体跳出循环的位置等待回填
        breakIndex.push(codes.size()-1);
    }

    /**
     * 生成算术表达式的中间代码
     */
    private String genArithmetic(TreeNode node, Map<String, String> argMap) throws SemanticException{
        // 返回算术表达式结果的变量名
        List<TreeNode> postTraversalResult = new ArrayList<>();
        postTraverseArithExp(node, postTraversalResult);
        Stack<TreeNode> stack = new Stack<>();
        for (TreeNode opNode: postTraversalResult) {
            switch (opNode.getType()) {
                case INT_LITERAL:
                case REAL_LITERAL:
                case IDENTIFIER:
                    stack.push(opNode);
                    if (argMap.containsKey(opNode.getSymbolName())) {
                        // 是参数，替换为参数名
                        opNode.setSymbolName(argMap.get(opNode.getSymbolName()));
                    }
                    break;
                case PLUS:
                    arithOpToCode(stack, TreeNodeType.PLUS, argMap);
                    break;
                case MINUS:
                    arithOpToCode(stack, TreeNodeType.MINUS, argMap);
                    break;
                case MULTIPLY:
                    arithOpToCode(stack, TreeNodeType.MULTIPLY, argMap);
                    break;
                case DIVIDE:
                    arithOpToCode(stack, TreeNodeType.DIVIDE, argMap);
                    break;
                case ARRAY_ACCESS:
                    genArrayAccess(stack, argMap);
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
     * 生成语句块的中间代码
     * @param argMap 需要被替换的参数名的键值对map
     */
    private void genStatementBlock(TreeNode node, Map<String, String> argMap) throws SemanticException {
        codes.add(CodeConstant.inCode);
        generate(node.getStatements(), argMap);
        codes.add(CodeConstant.outCode);
    }

    /**
     * 生成关系表达式的中间代码
     */
    private String genRelationalExp(TreeNode node, Map<String, String> argMap) throws SemanticException {
        if (optimEnabled) {
            // 对常数的比较进行优化
            if (node.left.getType()==TreeNodeType.INT_LITERAL
                    && node.right.getType()==TreeNodeType.INT_LITERAL) {
                int lVal = node.left.getIntValue(), rVal = node.right.getIntValue();
                switch (node.getType()) {
                    case LESS:
                        if (lVal < rVal) {
                            return CodeConstant.TRUE;
                        } else {
                            return CodeConstant.FALSE;
                        }
                    case EQUAL:
                        if (lVal == rVal) {
                            return CodeConstant.TRUE;
                        } else {
                            return CodeConstant.FALSE;
                        }
                    case GREATER:
                        if (lVal > rVal) {
                            return CodeConstant.TRUE;
                        } else {
                            return CodeConstant.FALSE;
                        }
                    case LESS_EQ:
                        if (lVal <= rVal) {
                            return CodeConstant.TRUE;
                        } else {
                            return CodeConstant.FALSE;
                        }
                    case GREATER_EQ:
                        if (lVal >= rVal) {
                            return CodeConstant.TRUE;
                        } else {
                            return CodeConstant.FALSE;
                        }
                    case NOT_EQUAL:
                        if (lVal != rVal) {
                            return CodeConstant.TRUE;
                        } else {
                            return CodeConstant.FALSE;
                        }
                }
            }
        }

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

        handleOperandLeft(code, node.left, argMap);
        handleOperandRight(code, node.right, argMap);
        code.dest = getNextTempName();
        codes.add(code);
        return code.dest;
    }

    /**
     * 生成变量声明的中间代码
     */
    private void genDeclaration(TreeNode node, Map<String, String>  argMap) throws SemanticException {
        Quadruple code = new Quadruple();
        if (argMap.containsKey(node.left.getSymbolName())) {
            // 和参数重名, 重定义
            redeclarationException(node.left.getSymbolName());
        }
        if(node.right != null) {
            // 声明的同时进行了赋值
            handleOperandLeft(code, node.right, argMap);
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
    private void genArrDeclaration(TreeNode node, Map<String, String>  argMap) throws SemanticException {
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
                code.firstOperand.name = genArithmetic(node.right, argMap);
                break;
            case ARRAY_ACCESS:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = genArrayAccess(node.right, argMap);
                break;
        }
        code.dest = node.left.getSymbolName();
        codes.add(code);
    }

    /**
     * 生成赋值操作的中间代码
     */
    private void genAssign(TreeNode node, Map<String, String> argMap) throws SemanticException {
        Quadruple code = new Quadruple();
        if(node.left.getType() == TreeNodeType.ARRAY_ACCESS) {
            Stack<TreeNode> stack = new Stack<>();
            stack.push(node.left.left);
            stack.push(node.left.right);
            code.dest = genArrayAccess(stack, argMap);
        } else {
            code.dest = node.left.getSymbolName();
        }
        code.operation = CodeConstant.ASSIGN;
        if (node.right.getType() == TreeNodeType.FUNC_CALL) {
            // 函数返回值的赋值
            genFunctionCall(node.right, argMap);
            code.firstOperandType = OperandType.IDENTIFIER;
            code.firstOperand.name = CodeConstant.RETURN_VALUE;
        } else {
            handleOperandLeft(code, node.right, argMap);
        }
        codes.add(code);
    }

    /**
     * 生成if语句的中间代码
     */
    private void genIf(TreeNode node, Map<String, String> argMap) throws SemanticException {
        // if语句块内部的待回填列表
        Stack<Integer> ifBackPatch = new Stack<>();
        // 判断条件
        String result = genSelect(node, ifBackPatch, argMap);
        if (result.equals(CodeConstant.TRUE)) {
            return;
        }
        if(node.getStatements().size()!=0) {
            // 生成else-if语句块
            for (TreeNode node1: node.getStatements()) {
                result = genSelect(node1, ifBackPatch, argMap);
                if (result.equals(CodeConstant.TRUE)) {
                    return;
                }
            }
        }
        if (node.right != null && result.equals(CodeConstant.FALSE)) {
            // 直接生成else的代码块
            codes.add(CodeConstant.inCode);
            generate(node.right.getStatements());
            codes.add(CodeConstant.outCode);
            return;
        }
        if(node.right!=null) {
            // 进入条件为false的语句块并回填之前的jump
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
            } else {
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
    private void genWhile(TreeNode node, Map<String, String> argMap) throws SemanticException {
        // 进入循环
        loopLevel++;
        String condition = genRelationalExp(node.getCondition(), argMap);
        // while的开头位置入栈
        backPatch.push(codes.size()-1);
        Quadruple code = new Quadruple();
        code.operation = CodeConstant.JMP_WITH_CONDITION;
        code.firstOperandType = OperandType.IDENTIFIER;
        code.firstOperand.name = condition;
        codes.add(code);
        codes.add(CodeConstant.inCode);
        generate(node.left.getStatements());

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
        codes.add(CodeConstant.outCode);
        // 退出循环
        loopLevel--;
    }


    private String genSelect(TreeNode node, Stack<Integer> innerBackFills, Map<String, String> argMap) throws SemanticException {
        boolean needBackFill = (node.getCondition() != null);
        if(needBackFill) {
            // condition为存储关系表达式的中间变量
            String condition = genRelationalExp(node.getCondition(), argMap);
            if (optimEnabled) {
                if (condition.equals(CodeConstant.TRUE)) {
                    // 不用生成跳转语句
                    // 只生成true时的语句块
                    codes.add(CodeConstant.inCode);
                    generate(node.left.getStatements());
                    codes.add(CodeConstant.outCode);
                    return CodeConstant.TRUE;
                }
                if (condition.equals(CodeConstant.FALSE)) {
                    // 不用生成跳转语句和语句块
                    return CodeConstant.FALSE;
                }

            }
            // 条件判断后的跳转语句
            Quadruple code = new Quadruple();
            code.operation = CodeConstant.JMP_WITH_CONDITION;
            code.firstOperandType = OperandType.IDENTIFIER;
            code.firstOperand.name = condition;
            codes.add(code);
            backPatch.push(codes.size()-1);
        }
        // 生成条件为true时的语句块
        genConditionTrue(node, innerBackFills, needBackFill);
        return CodeConstant.DEFAULT;
    }

    /**
     * 生成条件为真的时的语句块
     */
    private void genConditionTrue(TreeNode node, Stack<Integer> innerBackFills, boolean needBackFill) throws SemanticException {
        codes.add(CodeConstant.inCode);
        generate(node.left.getStatements());
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
        switch (node.left.getType()) {
            case IDENTIFIER:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = node.left.getSymbolName();
                break;
            case INT_LITERAL:
                code.firstOperandType = OperandType.INT_LITERAL;
                code.firstOperand = new IntOperand(node.left.getIntValue());
                break;
            case REAL_LITERAL:
                code.firstOperandType = OperandType.REAL_LITERAL;
                code.firstOperand = new RealOperand(node.left.getRealValue());
                break;
        }
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
        postTraverseArithExp(arithmeticExpression.left, result);
        postTraverseArithExp(arithmeticExpression.right, result);
        result.add(arithmeticExpression);
    }

    /**
     * 将算数操作转为中间代码
     */
    private void arithOpToCode(Stack<TreeNode> stack, TreeNodeType type, Map<String, String> argMap) throws SemanticException {
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
                if (operand1.isNegative()) {
                    code.firstOperand = new IntOperand(-operand1.getIntValue());
                } else {
                    code.firstOperand = new IntOperand(operand1.getIntValue());
                }
                break;
            case REAL_LITERAL:
                code.firstOperandType = OperandType.REAL_LITERAL;
                if (operand1.isNegative()) {
                    code.firstOperand = new RealOperand(-operand1.getRealValue());
                } else {
                    code.firstOperand = new RealOperand(operand1.getRealValue());
                }
                break;
            case IDENTIFIER:
                code.firstOperandType = OperandType.IDENTIFIER;
                if (argMap.containsKey(operand1.getSymbolName())) {
                    // 是参数，替换为参数名
                    code.firstOperand.name = argMap.get(operand1.getSymbolName());
                } else {
                    code.firstOperand.name = operand1.getSymbolName();
                }

                break;
        }
        switch (operand2.getType()) {
            case INT_LITERAL:
                code.secondOperandType = OperandType.INT_LITERAL;
                if (operand2.isNegative()) {
                    code.secondOperand = new IntOperand(-operand2.getIntValue());
                } else {
                    code.secondOperand = new IntOperand(operand2.getIntValue());
                }
                if(code.operation.equals(CodeConstant.DIV) &&
                        ((IntOperand)code.secondOperand).intLiteral == 0) {
                    divByZeroException();
                }
                break;
            case REAL_LITERAL:
                code.secondOperandType = OperandType.REAL_LITERAL;
                if (operand2.isNegative()) {
                    code.secondOperand = new RealOperand(-operand2.getRealValue());
                } else {
                    code.secondOperand = new RealOperand(operand2.getRealValue());
                }
                if(code.operation.equals(CodeConstant.DIV) &&
                        Math.abs(((RealOperand)code.secondOperand).realLiteral) < 1e-10) {
                    divByZeroException();
                }
                break;
            case IDENTIFIER:
                code.secondOperandType = OperandType.IDENTIFIER;
                if (argMap.containsKey(operand2.getSymbolName())) {
                    // 是参数，替换为参数名
                    code.secondOperand.name = argMap.get(operand2.getSymbolName());
                } else {
                    code.secondOperand.name = operand2.getSymbolName();
                }
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
     * 用临时变量来存储要访问的数组元素
     */
    private String genArrayAccess(Stack<TreeNode> stack, Map<String, String>  argMap) throws SemanticException {
        Quadruple code = new Quadruple();
        code.operation = CodeConstant.ARR_ACC;
        // 索引值
        TreeNode operand1 = stack.pop();
        if (operand1.getType()==TreeNodeType.INT_LITERAL && operand1.getIntValue()<0) {
            // 索引小于0
            arrayIndexOutOfBoundsException(operand1.getIntValue());
        }
        // 数组名
        TreeNode operand2 = stack.pop();
        // 索引值为右操作数
        handleOperandRight(code, operand1, argMap);
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
    private void handleOperandLeft (Quadruple code, TreeNode node, Map<String, String>  argMap) throws SemanticException {
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
                if (argMap.containsKey(node.getSymbolName())) {
                    // 是参数，替换为参数名
                    code.firstOperand.name = argMap.get(node.getSymbolName());
                } else {
                    code.firstOperand.name = node.getSymbolName();
                }

                break;
            case ARRAY_ACCESS:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = genArrayAccess(node, argMap);
                break;
            default:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = genArithmetic(node, argMap);
        }
    }

    /**
     * 生成右操作数的中间代码
     */
    private void handleOperandRight(Quadruple code, TreeNode node, Map<String, String>  argMap) throws SemanticException {
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
                if (argMap.containsKey(node.getSymbolName())) {
                    // 是参数，替换为参数名
                    code.secondOperand.name = argMap.get(node.getSymbolName());
                } else {
                    code.secondOperand.name = node.getSymbolName();
                }

                break;
            case ARRAY_ACCESS:
                code.firstOperandType = OperandType.IDENTIFIER;
                code.firstOperand.name = genArrayAccess(node, argMap);
                break;
            default:
                code.secondOperandType = OperandType.IDENTIFIER;
                code.secondOperand.name = genArithmetic(node, argMap);
        }
    }

    /**
     * 数组访问
     */
    private String genArrayAccess(TreeNode node, Map<String, String>  argMap) throws SemanticException {
        Stack<TreeNode> stack = new Stack<>();
        stack.push(node.left);
        stack.push(node.right);
        return genArrayAccess(stack, argMap);
    }

    /**
     * 未使用变量优化
     */
    private void optimizeUnusedVariables() {
        // 记录声明变量的行号
        Map<String, Integer> declaredVarMap = new HashMap<>();
        // 记录代码段移动的间隔
        List<Integer> offsetIntervals = new ArrayList<>();
        offsetIntervals.add(0);
        String operation;
        for (int i=0; i<codes.size(); i++) {
            operation = codes.get(i).operation;
            if (operation.equals(CodeConstant.INT) || operation.equals(CodeConstant.REAL) || operation.equals(CodeConstant.CHAR)) {
                // 声明了变量
                declaredVarMap.put(codes.get(i).dest, i);
            } else {
                // 使用了变量
                if (codes.get(i).firstOperandType == OperandType.IDENTIFIER) {
                    declaredVarMap.remove(codes.get(i).firstOperand.name);
                }
                if (codes.get(i).secondOperandType == OperandType.IDENTIFIER) {
                    declaredVarMap.remove(codes.get(i).secondOperand.name);
                }
            }
        }
        if (declaredVarMap.isEmpty()) {
            // 没有可以优化的
            return;
        }
        // 计算代码段移动的间隔
        for (int i=0; i<codes.size(); i++) {
            if (declaredVarMap.containsValue(i)) {
                offsetIntervals.add(i);
            }
        }
        // System.out.println(offsetIntervals);
        // 最后剩下的是声明但未使用的变量
        // 使用倒序删除法，不用考虑下标问题
        for (int i=codes.size()-1; i>=0; i--) {
            if (declaredVarMap.containsValue(i)) {
                //System.out.printf("中间代码第%d行变量声明但未使用\n", i);
                codes.remove(i);
            } else if (codes.get(i).operation.equals(CodeConstant.JMP) ||
                    codes.get(i).operation.equals(CodeConstant.JMP_WITH_CONDITION) ) {
                // 偏移跳转位置
                int originJmpIndex = codes.get(i).jumpLocation;
                // 更新跳转位置
                codes.get(i).jumpLocation = originJmpIndex-calcIntervalIndex(offsetIntervals, originJmpIndex);
            }
        }
    }

    /**
     * 计算代码所属行号要偏移的数量
     * @param offsetIntervals 移除的行号列表
     * @param lineIndex 原始行号
     * @return 要偏移的数量
     */
    private int calcIntervalIndex(List<Integer> offsetIntervals, int lineIndex) {
        if (offsetIntervals.size() <= 1) {
            return 0;
        }
        int i = 0;
        for (i=0; i<offsetIntervals.size()-1; i++) {
            // 左闭右开
            if (lineIndex>=offsetIntervals.get(i) && lineIndex<offsetIntervals.get(i+1)) {
                return i;
            }
        }
        return i;
    }


    /**
     * 抛出除以0异常
     */
    private void divByZeroException() throws SemanticException {
        throw new SemanticException("Illegal operation! Cannot divide by zero!");
    }

    /**
     * 重定义异常
     */
    private void redeclarationException(String name) throws SemanticException {
        throw new SemanticException("Redeclaration of "+name);
    }



    /**
     * 数组索引越界
     */
    private void arrayIndexOutOfBoundsException(int index) throws SemanticException{
        throw new SemanticException("Array index is out of bounds: "+index);
    }


    /**
     * 返回值类型错误
     * @param retType 实际的返回值类型
     * @throws SemanticException
     */
    private void returnTypeException(TreeNodeType retType) throws SemanticException{
        String err = String.format("Return value expected %s, found %s", returnType, retType);
        throw new SemanticException(err);
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

    public void setOptimEnabled(boolean optimEnabled) {
        this.optimEnabled = optimEnabled;
    }

}
