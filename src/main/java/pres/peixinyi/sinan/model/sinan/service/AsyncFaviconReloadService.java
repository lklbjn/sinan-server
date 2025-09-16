package pres.peixinyi.sinan.model.sinan.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pres.peixinyi.sinan.model.favicon.service.FaviconService;
import pres.peixinyi.sinan.model.sinan.entity.SnBookmark;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class AsyncFaviconReloadService {

    private final FaviconService faviconService;
    private final SnBookmarkService bookmarkService;

    @Async("taskExecutor")
    public void reloadFaviconsAsync(String userId, boolean force) {
        try {
            int count = 0;
            List<SnBookmark> bookmarks = bookmarkService.lambdaQuery()
                    .eq(SnBookmark::getUserId, userId)
                    .eq(SnBookmark::getDeleted, 0)
                    .isNull(!force, SnBookmark::getIcon)
                    .orderByDesc(SnBookmark::getUpdateTime)
                    .list();
            log.info("异步任务启动: 待处理总书签数: {}", bookmarks.size());

            for (SnBookmark bookmark : bookmarks) {
                count++;
                log.info("进度: {}/{}", count, bookmarks.size());
                String iconUrl = faviconService.getIconUrl(bookmark.getUrl());
                if (iconUrl != null) {
                    bookmark.setIcon(iconUrl);
                }
            }

            bookmarkService.updateBatchById(bookmarks);
            log.info("异步任务完成: 成功更新 {} 个书签的图标", bookmarks.size());
        } catch (Exception e) {
            log.error("异步更新favicon失败", e);
        }
    }
}
