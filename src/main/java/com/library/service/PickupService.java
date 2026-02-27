package com.library.service;

import com.library.dto.PickupConfirmRequest;
import com.library.dto.PickupConfirmResponse;

/**
 * 取书确认服务接口
 */
public interface PickupService {

    /**
     * 确认取书
     *
     * @param request 取书确认请求
     * @return 取书确认响应
     */
    PickupConfirmResponse confirmPickup(PickupConfirmRequest request);
}
