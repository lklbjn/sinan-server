package pres.peixinyi.sinan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务
 */
@Service
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
     * @param toEmail 收件人邮箱
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
}