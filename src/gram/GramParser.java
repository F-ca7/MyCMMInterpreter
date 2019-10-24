package gram;

import exception.GramException;
import exception.LexException;
import lex.LexParser;
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
    private LexParser lexicalParser;
    // 当前token
    private Token current;
    // 一条语句对应一棵语法树
    private List<TreeNode> treeNodes = new ArrayList<>();

    public GramParser(LexParser lexicalParser) {
        this.lexicalParser = lexicalParser;
    }


    public static void main(String[] args) {
        LexParser parser = new LexParser("E:\\desktop\\MyCMMInterpreter\\test_gram_err1.cmm");
        parser.getSourceCode();
        GramParser grammaticalParser = new GramParser(parser);
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
            current = lexicalParser.getNextToken();
        }while (current.getType()== TokenType.SINGLE_LINE_COMMENT ||
                current.getType() == TokenType.MULTIPLE_LINE_COMMENT ||
                current == null);
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
        if(current == null || current.getType() == TokenType.SEMICOLON|| needNext) {
            getNextToken();
        }
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.NULL);
        if(current.getType()!= TokenType.EOF) {
            switch (current.getType()) {
                case INT:
                    node = parseDeclarationStatement(TokenType.INT);
                    break;
                case REAL:
                    node = parseDeclarationStatement(TokenType.REAL);
                    break;
                case IDENTIFIER:
                    node = parseAssignStatement();
                    break;
                case READ:
                    node = parseReadOrWriteStatement(TokenType.READ);
                    break;
                case WRITE:
                    node = parseReadOrWriteStatement(TokenType.WRITE);
                    break;
                case IF:
                    node = parseIfStatement();
                    break;
                case WHILE:
                    node = parseWhileStatement();
                    break;
                case L_ANGLE_BRACKET:
                    node = parseStatementBlock(false);
                    break;
                default:
                    throw new RuntimeException("unexpected token!");
            }
        }
        if(!isRecursive && node.getType()!= TreeNodeType.NULL) {
            // 当不需要递归, 且结点类型不为空时
            // 单条语句解析完成
            treeNodes.add(node);
        }

        return node;
    }

    private TreeNode parseStatementBlock(boolean needNext) throws Exception {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.STATEMENT_BLOCK);
        if(needNext) {
            matchTokenNext(TokenType.L_ANGLE_BRACKET);
        }
        else {
            matchToken(TokenType.L_ANGLE_BRACKET);
        }
        while (!checkTokenNext(TokenType.R_ANGLE_BRACKET)) {
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
        left.setSymbolName(current.getStringValue());
        node.left = left;
        getNextToken();
        if(checkToken(TokenType.ASSIGN)) {
            // 赋值语句
            // 右边是算术表达式
            node.right = parseArithmeticExpression();
            matchToken(TokenType.SEMICOLON);
        } else if(checkToken(TokenType.L_SQUARE_BRACKET)) {
            // 如果后面接的是[
            // 则是数组
            isArray = true;
            node.right = parseArithmeticExpression();
            matchToken(TokenType.R_SQUARE_BRACKET);
            matchTokenNext(TokenType.SEMICOLON);
        }
        switch (type) {
            case INT:
                if(isArray) {
                    node.setType(TreeNodeType.INT_ARRAY_DECLARATION);
                }
                else {
                    node.setType(TreeNodeType.INT_DECLARATION);
                }
                break;
            case REAL:
                if(isArray) {
                    node.setType(TreeNodeType.REAL_ARRAY_DECLARATION);
                }
                else {
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
        left.setSymbolName(current.getStringValue());
        tokens.add(current);
        getNextToken();
        switch (current.getType()) {
            case ASSIGN:
                node.left = left;
                node.right = parseArithmeticExpression();
                matchToken(TokenType.SEMICOLON);
                break;
            case L_SQUARE_BRACKET:
                do {
                    tokens.add(current);
                    getNextToken();
                }while (current.getType() != TokenType.R_SQUARE_BRACKET);
                tokens.add(current);
                node.left = parseArrayAccess(tokens);
                matchTokenNext(TokenType.ASSIGN);
                node.right = parseArithmeticExpression();
                matchToken(TokenType.SEMICOLON);
        }

        return node;
    }

    private TreeNode parseReadOrWriteStatement(TokenType type) throws Exception {
        TreeNode node = new TreeNode();
        switch (type) {
            case READ:
                node.setType(TreeNodeType.READ);
            case WRITE:
                node.setType(TreeNodeType.WRITE);
        }
        TreeNode left = new TreeNode();
        left.setType(TreeNodeType.IDENTIFIER);
        matchTokenNext(TokenType.IDENTIFIER);
        left.setSymbolName(current.getStringValue());
        node.left = left;
        matchTokenNext(TokenType.SEMICOLON);

        return node;
    }

    private TreeNode parseIfStatement() throws Exception {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.IF);
        matchTokenNext(TokenType.L_BRACKET);
        node.setCondition(parseRelationalExpression());
        matchToken(TokenType.R_BRACKET);
        node.left = parseStatementBlock(true);
        while (true) {
            if (checkTokenNext(TokenType.ELSE)) {
                if (checkTokenNext(TokenType.IF)) {
                    TreeNode elseIf= new TreeNode();
                    elseIf.setType(TreeNodeType.ELSE_IF);
                    matchTokenNext(TokenType.L_BRACKET);
                    elseIf.setCondition(parseRelationalExpression());
                    matchToken(TokenType.R_BRACKET);
                    elseIf.left = parseStatementBlock(true);
                    node.addStatement(elseIf);
                }
                else {
                    node.right = parseStatementBlock(false);
                    break;
                }
            }
            else {
                break;
            }
        }

        return node;
    }

    private TreeNode parseWhileStatement() throws Exception {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.WHILE);
        matchTokenNext(TokenType.L_BRACKET);
        node.setCondition(parseRelationalExpression());
        matchToken(TokenType.R_BRACKET);
        node.left = parseStatementBlock(true);

        return node;
    }

    private TreeNode parseArithmeticExpression() throws LexException {
        List<Token> tokens = getAllExpressionTokens();
        return parseArithmeticExpression(tokens);
    }

    private TreeNode parseArithmeticExpression(List<Token> tokens) {
        Stack<TreeNode> operandStack = new Stack<>();//操作数栈
        Stack<TreeNode> operatorStack = new Stack<>();//操作符栈
        Stack<Character> brackets = new Stack<>();//括号栈
        ListIterator<Token> iterator = tokens.listIterator();

        while (true){
            if(iterator.hasNext()) {
                Token token = iterator.next();
                if (checkTokenOperand(token)) {
                    if (iterator.hasNext()) {
                        if (iterator.next().getType() != TokenType.L_SQUARE_BRACKET) {
                            operandStack.push(tokenToTreeNode(token));
                            iterator.previous();
                        } else {
                            int start = iterator.previousIndex();
                            int end;
                            brackets.push('[');
                            Token aim = null;
                            while (!brackets.empty()) {
                                aim = iterator.next();
                                if (aim.getType() == TokenType.L_SQUARE_BRACKET) {
                                    brackets.push('[');
                                } else if (aim.getType() == TokenType.R_SQUARE_BRACKET) {
                                    brackets.pop();
                                }
                            }
                            end = iterator.nextIndex();

                            operandStack.push(parseArrayAccess(tokens.subList(start-1, end)));
                        }
                    }
                    else {
                        operandStack.push(tokenToTreeNode(token));
                    }
                } else if (checkTokenArithmeticOperator(token)) {
                    if (operatorStack.empty()) {
                        operatorStack.push(tokenToTreeNode(token));
                    } else {
                        TreeNode currentOperator = tokenToTreeNode(token);
                        TreeNode previousOperator = operatorStack.peek();
                        if (priorityCompare(previousOperator, currentOperator)) {
                            Token next = iterator.next();
                            TreeNode currentOperand;
                            if (!checkTokenBracket(next)) {//如果下一个token不是左括号
                                currentOperand = tokenToTreeNode(next);
                            } else {//如果下一个token是左括号
                                //从后往前遍历找右括号
                                currentOperand = sliceExpressionInBrackets(iterator,tokens);
                            }
                            TreeNode previousOperand = operandStack.pop();
                            currentOperator.right = currentOperand;
                            currentOperator.left = previousOperand;
                            operandStack.push(currentOperator);
                        } else {

                            TreeNode operand1 = operandStack.pop();
                            TreeNode operand2 = operandStack.pop();
                            previousOperator.left = operand1;
                            previousOperator.right = operand2;
                            operandStack.push(previousOperator);
                            operatorStack.push(currentOperator);
                        }
                    }
                }
                else if(token.getType() == TokenType.L_BRACKET) {
                    TreeNode operand = sliceExpressionInBrackets(iterator,tokens);
                    operandStack.push(operand);
                }
                else {
                    throw new RuntimeException("unexpected token!");
                }
            }

            else  {//如果没有需要处理的token
                if(operandStack.size()>1) {
                    TreeNode operand1 = operandStack.pop();
                    TreeNode operand2 = operandStack.pop();
                    TreeNode operator = operatorStack.pop();
                    operator.left = operand1;
                    operator.right = operand2;
                    operandStack.push(operator);
                }
                else {
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
    private List<Token> getAllExpressionTokens() throws LexException {
        List<Token> tokens = new ArrayList<>();
        Stack<Character> s = new Stack<>();//小括号栈
        Stack<Character> s2 = new Stack<>();//中括号栈
        loop:
        while (true) {
            getNextToken();
            switch (current.getType()) {
                case IDENTIFIER:
                case PLUS:
                case MINUS:
                case MULTIPLY:
                case DIVIDE:
                case INT_LITERAL:
                case REAL_LITERAL:
                    tokens.add(current);
                    break;
                case L_BRACKET:
                    tokens.add(current);
                    s.push('(');
                    break;
                case R_BRACKET:
                    if(s.empty()) {//如果栈为空遇到右括号，则表达式结束
                        break loop;
                    }
                    else if(s.peek() == '(') {
                        s.pop();
                        tokens.add(current);
                    }
                    else {
                        throw new RuntimeException("unexpected token!");
                    }
                    break;
                case L_SQUARE_BRACKET:
                    tokens.add(current);
                    s2.push('[');
                    break;
                case R_SQUARE_BRACKET:
                    if(s2.empty()) {//如果栈为空遇到右括号，则表达式结束
                        break loop;
                    }
                    else if(s2.peek() == '[') {
                        s2.pop();
                        tokens.add(current);
                    }
                    else {
                        throw new RuntimeException("unexpected token!");
                    }
                    break;
                default:
                    //如果遇到上述token以外的其它token，表达式结束
                    break loop;
            }
        }

        return tokens;
    }

    private TreeNode parseRelationalExpression() throws LexException {
        TreeNode node = new TreeNode();
        node.left = parseArithmeticExpression();
        if(checkTokenRelationalOperator()) {
            switch (current.getType()) {
                case LESS:
                    node.setType(TreeNodeType.LESS);
                    break;
                case EQUAL:
                    node.setType(TreeNodeType.EQUAL);
                    break;
                case NOT_EQUAL:
                    node.setType(TreeNodeType.NOT_EQUAL);
                    break;
            }
        }
        else {
            throw new RuntimeException("unexpected token!");
        }
        node.right = parseArithmeticExpression();

        return node;
    }



    private void matchTokenNext (TokenType type) throws Exception {
        getNextToken();
        matchToken(type);
    }

    private void matchToken(TokenType type) throws Exception {
        if (current.getType() == TokenType.UNKNOWN) {
            String err = String.format("Expected %s, found %s at line %d",
                    type, current.getType(), current.getLineNum());
            throw new LexException(err);
        }
        if(current.getType() != type) {
            String err = String.format("Expected %s, found %s at line %d",
                    type, current.getType(), current.getLineNum());
            throw new GramException(err);
        }
    }

    private boolean checkTokenNext (TokenType type) throws LexException {
        getNextToken();
        return current.getType() == type;
    }

    private boolean checkToken(TokenType type) {
        return current.getType() == type;
    }


    private boolean checkTokenRelationalOperator() {
        TokenType type = current.getType();
        return type == TokenType.LESS||
                type == TokenType.EQUAL||
                type == TokenType.NOT_EQUAL;
    }

    private boolean checkTokenArithmeticOperator(Token token) {
        TokenType type = token.getType();
        return type == TokenType.PLUS||
                type == TokenType.MINUS||
                type == TokenType.MULTIPLY||
                type == TokenType.DIVIDE;
    }

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

    private boolean priorityCompare(TreeNode operator1,TreeNode operator2) {//比较运算符优先级
        return getPriority(operator1)<getPriority(operator2);
    }

    private int getPriority(TreeNode operator) {//获得运算符优先级
        if(operator.getType() == TreeNodeType.PLUS||operator.getType() == TreeNodeType.MINUS) {
            return 1;
        }
        else if(operator.getType() == TreeNodeType.MULTIPLY||operator.getType() == TreeNodeType.DIVIDE) {
            return 2;
        }
        throw new RuntimeException("unexpected operator!");
    }

    private boolean checkTokenOperand(Token token) {
        TokenType type = token.getType();
        return type == TokenType.IDENTIFIER
                ||type == TokenType.INT_LITERAL
                ||type == TokenType.REAL_LITERAL;
    }

    private boolean checkTokenBracket(Token token) {
        return token.getType() == TokenType.L_BRACKET;
    }


    public List<TreeNode> getTreeNodes() {
        return treeNodes;
    }

    private TreeNode parseArrayAccess(List<Token> tokens) {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.ARRAY_ACCESS);
        TreeNode left = new TreeNode();
        left.setType(TreeNodeType.IDENTIFIER);
        left.setSymbolName(tokens.get(0).getStringValue());
        node.left = left;
        if(tokens.size() == 4) {
            Token right = tokens.get(2);
            node.right = tokenToTreeNode(right);
        }else {
            node.right = parseArithmeticExpression(tokens.subList(2, tokens.size() - 1));
        }

        return node;
    }

    private TreeNode sliceExpressionInBrackets(ListIterator<Token> iterator,List<Token> tokens) {
        Stack<Character> brackets = new Stack<>();
        int start = iterator.previousIndex();
        int end;
        brackets.push('(');
        Token aim = null;
        while (!brackets.empty()) {
            aim = iterator.next();
            if (aim.getType() == TokenType.L_BRACKET) {
                brackets.push('(');
            } else if (aim.getType() == TokenType.R_BRACKET) {
                brackets.pop();
            }
        }
        end = iterator.nextIndex();
        return parseArithmeticExpression(tokens.subList(start + 1, end - 1));
    }

}
