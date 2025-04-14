package com.tk.admin.api.controller;

import com.tk.wallet.common.entity.WalletUser;
import com.tk.wallet.common.service.WalletUserService;
import com.tk.wallet.common.vo.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/walletUser")
public class WalletUserController {

    @Autowired
    private WalletUserService walletUserService;

    @PostMapping("/add")
    public R<Integer> add(@RequestBody WalletUser walletUser) {
        walletUser.setId(null);
        if (StringUtils.isBlank(walletUser.getCallbackUrl())) {
            return R.fail("充值上账回调地址错误");
        }
        walletUser.setCtime(new Date());
        walletUser.setMtime(new Date());
        return R.success(walletUser.getId());
    }

}
