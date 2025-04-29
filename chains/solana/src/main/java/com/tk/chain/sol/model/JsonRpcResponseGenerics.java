package com.tk.chain.sol.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
 public class JsonRpcResponseGenerics<E>{
    private String id;

    private Object error;

    private E result;

    private String jsonrpc;


   public JsonRpcResponseGenerics(E result) {
      this.result = result;
   }
}