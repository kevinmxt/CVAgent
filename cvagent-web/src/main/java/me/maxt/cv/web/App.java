package me.maxt.cv.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import me.maxt.cv.agent.ChatModelProvider;
import me.maxt.cv.config.AgentPromptConfig;
import me.maxt.cv.config.AppConfig;
import dev.langchain4j.model.chat.ChatModel;
import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.repository.CvScoringResultRepository;
import me.maxt.cv.store.repository.CvTemplateRepository;
import me.maxt.cv.store.repository.GeneratedCvRepository;
import me.maxt.cv.store.repository.JobDescriptionRepository;
import me.maxt.cv.store.repository.WorkExperienceRepository;
import me.maxt.cv.service.CvGenerationService;
import me.maxt.cv.service.CvScoringResultService;
import me.maxt.cv.service.CvTemplateService;
import me.maxt.cv.service.ExportService;
import me.maxt.cv.service.JobDescriptionService;
import me.maxt.cv.service.WorkExperienceService;
import me.maxt.cv.web.interceptor.CorsHandler;
import me.maxt.cv.web.interceptor.ExceptionHandler;
import me.maxt.cv.web.route.CvGenerationRoutes;
import me.maxt.cv.web.route.CvTemplateRoutes;
import me.maxt.cv.web.route.JobDescriptionRoutes;
import me.maxt.cv.web.route.WorkExperienceRoutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CVAgent 应用启动类。
 *
 * <p>负责初始化数据库、创建服务实例、配置 Javalin 路由并启动 HTTP 服务器。
 * 所有组件采用手动依赖注入方式组装。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    /**
     * 应用入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        log.info("CVAgent 启动中...");

        // 1. 加载配置
        AppConfig config = AppConfig.load();
        log.info("配置加载完成: port={}, dbMode={}, llmProvider={}",
                config.getServerPort(), config.getDbMode(), config.getLlmProvider());

        // 2. 初始化数据库
        DataSourceConfig.initialize(config);

        // 3. 创建 Repository 实例
        WorkExperienceRepository workExpRepo = new WorkExperienceRepository();
        CvTemplateRepository templateRepo = new CvTemplateRepository();
        JobDescriptionRepository jdRepo = new JobDescriptionRepository();
        GeneratedCvRepository generatedCvRepo = new GeneratedCvRepository();

        // 4. 创建 LLM 模型（用于工作经历智能解析）
        ChatModel chatModel = ChatModelProvider.createChatModel(config);

        // 5. 创建 Service 实例
        WorkExperienceService workExpService = new WorkExperienceService(workExpRepo);
        workExpService.setChatModel(chatModel);
        // 6. 创建 Service 实例（续）
        CvTemplateService templateService = new CvTemplateService(templateRepo);
        templateService.setChatModel(chatModel);
        JobDescriptionService jdService = new JobDescriptionService(jdRepo);
        CvGenerationService cvGenService = new CvGenerationService(
                workExpRepo, templateRepo, jdRepo, generatedCvRepo);
        cvGenService.setChatModel(chatModel);
        cvGenService.setMaxRawContentLength(config.getMaxTokens());
        CvScoringResultRepository scoringResultRepo = new CvScoringResultRepository();
        CvScoringResultService scoringResultService = new CvScoringResultService(
                scoringResultRepo, generatedCvRepo, jdRepo);
        ExportService exportService = new ExportService();

        // 7. 创建 Agent 相关配置
        AgentPromptConfig promptConfig = new AgentPromptConfig(config);

        // 8. 创建 Javalin 实例
        Javalin app = Javalin.create(javalinConfig -> {
            // 配置 Jackson 序列化 Java 8 时间类型为 ISO-8601 字符串
            javalinConfig.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));
            javalinConfig.http.defaultContentType = "application/json; charset=UTF-8";
            javalinConfig.staticFiles.add(staticFileConfig -> {
                staticFileConfig.directory = "public";
                staticFileConfig.location = io.javalin.http.staticfiles.Location.CLASSPATH;
            });
        });

        // 9. 注册全局拦截器
        app.before(new CorsHandler());
        new ExceptionHandler().register(app);

        // 10. 注册路由
        new WorkExperienceRoutes(workExpService).register(app);
        new CvTemplateRoutes(templateService).register(app);
        new JobDescriptionRoutes(jdService).register(app);
        new CvGenerationRoutes(cvGenService, scoringResultService, exportService, config, promptConfig, jdRepo).register(app);

        // 11. SPA 回退：404 时返回 index.html
        app.error(404, ctx -> {
            if (!ctx.path().startsWith("/api")) {
                ctx.contentType("text/html; charset=UTF-8");
                try (var stream = App.class.getClassLoader().getResourceAsStream("public/index.html")) {
                    if (stream != null) {
                        ctx.result(stream);
                    }
                }
            }
        });

        // 12. 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("CVAgent 关闭中...");
            app.stop();
            DataSourceConfig.shutdown();
        }));

        // 13. 启动服务器
        app.start(config.getServerPort());
        log.info("CVAgent 启动完成: http://localhost:{}", config.getServerPort());
    }
}
