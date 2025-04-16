# 指纹服务

## 涉及到指纹校验的数据

### 1、后管商户注册,商户uid,商户公钥,登录信息

### 2、商户链上地址，从私钥获取地址后，计算指纹存储在fingerprint字段中

### 3、提现信息,商户的私钥，加密后请求proxy-api,公钥解密，并回调验证（可选），计算指纹存储在fingerprint

### 4、数据上链，验证指纹，余额校验，按照from地址排队的方式推送交易

### 5、用户充值，拉取到交易后，匹配到商户的链上地址后，保存数据并计算指纹，通知商户入账

### 6、涉及到的表

~~~
`agg_task` 归集任务
`chain_transaction` 链上交易的映射表
`wallet_address`   地址表
`wallet_deposit` 充值表
`symbol_config`  币种配置表
`wallet_symbol_config` 商户币种配置表（商户需要手动开启链，并填写冷钱包地址和设置转冷阈值）
`wallet_withdraw` 提现表
`withdraw_to_cold_address` 转冷记录
~~~

## 交互接口

### 1、获取加密公钥 POST fingerprint/getPublicKey

~~~
参数
{
    
}
返回 公钥信息
{
    "value":"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArSJFrB6..."
}
~~~

### 2、数据加密 POST fingerprint/encrypt

~~~
参数
{
    "value":"需要加密的数据"
}
返回 加密后的数据
{
   "value“:"加密后的数据"
}
~~~

### 3、数据解密 POST fingerprint/encrypt

~~~
参数
{
    "value":"需要加密的数据"
}
返回 加密后的数据
{
   "value“:"加密后的数据"
}
~~~