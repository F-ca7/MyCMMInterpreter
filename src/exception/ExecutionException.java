package exception;

/**
 * @description 执行期异常
 * @author FANG
 * @date 2019/10/27 17:25
 **/
public class ExecutionException extends Exception{
    public ExecutionException() {
    }

    public ExecutionException(String message) {
        super(message);
    }
}
