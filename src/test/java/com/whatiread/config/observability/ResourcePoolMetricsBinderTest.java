package com.whatiread.config.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;

class ResourcePoolMetricsBinderTest {

    private static final String METRIC_THREAD_POOL_SIZE = "thread.pool.size";
    private static final String METRIC_THREAD_POOL_ACTIVE = "thread.pool.active";
    private static final String METRIC_THREAD_POOL_QUEUE_DEPTH = "thread.pool.queue.depth";
    private static final String POOL_TAG = "pool";
    private static final String HTTP_WORKERS_POOL = "http-workers";

    @Test
    void bindHttpWorkerPoolSkipsNonTomcatServers() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        WebServerApplicationContext ctx = mock(WebServerApplicationContext.class);
        org.springframework.boot.web.server.WebServer webServer = mock(org.springframework.boot.web.server.WebServer.class);
        when(ctx.getWebServer()).thenReturn(webServer);

        @SuppressWarnings("unchecked")
        ObjectProvider<WebServerApplicationContext> provider = mock(ObjectProvider.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, java.util.function.Consumer.class)
                    .accept(ctx);
            return null;
        }).when(provider).ifAvailable(any());

        ResourcePoolMetricsBinder binder = new ResourcePoolMetricsBinder(registry, provider);
        binder.bindHttpWorkerPool();

        assertThat(registry.find(METRIC_THREAD_POOL_SIZE).tag(POOL_TAG, HTTP_WORKERS_POOL).gauge()).isNull();
    }

    @Test
    void bindHttpWorkerPoolRegistersTomcatExecutorGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        TomcatWebServer tomcatWebServer = mock(TomcatWebServer.class);
        org.apache.catalina.startup.Tomcat tomcat = mock(org.apache.catalina.startup.Tomcat.class);
        Connector connector = mock(Connector.class);
        ProtocolHandler protocolHandler = mock(ProtocolHandler.class);
        ThreadPoolExecutor tomcatExecutor = mock(ThreadPoolExecutor.class);
        when(tomcatExecutor.getPoolSize()).thenReturn(10);
        when(tomcatExecutor.getActiveCount()).thenReturn(2);
        when(tomcatExecutor.getQueue()).thenReturn(new java.util.concurrent.LinkedBlockingQueue<>());
        when(tomcatWebServer.getTomcat()).thenReturn(tomcat);
        when(tomcat.getConnector()).thenReturn(connector);
        when(connector.getProtocolHandler()).thenReturn(protocolHandler);
        when(protocolHandler.getExecutor()).thenReturn(tomcatExecutor);

        WebServerApplicationContext ctx = mock(WebServerApplicationContext.class);
        when(ctx.getWebServer()).thenReturn(tomcatWebServer);

        @SuppressWarnings("unchecked")
        ObjectProvider<WebServerApplicationContext> provider = mock(ObjectProvider.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, java.util.function.Consumer.class)
                    .accept(ctx);
            return null;
        }).when(provider).ifAvailable(any());

        ResourcePoolMetricsBinder binder = new ResourcePoolMetricsBinder(registry, provider);
        binder.bindHttpWorkerPool();

        assertThat(registry.find(METRIC_THREAD_POOL_SIZE).tag(POOL_TAG, HTTP_WORKERS_POOL).gauge()).isNotNull();
        assertThat(registry.find(METRIC_THREAD_POOL_ACTIVE).tag(POOL_TAG, HTTP_WORKERS_POOL).gauge()).isNotNull();
        assertThat(registry.find(METRIC_THREAD_POOL_QUEUE_DEPTH).tag(POOL_TAG, HTTP_WORKERS_POOL).gauge()).isNotNull();
    }
}
