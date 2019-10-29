package gram;

import exception.GramException;
import exception.LexException;
import lex.Lexer;
import lex.Token;
import lex.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

/**
 * @author FANG
 * @description 语法分析器, 递归下降分析
 * @date 2019/10/22 16:36
 **/
public class GramParser {
    // 词法分析器
    private Lexer lexer;
    // 当前token
    private Token curToken;
    // 一条语句对应一棵语法树
    private List<TreeNode> treeNodes = new ArrayList<>();

    public GramParser(Lexer lexicalParser) {
        this.lexer = lexicalParser;
    }


    public static void main(String[] args) {
        Lexer lexer = new Lexer("E:\\desktop\\MyCMMInterpreter\\test_lex_err4.cmm");
        lexer.loadSourceCode();
        GramParser grammaticalParser = new GramParser(lexer);
        try {
            grammaticalParser.startParse();
        } catch (LexException e) {
            System.out.println("词法分析错误！" + e.getMessage());
        } catch (GramException e) {
            System.out.println("语法分析错误！" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void getNextToken() throws LexException {
        do {
            curToken = lexer.getNextToken();
        }while (curToken.getType()== TokenType.SINGLE_LINE_COMMENT ||
                curToken.getType() == TokenType.MULTIPLE_LINE_COMMENT ||
                curToken == null);
    }

    public void startParse() throws Exception {
        while (true) {
            TreeNode node = parseStatement(false,true);
            if(node.getType() == TreeNodeType.NULL) {
                break;
            }
        }
        System.out.println("分析成功");
    }

    /**
     * 解析单条语句
     * @param isRecursive 是否是递归调用
     * @param needNext 是否需要下一个token
     */
    private TreeNode parseStatement(boolean isRecursive,boolean needNext) throws Exception {
        if(needNext || curToken == null || curToken.getType() == TokenType.SEMICOLON) {
            getNextToken();
        }
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.NULL);
        if(curToken.getType()!= TokenType.EOF) {
            switch (curToken.getType()) {
                case INT:
                    node = parseDeclarationStatement(TokenType.INT);
                    break;
                case REAL:
                    node = parseDeclarationStatement(TokenType.REAL);
                    break;
                case IDENTIFIER:
                    node = parseAssignStatement();
                    break;
                case PRINT:
                    node = parsePrintStatement();
                    break;
                case IF:
                    node = parseIfStatement();
                    break;
                case WHILE:
                    node = parseWhileStatement();
                    break;
                case L_BRACE:
                    node = parseStatementBlock(false);
                    break;
                default:
                    throw new GramException("Unexpected token at line " + curToken.getLineNum());
            }
        }
        if(!isRecursive && node.getType()!= TreeNodeType.NULL) {
            // 当不需要递归, 且结点类型不为空时
            // 单条语句解析完成
            treeNodes.add(node);
        }

        return node;
    }

    /**
     * 解析语句块
     * @param needNext 是否需要下一个token
     */
    private TreeNode parseStatementBlock(boolean needNext) throws Exception {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.STATEMENT_BLOCK);
        if(needNext) {
            matchTokenNext(TokenType.L_BRACE);
        } else {
            matchToken(TokenType.L_BRACE);
        }
        while (!checkTokenNext(TokenType.R_BRACE)) {
            // 把每条语句存储到结点中
            node.addStatement(parseStatement(true,false));
        }

        return node;

    }

    /**
     * 解析声明语句
     * @param type
     */
    private TreeNode parseDeclarationStatement(TokenType type) throws Exception {
        TreeNode node = new TreeNode();
        boolean isArray = false;
        matchTokenNext(TokenType.IDENTIFIER);
        TreeNode left = new TreeNode();
        left.setType(TreeNodeType.IDENTIFIER);
        left.setSymbolName(curToken.getStringValue());
        node.left = left;
        getNextToken();
        if(checkToken(TokenType.ASSIGN)) {
            // 赋值语句
            // 右边是算术表达式
            node.right = parseArithmeticExpression();
            matchToken(TokenType.SEMICOLON);
        } else if(checkToken(TokenType.L_BRACKET)) {
            // 如果后面接的是[
            // 则是数组
            isArray = true;
            node.right = parseArithmeticExpression();
            matchToken(TokenType.R_BRACKET);
            matchTokenNext(TokenType.SEMICOLON);
        }
        // 设置结点类型
        switch (type) {
            case INT:
                if(isArray) {
                    node.setType(TreeNodeType.INT_ARRAY_DECLARATION);
                } else {
                    node.setType(TreeNodeType.INT_DECLARATION);
                }
                break;
            case REAL:
                if(isArray) {
                    node.setType(TreeNodeType.REAL_ARRAY_DECLARATION);
                } else {
                    node.setType(TreeNodeType.REAL_DECLARATION);
                }
        }

        return node;
    }

    /**
     * 解析赋值语句
     */
    private TreeNode parseAssignStatement() throws Exception {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.ASSIGN);
        TreeNode left = new TreeNode();
        List<Token> tokens = new ArrayList<>();
        left.setType(TreeNodeType.IDENTIFIER);
        left.setSymbolName(curToken.getStringValue());
        tokens.add(curToken);
        getNextToken();
        switch (curToken.getType()) {
            case ASSIGN:
                node.left = left;
                node.right = parseArithmeticExpression();
                matchToken(TokenType.SEMICOLON);
                break;
            case L_BRACKET:
                do {
                    tokens.add(curToken);
                    getNextToken();
                } while (curToken.getType() != TokenType.R_BRACKET);
                tokens.add(curToken);
                node.left = parseArrayAccess(tokens);
                matchTokenNext(TokenType.ASSIGN);
                node.right = parseArithmeticExpression();
                matchToken(TokenType.SEMICOLON);
        }

        return node;
    }

    /**
     * 解析输出语句
     */
    private TreeNode parsePrintStatement() throws Exception {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.PRINT);
        TreeNode left = new TreeNode();
        left.setType(TreeNodeType.IDENTIFIER);
        matchTokenNext(TokenType.IDENTIFIER);
        left.setSymbolName(curToken.getStringValue());
        node.left = left;
        matchTokenNext(TokenType.SEMICOLON);

        return node;
    }

    /**
     * 解析if语句
     */
    private TreeNode parseIfStatement() throws Exception {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.IF);
        // bool表达式
        matchTokenNext(TokenType.L_PARENTHESIS);
        node.setCondition(parseRelationalExpression());
        matchToken(TokenType.R_PARENTHESIS);
        // 满足条件的语句块
        node.left = parseStatementBlock(true);
        while (true) {
            if (checkTokenNext(TokenType.ELSE)) {
                // 判断是else if还是else
                if (checkTokenNext(TokenType.IF)) {
                    TreeNode elseIf= new TreeNode();
                    elseIf.setType(TreeNodeType.ELSE_IF);
                    matchTokenNext(TokenType.L_PARENTHESIS);
                    elseIf.setCondition(parseRelationalExpression());
                    matchToken(TokenType.R_PARENTHESIS);
                    elseIf.left = parseStatementBlock(true);
                    node.addStatement(elseIf);
                } else {
                    node.right = parseStatementBlock(false);
                    break;
                }
            } else {
                break;
            }
        }

        return node;
    }

    /**
     * 解析while语句
     */
    private TreeNode parseWhileStatement() throws Exception {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.WHILE);
        matchTokenNext(TokenType.L_PARENTHESIS);
        node.setCondition(parseRelationalExpression());
        matchToken(TokenType.R_PARENTHESIS);
        node.left = parseStatementBlock(true);

        return node;
    }

    /**
     * 解析算术表达式
     */
    private TreeNode parseArithmeticExpression() throws Exception {
        List<Token> tokens = getAllExpressionTokens();
        return parseArithmeticExpression(tokens);
    }

    /**
     * 根据token列表解析算术表达式
     * 不是做运算，只解析成对应的树
     * @param tokens 对应的token列表
     */
    private TreeNode parseArithmeticExpression(List<Token> tokens) throws GramException{
        // 操作数栈
        Stack<TreeNode> operandStack = new Stack<>();
        // 操作符栈
        Stack<TreeNode> operatorStack = new Stack<>();
        // 括号栈
        Stack<Character> brackets = new Stack<>();
        // token指针
        ListIterator<Token> iterator = tokens.listIterator();

        while (true){
            if(iterator.hasNext()) {
                Token token = iterator.next();
                if (checkTokenOperand(token)) {
                    // token是操作数
                    if (iterator.hasNext()) {
                        if (iterator.next().getType() != TokenType.L_BRACKET) {
                            operandStack.push(tokenToTreeNode(token));
                            iterator.previous();
                        } else {
                            int start = iterator.previousIndex();
                            int end;
                            brackets.push('[');
                            Token aim;
                            while (!brackets.empty()) {
                                aim = iterator.next();
                                if (aim.getType() == TokenType.L_BRACKET) {
                                    brackets.push('[');
                                } else if (aim.getType() == TokenType.R_BRACKET) {
                                    brackets.pop();
                                }
                            }
                            end = iterator.nextIndex();
                            operandStack.push(parseArrayAccess(tokens.subList(start-1, end)));
                        }
                    } else {
                        // 当前是最后一个token
                        // 直接入栈
                        operandStack.push(tokenToTreeNode(token));
                    }
                } else if (checkTokenArithmeticOperator(token)) {
                    // token是运算符
                    if (operatorStack.empty()) {
                        operatorStack.push(tokenToTreeNode(token));
                    } else {
                        // 取得前一个操作符与当前操作符
                        TreeNode preOperator = operatorStack.peek();
                        TreeNode curOperator = tokenToTreeNode(token);
                        try {
                            if (priorityCompare(preOperator, curOperator)) {
                                Token next = iterator.next();
                                TreeNode currentOperand;
                                if (!checkTokenBracket(next)) {
                                    //如果下一个token不是左括号
                                    currentOperand = tokenToTreeNode(next);
                                } else {
                                    // 如果下一个token是左括号
                                    // 从后往前遍历找右括号
                                    currentOperand = sliceExpressionInBrackets(iterator,tokens);
                                }
                                TreeNode previousOperand = operandStack.pop();
                                curOperator.right = currentOperand;
                                curOperator.left = previousOperand;
                                operandStack.push(curOperator);
                            } else {

                                TreeNode operand1 = operandStack.pop();
                                TreeNode operand2 = operandStack.pop();
                                preOperator.left = operand1;
                                preOperator.right = operand2;
                                operandStack.push(preOperator);
                                operatorStack.push(curOperator);
                            }
                        } catch (GramException e) {
                            throw new GramException("Invalid relational operator at line " + curToken.getLineNum());
                        }

                    }
                } else if(token.getType() == TokenType.L_PARENTHESIS) {
                    // token是左括号
                    TreeNode operand = sliceExpressionInBrackets(iterator,tokens);
                    operandStack.push(operand);
                } else {
                    throw new GramException("Invalid arithmetic expression at line " + token.getLineNum());
                }
            } else  {
                // token处理完毕
                if(operandStack.size() > 1) {
                    // 当操作数栈中数量大于1时
                    //
                    TreeNode operand1 = operandStack.pop();
                    TreeNode operand2 = operandStack.pop();
                    TreeNode operator = operatorStack.pop();
                    operator.left = operand1;
                    operator.right = operand2;
                    // ？？
                    operandStack.push(operator);
                } else {
                    // 栈中只剩一个结点
                    // 结束循环
                    break;
                }
            }

        }

        return operandStack.peek();
    }

    /**
     * 获取表达式所有词法单元
     * @return token序列
     */
    private List<Token> getAllExpressionTokens() throws LexException, GramException {
        List<Token> tokens = new ArrayList<>();
        // 小括号栈
        Stack<Character> s = new Stack<>();
        // 中括号栈
        Stack<Character> s2 = new Stack<>();
        loop:
        while (true) {
            getNextToken();
            switch (curToken.getType()) {
                case IDENTIFIER:
                case PLUS:
                case MINUS:
                case MULTIPLY:
                case DIVIDE:
                case INT_LITERAL:
                case REAL_LITERAL:
                    tokens.add(curToken);
                    break;
                case L_PARENTHESIS:
                    tokens.add(curToken);
                    s.push('(');
                    break;
                case R_PARENTHESIS:
                    if(s.empty()) {
                        // 如果栈为空遇到右括号，则表达式结束
                        break loop;
                    } else if(s.peek() == '(') {
                        // 弹出左括号
                        s.pop();
                        tokens.add(curToken);
                    } else {
                        throw new GramException("Unexpected token at line " + curToken.getLineNum());
                    }
                    break;
                case L_BRACKET:
                    tokens.add(curToken);
                    s2.push('[');
                    break;
                case R_BRACKET:
                    if(s2.empty()) {
                        // 如果栈为空遇到右括号，则表达式结束
                        break loop;
                    } else if(s2.peek() == '[') {
                        s2.pop();
                        tokens.add(curToken);
                    } else {
                        throw new GramException("Unexpected token at line " + curToken.getLineNum());
                    }
                    break;
                default:
                    // 遇到其它token，表达式结束
                    break loop;
            }
        }

        return tokens;
    }

    /**
     * 解析关系表达式
     */
    private TreeNode parseRelationalExpression() throws Exception {
        // 左右结点均为算术表达式
        // 中间是关系运算符
        TreeNode node = new TreeNode();
        node.left = parseArithmeticExpression();
        switch (curToken.getType()) {
            case LESS:
                node.setType(TreeNodeType.LESS);
                break;
            case EQUAL:
                node.setType(TreeNodeType.EQUAL);
                break;
            case NOT_EQUAL:
                node.setType(TreeNodeType.NOT_EQUAL);
                break;
            default:
                throw new GramException("Invalid relational operator at line " + curToken.getLineNum());
        }
        node.right = parseArithmeticExpression();

        return node;
    }

    /**
     * 用预期类型匹配下一个token
     */
    private void matchTokenNext (TokenType type) throws GramException, LexException {
        getNextToken();
        matchToken(type);
    }

    /**
     * 用预期类型匹配当前token
     */
    private void matchToken(TokenType type) throws GramException {
        if(curToken.getType() != type) {
            String err = String.format("Expected %s, found %s at line %d",
                    type, curToken.getType(), curToken.getLineNum());
            throw new GramException(err);
        }
    }

    /**
     * 检查下一个token是否为预期类型
     * @param type 预期类型
     */
    private boolean checkTokenNext (TokenType type) throws LexException {
        getNextToken();
        return curToken.getType() == type;
    }

    /**
     * 检查当前token是否为预期类型
     * @param type 预期类型
     */
    private boolean checkToken(TokenType type) {
        return curToken.getType() == type;
    }


    /**
     * 检查词法单元是否为算算术运算符
     * @param token 待检查词法单元
     */
    private boolean checkTokenArithmeticOperator(Token token) {
        TokenType type = token.getType();
        return type == TokenType.PLUS||
                type == TokenType.MINUS||
                type == TokenType.MULTIPLY||
                type == TokenType.DIVIDE;
    }

    /**
     * 将词法单元转换为语法树的结点
     * @param token 词法单元
     * @return 语法树结点
     */
    private TreeNode tokenToTreeNode(Token token) {
        TreeNode node = new TreeNode();
        switch (token.getType()) {
            case IDENTIFIER:
                node.setType(TreeNodeType.IDENTIFIER);
                node.setSymbolName(token.getStringValue());
                break;
            case INT_LITERAL:
                node.setType(TreeNodeType.INT_LITERAL);
                node.setIntValue(token.getIntValue());
                break;
            case REAL_LITERAL:
                node.setType(TreeNodeType.REAL_LITERAL);
                node.setRealValue(token.getRealValue());
                break;
            case PLUS:
                node.setType(TreeNodeType.PLUS);
                break;
            case MINUS:
                node.setType(TreeNodeType.MINUS);
                break;
            case MULTIPLY:
                node.setType(TreeNodeType.MULTIPLY);
                break;
            case DIVIDE:
                node.setType(TreeNodeType.DIVIDE);
                break;
        }
        return node;
    }


    /**
     * 比较两个运算符优先级
     * @param operator1 运算符1
     * @param operator2 运算符2
     * @return 运算符2的优先级是否更高
     */
    private boolean priorityCompare(TreeNode operator1, TreeNode operator2) throws GramException {
        return getPriority(operator2) > getPriority(operator1);
    }

    /**
     * 获取运算符优先级
     * 数值越大，优先级越高
     * @param operator 运算符
     */
    private int getPriority(TreeNode operator) throws GramException {
        switch (operator.getType()) {
            case PLUS:
            case MINUS:
                return 0;
            case MULTIPLY:
            case DIVIDE:
                return 1;
            default:
                throw new GramException("Invalid arithmetic operator!");
        }
    }

    /**
     * 检查词法单元是否为合法的操作数
     * @param token 待检查词法单元
     */
    private boolean checkTokenOperand(Token token) {
        TokenType type = token.getType();
        return type == TokenType.IDENTIFIER
                ||type == TokenType.INT_LITERAL
                ||type == TokenType.REAL_LITERAL;
    }

    /**
     * 检查词法单元是否为左括号
     * @param token 待检查词法单元
     */
    private boolean checkTokenBracket(Token token) {
        return token.getType() == TokenType.L_PARENTHESIS;
    }


    /**
     * 解析数组
     */
    private TreeNode parseArrayAccess(List<Token> tokens) throws GramException {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.ARRAY_ACCESS);
        TreeNode left = new TreeNode();
        left.setType(TreeNodeType.IDENTIFIER);
        left.setSymbolName(tokens.get(0).getStringValue());
        node.left = left;
        if(tokens.size() == 4) {
            // 如果大小为4的话
            // 说明是最基础的一维数组+索引
            // 比如A[1]
            Token right = tokens.get(2);
            // 索引位设为右节点
            node.right = tokenToTreeNode(right);
        } else {
            // 可能是算术表达式
            // 或多维数组
            node.right = parseArithmeticExpression(tokens.subList(2, tokens.size() - 1));
        }

        return node;
    }


    private TreeNode sliceExpressionInBrackets(ListIterator<Token> iterator,
                                               List<Token> tokens) throws GramException {
        Stack<Character> brackets = new Stack<>();
        int start = iterator.previousIndex();
        int end;
        brackets.push('(');
        Token aim;
        while (!brackets.empty()) {
            aim = iterator.next();
            if (aim.getType() == TokenType.L_PARENTHESIS) {
                brackets.push('(');
            } else if (aim.getType() == TokenType.R_PARENTHESIS) {
                brackets.pop();
            }
        }
        end = iterator.nextIndex();
        return parseArithmeticExpression(tokens.subList(start + 1, end - 1));
    }



    public List<TreeNode> getTreeNodes() {
        return treeNodes;
    }

    /**
     * 所有语法树的格式化字符串表示
     */
    private String treesToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0; i<treeNodes.size(); i++) {
            stringBuilder.append("语法树").append(i).append(":\n");

            stringBuilder.append("---------------------\n");
        }
        return stringBuilder.toString();
    }

}
