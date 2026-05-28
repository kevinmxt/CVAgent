package me.maxt.cv.common.error;

/**
 * 全局错误码枚举，按业务域分段定义。
 *
 * <p>编码规则：
 * <ul>
 *   <li>1000-1999：系统级错误</li>
 *   <li>2000-2999：参数校验错误</li>
 *   <li>3000-3999：业务错误</li>
 *   <li>4000-4999：Agent 相关错误</li>
 *   <li>5000-5999：认证与权限错误（预留）</li>
 * </ul>
 *
 * @author maxt
 * @since 1.0
 */
public enum ErrorCode {

    // ==================== 系统级错误 (1000-1999) ====================

    /** 系统内部未知错误 */
    SYSTEM_ERROR(1000, "系统内部错误"),
    /** 配置加载或解析错误 */
    CONFIG_ERROR(1001, "配置错误"),
    /** 数据库操作异常 */
    DATABASE_ERROR(1002, "数据库操作异常"),
    /** 文件操作异常（读写失败等） */
    FILE_OPERATION_ERROR(1003, "文件操作异常"),

    // ==================== 参数校验错误 (2000-2999) ====================

    /** 请求参数校验失败 */
    VALIDATION_ERROR(2000, "参数校验失败"),
    /** 不支持的文件类型 */
    FILE_TYPE_NOT_SUPPORTED(2001, "不支持的文件类型，支持的格式：txt、docx、html、pdf"),
    /** 文件内容为空 */
    FILE_EMPTY(2002, "文件内容为空"),
    /** 文件过大 */
    FILE_TOO_LARGE(2003, "文件大小超出限制"),
    /** 必填参数缺失 */
    REQUIRED_PARAM_MISSING(2004, "必填参数缺失"),

    // ==================== 业务错误 (3000-3999) ====================

    /** 工作经历不存在 */
    WORK_EXPERIENCE_NOT_FOUND(3001, "工作经历不存在"),
    /** 简历模板不存在 */
    CV_TEMPLATE_NOT_FOUND(3002, "简历模板不存在"),
    /** 岗位描述不存在 */
    JOB_DESCRIPTION_NOT_FOUND(3003, "岗位描述不存在"),
    /** 生成的简历不存在 */
    GENERATED_CV_NOT_FOUND(3004, "生成的简历不存在"),
    /** 不能删除预置模板 */
    CANNOT_DELETE_PRESET_TEMPLATE(3005, "预置模板不可删除"),
    /** 简历生成中，请稍后 */
    CV_GENERATION_IN_PROGRESS(3006, "简历正在生成中，请稍后再试"),

    // ==================== Agent 错误 (4000-4999) ====================

    /** Agent 执行过程中发生异常 */
    AGENT_EXECUTION_FAILED(4000, "Agent 执行失败"),
    /** 不支持的 LLM 提供者 */
    LLM_PROVIDER_NOT_SUPPORTED(4001, "不支持的 LLM 提供者"),
    /** 简历生成失败 */
    CV_GENERATION_FAILED(4002, "简历生成失败，请检查输入内容"),
    /** LLM 调用超时 */
    LLM_TIMEOUT(4003, "LLM 调用超时"),
    /** Agent 评分解析失败 */
    AGENT_RESULT_PARSE_ERROR(4004, "Agent 评分结果解析失败"),

    // ==================== 认证预留 (5000-5999) ====================

    /** 未认证 */
    UNAUTHORIZED(5000, "未认证，请先登录"),
    /** 无权限 */
    FORBIDDEN(5001, "无权限执行此操作");

    /** 错误码 */
    private final int code;
    /** 错误描述信息 */
    private final String message;

    /**
     * 构造错误码枚举值。
     *
     * @param code    错误码编号
     * @param message 错误描述信息
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码编号。
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取错误描述信息。
     *
     * @return 错误描述
     */
    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码编号查找对应的枚举值。
     *
     * @param code 错误码编号
     * @return 对应的 ErrorCode，找不到则返回 {@link #SYSTEM_ERROR}
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode ec : values()) {
            if (ec.code == code) {
                return ec;
            }
        }
        return SYSTEM_ERROR;
    }
}
