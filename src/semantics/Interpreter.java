package semantics;

import exception.ExecutionException;
import exception.GramException;
import exception.SemanticException;
import gram.GramParser;
import gram.TreeNode;
import gram.TreeNodeType;
import lex.Lexer;
import symbols.SymValueType;
import symbols.Symbol;
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
    // 代码块层级
    private int blockLevel = 0;
    // 每一层代码块的临时变量表
    private Map<Integer, List<String>> tempVars = new HashMap<>();
    // 根据函数名找到入口地址
    private Map<String, Integer> funcInstrMap;
    // main函数出口地址，即执行结束
    private final int MAIN_OUT_ADDR = -1;
    // 函数栈帧
    private Stack<Frame> stackFrames = new Stack<>();
    // 参数列表
    private List<TreeNode> argsList = new ArrayList<>();
    // 返回值
    private Symbol retValue = new Symbol(CodeConstant.RETURN_VALUE);

    // 操作数是整数还是实数
    private boolean isFirstOperandInt;
    private boolean isSecondOperandInt;
    // 跳转条件
    private Symbol condition;
    // 命令行输入
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Lexer lexer = new Lexer("E:\\desktop\\MyCMMInterpreter\\test_opt_2.cmm");
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
        Interpreter interpreter = new Interpreter(generator);
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
     * 初始化工作
     */
    public Interpreter(InterGenerator generator) {
        List<String> level0 = new ArrayList<>();
        tempVars.put(0, level0);
        this.codes = generator.getCodes();
        this.funcInstrMap = generator.funcInstrMap;
        // 出口栈帧
        Frame outFrame = new Frame(MAIN_OUT_ADDR);
        stackFrames.push(outFrame);
    }

    /**
     * 执行每一行中间代码
     */
    private void run() throws ExecutionException {
        // 从main函数开始执行
        if (funcInstrMap.containsKey("main")) {
            instrIndex = funcInstrMap.get("main");
        } else {
            noMainFuncException();
            return;
        }

        while(instrIndex < codes.size()) {
            if (instrIndex == MAIN_OUT_ADDR) {
                System.out.println("Main exited.");
                // main执行结束, 退出
                return;
            }
            Quadruple code = codes.get(instrIndex);
            switch (code.operation) {
                case CodeConstant.JMP_WITH_CONDITION:
                    jumpWithCondition(code);
                    break;
                case CodeConstant.JMP:
                    jump(code);
                    break;
                case CodeConstant.SCAN:
                    scan(code);
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
                case CodeConstant.CHAR:
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
                case CodeConstant.GR:
                case CodeConstant.GR_EQ:
                case CodeConstant.LE_EQ:
                    relationOperation(code);
                    break;
                case CodeConstant.ASSIGN:
                    assign(code);
                    break;
                case CodeConstant.RETURN:
                    ret(code);
                    break;
                case CodeConstant.ARG:
                    loadArg(code);
                    break;
                case CodeConstant.CALL:
                    call(code);
                    break;
                default:
                    throw new ExecutionException("Unexpected code!");
            }
        }
    }

    /**
     * 装载参数
     */
    private void loadArg(Quadruple code) {
        TreeNode arg = new TreeNode();
        switch (code.firstOperandType) {
            case INT_LITERAL:
                arg.setType(TreeNodeType.INT_LITERAL);
                arg.setIntValue(((IntOperand)code.firstOperand).intLiteral);
                break;
            case REAL_LITERAL:
                arg.setType(TreeNodeType.REAL_LITERAL);
                arg.setRealValue(((RealOperand)code.firstOperand).realLiteral);
                break;
            case IDENTIFIER:
                Symbol tmpSymbol = stackFrames.peek().localVarTable.getSymbol(code.firstOperand.name);
                if (tmpSymbol.getType() == SymValueType.INT) {
                    arg.setType(TreeNodeType.INT_LITERAL);
                    arg.setIntValue(tmpSymbol.getIntValue());
                } else if(tmpSymbol.getType() == SymValueType.REAL) {
                    arg.setType(TreeNodeType.REAL_LITERAL);
                    arg.setRealValue(tmpSymbol.getRealValue());
                }
                break;
        }
        argsList.add(arg);
        nextInstruction();
    }

    /**
     * 调用函数
     */
    private void call(Quadruple code) throws ExecutionException {
        Frame frame = new Frame();
        // 返回地址为当前的下一条语句
        frame.retAddr = instrIndex+1;
        // 已经按顺序排列的参数
        frame.argStack = argsList;
        argsList = new ArrayList<>();
        // 跳转到指定位置
        String callName = code.firstOperand.name;
        if (!funcInstrMap.containsKey(callName)) {
            // 找不到指定位置
            undefinedFuncException(callName);
        } else {
            // 压入栈帧
            stackFrames.push(frame);
            instrIndex = funcInstrMap.get(callName);
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
        if (code.firstOperandType == OperandType.IDENTIFIER) {
            Symbol symbol = getSymbol(code.firstOperand.name);
            switch (symbol.getType()) {
                case INT_ARRAY_ELEMENT:
                case INT:
                    System.out.println(symbol.getIntValue());
                    break;
                case REAL_ARRAY_ELEMENT:
                case REAL:
                    System.out.println(symbol.getRealValue());
                    break;
                case CHAR:
                    System.out.printf("%c\n", symbol.getIntValue());
                    break;
                case INT_ARRAY:
                    Integer[] arrInt =  Arrays.stream(symbol.getIntArray()).boxed().toArray(Integer[]::new);
                    System.out.println(arrToString(arrInt));
                    break;
                case REAL_ARRAY:
                    Double[] arrReal =  Arrays.stream(symbol.getRealArray()).boxed().toArray(Double[]::new);
                    System.out.println(arrToString(arrReal));
                    break;
                case TRUE:
                    System.out.println("true");
                    break;
                case FALSE:
                    System.out.println("false");
                    break;
            }
        } else if (code.firstOperandType == OperandType.INT_LITERAL) {
            System.out.println(((IntOperand)code.firstOperand).intLiteral);
        } else if (code.firstOperandType == OperandType.REAL_LITERAL) {
            System.out.println(((RealOperand)code.firstOperand).realLiteral);
        }
        nextInstruction();
    }

    /**
     * 从控制台读取输入
     */
    private void scan(Quadruple code) {
        Symbol symbol = getSymbol(code.dest);
        if(symbol.getType() == SymValueType.INT) {
            symbol.setIntValue(scanner.nextInt());
        } else if(symbol.getType() == SymValueType.REAL){
            symbol.setRealValue(scanner.nextDouble());
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
        // 删除当前栈帧局部变量表的临时符号
        stackFrames.peek().localVarTable.deleteSymbols(tempVars.get(blockLevel));
        tempVars.remove(blockLevel);
        blockLevel--;
        nextInstruction();
    }

    /**
     * 访问数组
     */
    private void arrayAccess(Quadruple code) throws ExecutionException {
        Symbol symbol = new Symbol(code.dest);
        int index = 0;
        switch (code.secondOperandType) {
            case IDENTIFIER:
                // 索引是标识符
                index = stackFrames.peek().localVarTable.getSymbol(code.secondOperand.name).getIntValue();
                break;
            case INT_LITERAL:
                index = ((IntOperand)(code.secondOperand)).intLiteral;
                break;
        }

        Symbol array = stackFrames.peek().localVarTable.getSymbol(code.firstOperand.name);
        if (index < 0) {
            // 越下界
            arrayIndexOutOfBoundsException(index);
        }
        symbol.setArrName(array.getName());
        symbol.setIndex(index);

        if(array.getType() == SymValueType.INT_ARRAY) {
            if (index >= (array.getIntArray().length)) {
                // 越上界
                arrayIndexOutOfBoundsException(index);
            }
            symbol.setType(SymValueType.INT_ARRAY_ELEMENT);
            symbol.setIntValue(array.getIntArray()[index]);
        } else if(array.getType() == SymValueType.REAL_ARRAY) {
            if (index >= (array.getRealArray().length)) {
                // 越上界
                arrayIndexOutOfBoundsException(index);
            }
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
    private void relationOperation(Quadruple code) throws ExecutionException {
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
            case CodeConstant.GR:
                if(operand1 > operand2) {
                    symbol.setType(SymValueType.TRUE);
                } else {
                    symbol.setType(SymValueType.FALSE);
                }
                break;
            case CodeConstant.LE_EQ:
                if(operand1 <= operand2) {
                    symbol.setType(SymValueType.TRUE);
                } else {
                    symbol.setType(SymValueType.FALSE);
                }
                break;
            case CodeConstant.GR_EQ:
                if(operand1 >= operand2) {
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
    private void assign(Quadruple code) throws ExecutionException {
        Symbol target = stackFrames.peek().localVarTable.getSymbol(code.dest);
        if (target == null) {
            // 变量还没有被声明
            // 不能赋值
            varNotDeclaredException(code.dest);
        }
        double source = getFirstOperand(code);
        Symbol array;
        if(target.getType() == SymValueType.INT_ARRAY_ELEMENT) {
            array = stackFrames.peek().localVarTable.getSymbol(target.getArrName());
            array.getIntArray()[target.getIndex()] = (int)source;
            target.setIntValue((int)source);
        } else if(target.getType() == SymValueType.REAL_ARRAY_ELEMENT) {
            array = stackFrames.peek().localVarTable.getSymbol(target.getArrName());
            array.getRealArray()[target.getIndex()] = source;
            target.setRealValue(source);
        } else {
            switch (target.getType()) {
                case INT:
                    // 以变量类型进行类型转换
                    target.setIntValue((int)source);
                    break;
                case REAL:
                    target.setRealValue(source);
                    break;
            }
        }
        nextInstruction();
    }

    /**
     * 声明变量
     */
    private void declaration(Quadruple code) throws ExecutionException {
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
            case CodeConstant.CHAR:
                symbol.setType(SymValueType.CHAR);
                if (code.firstOperandType != OperandType.NULL) {
                    symbol.setIntValue((int) right);
                }
                break;
        }
        // 添加到当前语句块的变量列表
        if (tempVars.get(blockLevel).contains(symbol.getName())) {
            // 当前块已经有该变量名
            redeclarationException(symbol.getName());
        }
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
     * 函数返回
     */
    private void ret(Quadruple code) {
        blockLevel--;
        // 弹出栈帧
        Frame curFrame = stackFrames.pop();
        // 设置返回地址
        instrIndex = curFrame.retAddr;
        if (code.firstOperandType != null) {
            // 有返回值
            // 设置返回值
            switch (code.firstOperandType) {
                case INT_LITERAL:
                    retValue.setType(SymValueType.INT);
                    retValue.setIntValue(((IntOperand)code.firstOperand).intLiteral);
                    break;
                case REAL_LITERAL:
                    retValue.setType(SymValueType.REAL);
                    retValue.setRealValue(((RealOperand)code.firstOperand).realLiteral);
                    break;
                case IDENTIFIER:
                    // 返回变量要进行取值
                    retValue = curFrame .localVarTable.getSymbol(code.firstOperand.name);

            }
        }

    }



    /**
     * 根据符号名取出对应符号
     */
    private Symbol getSymbol(String name) {
        return stackFrames.peek().localVarTable.getSymbol(name);
    }

    /**
     * 在当前块层次添加临时变量
     */
    private void addTempSymbol(Symbol symbol)  {
        stackFrames.peek().localVarTable.addSymbol(symbol);
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
    private double getFirstOperand(Quadruple code) throws ExecutionException {
        if(code.firstOperandType == OperandType.INT_LITERAL) {
            isFirstOperandInt = true;
            return (double)((IntOperand)code.firstOperand).intLiteral;
        } else if(code.firstOperandType == OperandType.REAL_LITERAL) {
            isFirstOperandInt = false;
            return ((RealOperand)code.firstOperand).realLiteral;
        } else {
            if (code.firstOperand.name.startsWith("%arg")) {
                // 是参数，在当前栈帧中获取
                int argIndex = Integer.parseInt(code.firstOperand.name.substring(4));
                TreeNode arg = stackFrames.peek().argStack.get(argIndex);
                switch (arg.getType()) {
                    case INT_LITERAL:
                        isFirstOperandInt = true;
                        return (double)arg.getIntValue();
                    case REAL_LITERAL:
                        isFirstOperandInt = false;
                        return arg.getRealValue();
                }
            }
            if (code.firstOperand.name.equals(CodeConstant.RETURN_VALUE)) {
                // 是返回值
                switch (retValue.getType()) {
                    case INT:
                        isFirstOperandInt = true;
                        return (double) retValue.getIntValue();
                    case REAL:
                        isFirstOperandInt = false;
                        return retValue.getRealValue();
                }
            }

            // 是临时变量，在符号表中查找
            Symbol symbol = stackFrames.peek().localVarTable.getSymbol(code.firstOperand.name);
            if (symbol == null) {
                symbolNotFoundException(code.firstOperand.name);
            }
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
    private double getSecondOperand(Quadruple code) throws ExecutionException {
        if(code.secondOperandType == OperandType.INT_LITERAL) {
            isSecondOperandInt = true;
            return (double)((IntOperand)code.secondOperand).intLiteral;
        } else if(code.secondOperandType == OperandType.REAL_LITERAL) {
            return ((RealOperand)code.secondOperand).realLiteral;
        } else {
            if (code.secondOperand.name.startsWith("%arg")) {
                // 是参数，在当前栈帧中获取
                int argIndex = Integer.parseInt(code.secondOperand.name.substring(4));
                TreeNode arg = stackFrames.peek().argStack.get(argIndex);
                switch (arg.getType()) {
                    case INT_LITERAL:
                        isSecondOperandInt = true;
                        return (double)arg.getIntValue();
                    case REAL_LITERAL:
                        isSecondOperandInt = false;
                        return arg.getRealValue();
                }

            }
            if (code.secondOperand.name.equals(CodeConstant.RETURN_VALUE)) {
                // 是返回值
                switch (retValue.getType()) {
                    case INT:
                        isSecondOperandInt = true;
                        return (double) retValue.getIntValue();
                    case REAL:
                        isSecondOperandInt = false;
                        return retValue.getRealValue();
                }
            }
            // 是临时变量，在符号表中查找
            Symbol symbol = getSymbol(code.secondOperand.name);
            if (symbol == null) {
                symbolNotFoundException(code.secondOperand.name);
            }
            if (symbol.getType() == SymValueType.INT ||
                    symbol.getType() == SymValueType.INT_ARRAY_ELEMENT) {
                isSecondOperandInt = true;
                return (double) symbol.getIntValue();
            } else{
                return symbol.getRealValue();
            }

        }
    }

    /**
     * 格式化输出数组
     */
    private  <E> String arrToString(E[] arr) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        if (arr.length > 1) {
            for (int i=0; i<arr.length-1; i++) {
                stringBuilder.append(arr[i]).append(", ");
            }
            stringBuilder.append(arr[arr.length-1]);
        }
        stringBuilder.append("]");

        return stringBuilder.toString();
    }


    /**
     * 重复声明异常
     */
    private void redeclarationException(String varName) throws ExecutionException{
        throw new ExecutionException("Redeclaration of variable " + varName);
    }


    /**
     * 找不到符号声明
     */
    private void symbolNotFoundException(String varName) throws ExecutionException{
        throw new ExecutionException("Cannot find symbol " + varName);
    }

    /**
     * 找不到变量声明
     */
    private void varNotDeclaredException(String varName) throws ExecutionException{
        throw new ExecutionException("Variable " + varName + " is not declared!");
    }

    /**
     * 数组索引越界
     */
    private void arrayIndexOutOfBoundsException(int index) throws ExecutionException{
        throw new ExecutionException("Array index is out of bounds: "+index);
    }

    /**
     * 找不到主函数
     */
    private void noMainFuncException() throws ExecutionException{
        throw new ExecutionException("No main function!");
    }

    /**
     * 调用未定义的函数
     */
    private void undefinedFuncException(String callName) throws ExecutionException {
        throw new ExecutionException("Undefined function "+callName);
    }
}
