package com.whatiread.config.datasource;

public final class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceType> CONTEXT = ThreadLocal.withInitial(() -> DataSourceType.WRITE);

    private DataSourceContextHolder() {
    }

    public static void setReadOnly() {
        CONTEXT.set(DataSourceType.READ);
    }

    public static void setWrite() {
        CONTEXT.set(DataSourceType.WRITE);
    }

    public static DataSourceType current() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
