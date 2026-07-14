package com.whatiread.config.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class ResourcePoolMetricsBinder {

    private final MeterRegistry meterRegistry;
    private final ObjectProvider<WebServerApplicationContext> webServerApplicationContext;

    public ResourcePoolMetricsBinder(
            MeterRegistry meterRegistry,
            ObjectProvider<WebServerApplicationContext> webServerApplicationContext
    ) {
        this.meterRegistry = meterRegistry;
        this.webServerApplicationContext = webServerApplicationContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    void bindHttpWorkerPool() {
        webServerApplicationContext.ifAvailable(ctx -> bindTomcatThreadPool(meterRegistry, ctx));
    }

    private void bindTomcatThreadPool(MeterRegistry meterRegistry, WebServerApplicationContext webServerApplicationContext) {
        if (!(webServerApplicationContext.getWebServer() instanceof TomcatWebServer tomcatWebServer)) {
            return;
        }
        var executor = tomcatWebServer.getTomcat().getConnector().getProtocolHandler().getExecutor();
        if (!(executor instanceof org.apache.tomcat.util.threads.ThreadPoolExecutor tomcatExecutor)) {
            return;
        }
        Gauge.builder("thread.pool.size", tomcatExecutor, org.apache.tomcat.util.threads.ThreadPoolExecutor::getPoolSize)
                .description("Thread pool current size")
                .tag("pool", "http-workers")
                .register(meterRegistry);
        Gauge.builder("thread.pool.active", tomcatExecutor, org.apache.tomcat.util.threads.ThreadPoolExecutor::getActiveCount)
                .description("Active threads in pool")
                .tag("pool", "http-workers")
                .register(meterRegistry);
        Gauge.builder("thread.pool.queue.depth", tomcatExecutor, e -> e.getQueue().size())
                .description("Queued tasks waiting for a thread")
                .tag("pool", "http-workers")
                .register(meterRegistry);
    }
}
