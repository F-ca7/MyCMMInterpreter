package exception;

/**
 * @description 语法错误
 * @author FANG
 * @date 2019/10/24 16:31
 **/
public class GramException extends Exception {
    public GramException() {
    }

    public GramException(String message) {
        super(message);
    }
}
