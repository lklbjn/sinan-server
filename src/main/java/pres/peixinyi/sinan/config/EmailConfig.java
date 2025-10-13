package pres.peixinyi.sinan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件配置类
 * 根据配置动态创建邮件发送器，避免在没有密码时出现认证失败
 */
@Configuration
@ConditionalOnExpression("'${spring.mail.host:}'.trim() != ''")
public class EmailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}")
    private boolean sslEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean starttlsEnable;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(host);
        mailSender.setPort(port);

        // 只有在提供了用户名和密码时才设置认证信息
        if (!username.isEmpty() && !password.isEmpty()) {
            mailSender.setUsername(username);
            mailSender.setPassword(password);
        }

        Properties props = mailSender.getJavaMailProperties();

        // 根据是否有密码来动态决定是否启用认证
        boolean requireAuth = !username.isEmpty() && !password.isEmpty() && smtpAuth;
        props.put("mail.smtp.auth", requireAuth);

        // SSL配置
        props.put("mail.smtp.ssl.enable", sslEnable && requireAuth);
        props.put("mail.smtp.ssl.required", sslEnable && requireAuth);

        if (sslEnable && requireAuth) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
        }

        // STARTTLS配置
        props.put("mail.smtp.starttls.enable", starttlsEnable);
        props.put("mail.smtp.starttls.required", starttlsEnable);

        // 超时配置
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "3000");
        props.put("mail.smtp.writetimeout", "5000");

        // 调试模式
        props.put("mail.debug", "false");

        return mailSender;
    }
}