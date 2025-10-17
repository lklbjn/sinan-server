package pres.peixinyi.sinan.module.feedback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pres.peixinyi.sinan.module.feedback.dto.request.CreateFeedbackReq;
import pres.peixinyi.sinan.module.feedback.entity.Feedback;
import pres.peixinyi.sinan.module.feedback.mapper.FeedbackMapper;
import pres.peixinyi.sinan.service.EmailService;

import java.util.Date;
import java.util.List;

/**
 * 反馈服务
 */
@Slf4j
@Service
public class FeedbackService extends ServiceImpl<FeedbackMapper, Feedback> {


    private final EmailService emailService;

    @Value("${sinan.feedback.notification-email:}")
    private String notificationEmail;

    public FeedbackService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * 创建反馈
     *
     * @param request 反馈请求
     * @param userId 用户ID（可选）
     * @return 反馈ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String createFeedback(CreateFeedbackReq request, String userId) {
        Feedback feedback = new Feedback();
        feedback.setId(null); // 自动生成UUID
        feedback.setUserId(userId);
        feedback.setContact(request.getContact());
        feedback.setContent(request.getContent());
        feedback.setStatus(0); // 未处理状态
        feedback.setCreateTime(new Date());
        feedback.setUpdateTime(new Date());
        feedback.setDeleted(0); // 未删除

        boolean saved = save(feedback);
        if (!saved) {
            throw new RuntimeException("保存反馈失败");
        }

        // 异步发送邮件通知
        try {
            emailService.sendFeedbackNotificationEmail(
                notificationEmail,
                feedback.getContact(),
                feedback.getId(),
                feedback.getContent(),
                feedback.getCreateTime()
            );
        } catch (Exception e) {
            log.error("发送反馈通知邮件失败，反馈ID: {}", feedback.getId(), e);
            // 邮件发送失败不影响反馈创建
        }

        log.info("用户反馈创建成功，反馈ID: {}, 联系方式: {}", feedback.getId(), feedback.getContact());
        return feedback.getId();
    }

    /**
     * 获取反馈列表（管理员用）
     *
     * @param page 页码
     * @param size 每页大小
     * @param status 状态过滤（可选）
     * @return 反馈列表
     */
    public List<Feedback> getFeedbackList(int page, int size, Integer status) {
        QueryWrapper<Feedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);

        if (status != null) {
            queryWrapper.eq("status", status);
        }

        queryWrapper.orderByDesc("create_time");

        int offset = (page - 1) * size;
        queryWrapper.last("LIMIT " + offset + "," + size);

        return list(queryWrapper);
    }

    /**
     * 获取反馈总数（管理员用）
     *
     * @param status 状态过滤（可选）
     * @return 总数
     */
    public long getFeedbackCount(Integer status) {
        QueryWrapper<Feedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);

        if (status != null) {
            queryWrapper.eq("status", status);
        }

        return count(queryWrapper);
    }

    /**
     * 更新反馈状态（管理员用）
     *
     * @param feedbackId 反馈ID
     * @param status 新状态
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFeedbackStatus(String feedbackId, Integer status) {
        Feedback feedback = getById(feedbackId);
        if (feedback == null || feedback.getDeleted() == 1) {
            return false;
        }

        feedback.setStatus(status);
        feedback.setUpdateTime(new Date());

        boolean updated = updateById(feedback);
        if (updated) {
            log.info("反馈状态更新成功，反馈ID: {}, 新状态: {}", feedbackId, status);
        }

        return updated;
    }

    /**
     * 删除反馈（软删除，管理员用）
     *
     * @param feedbackId 反馈ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFeedback(String feedbackId) {
        Feedback feedback = getById(feedbackId);
        if (feedback == null || feedback.getDeleted() == 1) {
            return false;
        }

        feedback.setDeleted(1);
        feedback.setUpdateTime(new Date());

        boolean deleted = updateById(feedback);
        if (deleted) {
            log.info("反馈删除成功，反馈ID: {}", feedbackId);
        }

        return deleted;
    }
}
