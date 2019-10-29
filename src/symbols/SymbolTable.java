package symbols;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description 符号表
 *              将符号信息从声明的地方传到实际使用的地方
 *              放在内存中，不保存为中间文件
 * @author FANG
 * @date 2019/10/25 23:21
 **/
public class SymbolTable {
    // 内部用hash表实现
    private final Map<String,Symbol> map = new HashMap<>();;

    /**
     * 添加符号至表中
     * @param symbol 待添加符号
     */
    public void addSymbol(Symbol symbol) {
        if(!map.containsKey(symbol.getName())) {
            map.put(symbol.getName(),symbol);
        }
        else {
            symbol.next= map.get(symbol.getName());
            map.put(symbol.getName(),symbol);
        }
    }

    /**
     * 从表中删除所有对应名字的符号
     */
    public void deleteSymbols(List<String> symNames) {
        for (String name:symNames) {
            deleteSymbol(name);
        }
    }

    /**
     * 从表中删除对应名字的符号
     */
    private void deleteSymbol(String name) {
        Symbol previous = map.get(name);
        map.put(name,previous.next);
    }

    /**
     * 根据名字获取对应符号
     */
    public Symbol getSymbol(String name) {
        return map.get(name);
    }
}
