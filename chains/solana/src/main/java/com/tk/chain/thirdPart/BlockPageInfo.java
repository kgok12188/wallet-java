package com.tk.chain.thirdPart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockPageInfo {

    private Block block;

    private List<Transaction> transactions;

    private String page;

}
