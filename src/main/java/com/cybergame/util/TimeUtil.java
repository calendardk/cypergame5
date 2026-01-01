package com.cybergame.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeUtil {

    public static final ZoneId VIETNAM_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    public static LocalDateTime nowVN() {
        return LocalDateTime.now(VIETNAM_ZONE);
    }
}
