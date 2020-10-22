package com.leeroy.forwordpanel.forwordpanel.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: meishaoyong
 * @Date: 2019/8/28 10:58
 * @Description:
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private Object errorData;
    private Throwable cause = null;
    private String errorCode = "500";
    private String errorMsg = "";

    public WebException(String errorCode, String msg) {
        super(msg);
        this.errorMsg = msg;
        this.errorCode = errorCode;
    }

    public WebException(String msg) {
        super(msg);
        this.errorMsg = msg;
    }


    public WebException(Throwable cause) {
        super(cause);
    }

    public WebException(String errorCode, String msg, Throwable cause) {
        super(msg);
        this.cause = cause;
        this.errorCode = errorCode;
    }

    public WebException(String errorCode, String msg, Object data) {
        super(msg);
        this.errorData = data;
        this.errorCode = errorCode;
    }

    @Override
    public Throwable getCause() {
        return this.cause;
    }

    public String getExceptionMessage() {
        if (super.getMessage() != null) {
            return super.getMessage();
        }
        if (this.cause != null) {
            return this.cause.toString();
        }
        return null;
    }
}
