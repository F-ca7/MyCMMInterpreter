package semantics;


import gram.TreeNode;
import symbols.SymbolTable;

import java.util.List;
import java.util.Stack;

/**
 * @description 函数栈帧
 * @author FANG
 * @date 2019/11/5 14:02
 **/
public class Frame {
    // 参数栈
    List<TreeNode> argStack;
    // 函数的局部变量表
    SymbolTable localVarTable = new SymbolTable();
    // 方法返回地址
    int retAddr;

    public Frame() {
    }

    public Frame(int retAddr) {
        this.retAddr = retAddr;
    }
}
