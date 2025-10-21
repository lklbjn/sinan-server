package pres.peixinyi.sinan.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件服务
 */
@Slf4j
@Service
@ConditionalOnExpression("'${spring.mail.host:}'.trim() != ''")
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@sinan.host}")
    private String fromEmail;

    @Value("${sinan.server.base-url}")
    private String baseUrl;

    /**
     * 发送密码重置邮件
     *
     * @param toEmail   收件人邮箱
     * @param resetCode 重置验证码
     */
    public void sendPasswordResetEmail(String toEmail, String resetCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Sinan - 密码重置");

            String resetUrl = baseUrl + "/forgetpassword?code=" + resetCode;
            String emailContent = String.format(
                    "您好，\n\n" +
                            "您请求重置您的 Sinan 账户密码。请点击以下链接重置您的密码：\n\n" +
                            "%s\n\n" +
                            "此链接将在15分钟后过期。如果您没有请求重置密码，请忽略此邮件。\n\n" +
                            "Sinan 团队",
                    resetUrl
            );

            message.setText(emailContent);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("发送邮件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送反馈通知邮件
     *
     * @param toEmail    收件人邮箱
     * @param contact    反馈者联系方式
     * @param feedbackId 反馈ID
     * @param content    反馈内容
     * @param createTime 创建时间
     */
    public void sendFeedbackNotificationEmail(String toEmail, String contact, String feedbackId, String content, java.util.Date createTime) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("反馈通知邮箱未配置，跳过邮件发送");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("【用户反馈】来自" + contact + "的新反馈");

            String htmlContent = buildFeedbackEmailContent(contact, feedbackId, content, createTime);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("反馈通知邮件发送成功，反馈ID: {}", feedbackId);
        } catch (MessagingException e) {
            log.error("发送反馈通知邮件失败，反馈ID: {}", feedbackId, e);
        }
    }

    /**
     * 构建反馈邮件内容
     *
     * @param contact    反馈者联系方式
     * @param feedbackId 反馈ID
     * @param content    反馈内容
     * @param createTime 创建时间
     * @return HTML邮件内容
     */
    private String buildFeedbackEmailContent(String contact, String feedbackId, String content, java.util.Date createTime) {
        return "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                ".header { color: #333; text-align: center; margin-bottom: 30px; }" +
                ".info-item { margin: 15px 0; padding: 10px; background-color: #f8f9fa; border-radius: 4px; }" +
                ".label { font-weight: bold; color: #555; display: inline-block; width: 100px; }" +
                ".content { background-color: #fff; padding: 20px; border: 1px solid #e9ecef; border-radius: 4px; margin-top: 20px; }" +
                ".footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2>🔔 用户反馈通知</h2>" +
                "</div>" +

                "<div class='info-item'>" +
                "<span class='label'>反馈ID：</span>" + feedbackId +
                "</div>" +

                "<div class='info-item'>" +
                "<span class='label'>联系方式：</span>" + contact +
                "</div>" +

                "<div class='info-item'>" +
                "<span class='label'>提交时间：</span>" + (createTime != null ? createTime.toString() : "未知") +
                "</div>" +

                "<div class='info-item'>" +
                "<span class='label'>处理状态：</span>未处理" +
                "</div>" +

                "<div class='content'>" +
                "<h3>反馈内容：</h3>" +
                "<p>" + (content != null ? content.replace("\n", "<br>") : "") + "</p>" +
                "</div>" +

                "<div class='footer'>" +
                "<p>此邮件由 Sinan 书签管理系统自动发送</p>" +
                "<p>如有疑问，请联系系统管理员</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}