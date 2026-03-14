package com.example.backend.module.message.schedule;

import com.example.backend.module.message.controller.MessageController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MessageScanScheduler {
    private final MessageController messageController;

    public MessageScanScheduler(MessageController messageController) {
        this.messageController = messageController;
    }

    /**
     * Notes:
     * - 每 30 分钟自动执行一次预警扫描，包括：
     *   - 工单/订单超期提醒
     *   - 库存低于安全库存提醒
     *
     * Args:
     * - 无
     *
     * Returns:
     * - (Void)
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void autoScan() {
        // 仅自动扫描工单/订单超期；库存预警已在出入库操作时实时生成
        messageController.scan(null);
    }
}

