package pres.peixinyi.sinan.module.feedback.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pres.peixinyi.sinan.common.Result;
import pres.peixinyi.sinan.module.feedback.dto.request.CreateFeedbackReq;
import pres.peixinyi.sinan.module.feedback.dto.response.FeedbackCreateResp;
import pres.peixinyi.sinan.module.feedback.entity.Feedback;
import pres.peixinyi.sinan.module.feedback.service.FeedbackService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 反馈控制器
 */
@Slf4j
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * 创建反馈
     */
    @PostMapping
    public Result<FeedbackCreateResp> createFeedback(@Validated @RequestBody CreateFeedbackReq request) {
        try {
            // 获取当前登录用户ID，如果用户未登录则为null
            String userId = null;
            if (StpUtil.isLogin()) {
                userId = StpUtil.getLoginIdAsString();
            }
            String feedbackId = feedbackService.createFeedback(request, userId);
            FeedbackCreateResp response = new FeedbackCreateResp(feedbackId, "反馈提交成功，我们会尽快处理您的反馈！");
            return Result.success(response);
        } catch (Exception e) {
            log.error("创建反馈失败", e);
            return Result.error("反馈提交失败，请稍后重试");
        }
    }

}
