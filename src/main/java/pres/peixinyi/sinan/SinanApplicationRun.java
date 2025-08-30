package pres.peixinyi.sinan;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/8 15:32
 * @Version : 0.0.0
 */
@SpringBootApplication
public class SinanApplicationRun {
    public static void main(String[] args) {
        SpringApplication.run(SinanApplicationRun.class, args);
    }
}
