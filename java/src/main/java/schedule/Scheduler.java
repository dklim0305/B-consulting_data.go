package schedule;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    public static final String SEOUL_ZONE = "Asia/Seoul";
    public static final int ONE_DAY = 1;
    public static final int ONE_DAY_AS_SECOND = 24 * 60 * 60;
    public static final int SINGLE_POOL_SIZE = 1;

    private final ScheduledExecutorService scheduler;

    public Scheduler() {
        this.scheduler = Executors.newScheduledThreadPool(SINGLE_POOL_SIZE);
    }

    public void execute(Runnable command, int hour, int minute, int second) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(SEOUL_ZONE));
        ZonedDateTime nextExecutionTime = this.getNextExecutionTime(hour, minute, second, now);
        scheduler.scheduleAtFixedRate(command, this.getInitialExecutionTime(now, nextExecutionTime), ONE_DAY_AS_SECOND, TimeUnit.SECONDS);
    }

    private ZonedDateTime getNextExecutionTime(int hour, int minute, int second, ZonedDateTime now) {
        ZonedDateTime nextExecutionTime;
        nextExecutionTime = now.withHour(hour).withMinute(minute).withSecond(second);

        if (this.isOverDay(now, nextExecutionTime)) {
            nextExecutionTime = nextExecutionTime.plusDays(ONE_DAY);
        }

        return nextExecutionTime;
    }

    private boolean isOverDay(ZonedDateTime zonedNow, ZonedDateTime nextExecutionTime) {
        return zonedNow.compareTo(nextExecutionTime) > 0;
    }

    private long getInitialExecutionTime(ZonedDateTime now, ZonedDateTime nextExecutionTime) {
        Duration duration = Duration.between(now, nextExecutionTime);
        return duration.getSeconds();
    }

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();
        // 공공데이터포털 조달청_계약정보
        scheduler.execute(new Egov_Cntrct_Daily(), 0, 0, 0);
        // 공공데이터포털 조달청_입찰공고목록 정보에 대한 물품조회
        scheduler.execute(new Egov_Bid_Pblanc_Thng_Daily(), 0, 10, 0);
        // 오픈다트
        scheduler.execute(new OpenDart_Daily(), 10, 0, 0);
    }

}
