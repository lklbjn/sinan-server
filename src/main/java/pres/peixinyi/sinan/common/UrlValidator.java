package pres.peixinyi.sinan.common;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * URL验证工具类
 * 用于验证URL的有效性,并过滤内网地址
 *
 * @author peixinyi
 * @since 2025/10/09
 */
public class UrlValidator {

    /**
     * 验证URL是否有效且不是内网地址
     *
     * @param urlString 要验证的URL字符串
     * @return 验证结果消息,如果有效则返回null
     */
    public static String validate(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return "URL不能为空";
        }

        try {
            // 解析URL
            URL url = new URL(urlString.trim());

            // 检查协议
            String protocol = url.getProtocol().toLowerCase();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return "只支持 HTTP 和 HTTPS 协议";
            }

            // 获取主机名
            String host = url.getHost();
            if (host == null || host.isEmpty()) {
                return "URL格式不正确,缺少主机名";
            }

            // 检查是否为内网地址
            if (isInternalAddress(host)) {
                return "不允许访问内网地址";
            }

            return null; // 验证通过
        } catch (Exception e) {
            return "URL格式不正确: " + e.getMessage();
        }
    }

    /**
     * 检查主机是否为内网地址
     *
     * @param host 主机名或IP地址
     * @return true表示是内网地址
     */
    private static boolean isInternalAddress(String host) {
        try {
            // 特殊主机名检查
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("localhost")
                || lowerHost.equals("127.0.0.1")
                || lowerHost.equals("0.0.0.0")
                || lowerHost.equals("::1")
                || lowerHost.endsWith(".local")
                || lowerHost.endsWith(".localhost")) {
                return true;
            }

            // 解析IP地址
            InetAddress address = InetAddress.getByName(host);

            // 检查是否为内网IP
            return address.isLoopbackAddress()      // 127.0.0.1, ::1
                || address.isLinkLocalAddress()     // 169.254.0.0/16, fe80::/10
                || address.isSiteLocalAddress()     // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
                || address.isAnyLocalAddress()      // 0.0.0.0, ::
                || isPrivateIPv4(address);

        } catch (UnknownHostException e) {
            // 无法解析主机名,为了安全起见,认为是内网地址
            return true;
        }
    }

    /**
     * 检查是否为私有IPv4地址
     * 包括:
     * - 10.0.0.0/8
     * - 172.16.0.0/12
     * - 192.168.0.0/16
     *
     * @param address IP地址
     * @return true表示是私有IP
     */
    private static boolean isPrivateIPv4(InetAddress address) {
        byte[] ip = address.getAddress();
        if (ip.length != 4) {
            return false; // 不是IPv4
        }

        // 10.0.0.0/8
        if (ip[0] == 10) {
            return true;
        }

        // 172.16.0.0/12
        if (ip[0] == (byte) 172 && (ip[1] & 0xFF) >= 16 && (ip[1] & 0xFF) <= 31) {
            return true;
        }

        // 192.168.0.0/16
        if (ip[0] == (byte) 192 && ip[1] == (byte) 168) {
            return true;
        }

        return false;
    }

    /**
     * 检查URL是否有效(不检查内网地址)
     *
     * @param urlString 要验证的URL字符串
     * @return true表示URL格式有效
     */
    public static boolean isValidUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return false;
        }

        try {
            URL url = new URL(urlString.trim());
            String protocol = url.getProtocol().toLowerCase();
            return (protocol.equals("http") || protocol.equals("https"))
                && url.getHost() != null
                && !url.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
