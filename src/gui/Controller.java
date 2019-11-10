package gui;

import exception.ExecutionException;
import exception.SemanticException;
import gram.GramParser;
import gram.TreeNode;
import gram.TreeNodeType;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lex.Lexer;
import lex.Token;
import semantics.InterGenerator;
import semantics.Interpreter;

import javafx.scene.control.TextArea;
import java.io.File;

public class Controller {
    // 词法分析器
    Lexer lexer;
    // 语法分析器
    GramParser parser;
    // 语义分析中间代码生成器
    InterGenerator generator;
    // 解释执行器
    Interpreter interpreter;

    // 源文件路径
    private String filepath;

    public TextArea txa_lex_result;
    public TextArea txa_gram_result;
    public TextArea txa_semantic_result;
    public TreeView tree_syntax;
    public CheckBox ckb_optimized;

    /**
     * 载入源文件
     */
    public void loadSourceCode(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CMM", "*.cmm"));
        File file = fileChooser.showOpenDialog((Stage) ((Node) event.getSource()).getScene().getWindow());
        filepath = file.toString();
    }

    /**
     * 进行词法分析
     */
    public void doLexAnalysis(ActionEvent event) {
        if (filepath==null) {
            return;
        }
        txa_lex_result.setText("");
        lexer = new Lexer(filepath);
        lexer.loadSourceCode();
        lexer.loadTokenList();
        if (!lexer.ifSuccess) {
            // 报告错误
            appendLexResult(lexer.errInfoBuffer.toString());
        }else {
            for (Token token:lexer.tokenList) {
                appendLexResult(token.toString());
            }
            appendLexResult("无错误发生！");
        }
    }

    /**
     * 进行语法分析
     */
    public void doGrammarParse(ActionEvent event) {
        if (lexer==null) {
            return;
        }
        parser = new GramParser(lexer);
        try {
            parser.startParse();
        }  catch (Exception e) {
            e.printStackTrace();
        }
        if (!parser.ifSuccess) {
            // 报告错误
            appendGramResult(parser.errInfoBuffer.toString());
        } else {
            // 生成语法树
            showSyntaxTree();
        }
    }


    /**
     * 进行语义分析
     * 生成中间代码
     */
    public void doSemantics(ActionEvent event) {
        generator = new InterGenerator(parser);
        boolean ifOptimized = ckb_optimized.isSelected();
        txa_semantic_result.setText("");
        try {
            // 是否开启优化
            generator.setOptimEnabled(ifOptimized);
            generator.start();
            // 输出函数入口地址
            appendSemanticResult("函数入口地址");
            appendSemanticResult(generator.funcInstrMap.toString());
            // 输出函数参数类型
            appendSemanticResult("函数参数类型");
            appendSemanticResult(generator.funcArgTypeMap.toString());
            // 输出中间代码-四元式表示
            appendSemanticResult(generator.getFormattedCodes());
        }catch (SemanticException e){
            appendSemanticResult("语义分析错误！" + e.getMessage());
        }

    }


    /**
     * 解释执行
     */
    public void execute(ActionEvent event) {
        interpreter = new Interpreter(generator);
        long startTime = System.currentTimeMillis();    //获取开始时间

        try {
            interpreter.run();
        } catch (ExecutionException e) {
            System.out.println("执行期间错误！" + e.getMessage());
        }

        long endTime = System.currentTimeMillis();    //获取结束时间

        System.out.println("----------------");
        System.out.println("解释器执行完毕");
        System.out.printf("执行时间为 %dms\n", endTime-startTime);
    }

    /**
     * 向词法分析结果输出
     */
    private void appendLexResult(String text) {
        txa_lex_result.appendText(text+"\n");
    }

    /**
     * 向语法分析结果输出
     */
    private void appendGramResult(String text) {
        txa_gram_result.setText("");
        txa_gram_result.setVisible(true);
        tree_syntax.setVisible(false);
        txa_gram_result.appendText(text+"\n");
    }

    /**
     * 向语义分析结果输出
     */
    private void appendSemanticResult(String text) {
        txa_semantic_result.appendText(text+"\n");
    }



    /**
     * 生成语法树
     */
    private void showSyntaxTree() {
        txa_gram_result.setVisible(false);
        tree_syntax.setVisible(true);
        TreeItem<String> rootItem = new TreeItem<>("Syntax Trees");
        rootItem.setExpanded(true);
        for (TreeNode treeNode : parser.treeNodes) {
            // 以每个函数为根
            TreeItem<String> funcRoot = new TreeItem<>(treeNode.getType().toString());
            TreeItem<String> funcSign = getFuncSignTree(treeNode.left);
            funcRoot.getChildren().add(funcSign);
            TreeItem<String> funcBlock = getFuncBlockTree(treeNode.right);
            funcRoot.getChildren().add(funcBlock);

            rootItem.getChildren().add(funcRoot);
        }

        tree_syntax.setRoot(rootItem);
    }

    /**
     * 生成函数签名子树
     */
    private TreeItem<String> getFuncSignTree(TreeNode funcSign) {
        TreeItem<String> funcSignTree = new TreeItem<>(funcSign.getType().toString());
        // 左边是参数列表
        TreeItem<String> argListTree = new TreeItem<>(funcSign.left.getType().toString());
        for (TreeNode argNode : funcSign.left.getArgList()) {
            argListTree.getChildren().add(new TreeItem<>(argNode.getType().toString()));
        }
        funcSignTree.getChildren().add(argListTree);
        // 右边是返回值
        funcSignTree.getChildren().add(new TreeItem<>(funcSign.right.getType().toString()));
        return funcSignTree;
    }

    /**
     * 生成函数语句块子树
     */
    private TreeItem<String> getFuncBlockTree(TreeNode stmtBlock) {
        TreeItem<String> stmtBlockTree = new TreeItem<>(stmtBlock.getType().toString());
        TreeItem<String> stmtSubTree;
        // 遍历语句块的每条语句
        for (TreeNode stmtNode : stmtBlock.getStatements()) {
            stmtSubTree = new TreeItem<>(stmtNode.getType().toString());
            // 递归遍历单条语句结点
            traverseStmtNode(stmtSubTree, stmtNode);
            stmtBlockTree.getChildren().add(stmtSubTree);
        }

        return stmtBlockTree;
    }

    /**
     * 递归遍历单条语句结点
     */
    private void traverseStmtNode(TreeItem<String> stmtSubTree, TreeNode stmtNode) {
        if (stmtNode == null) {
            return;
        }
        // 带关系表达式条件单独设置
        if (stmtNode.getType() == TreeNodeType.WHILE || stmtNode.getType() == TreeNodeType.IF) {
            TreeItem<String> conditionTreeItem = new TreeItem<>(
                    stmtNode.getCondition().getType().toString());

            traverseStmtNode(conditionTreeItem, stmtNode.getCondition());

            stmtSubTree.getChildren().add(conditionTreeItem);
        }
        if (stmtNode.left != null) {
            TreeItem<String> leftTreeItem;
            switch (stmtNode.left.getType()) {
                case IDENTIFIER:
                    leftTreeItem = new TreeItem<>(stmtNode.left.getType().toString()+": "
                            +stmtNode.left.getSymbolName());
                    break;
                case INT_LITERAL:
                    leftTreeItem = new TreeItem<>(stmtNode.left.getType().toString()+": "
                            +stmtNode.left.getIntValue());
                    break;
                case REAL_LITERAL:
                    leftTreeItem = new TreeItem<>(stmtNode.left.getType().toString()+": "
                            +stmtNode.left.getRealValue());
                    break;
                case STATEMENT_BLOCK:
                    // 内部语句块再循环递归遍历
                    leftTreeItem = new TreeItem<>(stmtNode.left.getType().toString());
                    TreeItem<String> subTreeItem;
                    for (TreeNode node: stmtNode.left.getStatements()) {
                        subTreeItem = new TreeItem<>(node.getType().toString());
                        traverseStmtNode(subTreeItem, node);
                        leftTreeItem.getChildren().add(subTreeItem);
                    }
                    break;
                default:
                    leftTreeItem = new TreeItem<>(stmtNode.left.getType().toString());
            }
            traverseStmtNode(leftTreeItem, stmtNode.left);
            stmtSubTree.getChildren().add(leftTreeItem);
        }

        if (stmtNode.right != null) {
            TreeItem<String> rightTreeItem;
            // 根据不同类别显示
            switch (stmtNode.right.getType()) {
                case IDENTIFIER:
                    rightTreeItem = new TreeItem<>(stmtNode.right.getType().toString()+": "
                            +stmtNode.right.getSymbolName());
                    break;
                case INT_LITERAL:
                    rightTreeItem = new TreeItem<>(stmtNode.right.getType().toString()+": "
                            +stmtNode.right.getIntValue());
                    break;
                case REAL_LITERAL:
                    rightTreeItem = new TreeItem<>(stmtNode.right.getType().toString()+": "
                            +stmtNode.right.getRealValue());
                    break;
                case STATEMENT_BLOCK:
                    // 内部语句块再循环递归遍历
                    rightTreeItem = new TreeItem<>(stmtNode.right.getType().toString());
                    TreeItem<String> subTreeItem;
                    for (TreeNode node: stmtNode.right.getStatements()) {
                        subTreeItem = new TreeItem<>(node.getType().toString());
                        traverseStmtNode(subTreeItem, node);
                        rightTreeItem.getChildren().add(subTreeItem);
                    }
                    break;
                default:
                    rightTreeItem = new TreeItem<>(stmtNode.right.getType().toString());
            }
            traverseStmtNode(rightTreeItem, stmtNode.right);
            stmtSubTree.getChildren().add(rightTreeItem);
        }
    }

}
