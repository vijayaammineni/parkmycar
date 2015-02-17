package com.parkmycar;

public class AsyncTaskResult<T> {
private T result;
private Exception error;
private String message;


public T getResult() {
    return result;
}
public String getError() {
    return error.getMessage();
}
public String getMessage() {
    return message;
}


public AsyncTaskResult(T result) {
    super();
    this.result = result;
}


public AsyncTaskResult(Exception error) {
    super();
    this.error = error;
}

public AsyncTaskResult(String message) {
    super();
    this.message = message;
}
}