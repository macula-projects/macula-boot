package dev.macula.example.task;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.common.core.util.JsonUtil;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.springframework.stereotype.Component;

/**
 * {@code SnailJobHandler} is SnailJobHandler测试类
 *
 * @author Rain
 * @since 2025/12/10 15:33
 */
@Component
public class SnailJobHandler {
    @JobExecutor(name = "demoJobHandler")
    public ExecuteResult demoJobHandler(JobArgs jobArgs) {
        SnailJobLog.REMOTE.info("哈哈，测试成功了");
        System.out.println(JsonUtil.toJsonString(jobArgs));
        return ExecuteResult.success();
    }
}
