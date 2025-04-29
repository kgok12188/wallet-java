package com.tk.chain.sol.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcRequest<T> {

    private String jsonrpc = "2.0";

    private Object id = "1";

    private String method;

    private T params;


    public JsonRpcRequest(String method) {
        this.method = method;
    }

    public JsonRpcRequest(String method, T params) {
        this.method = method;
        this.params = params;
    }

    public JsonRpcRequest(String method, T params, Object id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }
}
