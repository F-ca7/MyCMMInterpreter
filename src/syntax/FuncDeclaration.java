package syntax;

import java.util.List;
import java.util.Objects;

/**
 * @description 函数的声明
 * @author FANG
 * @date 2019/11/1 11:28
 **/
public class FuncDeclaration {
    // 函数名
    String name;
    // 参数列表
    List<TreeNode> argListType;
    // 返回值
    TreeNodeType retType;

    public FuncDeclaration() {
    }

    public FuncDeclaration(String name) {
        this.name = name;
    }

    public FuncDeclaration(String name, List<TreeNode> argListType, TreeNodeType retType) {
        this.name = name;
        this.argListType = argListType;
        this.retType = retType;
    }

    /**
     * 判断函数名是一致的
     * 不用判断函数签名
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FuncDeclaration that = (FuncDeclaration) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, argListType, retType);
    }
}
