package com.tk.admin.api.controller;

import com.tk.wallet.common.entity.WalletSymbolConfig;
import com.tk.wallet.common.service.WalletSymbolConfigService;
import com.tk.wallet.common.vo.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/walletSymbolConfig")
public class WalletSymbolConfigController {

    @Autowired
    private WalletSymbolConfigService walletSymbolConfigService;

    @PostMapping("/add")
    public R<Boolean> add(@RequestBody WalletSymbolConfig walletSymbolConfig) {
        return null;
    }

}
