package semantics;

import exception.ExecutionException;
import exception.GramException;
import exception.LexException;
import exception.SemanticException;
import gram.GramParser;
import lex.Lexer;
import symbols.SymValueType;
import symbols.Symbol;
import symbols.SymbolTable;

import java.util.*;

/**
 * @description 执行四元式代码的解释器
 * @author FANG
 * @date 2019/10/27 14:10
 **/
public class Interpreter {
    // 程序计数器
    private int instrIndex = 0;
    // 四元组形式的中间代码
    private List<Quadruple> codes;
    // 符号表
    private SymbolTable symbolTable = new SymbolTable();
    // 代码块层级
    private int blockLevel = 0;
    // 每一层代码块的临时变量表
    private Map<Integer, List<String>> tempVars = new HashMap<>();

    // 操作数是整数还是实数
    private boolean isFirstOperandInt;
    private boolean isSecondOperandInt;

    private Symbol condition;


    public static void main(String[] args) {
        Lexer lexer = new Lexer("E:\\desktop\\MyCMMInterpreter\\test.cmm");
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
        }catch (SemanticException e){
            System.out.println("语义分析错误！" + e.getMessage());
            return;
        }

        // 输出中间代码-四元式表示
        System.out.println(generator.getFormattedCodes());
        // 将中间代码输入解释器执行
        Interpreter interpreter = new Interpreter(generator.getCodes());
        try {
            interpreter.run();
        } catch (ExecutionException e) {
            System.out.println("执行期间错误！" + e.getMessage());
        }

        System.out.println("----------------");
        System.out.println("解释器执行完毕");
    }


    public Interpreter(List<Quadruple> codes) {
        List<String> level0 = new ArrayList<>();
        tempVars.put(0,level0);
        this.codes = codes;
    }

    public void run() throws ExecutionException {
        // 执行每一行中间代码
        while(instrIndex < codes.size()) {
            Quadruple code = codes.get(instrIndex);
            switch (code.operation) {
                case CodeConstant.JMP_WITH_CONDITION:
                    jumpWithCondition(code);
                    break;
                case CodeConstant.JMP:
                    jump(code);
                    break;
                case CodeConstant.PRINT:
                    print(code);
                    break;
                case CodeConstant.IN:
                    in();
                    break;
                case CodeConstant.OUT:
                    out();
                    break;
                case CodeConstant.INT:
                case CodeConstant.REAL:
                    declaration(code);
                    break;
                case CodeConstant.INT_ARR:
                case CodeConstant.REAL_ARR:
                    array(code);
                    break;
                case CodeConstant.ARR_ACC:
                    arrayAccess(code);
                    break;
                case CodeConstant.PLUS:
                case CodeConstant.MINUS:
                case CodeConstant.MUL:
                case CodeConstant.DIV:
                    arithmeticOperation(code);
                    break;
                case CodeConstant.LE:
                case CodeConstant.EQ:
                case CodeConstant.NEQ:
                    relationOperation(code);
                    break;
                case CodeConstant.ASSIGN:
                    assign(code);
                    break;
                default:
                    throw new ExecutionException("Unexpected code!");
            }
        }
    }

    /**
     * 无条件跳转
     */
    private void jump(Quadruple code) {
        instrIndex = code.jumpLocation;
    }

    /**
     * 条件为假时跳转
     */
    private void jumpWithCondition(Quadruple code) {
        if(condition.getType() == SymValueType.FALSE) {
            instrIndex = code.jumpLocation;
        }
        else {
            nextInstruction();
        }
        condition = null;
    }


    /**
     * 向控制台输出
     */
    private void print(Quadruple code) {
        Symbol symbol = getSymbol(code.dest);
        if(symbol.getType() == SymValueType.INT) {
            System.out.println(symbol.getIntValue());
        } else if(symbol.getType() == SymValueType.REAL) {
            System.out.println(symbol.getRealValue());
        }
        nextInstruction();
    }

    /**
     * 进入语句块
     */
    private void in() {
        blockLevel++;
        // 初始化对应的临时变量列表
        List<String> vars = new ArrayList<>();
        tempVars.put(blockLevel, vars);
        nextInstruction();
    }

    /**
     * 退出语句块
     */
    private void out() {
        symbolTable.deleteSymbols(tempVars.get(blockLevel));
        tempVars.remove(blockLevel);
        blockLevel--;
        nextInstruction();
    }

    /**
     * 访问数组
     */
    private void arrayAccess(Quadruple code) {
        Symbol symbol = new Symbol(code.dest);
        int index = 0;
        switch (code.secondOperandType) {
            case IDENTIFIER:
                index = symbolTable.getSymbol(code.secondOperandName).getIntValue();
                break;
            case INT_LITERAL:
                index = code.secondOperandIntLiteral;
                break;
        }
        Symbol array = symbolTable.getSymbol(code.firstOperandName);
        symbol.setArrName(array.getName());
        symbol.setIndex(index);
        if(array.getType() == SymValueType.INT_ARRAY) {
            symbol.setType(SymValueType.INT_ARRAY_ELEMENT);
            symbol.setIntValue(array.getIntArray()[index]);
        } else if(array.getType() == SymValueType.REAL_ARRAY) {
            symbol.setType(SymValueType.REAL_ARRAY_ELEMENT);
            symbol.setRealValue(array.getRealArray()[index]);
        }
        addTempSymbol(symbol);
        nextInstruction();
    }

    /**
     * 算术操作
     */
    private void arithmeticOperation (Quadruple code) throws ExecutionException {
        double operand1 = getFirstOperand(code);
        double operand2 = getSecondOperand(code);
        Symbol symbol = new Symbol(code.dest);
        switch (code.operation) {
            case CodeConstant.PLUS:
                if (isFirstOperandInt && isSecondOperandInt) {
                    symbol.setIntValue((int) operand1 + (int) operand2);
                    symbol.setType(SymValueType.INT);
                } else {
                    symbol.setRealValue(operand1 + operand2);
                    symbol.setType(SymValueType.REAL);
                }
                break;
            case CodeConstant.MINUS:
                if (isFirstOperandInt && isSecondOperandInt) {
                    symbol.setIntValue((int) operand1 - (int) operand2);
                    symbol.setType(SymValueType.INT);
                } else {
                    symbol.setRealValue(operand1 - operand2);
                    symbol.setType(SymValueType.REAL);
                }
                break;
            case CodeConstant.MUL:
                if (isFirstOperandInt && isSecondOperandInt) {
                    symbol.setIntValue((int) operand1 * (int) operand2);
                    symbol.setType(SymValueType.INT);
                } else {
                    symbol.setRealValue(operand1 * operand2);
                    symbol.setType(SymValueType.REAL);
                }
                break;
            case CodeConstant.DIV:
                // 注意是第一操作数是除数
                // 第二操作数是被除数
                if (isFirstOperandInt && isSecondOperandInt) {
                    if((int)operand2 == 0) {
                        throw new ExecutionException("Cannot divide by zero!");
                    }
                    symbol.setIntValue((int) operand2 / (int) operand1);
                    symbol.setType(SymValueType.INT);
                } else {
                    // 浮点数精度小数点后10位
                    if(Math.abs(operand2) < 1e-10) {
                        throw new ExecutionException("Cannot divide by zero!");
                    }
                    symbol.setRealValue(operand2 / operand1);
                    symbol.setType(SymValueType.REAL);
                }
                break;
        }
        addTempSymbol(symbol);
        nextInstruction();
    }

    /**
     * 关系运算
     */
    private void relationOperation(Quadruple code) {
        double operand1 = getFirstOperand(code);
        double operand2 = getSecondOperand(code);
        Symbol symbol = new Symbol(code.dest);
        switch (code.operation) {
            case CodeConstant.LE:
                if(operand1 < operand2) {
                    symbol.setType(SymValueType.TRUE);
                } else {
                    symbol.setType(SymValueType.FALSE);
                }
                break;
            case CodeConstant.EQ:
                if(operand1 == operand2) {
                    symbol.setType(SymValueType.TRUE);
                } else {
                    symbol.setType(SymValueType.FALSE);
                }
                break;
            case CodeConstant.NEQ:
                if(operand1!=operand2) {
                    symbol.setType(SymValueType.TRUE);
                } else {
                    symbol.setType(SymValueType.FALSE);
                }
        }
        condition = symbol;
        nextInstruction();
    }

    /**
     * 赋值
     */
    private void assign(Quadruple code) {
        Symbol left = symbolTable.getSymbol(code.dest);
        double right = getFirstOperand(code);
        Symbol array;
        if(left.getType() == SymValueType.INT_ARRAY_ELEMENT) {
            array = symbolTable.getSymbol(left.getArrName());
            array.getIntArray()[left.getIndex()] = (int)right;
            left.setIntValue((int)right);
        } else if(left.getType() == SymValueType.REAL_ARRAY_ELEMENT) {
            array = symbolTable.getSymbol(left.getArrName());
            array.getRealArray()[left.getIndex()] = right;
            left.setRealValue(right);
        } else {
            switch (left.getType()) {
                case INT:
                    // 以变量类型进行类型转换
                    left.setIntValue((int)right);
                    break;
                case REAL:
                    left.setRealValue(right);
                    break;
            }
        }
        nextInstruction();
    }

    /**
     * 声明变量
     */
    private void declaration(Quadruple code) {
        Symbol symbol = new Symbol(code.dest);
        double right = getFirstOperand(code);
        switch (code.operation) {
            case CodeConstant.INT:
                symbol.setType(SymValueType.INT);
                if (code.firstOperandType != OperandType.NULL) {
                    symbol.setIntValue((int) right);
                }
                break;
            case CodeConstant.REAL:
                symbol.setType(SymValueType.REAL);
                if (code.firstOperandType != OperandType.NULL) {
                    symbol.setRealValue(right);
                }
                break;
        }
        // 添加到当前语句块的变量列表
        addTempSymbol(symbol);
        nextInstruction();
    }

    /**
     * 数组
     */
    private void array(Quadruple code) throws ExecutionException {
        Symbol symbol = new Symbol(code.dest);
        double length = getFirstOperand(code);
        if (length < 0) {
            throw new ExecutionException("Array length less than 1!");
        }
        switch (code.operation) {
            case CodeConstant.INT_ARR:
                symbol.setIntArray(new int[(int)length]);
                symbol.setType(SymValueType.INT_ARRAY);
                break;
            case CodeConstant.REAL_ARR:
                symbol.setRealArray(new double[(int)length]);
                symbol.setType(SymValueType.REAL_ARRAY);
                break;
        }
        addTempSymbol(symbol);
        nextInstruction();
    }

    /**
     * 根据符号名取出对应符号
     */
    private Symbol getSymbol(String name) {
        return symbolTable.getSymbol(name);
    }

    /**
     * 在当前块层次添加临时变量
     */
    private void addTempSymbol(Symbol symbol) {
        symbolTable.addSymbol(symbol);
        tempVars.get(blockLevel).add(symbol.getName());
    }

    /**
     * 移至下一条指令位置
     */
    private void nextInstruction() {
        instrIndex++;
    }


    /**
     * 从四元组中获得第一个操作数
     */
    private double getFirstOperand(Quadruple code) {
        if(code.firstOperandType == OperandType.INT_LITERAL) {
            isFirstOperandInt = true;
            return (double)code.firstOperandIntLiteral;
        } else if(code.firstOperandType == OperandType.REAL_LITERAL) {
            return code.firstOperandRealLiteral;
        } else {
            // 是临时变量，在符号表中查找
            Symbol symbol = symbolTable.getSymbol(code.firstOperandName);
            // todo 查找不到符号后的处理
            if (symbol.getType() == SymValueType.INT ||
                    symbol.getType() == SymValueType.INT_ARRAY_ELEMENT) {
                isFirstOperandInt = true;
                return (double) symbol.getIntValue();
            } else{
                return symbol.getRealValue();
            }
        }
    }

    /**
     * 从四元组中获得第二个操作数
     */
    private double getSecondOperand(Quadruple code) {
        if(code.secondOperandType == OperandType.INT_LITERAL) {
            isSecondOperandInt = true;
            return (double)code.secondOperandIntLiteral;
        } else if(code.secondOperandType == OperandType.REAL_LITERAL) {
            return code.firstOperandRealLiteral;
        } else {
            Symbol symbol = symbolTable.getSymbol(code.secondOperandName);
            if(symbol.getType() == SymValueType.INT||symbol.getType() == SymValueType.REAL_ARRAY_ELEMENT) {
                isSecondOperandInt = true;
                return (double)symbol.getIntValue();
            } else {
                return symbol.getRealValue();
            }
        }
    }
}
