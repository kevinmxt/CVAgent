package me.maxt.cv.web.dto.request;

/**
 * CV 内容手动修改请求 DTO。
 *
 * @author maxt
 * @since 1.0
 */
public class CvContentUpdateRequest {

    /** 修改后的 HTML 简历内容（必填） */
    private String finalContent;

    public String getFinalContent() { return finalContent; }
    public void setFinalContent(String finalContent) { this.finalContent = finalContent; }

    /**
     * 校验内容是否为空。
     *
     * @throws IllegalArgumentException 如果内容为空
     */
    public void validate() {
        if (finalContent == null || finalContent.isBlank()) {
            throw new IllegalArgumentException("finalContent 不能为空");
        }
    }
}
