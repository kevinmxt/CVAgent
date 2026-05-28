package me.maxt.cv.web.dto.response;

import java.util.List;

/**
 * 通用分页响应 DTO。
 *
 * @param <T> 列表元素类型
 * @author maxt
 * @since 1.0
 */
public class PageResult<T> {

    /** 数据列表 */
    private List<T> items;
    /** 当前页码 */
    private int page;
    /** 每页条数 */
    private int size;
    /** 总记录数 */
    private int total;

    /**
     * 构造分页结果。
     *
     * @param items 数据列表
     * @param page  当前页码
     * @param size  每页条数
     * @param total 总记录数
     */
    public PageResult(List<T> items, int page, int size, int total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getTotal() { return total; }

    /**
     * 计算总页数。
     *
     * @return 总页数
     */
    public int getPages() {
        return (int) Math.ceil((double) total / size);
    }
}
