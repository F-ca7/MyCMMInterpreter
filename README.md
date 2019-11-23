## My CMM Interpreter

### 词法测试

-------

1. test_lex_err1.cmm 第三行，标识符以数字开头错误
2. test_lex_err2.cmm 第二行，非法字符
3. test_lex_err3.cmm 第二行，非法字面量 两个小数点
4. test_lex_err4.cmm 第二 三行，注释未闭合 多个错误 

通过lex_1~6的测试
1. 测试注释
2. 数字字面量
3. 各种符号
4. 标识符
5. 关键字
6. 综合 全部正确


### 语法测试

---------

1. test_gram_1.cmm 正确， 为expr2_test.txt的**no4.测试**和**f测试**, 
以及extended的**number format**和**identifier format** 以及 **function**
1. test_gram_err1.cmm 第三行，if的条件语句没有右括号
2. test_gram_err2.cmm 第三行，算术表达式错误，缺少分号错误，
缺少右括号错误，算术表达式括号不匹配，缺少返回语句；多个错误
3. test_gram_err3.cmm 字面量不能作为左值
4. test_gram_err4.cmm 未定义函数
#### 错误恢复
1. 跳过BeginSym和EndSym之外的符号；
2. 若遇到BeginSym，开始新的识别；若遇到EndSym结束。

if和while的条件不能为数字。

```
文法:
# program -> stmt-sequence

program -> func-sequence
func-sequence -> function func-sequence
function -> func identifier ( arg-list ) ret-value stmt-block
ret-value -> void | int | real | char 

stmt-sequence -> statement ; stmt-sequence | statement | ε
statement -> declare-stmt | assign-stmt | if-stmt | while-stmt | print-stmt | scan-stmt

stmt-block -> { stmt-sequence }

if-stmt -> if ( rel-exp ) stmt-block | if ( rel-exp ) stmt-block else stmt-block | if ( rel-exp ) stmt-block
        else-ifs
else-ifs -> else if ( rel-exp ) stmt-block else-ifs | else stmt-block | else if ( rel-exp ) stmt-block

while-stmt -> while ( rel-exp ) stmt-block

assign-stmt -> variable = exp ;
print-stmt -> print exp ;

# 可以同时赋值或者不赋值
declare-stmt -> (int | real | char) ((identifier [ = exp ]) | (identifier [ exp ])) ;

variable -> identifier [ [ exp ] ]

exp ->  exp
rel-exp -> exp logical-op exp
exp -> term add-op exp | term
term -> factor mul-op term | factor

factor -> ( exp ) | number | variable | add-op exp

logical-op -> > | < | >= | <= | <> | ==

add-op -> + | -
mul-op -> * | /
```

1. 没有函数重载：
    Method dispatch is simplified if it doesn't need to do type matching 
    as well.
    在Go语言里面，函数名称的唯一性规定是为了让程序看起来简洁明了
2. 函数不支持 嵌套， 重载和默认参数
3. 定义函数使用关键字func，对于函数指针来说 阅读方便理解
    int (\*(\*fp)(int (\*)(int, int), int))(int, int) 
    <br>
    f func(func(int,int) int, int) func(int, int) int
    
### 语义测试

--------
中间代码格式：
```
jne,条件,null,目标
jmp,null,null,目标
print,变量,null,null
scan,变量,null,null
in,null,null,null
out,null,null,null
int,右值,null,左值
real,右值,null,左值
int,长度,null,数组名
real,长度,null,数组名
assign,右值,null,左值
plus,第一个操作数,第二个操作数,目标
arr_acc,数组名,索引,临时变量名
arg,参数值,null,null
call,函数名,null,null
ret,null,null,null
```
1. test_sem_err1 检查语义期间错返回值类型匹配错误
2. test_sem_err2 检查语义期间除以0错误

3. test_opt_1 中间代码优化-if-elseif-else的常量判断优化
4. test_opt_2 赋值但未使用的变量优化
### 解释执行测试

---------
1. test_func 成功 基础综合，包括输入输出、if、while、算术表达式(负号, 类型隐式转换) 数组的输出 while中的break
2. test_func_1 成功 字符输出 数组默认值
3. test_func_err1 报告错误 运行期间数组上下界检查
4. test_func_err2 错误 变量的重定义
5. test_func_err3 错误 变量的未声明
6. test_func_call1 成功 函数调用 局部变量不会影响 入参 和 返回值
7. test_func_call2 成功 函数的递归调用

### feat:

1. 检查符号、变量是否声明
2. 执行期间数组越界 上下界检查
3. 控制台输入输出
4. 实现6个比较运算符
5. 实现char类型的赋值、输出
6. 实现算数表达式负号的检测
7. 实现函数词法、语法、语义
8. 语义分析检查函数返回值类型
9. 函数调用(包括递归)
10. 中间代码优化(包括if的常量判断、声明但未使用的变量检测)
11. 可视化界面展示语法树，可选择是否代码优化
