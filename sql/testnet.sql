use wallet;
-- 链配置
replace into chain_scan_config (chain_id, endpoints, status, ctime, mtime, block_number, block_height, scan_time,
                                task_id, task_update_time, un_scan_block, delay_blocks, block_interval, last_block_time,
                                address_symbol, multi_thread, multi_thread_numbers,
                                retry_interval, json_config, sign_url, address_url)
values ('BSC',
        '{"fields":{"url":"节点地址"},"value":[{"title":"地址1","url":"https://data-seed-prebsc-1-s1.bnbchain.org:8545","enable":true},{"title":"地址2","url":"https://data-seed-prebsc-2-s1.bnbchain.org:8545","enable":true},{"title":"地址3","url":"https://data-seed-prebsc-1-s3.bnbchain.org:8545","enable":true},{"title":"地址4","url":"https://data-seed-prebsc-2-s3.bnbchain.org:8545","enable":true}]}',
        '1', now(), now(), '0', '0', now(), '',
        now(), 200, 0, 10, now(), 'ETH', 0, 0, 0, '', 'http://127.0.0.1:8080/eth/sign',
        'http://127.0.0.1:8080/eth/getAddress');

replace into chain_scan_config (chain_id, endpoints, status, ctime, mtime, block_number, block_height, scan_time,
                                task_id, task_update_time, un_scan_block, delay_blocks, block_interval, last_block_time,
                                sign_url, address_url, address_symbol, multi_thread, multi_thread_numbers,
                                retry_interval, json_config)
values ('BTC',
        '{"fields":["type","title","sendTxUrl","api-key","url","listUnspent","sort"],"value":[{"type":"nowNodes","title":"私有节点","sendTxUrl":"https://btc.nownodes.io","api-key":"4e2966c3-bc74-40a8-883e-2edb4550c557","url":"https://btcbook.nownodes.io","sort":1,"enable":true}]}',
        '1', '2023-09-14 14:53:01', now(), '0', '0', now(),
        '', now(), 200, 0, 300,
        now(), '0', '0', '0', '0', 0, 0, '0');



replace into chain_scan_config (chain_id, endpoints, status, ctime, mtime, block_number, block_height, scan_time,
                                task_id, task_update_time, un_scan_block, delay_blocks, block_interval, last_block_time,
                                sign_url, address_url, address_symbol, multi_thread, multi_thread_numbers,
                                retry_interval, json_config)
values ('ETH',
        '{"fields":{"url":"节点地址"},"value":[{"title":"地址1","url":"https://eth-sepolia.api.onfinality.io/public","enable":true},{"title":"地址2","url":"https://eth-sepolia.public.blastapi.io","enable":true},{"title":"地址3","url":"https://0xrpc.io/sep","enable":true}]}',
        '1', now(), now(), '0', '0', now(), '',
        now(), 200, 0, 15, now(), 'http://127.0.0.1:8080/eth/sign',
        'http://127.0.0.1:8080/eth/getAddress', '0', '0', 0, 0, '');


replace into chain_scan_config (chain_id, endpoints, status, ctime, mtime, block_number, block_height, scan_time,
                                task_id, task_update_time, un_scan_block, delay_blocks, block_interval, last_block_time,
                                sign_url, address_url, address_symbol, multi_thread, multi_thread_numbers,
                                retry_interval, json_config)
values ('TRX',
        '{"fields":["apiKey","privateKey","grpcEndpointSolidity","url","sort"],"value":[{"apiKey":"73df8fa1-7664-494c-88c9-5b1585f0ef6d","privateKey":"dfe76a50c6a32b3d2f1c66b56df238d3eb46c6094a3ea8847d8fdb0a41e4ff84","grpcEndpointSolidity":"grpc.nile.trongrid.io:50061","url":"grpc.nile.trongrid.io:50051","triggerSmartContract":"https://nile.trongrid.io/wallet/triggersmartcontract","broadcastTransaction":"https://nile.trongrid.io/wallet/broadcasttransaction","createTransaction":"https://nile.trongrid.io/wallet/createtransaction","enable":true}]}',
        '1', now(), now(), '0', '0', now(),
        '', now(), 200, 3, 0,
        now(), 'http://127.0.0.1:8080/tron/sign', 'http://127.0.0.1:8080/tron/getAddress', '0', '0', 0,
        0, '');


-- 币种配置

replace into symbol_config(base_symbol, symbol, confirm_count, contract_address, status, ctime, mtime, symbol_precision,
                           token_symbol, config_json)
VALUES ('TRX', 'TRX', 20, '', 1, now(), now(), 6, 'TRX', '{\"feeLimit\":15000000,\"gas\":15}');

replace into symbol_config(base_symbol, symbol, confirm_count, contract_address, status, ctime, mtime, symbol_precision,
                           token_symbol, config_json)
VALUES ('TRX', 'TRC20-USDT', 20, '', 1, now(), now(), 6, 'USDT', '{\"feeLimit\":28000000,\"gas\":28}');


replace into symbol_config(base_symbol, symbol, confirm_count, contract_address, status, ctime, mtime, symbol_precision,
                           token_symbol)
VALUES ('ETH', 'ETH', 2, '', 1, now(), now(), 18, 'ETH');

replace into symbol_config(base_symbol, symbol, confirm_count, contract_address, status, ctime, mtime, symbol_precision,
                           token_symbol)
VALUES ('ETH', 'ERC20-USDT', 2, '0xe70252bb07dea8d3362b58e77476366855a125a4', 1, now(), now(), 18, 'USDT');



replace into symbol_config(id,base_symbol, symbol, confirm_count, contract_address, status, ctime, mtime, symbol_precision,
                           token_symbol,config_json)
values (15,'BSC', 'BNB', 1, '', 1, now(), now(), 18, 'BNB','{\"limit\":210000,\"gas\":\"0.0015\"}');

replace into symbol_config(id,base_symbol, symbol, confirm_count, contract_address, status, ctime, mtime, symbol_precision,
                           token_symbol,config_json)
values (16,'BSC', 'BEP20-USDT', 1, '0x01bc06b31e9bd4ca058cd4982a9241321c08a363', 1, now(), now(), 18, 'USDT','{\"limit\":60000,\"gas\":\"0.0015\"}');

