package com.tk.chain.thirdPart;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressParam {

    private Long index;

    private String keyData;
}
