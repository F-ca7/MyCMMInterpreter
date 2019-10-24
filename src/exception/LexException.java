package exception;

/**
 * @description 词法异常
 * @author FANG
 * @date 2019/10/24 15:45
 **/
public class LexException extends Exception {
    public LexException() {
    }

    public LexException(String message) {
        super(message);
    }
}
