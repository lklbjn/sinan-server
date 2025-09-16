package pres.peixinyi.sinan.model.favicon.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import pres.peixinyi.sinan.model.favicon.dto.FaviconExtractResult;
import pres.peixinyi.sinan.model.favicon.dto.FaviconInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Favicon提取器
 * 负责从网页HTML中提取各种类型的favicon信息
 * 支持7种类型：默认favicon.ico、icon标签、apple-touch-icon、twitter:image、mask-icon、theme-color、shortcut icon
 * 
 * @author Claude
 * @since 2.0
 */
@Slf4j
@Component
public class FaviconExtractor {

    private final OkHttpClient httpClient;
    
    private static final String DEFAULT_FAVICON_PATH = "/favicon.ico";
    private static final int TIMEOUT_SECONDS = 10;

    /**
     * 构造函数，初始化HTTP客户端
     * 设置统一的超时时间为10秒
     */
    public FaviconExtractor() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .readTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .writeTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * 从指定URL提取所有favicon信息
     * 包括从 HTML 中解析各种类型的图标链接，检查默认favicon.ico，选择最优图标
     * 
     * @param url 目标网站URL
     * @return 包含所有找到的favicon信息和最优选择的提取结果
     */
    public FaviconExtractResult extractFavicons(String url) {
        long startTime = System.currentTimeMillis();
        
        try {
            URL parsedUrl = new URL(url);
            String domain = parsedUrl.getHost();
            String baseUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + 
                           (parsedUrl.getPort() != -1 ? ":" + parsedUrl.getPort() : "");
            
            List<FaviconInfo> faviconList = new ArrayList<>();
            
            String htmlContent = fetchHtmlContent(url);
            if (htmlContent != null) {
                extractFromHtml(htmlContent, baseUrl, faviconList);
            }
            
            checkDefaultFavicon(baseUrl, faviconList);
            
            FaviconInfo bestFavicon = selectBestFavicon(faviconList);
            
            return FaviconExtractResult.builder()
                    .domain(domain)
                    .url(url)
                    .faviconList(faviconList)
                    .bestFavicon(bestFavicon)
                    .success(true)
                    .extractTime(System.currentTimeMillis() - startTime)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to extract favicons from URL: {}", url, e);
            return FaviconExtractResult.builder()
                    .url(url)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .extractTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
    
    /**
     * 获取指定URL的HTML内容
     * 
     * @param url 目标URL
     * @return HTML内容字符串，失败返回null
     */
    private String fetchHtmlContent(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (compatible; FaviconExtractor/1.0)")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch HTML content from: {}", url, e);
        }
        return null;
    }
    
    /**
     * 从HTML内容中提取favicon信息
     * 
     * @param htmlContent HTML内容
     * @param baseUrl 基本URL，用于解析相对路径
     * @param faviconList 用于存储提取结果的列表
     */
    private void extractFromHtml(String htmlContent, String baseUrl, List<FaviconInfo> faviconList) {
        try {
            Document doc = Jsoup.parse(htmlContent);
            
            extractIconLinks(doc, baseUrl, faviconList);
            extractMetaTags(doc, baseUrl, faviconList);
            
        } catch (Exception e) {
            log.warn("Failed to parse HTML content", e);
        }
    }
    
    /**
     * 提取HTML中link标签的图标信息
     * 支持icon、shortcut icon、apple-touch-icon、mask-icon类型
     * 
     * @param doc JSoup解析的HTML文档
     * @param baseUrl 基本URL
     * @param faviconList 结果存储列表
     */
    private void extractIconLinks(Document doc, String baseUrl, List<FaviconInfo> faviconList) {
        Elements iconLinks = doc.select("link[rel~=(?i)(icon|shortcut\\s+icon|apple-touch-icon|mask-icon)]");
        
        for (Element link : iconLinks) {
            String rel = link.attr("rel").toLowerCase().trim();
            String href = link.attr("href");
            
            if (href == null || href.trim().isEmpty()) {
                continue;
            }
            
            String absoluteHref = resolveUrl(baseUrl, href.trim());
            String type = link.attr("type");
            String sizes = link.attr("sizes");
            String color = link.attr("color");
            
            FaviconInfo.FaviconType faviconType = determineFaviconType(rel);
            
            FaviconInfo faviconInfo = FaviconInfo.builder()
                    .url(absoluteHref)
                    .href(href.trim())
                    .type(type.isEmpty() ? null : type)
                    .sizes(sizes.isEmpty() ? null : sizes)
                    .color(color.isEmpty() ? null : color)
                    .faviconType(faviconType)
                    .priority(faviconType.getDefaultPriority())
                    .cached(false)
                    .build();
            
            faviconList.add(faviconInfo);
        }
    }
    
    /**
     * 提取HTML中meta标签的图标信息
     * 支持twitter:image和theme-color类型
     * 
     * @param doc JSoup解析的HTML文档
     * @param baseUrl 基本URL
     * @param faviconList 结果存储列表
     */
    private void extractMetaTags(Document doc, String baseUrl, List<FaviconInfo> faviconList) {
        Elements twitterImageMeta = doc.select("meta[name=twitter:image]");
        for (Element meta : twitterImageMeta) {
            String content = meta.attr("content");
            if (!content.isEmpty()) {
                String absoluteUrl = resolveUrl(baseUrl, content);
                
                FaviconInfo faviconInfo = FaviconInfo.builder()
                        .url(absoluteUrl)
                        .href(content)
                        .type("image/png")
                        .faviconType(FaviconInfo.FaviconType.TWITTER_IMAGE)
                        .priority(FaviconInfo.FaviconType.TWITTER_IMAGE.getDefaultPriority())
                        .cached(false)
                        .build();
                
                faviconList.add(faviconInfo);
            }
        }
        
        Elements themeColorMeta = doc.select("meta[name=theme-color]");
        for (Element meta : themeColorMeta) {
            String content = meta.attr("content");
            if (!content.isEmpty()) {
                FaviconInfo faviconInfo = FaviconInfo.builder()
                        .color(content)
                        .faviconType(FaviconInfo.FaviconType.THEME_COLOR)
                        .priority(FaviconInfo.FaviconType.THEME_COLOR.getDefaultPriority())
                        .cached(false)
                        .build();
                
                faviconList.add(faviconInfo);
            }
        }
    }
    
    /**
     * 检查默认favicon.ico文件是否存在
     * 如果列表中没有默认favicon且根目录下的favicon.ico可访问，则添加到列表
     * 
     * @param baseUrl 网站基本URL
     * @param faviconList favicon列表
     */
    private void checkDefaultFavicon(String baseUrl, List<FaviconInfo> faviconList) {
        boolean hasDefaultFavicon = faviconList.stream()
                .anyMatch(f -> f.getFaviconType() == FaviconInfo.FaviconType.DEFAULT_FAVICON);
        
        if (!hasDefaultFavicon) {
            String defaultFaviconUrl = baseUrl + DEFAULT_FAVICON_PATH;
            if (isUrlAccessible(defaultFaviconUrl)) {
                FaviconInfo faviconInfo = FaviconInfo.builder()
                        .url(defaultFaviconUrl)
                        .href(DEFAULT_FAVICON_PATH)
                        .type("image/x-icon")
                        .faviconType(FaviconInfo.FaviconType.DEFAULT_FAVICON)
                        .priority(FaviconInfo.FaviconType.DEFAULT_FAVICON.getDefaultPriority())
                        .cached(false)
                        .build();
                
                faviconList.add(faviconInfo);
            }
        }
    }
    
    /**
     * 从所有favicon中选择最优的一个
     * 按优先级排序，相同优先级下选择尺寸更大的
     * 
     * @param faviconList 所有找到的favicon列表
     * @return 最优favicon，无可用图标返回null
     */
    private FaviconInfo selectBestFavicon(List<FaviconInfo> faviconList) {
        if (faviconList.isEmpty()) {
            return null;
        }
        
        // 首先查找SVG格式的图标
        FaviconInfo svgIcon = faviconList.stream()
                .filter(f -> f.getUrl() != null && !f.getUrl().trim().isEmpty())
                .filter(f -> f.getFaviconType() != FaviconInfo.FaviconType.THEME_COLOR)
                .filter(this::isSvgIcon)
                .min(Comparator.comparingInt(FaviconInfo::getPriority))
                .orElse(null);
                
        // 如果找到SVG图标，优先返回
        if (svgIcon != null) {
            return svgIcon;
        }
        
        // 否则按原有逻辑选择
        return faviconList.stream()
                .filter(f -> f.getUrl() != null && !f.getUrl().trim().isEmpty())
                .filter(f -> f.getFaviconType() != FaviconInfo.FaviconType.THEME_COLOR)
                .min(Comparator.comparingInt(FaviconInfo::getPriority)
                     .thenComparing(f -> parseSizeValue(f.getSizes()), Comparator.reverseOrder()))
                .orElse(faviconList.stream()
                        .filter(f -> f.getUrl() != null && !f.getUrl().trim().isEmpty())
                        .findFirst()
                        .orElse(faviconList.get(0)));
    }

    /**
     * 检查是否为SVG图标
     *
     * @param favicon favicon信息
     * @return true表示是SVG图标
     */
    private boolean isSvgIcon(FaviconInfo favicon) {
        if (favicon.getType() != null && favicon.getType().toLowerCase().contains("svg")) {
            return true;
        }
        if (favicon.getUrl() != null && favicon.getUrl().toLowerCase().endsWith(".svg")) {
            return true;
        }
        return favicon.getFaviconType() == FaviconInfo.FaviconType.MASK_ICON;
    }
    
    private FaviconInfo.FaviconType determineFaviconType(String rel) {
        if (rel.contains("apple-touch-icon")) {
            return FaviconInfo.FaviconType.APPLE_TOUCH_ICON;
        } else if (rel.contains("mask-icon")) {
            return FaviconInfo.FaviconType.MASK_ICON;
        } else if (rel.contains("shortcut")) {
            return FaviconInfo.FaviconType.SHORTCUT_ICON;
        } else {
            return FaviconInfo.FaviconType.ICON;
        }
    }
    
    private String resolveUrl(String baseUrl, String href) {
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        
        if (href.startsWith("//")) {
            try {
                URL base = new URL(baseUrl);
                return base.getProtocol() + ":" + href;
            } catch (MalformedURLException e) {
                return "https:" + href;
            }
        }
        
        if (href.startsWith("/")) {
            return baseUrl + href;
        }
        
        return baseUrl + "/" + href;
    }
    
    private boolean isUrlAccessible(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .head()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private int parseSizeValue(String sizes) {
        if (sizes == null || sizes.trim().isEmpty()) {
            return 0;
        }
        
        String[] parts = sizes.split("x");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 从指定URL获取图标直接地址（完全遵循Python getIcon.py的逻辑）
     * 支持多种图标获取方式：根目录favicon.ico、HTML中的各种图标链接
     * 包含重定向跟踪和完整的图像验证
     * 
     * @param url 目标网站URL
     * @return 图标直接URL地址，失败返回null
     */
    public String getIconUrl(String url) {
        try {
            // 先跟踪重定向获取最终URL
            String finalUrl = getFinalUrlAfterRedirects(url);
            URL parsedUrl = new URL(finalUrl);
            String baseUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + 
                           (parsedUrl.getPort() != -1 ? ":" + parsedUrl.getPort() : "");
            
            log.info("实际解析的URL: {}", baseUrl);
            
            // 方法1: 尝试从根目录获取favicon.ico（类似Python的get_favicon_from_root）
            String faviconUrl = baseUrl + "/favicon.ico";
            byte[] iconData = downloadImage(faviconUrl);
            if (iconData != null) {
                log.info("从根目录获取到图标: {}", faviconUrl);
                return faviconUrl;
            }
            
            // 方法2: 解析HTML获取各种图标（类似Python的parse_html_for_icons）
            String htmlContent = fetchHtmlContent(finalUrl);
            if (htmlContent != null) {
                List<String> iconUrls = parseHtmlForIcons(htmlContent, baseUrl);
                log.info("找到 {} 个候选图标链接", iconUrls.size());
                
                for (String iconUrl : iconUrls) {
                    iconData = downloadImage(iconUrl);
                    if (iconData != null) {
                        log.info("成功获取图标: {}", iconUrl);
                        return iconUrl;
                    }
                }
            }
            
            log.warn("无法获取图标: {}", url);
            return null;
            
        } catch (Exception e) {
            log.error("获取图标失败: {}", url, e);
            return null;
        }
    }
    
    /**
     * 下载图像并验证是否为有效图像（类似Python的download_image方法）
     * 不仅检查HTTP状态码，还验证内容类型是否为图像或SVG
     * 
     * @param url 图像URL
     * @return 图像数据字节数组，如果不是有效图像则返回null
     */
    private byte[] downloadImage(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (compatible; FaviconExtractor/1.0)")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String contentType = response.header("Content-Type", "");
                    if (contentType != null && 
                        (contentType.toLowerCase().contains("image") || 
                         isSvgFormat(url, contentType))) {
                        // 确认是有效图像，返回数据
                        return response.body().bytes();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("下载图像失败 {}: {}", url, e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取重定向后的最终URL（类似Python的get_final_url_after_redirects方法）
     * 使用HEAD请求跟踪重定向，更高效
     * 
     * @param url 原始URL
     * @return 重定向后的最终URL，失败返回原URL
     */
    private String getFinalUrlAfterRedirects(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .head()
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String finalUrl = response.request().url().toString();
                log.info("重定向跟踪: {} -> {}", url, finalUrl);
                return finalUrl;
            }
        } catch (Exception e) {
            log.warn("跟踪重定向失败 {}: {}", url, e.getMessage());
            return url; // 如果失败，返回原URL
        }
    }
    
    /**
     * 下载图标数据
     * 
     * @param iconUrl 图标URL
     * @return 图标数据字节数组，失败返回null
     */
    private byte[] downloadIconData(String iconUrl) {
        try {
            Request request = new Request.Builder()
                    .url(iconUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (compatible; FaviconExtractor/1.0)")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String contentType = response.header("Content-Type", "");
                    assert contentType != null;
                    if (contentType.toLowerCase().contains("image") ||
                        iconUrl.toLowerCase().endsWith(".svg") ||
                        iconUrl.toLowerCase().endsWith(".ico")) {
                        return response.body().bytes();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("下载图标失败 {}: {}", iconUrl, e.getMessage());
        }
        return null;
    }
    
    /**
     * 解析HTML获取各种图标链接（类似Python的parse_html_for_icons方法）
     * 包含完整的优先级计算和排序
     * 
     * @param htmlContent HTML内容
     * @param baseUrl 基本URL
     * @return 图标URL列表（按优先级排序）
     */
    private List<String> parseHtmlForIcons(String htmlContent, String baseUrl) {
        List<IconUrlWithPriority> iconList = new ArrayList<>();
        List<String> iconUrls = new ArrayList<>();
        
        try {
            Document doc = Jsoup.parse(htmlContent);
            
            // 标准的icon链接
            Elements iconLinks = doc.select("link[rel~=(?i)(icon|shortcut\\s+icon)]");
            for (Element link : iconLinks) {
                String href = link.attr("href");
                if (href != null && !href.trim().isEmpty()) {
                    String absoluteUrl = resolveUrl(baseUrl, href.trim());
                    String rel = link.attr("rel").toLowerCase();
                    String sizes = link.attr("sizes");
                    String type = link.attr("type");
                    
                    int priority = getIconPriority(rel, sizes, absoluteUrl, type);
                    iconList.add(new IconUrlWithPriority(absoluteUrl, priority));
                    log.debug("找到icon链接: {} -> {} (优先级: {})", href, absoluteUrl, priority);
                }
            }
            
            // Apple touch icon
            Elements appleTouchIcons = doc.select("link[rel~=(?i)apple-touch-icon]");
            for (Element link : appleTouchIcons) {
                String href = link.attr("href");
                if (href != null && !href.trim().isEmpty()) {
                    String absoluteUrl = resolveUrl(baseUrl, href.trim());
                    String sizes = link.attr("sizes");
                    String type = link.attr("type");
                    
                    int priority = getIconPriority("apple-touch-icon", sizes, absoluteUrl, type);
                    iconList.add(new IconUrlWithPriority(absoluteUrl, priority));
                    log.debug("找到apple-touch-icon: {} -> {} (优先级: {})", href, absoluteUrl, priority);
                }
            }
            
            // Twitter卡片图标
            Elements twitterImages = doc.select("meta[name=twitter:image]");
            for (Element meta : twitterImages) {
                String content = meta.attr("content");
                if (content != null && !content.trim().isEmpty()) {
                    String absoluteUrl = resolveUrl(baseUrl, content.trim());
                    int priority = getIconPriority("twitter:image", "", absoluteUrl, "");
                    iconList.add(new IconUrlWithPriority(absoluteUrl, priority));
                    log.debug("找到twitter:image: {} -> {} (优先级: {})", content, absoluteUrl, priority);
                }
            }
            
            // Safari mask icon
            Elements maskIcons = doc.select("link[rel~=(?i)mask-icon]");
            for (Element link : maskIcons) {
                String href = link.attr("href");
                if (href != null && !href.trim().isEmpty()) {
                    String absoluteUrl = resolveUrl(baseUrl, href.trim());
                    String type = link.attr("type");
                    
                    int priority = getIconPriority("mask-icon", "", absoluteUrl, type);
                    iconList.add(new IconUrlWithPriority(absoluteUrl, priority));
                    log.debug("找到mask-icon: {} -> {} (优先级: {})", href, absoluteUrl, priority);
                }
            }
            
            // 按优先级排序（优先级高的在前面）
            iconList.sort((a, b) -> Integer.compare(b.priority, a.priority));
            
            // 提取排序后的URL列表
            for (IconUrlWithPriority item : iconList) {
                iconUrls.add(item.url);
            }
            
        } catch (Exception e) {
            log.warn("解析HTML获取图标链接失败", e);
        }
        
        return iconUrls;
    }
    
    /**
     * 根据rel属性、sizes、URL和type确定优先级（类似Python的get_icon_priority方法）
     * 
     * @param rel rel属性
     * @param sizes sizes属性
     * @param url 图标URL
     * @param type type属性
     * @return 优先级数值
     */
    private int getIconPriority(String rel, String sizes, String url, String type) {
        int priority = 5; // 基础优先级
        
        // SVG格式获得最高优先级加成
        if (isSvgFormat(url, type)) {
            priority += 10;
            log.debug("SVG格式，优先级+10: {}", url);
        }
        
        // PNG/JPG格式优先级加成
        if (isPngOrJpgFormat(url, type)) {
            priority += 5;
            log.debug("PNG或者JPG格式，优先级+5: {}", url);
        }
        
        // 根据rel类型调整优先级
        if (rel.contains("apple-touch-icon")) {
            priority += 3;
        } else if (rel.contains("icon")) {
            priority += 2;
        } else if (rel.contains("mask-icon")) {
            priority += 1;
        }
        
        // 根据尺寸调整优先级
        if (sizes != null && !sizes.trim().isEmpty()) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)x(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(sizes);
            if (matcher.find()) {
                try {
                    int width = Integer.parseInt(matcher.group(1));
                    int height = Integer.parseInt(matcher.group(2));
                    if (width >= 64 && height >= 64) {
                        priority += 3;
                    } else if (width >= 32 && height >= 32) {
                        priority += 2;
                    } else if (width >= 16 && height >= 16) {
                        priority += 1;
                    }
                } catch (NumberFormatException e) {
                    // 忽略格式错误
                }
            }
        }
        
        // 根据文件扩展名调整优先级
        if (url != null) {
            String urlLower = url.toLowerCase();
            if (urlLower.endsWith(".svg")) {
                priority += 10; // SVG最高优先级
            } else if (urlLower.endsWith(".png")) {
                priority += 3;
            } else if (urlLower.endsWith(".jpg") || urlLower.endsWith(".jpeg")) {
                priority += 2;
            } else if (urlLower.endsWith(".ico")) {
                priority += 1;
            }
        }
        
        return priority;
    }
    
    /**
     * 判断是否为SVG格式（类似Python的is_svg_format方法）
     */
    private boolean isSvgFormat(String url, String contentType) {
        if (contentType != null && contentType.toLowerCase().contains("svg")) {
            return true;
        }
        if (url != null && url.toLowerCase().endsWith(".svg")) {
            return true;
        }
        return false;
    }
    
    /**
     * 判断是否为PNG或JPG格式（类似Python的is_png_or_jpg_format方法）
     */
    private boolean isPngOrJpgFormat(String url, String contentType) {
        if (contentType != null && contentType.toLowerCase().contains("png")) {
            return true;
        }
        if (url != null && url.toLowerCase().endsWith(".png")) {
            return true;
        }
        if (contentType != null && contentType.toLowerCase().contains("jpg")) {
            return true;
        }
        if (url != null && url.toLowerCase().endsWith(".jpg")) {
            return true;
        }
        return false;
    }
    
    /**
     * 内部类用于存储URL和优先级
     */
    private static class IconUrlWithPriority {
        String url;
        int priority;
        
        IconUrlWithPriority(String url, int priority) {
            this.url = url;
            this.priority = priority;
        }
    }
    
    /**
     * 根据URL确定优先级（SVG最高，PNG/JPG次之，ICO最低）
     * 
     * @param url 图标URL
     * @return 优先级数值
     */
    private int getUrlPriority(String url) {
        if (url == null) {
            return 0;
        }
        
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".svg")) {
            return 10;
        }
        if (lowerUrl.endsWith(".png")) {
            return 8;
        }
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return 7;
        }
        if (lowerUrl.endsWith(".ico")) {
            return 5;
        }
        return 3; // 其他格式
    }
}