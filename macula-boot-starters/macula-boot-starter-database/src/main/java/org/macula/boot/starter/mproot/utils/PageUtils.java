package org.macula.boot.starter.mproot.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.macula.boot.starter.api.Search;

/**
 * <p>
 * <b>PageUtils</b> 分页实体构造器
 * </p>
 *
 * @author Rain
 * @since 2022-01-27
 */
public class PageUtils {
    public static <T> IPage<T> getPage(Search search) {
        return new Page<T>(search.getCurrent(), search.getSize());
    }
}
