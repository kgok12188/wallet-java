package com.tk.chain.thirdPart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class BlockHeight {
    private Long blockHeight;
    private String blockId;
}
