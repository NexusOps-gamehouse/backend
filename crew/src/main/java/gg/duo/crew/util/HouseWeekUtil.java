package gg.duo.crew.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;

/** House 주간 기능이 공유하는 KST 월요일 시작 주차 계산. */
public final class HouseWeekUtil {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private HouseWeekUtil() {
    }

    public static LocalDateTime startOfCurrentWeek() {
        return LocalDateTime.now(KST)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    public static LocalDateTime endOfCurrentWeek() {
        return startOfCurrentWeek().plusDays(6)
                .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
    }

    public static String currentWeekId() {
        LocalDate date = startOfCurrentWeek().toLocalDate();
        WeekFields fields = WeekFields.ISO;
        return String.format(Locale.ROOT, "%04d-W%02d",
                date.get(fields.weekBasedYear()), date.get(fields.weekOfWeekBasedYear()));
    }
}
