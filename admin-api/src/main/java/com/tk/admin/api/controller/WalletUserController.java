package com.tk.admin.api.controller;

import com.tk.wallet.common.vo.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/walletUser")
public class WalletUserController {


    @PostMapping("/add")
    public R<Boolean> add() {
        return R.success();
    }

}
