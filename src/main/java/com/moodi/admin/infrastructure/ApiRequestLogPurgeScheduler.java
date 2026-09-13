package com.moodi.admin.infrastructure;

import com.moodi.admin.application.ApiRequestLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 새벽 보관 기간이 지난 API 요청 로그를 지운다. 인스턴스가 0으로 내려가 있으면 다음 기동 후 첫 스케줄에 돈다. */
@EnableScheduling
@Component
public class ApiRequestLogPurgeScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLogPurgeScheduler.class);

    private final ApiRequestLogService apiRequestLogService;

    public ApiRequestLogPurgeScheduler(ApiRequestLogService apiRequestLogService) {
        this.apiRequestLogService = apiRequestLogService;
    }

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purge() {
        long deleted = apiRequestLogService.purgeExpired();
        if (deleted > 0) {
            log.info("보관 기간이 지난 API 요청 로그 {}건을 지웠습니다.", deleted);
        }
    }
}
