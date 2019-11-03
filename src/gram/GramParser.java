package gram;

import exception.GramException;
import lex.Lexer;
import lex.Token;
import lex.TokenType;


import java.util.*;

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
    // token指针
    private int tokenPtr = -1;
    // 错误信息
    private StringBuffer errInfoBuffer = new StringBuffer();
    // 是否成功
    private boolean ifSuccess = true;

    // 待定义的函数
    private List<FuncDeclaration> toBeDefinedFuncs = new LinkedList<>();
    // 已定义的函数
    private List<FuncDeclaration> definedFuncs = new LinkedList<>();

    private HashSet<TokenType> FIRST_STATEMENT = new HashSet<TokenType>(){{
        add(TokenType.IDENTIFIER);
        add(TokenType.IF);
        add(TokenType.WHILE);
        add(TokenType.PRINT);
        add(TokenType.SCAN);
    }};

    private HashSet<TokenType> FOLLOW_STATEMENT = new HashSet<TokenType>(){{
        add(TokenType.SEMICOLON);
    }};

    /**
     * 函数签名的Follow集合
     */
    private HashSet<TokenType> FOLLOW_FUNCTION_ARGS = new HashSet<TokenType>(){{
        add(TokenType.L_BRACE);
        add(TokenType.L_PARENTHESIS);
    }};





    public GramParser(Lexer lexicalParser) {
        this.lexer = lexicalParser;
    }


    public static void main(String[] args) {
        Lexer lexer = new Lexer("E:\\desktop\\MyCMMInterpreter\\test_func.cmm");
        lexer.loadSourceCode();
        lexer.loadTokenList();
        GramParser parser = new GramParser(lexer);
        try {
            parser.startParse();
        }  catch (GramException e) {
            System.out.println("语法分析错误！" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 从lexer获取下一个token
     */
    private void getNextToken() {
        do {
            curToken = getNextTokenFromLexer();
        }while (curToken.getType()== TokenType.SINGLE_LINE_COMMENT ||
                curToken.getType() == TokenType.MULTIPLE_LINE_COMMENT ||
                curToken == null);
    }

    private Token getNextTokenFromLexer() {
        if (tokenPtr >= lexer.tokenList.size()-1) {
            return new Token(TokenType.EOF);
        }
        tokenPtr++;
        return lexer.tokenList.get(tokenPtr);
    }

    /**
     * 开始语法分析
     */
    public void startParse() throws GramException {
        while (true) {
            //TreeNode node = parseStatement(false, true);
            TreeNode node = parseFunction();
            if(node.getType() == TreeNodeType.NULL) {
                break;
            }
            treeNodes.add(node);
            getNextToken();
        }
         System.out.println("语法分析结束");
        if (!ifSuccess) {
            // 报告错误
            System.out.println("语法分析错误！");
            System.out.println(errInfoBuffer.toString());
        } else {
            System.out.println("语法分析成功");
            for(int i = 0;i<treeNodes.size(); i++) {
                System.out.printf("第%d棵语法树:\n", i+1);
                System.out.println(TreeNode.getLevelOrderString(treeNodes.get(i)));
            }
        }

    }

    /**
     * 解析函数
     */
    private TreeNode parseFunction() throws GramException {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.NULL);
        if (curToken == null) {
            getNextToken();
        }
        if (!checkToken(TokenType.FUNC)) {
            // 不符合函数定义
            return node;
        }
        // 函数定义
        node.setType(TreeNodeType.FUNCTION);
        // 函数名
        getNextToken();
        String funcName = curToken.getStringValue();
        node.setSymbolName(funcName);
        // 左结点: 函数签名
        node.left = parseFuncSignature();
        // 右结点: 具体函数实现的语句块
        // todo 考虑为封装成函数语句块
        node.right = parseStatementBlock(true);
        definedFuncs.add(new FuncDeclaration(funcName, node.left.left.getArgList(), node.left.right.getType()));
        return node;
    }

    /**
     * 解析函数签名
     */
    private TreeNode parseFuncSignature() {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.FUNC_SIGN);
        // 左结点: 参数列表
        TreeNode left =  new TreeNode();
        left.setType(TreeNodeType.ARGS);
        left.setArgList(parseArgList(true));
        node.left = left;
        // 右结点: 返回值
        getNextToken();
        TreeNode right = new TreeNode();
        if(curToken.getType()!= TokenType.EOF) {
            switch (curToken.getType()) {
                case VOID:
                    right.setType(TreeNodeType.VOID);
                    break;
                case INT:
                    right.setType(TreeNodeType.INT_DECLARATION);
                    break;
                case REAL:
                    right.setType(TreeNodeType.REAL_DECLARATION);
                    break;
                case CHAR:
                    right.setType(TreeNodeType.CHAR_DECLARATION);
                    break;
                default:
                    // 其他类型token不能作为返回值
                    wrongReturnValueTypeException(curToken.getLineNum());
                    break;
            }
        } else {
            unexpectedEOFException(curToken.getLineNum());
        }
        node.right = right;
        return node;
    }

    /**
     * 解析参数列表
     * 从左括号开始
     * @param needNext 是否需要下一个token
     */
    private List<TreeNode> parseArgList(boolean needNext) {
        List<TreeNode> treeNodeList = new ArrayList<>();
        if (needNext) {
            matchTokenNext(TokenType.L_PARENTHESIS, true);
        } else {
            matchToken(TokenType.L_PARENTHESIS, true);
        }
        getNextToken();
        TreeNode node;
        // 参数类型和参数名
        loop:
        while (curToken.getType() != TokenType.R_PARENTHESIS) {
            node = new TreeNode();
            switch (curToken.getType()) {
                case INT:
                    node.setType(TreeNodeType.INT_DECLARATION);
                    break;
                case REAL:
                    node.setType(TreeNodeType.REAL_DECLARATION);
                    break;
                case CHAR:
                    node.setType(TreeNodeType.CHAR_DECLARATION);
                    break;
                default:
                    wrongArgTypeException(curToken.getLineNum());
                    break loop;
            }
            if (curToken.getType() == TokenType.EOF) {
                expectedException(TokenType.R_BRACE, curToken.getType(), curToken.getLineNum(), false);
                break;
            }
            // 设置形参名
            getNextToken();
            if (curToken.getType()== TokenType.IDENTIFIER) {
                node.setSymbolName(curToken.getStringValue());
                treeNodeList.add(node);
            } else {
                wrongArgTypeException(curToken.getLineNum());
                break;
            }
            // 再匹配一个逗号
            getNextToken();
            if (curToken.getType()!=TokenType.COMMA) {
                if (curToken.getType() == TokenType.R_PARENTHESIS) {
                    // 结束
                    break;
                } else {
                    // 缺少逗号, 进入错误恢复
                    recover(FIRST_STATEMENT, FOLLOW_FUNCTION_ARGS);
                }
            } else {
                // 是逗号, 还有下一个参数
                getNextToken();
            }
        }
        matchToken(TokenType.R_PARENTHESIS, true);
        return treeNodeList;
    }

    /**
     * 解析单条语句
     * @param isRecursive 是否处于在递归调用中
     *                    是的话，得到的结点不会加入最终的List中，而会直接返回
     *                    不是的话，得到的结点先加入最终的List中，再返回
     * @param needNext 是否需要下一个token
     */
    private TreeNode parseStatement(boolean isRecursive, boolean needNext) throws  GramException {
        if(needNext || curToken == null ) {
            getNextToken();
        }
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.NULL);
        if(curToken.getType()!= TokenType.EOF) {
            switch (curToken.getType()) {
                case INT:
                    node = parseDeclarationStatement(TokenType.INT);
                    break;
                case CHAR:
                    node = parseDeclarationStatement(TokenType.CHAR);
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
                case SCAN:
                    node = parseScanStatement();
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
                case BREAK:
                    node = parseBreak();
                    break;
                case CONTINUE:
                    node = parseContinue();
                    break;
                case RETURN:
                    node = parseReturn();
                    break;
                case SEMICOLON:
                    // 空语句
                    node.setType(TreeNodeType.EMPTY);
                    break;
                default:
                    throw new GramException("Unexpected token "+ curToken+ "at line " + curToken.getLineNum());
            }
        }
        if(!isRecursive && node.getType()!= TreeNodeType.NULL && node.getType() != TreeNodeType.EMPTY) {
            // 当不需要递归, 且结点类型不为空时
            // 单条语句解析完成
            treeNodes.add(node);
        }

        return node;
    }

    /**
     * 解析返回语句
     */
    private TreeNode parseReturn() {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.RETURN);
        // left: 返回值或变量
        TreeNode left = new TreeNode();
        left.setType(TreeNodeType.VOID);
        getNextToken();
        // 如果return; 默认返回值类型是void
        switch (curToken.getType()) {
            case IDENTIFIER:
                left.setType(TreeNodeType.IDENTIFIER);
                left.setSymbolName(curToken.getStringValue());
                break;
            case INT_LITERAL:
                left.setType(TreeNodeType.INT_LITERAL);
                left.setIntValue(curToken.getIntValue());
                break;
            case REAL_LITERAL:
                left.setType(TreeNodeType.REAL_LITERAL);
                left.setRealValue(curToken.getRealValue());
                break;
                // todo 若返回一个表达式
            default:

        }
        node.left = left;
        if (curToken.getType()==TokenType.SEMICOLON) {
            tokenPtr--;
        }
        matchTokenNext(TokenType.SEMICOLON, true);
        return node;
    }


    /**
     * 解析continue
     */
    private TreeNode parseBreak() {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.BREAK);
        matchTokenNext(TokenType.SEMICOLON, true);
        return node;
    }

    /**
     * 解析continue
     */
    private TreeNode parseContinue() {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.CONTINUE);
        matchTokenNext(TokenType.SEMICOLON, true);
        return node;
    }

    /**
     * 解析语句块
     * @param needNext 是否需要匹配掉下一个{
     */
    private TreeNode parseStatementBlock(boolean needNext) throws GramException {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.STATEMENT_BLOCK);
        if(needNext) {
            matchTokenNext(TokenType.L_BRACE, true);
        } else {
            matchToken(TokenType.L_BRACE);
        }
        // 跳过{
        getNextToken();
        while (!checkToken(TokenType.R_BRACE)) {
            // 把每条语句存储到结点中
            node.addStatement(parseStatement(true,false));
            getNextToken();
            if (curToken.getType() == TokenType.EOF) {
                expectedException(TokenType.R_BRACE, curToken.getType(), curToken.getLineNum(), false);
                break;
            }
        }
        return node;

    }



    /**
     * 解析声明语句
     */
    private TreeNode parseDeclarationStatement(TokenType type) throws GramException {
        TreeNode node = new TreeNode();
        boolean isArray = false;
        matchTokenNext(TokenType.IDENTIFIER);
        TreeNode left = new TreeNode();
        // 左孩子设为标识符的名字
        left.setType(TreeNodeType.IDENTIFIER);
        left.setSymbolName(curToken.getStringValue());
        node.left = left;
        getNextToken();
        if(checkToken(TokenType.ASSIGN)) {
            // 赋值语句
            // 右边是算术表达式
            node.right = parseArithmeticExpression();
            matchToken(TokenType.SEMICOLON, true);
        } else if(checkToken(TokenType.L_BRACKET)) {
            // 如果后面接的是[
            // 则是数组
            isArray = true;
            node.right = parseArithmeticExpression();
            matchToken(TokenType.R_BRACKET);
            matchTokenNext(TokenType.SEMICOLON, true);
        } else if(checkToken(TokenType.SEMICOLON)) {
            // 单纯的声明变量
        } else {
            // 都不是，进入错误恢复
            recover(FOLLOW_STATEMENT, FIRST_STATEMENT);
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

                break;
            case CHAR:
                if(isArray) {
                    // todo 字符串数组
                    node.setType(TreeNodeType.INT_ARRAY_DECLARATION);
                } else {
                    node.setType(TreeNodeType.CHAR_DECLARATION);
                }
                break;
        }

        return node;
    }

    /**
     * 语法层恢复
     * @param s1 需要的集合
     * @param s2 补救的集合
     */
    private void recover(HashSet<TokenType> s1, HashSet<TokenType> s2) {
        String err = String.format("Unexpected token %s at line %d",
                curToken, curToken.getLineNum());
        ifSuccess = false;
        errInfoBuffer.append(err).append("\n");
        while (!s1.contains(curToken.getType())) {
            if (s2.contains(curToken.getType())){
                tokenPtr--;
                break;
            }
            getNextToken();
        }
    }

    /**
     * 解析赋值语句
     */
    private TreeNode parseAssignStatement() throws GramException {
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
                matchToken(TokenType.SEMICOLON, true);
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
                matchToken(TokenType.SEMICOLON, true);
                break;
            default:
                // 两个都不是
                // 进入错误恢复
                recover(FOLLOW_STATEMENT, FIRST_STATEMENT);

        }
        return node;
    }

    /**
     * 解析输出语句
     */
    private TreeNode parsePrintStatement() {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.PRINT);
        TreeNode left = new TreeNode();
        left.setType(TreeNodeType.IDENTIFIER);
        matchTokenNext(TokenType.IDENTIFIER);
        left.setSymbolName(curToken.getStringValue());
        node.left = left;
        matchTokenNext(TokenType.SEMICOLON, true);

        return node;
    }

    /**
     * 解析输入语句
     */
    private TreeNode parseScanStatement() {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.SCAN);
        TreeNode left = new TreeNode();
        left.setType(TreeNodeType.IDENTIFIER);
        matchTokenNext(TokenType.IDENTIFIER);
        left.setSymbolName(curToken.getStringValue());
        node.left = left;
        matchTokenNext(TokenType.SEMICOLON, true);

        return node;
    }


    /**
     * 解析if语句
     */
    private TreeNode parseIfStatement() throws GramException {
        TreeNode node = new TreeNode();
        node.setType(TreeNodeType.IF);
        // bool表达式
        matchTokenNext(TokenType.L_PARENTHESIS, true);
        node.setCondition(parseRelationalExpression());
        matchToken(TokenType.R_PARENTHESIS, true);
        // 满足条件的语句块
        node.left = parseStatementBlock(true);
        while (true) {
            if (checkTokenNext(TokenType.ELSE)) {
                // 向后看一个判断是else if还是else
                if (checkTokenNext(TokenType.IF)) {
                    // 是else if
                    TreeNode elseIf= new TreeNode();
                    elseIf.setType(TreeNodeType.ELSE_IF);
                    // 匹配(
                    matchTokenNext(TokenType.L_PARENTHESIS);
                    elseIf.setCondition(parseRelationalExpression());
                    // 匹配)
                    matchToken(TokenType.R_PARENTHESIS, true);
                    elseIf.left = parseStatementBlock(true);
                    node.addStatement(elseIf);
                } else {
                    // 是else
                    node.right = parseStatementBlock(false);
                    break;
                }
            } else {
                // 回退
                tokenPtr--;
                break;
            }
        }
        return node;
    }

    /**
     * 解析while语句
     */
    private TreeNode parseWhileStatement() throws GramException {
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
    private TreeNode parseArithmeticExpression() throws GramException {
        List<Token> tokens = getAllExpressionTokens();
        return parseArithmeticExpression(tokens);
    }

    /**
     * 根据token列表解析算术表达式
     * 不做运算，只解析成对应的树
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
        // 前一个token是否为操作符
        boolean isPrevOperator = false;
        // 用来判断下一个操作数是不是负的
        boolean isNegative = false;
        while (true){
            if(iterator.hasNext()) {
                Token token = iterator.next();
                if (checkTokenOperand(token)) {
                    isPrevOperator = false;
                    // token是操作数
                    if (iterator.hasNext()) {
                        // 操作数后还有符号
                        if (iterator.next().getType() != TokenType.L_BRACKET) {
                            operandStack.push(tokenToTreeNode(token, isNegative));
                            isNegative = false;
                            // 指针回退一格
                            iterator.previous();
                        } else {
                            // 记录用于数组的中括号
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
                        // 操作数后还有符号
                        // 当前是最后一个token
                        // 直接入栈
                        operandStack.push(tokenToTreeNode(token, isNegative));
                    }
                } else if (checkTokenArithmeticOperator(token)) {
                    // token是运算符
                    if (operatorStack.empty()) {
                        if (token.getType()== TokenType.MINUS && operandStack.empty()) {
                            // 可能一开始就是负号
                            isNegative = true;
                        } else if (operandStack.empty()) {
                            // 算术表达式一开始不能有其他符号
                            wrongArithmeticExpException(token.getLineNum());
                            // todo
                            return null;
                        } else {
                            operatorStack.push(tokenToTreeNode(token));
                            isPrevOperator = true;
                        }
                    } else {
                        // 取得前一个操作符
                        TreeNode preOperator = operatorStack.peek();

                        if (token.getType()==TokenType.MINUS && isPrevOperator) {
                            // 设置负号
                            isNegative = !isNegative;
                            continue;
                        }
                        // 取得当前操作符
                        TreeNode curOperator = tokenToTreeNode(token);

                        if (priorityCompare(preOperator, curOperator)) {
                            // 当前操作符优先级更高
                            Token next = iterator.next();
                            TreeNode curOperand;
                            if (!checkTokenLParenth(next)) {
                                // 下一个token不是左括号
                                curOperand = tokenToTreeNode(next, isNegative);
                                // 消耗掉负号
                                isNegative = false;
                            } else {
                                // 下一个token是左括号
                                // 则从后往前遍历找右括号
                                curOperand = parseExpInParenthesis(iterator, tokens);
                            }
                            // 由于当前操作符优先级更高
                            // 需要先计算当前操作符
                            TreeNode previousOperand = operandStack.pop();
                            // 把当前操作符与(前一个操作数和当前操作数)结合起来
                            curOperator.right = curOperand;
                            curOperator.left = previousOperand;
                            operandStack.push(curOperator);
                            isPrevOperator = false;
                        } else {
                            // 前一个操作符优先级更高或一样
                            // 则取出前一个操作符和前两个操作数进行结合
                            TreeNode operand1 = operandStack.pop();
                            TreeNode operand2 = operandStack.pop();
                            preOperator.left = operand1;
                            preOperator.right = operand2;
                            operandStack.push(preOperator);
                            operatorStack.push(curOperator);
                            // 当前操作符还没有消耗掉
                            isPrevOperator = true;
                        }

                    }
                } else if(token.getType() == TokenType.L_PARENTHESIS) {
                    // token是左括号
                    TreeNode operand = parseExpInParenthesis(iterator,tokens);
                    operandStack.push(operand);
                } else {
                    throw new GramException("Invalid arithmetic expression at line " + token.getLineNum());
                }
            } else  {
                // token处理完毕
                if(operandStack.size() > 1) {
                    // 当操作数栈中数量大于1时
                    TreeNode operand1 = operandStack.pop();
                    TreeNode operand2 = operandStack.pop();
                    TreeNode operator = operatorStack.pop();
                    operator.left = operand1;
                    operator.right = operand2;
                    operandStack.push(operator);
                } else {
                    // 栈中只剩一个结点
                    // 即形成了完整的一棵表达式树, 结束
                    break;
                }
            }

        }

        return operandStack.peek();
    }

    /**
     * 获取表达式所有词法单元
     * 同时检查括号个数、类型的匹配
     * @return token序列
     */
    private List<Token> getAllExpressionTokens() {
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
                        parenthMismatchException(curToken.getLineNum());
                    }
                    break;
                case L_BRACKET:
                    tokens.add(curToken);
                    s2.push('[');
                    break;
                case R_BRACKET:
                    if(s2.empty()) {
                        // 如果栈为空遇到右括号，可能是在if的条件中
                        // 则表达式结束
                        break loop;
                    } else if(s2.peek() == '[') {
                        // 括号只能一一匹配
                        // 不能 ([)]
                        s2.pop();
                        tokens.add(curToken);
                    } else {
                        parenthMismatchException(curToken.getLineNum());
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
    private TreeNode parseRelationalExpression() throws GramException {
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
            case GREATER:
                node.setType(TreeNodeType.GREATER);
                break;
            case GREATER_EQ:
                node.setType(TreeNodeType.GREATER_EQ);
                break;
            case LESS_EQ:
                node.setType(TreeNodeType.LESS_EQ);
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
    private void matchTokenNext (TokenType type) {
        getNextToken();
        matchToken(type);
    }

    /**
     * 用预期类型匹配下一个token
     */
    private void matchTokenNext (TokenType type, boolean ifAmend)  {
        getNextToken();
        matchToken(type, ifAmend);
    }

    /**
     * 用预期类型匹配当前token
     */
    private void matchToken(TokenType type) {
        if(curToken.getType() != type) {
            expectedException(type, curToken.getType(), curToken.getLineNum(), false);
        }
    }

    /**
     * 用预期类型匹配当前token
     * @param type 预期类型
     * @param ifAmend 是否进行短语层恢复
     */
    private void matchToken(TokenType type, boolean ifAmend) {
        if(curToken.getType() != type) {
            expectedException(type, curToken.getType(), curToken.getLineNum(), ifAmend);
        }
    }

    /**
     * 检查下一个token是否为预期类型
     * @param type 预期类型
     */
    private boolean checkTokenNext (TokenType type) {
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
     * 匹配然后前进
     * @param type 匹配掉的类型
     */
    private void matchAndAdvance(TokenType type) throws GramException {
        matchToken(type);
        getNextToken();
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
     * 将词法单元转换为语法树的结点
     * @param token 词法单元
     * @return 语法树结点
     */
    private TreeNode tokenToTreeNode(Token token, boolean isNegative) {
        TreeNode node = new TreeNode();
        switch (token.getType()) {
            case IDENTIFIER:
                node.setType(TreeNodeType.IDENTIFIER);
                node.setSymbolName(token.getStringValue());
                node.setNegative(isNegative);
                break;
            case INT_LITERAL:
                node.setType(TreeNodeType.INT_LITERAL);
                node.setIntValue(token.getIntValue());
                node.setNegative(isNegative);
                break;
            case REAL_LITERAL:
                node.setType(TreeNodeType.REAL_LITERAL);
                node.setRealValue(token.getRealValue());
                node.setNegative(isNegative);
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
    private boolean checkTokenLParenth(Token token) {
        return token.getType() == TokenType.L_PARENTHESIS;
    }

    /**
     * 检查词法单元是否为参数类型
     * @param token 待检查词法单元
     */
    private boolean checkTokenArgType(Token token) {
        TokenType type = token.getType();
        return type == TokenType.INT
                || type == TokenType.REAL
                || type == TokenType.CHAR;

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
        // todo 此处不太安全
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

    /**
     * 根据小括号解析表达式
     */
    private TreeNode parseExpInParenthesis(ListIterator<Token> iterator,
                                           List<Token> tokens) throws GramException {

        Stack<Character> parenths = new Stack<>();
        // 记录小括号开始的位置
        int start = iterator.previousIndex();
        int end;
        parenths.push('(');
        Token aim;
        while (!parenths.empty()) {
            if (!iterator.hasNext()) {
                // 内容结束，小括号没匹配完
                parenthMismatchException(curToken.getLineNum());
                break;
            }
            aim = iterator.next();
            if (aim.getType() == TokenType.L_PARENTHESIS) {
                parenths.push('(');
            } else if (aim.getType() == TokenType.R_PARENTHESIS) {
                parenths.pop();
            }
        }
        end = iterator.nextIndex();
        return parseArithmeticExpression(tokens.subList(start + 1, end - 1));
    }


    /**
     * 未找到期望的token类型异常
     * @param expectedType 期望类型
     * @param foundType 找到类型
     * @param lineNum 出现的行号
     * @param ifAmend 是否进行短语层恢复
     */
    private void expectedException(TokenType expectedType, TokenType foundType, int lineNum, boolean ifAmend) {
            String err = String.format("Expected %s, found %s at line %d",
                    expectedType, foundType, lineNum);
            ifSuccess = false;
            errInfoBuffer.append(err).append("\n");
            if (ifAmend) {
                tokenPtr--;
            }
    }

    /**
     * 小括号不匹配异常
     */
    private void parenthMismatchException(int lineNum) {
        ifSuccess = false;
        errInfoBuffer.append("Parenthesis mismatch at line ").append(lineNum).append("\n");
    }

    /**
     * 算术表达式错误异常
     */
    private void wrongArithmeticExpException(int lineNum) {
        ifSuccess = false;
        errInfoBuffer.append("Wrong arithmetic expression at line ").append(lineNum).append("\n");
    }

    /**
     * 文件尾异常
     */
    private void unexpectedEOFException(int lineNum) {
        ifSuccess = false;
        errInfoBuffer.append("Unexpected EOF at line ").append(lineNum).append("\n");
    }

    /**
     * 返回值类型错误
     */
    private void wrongReturnValueTypeException(int lineNum) {
        ifSuccess = false;
        errInfoBuffer.append("Wrong return value type at line ").append(lineNum).append("\n");
    }

    /**
     * 函数参数类型错误
     */
    private void wrongArgTypeException(int lineNum) {
        ifSuccess = false;
        errInfoBuffer.append("Wrong argument type at line ").append(lineNum).append("\n");
    }
    public List<TreeNode> getTreeNodes() {
        return treeNodes;
    }


}
