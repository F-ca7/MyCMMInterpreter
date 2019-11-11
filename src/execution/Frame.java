package execution;


import syntax.TreeNode;
import execution.symbols.SymbolTable;

import java.util.List;

/**
 * @description 函数栈帧
 * @author FANG
 * @date 2019/11/5 14:02
 **/
class Frame {
    // 参数栈
    List<TreeNode> argStack;
    // 函数的局部变量表
    SymbolTable localVarTable = new SymbolTable();
    // 方法返回地址
    int retAddr;

    Frame() {
    }

    Frame(int retAddr) {
        this.retAddr = retAddr;
    }
}
