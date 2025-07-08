-- ClickHouse Initialization SQL for Logs Database
-- This file sets up the logs table for storing application logs with optimal performance

-- Create the logs database if it doesn't exist
CREATE DATABASE IF NOT EXISTS logs;

-- Use the logs database
USE logs;

-- Create the main logs table optimized for log storage and analytics
CREATE TABLE IF NOT EXISTS application_logs (
    -- Timestamp fields for time-based queries and partitioning
    timestamp DateTime64(3) CODEC(Delta, ZSTD),
    date Date DEFAULT toDate(timestamp) CODEC(ZSTD),

    -- Service identification
    service_name LowCardinality(String) CODEC(ZSTD),
    service_version LowCardinality(String) CODEC(ZSTD),
    deployment_environment LowCardinality(String) CODEC(ZSTD),

    -- Log level and basic metadata
    level LowCardinality(String) CODEC(ZSTD),
    logger_name String CODEC(ZSTD),
    message String CODEC(ZSTD),

    -- Tracing integration
    trace_id String CODEC(ZSTD),
    span_id String CODEC(ZSTD),

    -- Request context
    request_id String CODEC(ZSTD),
    method LowCardinality(String) CODEC(ZSTD),
    uri String CODEC(ZSTD),
    status_code UInt16 CODEC(ZSTD),
    response_time_ms UInt32 CODEC(ZSTD),

    -- Thread and process information
    thread_name String CODEC(ZSTD),
    process_id UInt32 CODEC(ZSTD),

    -- Exception handling
    exception_class String CODEC(ZSTD),
    exception_message String CODEC(ZSTD),
    stack_trace String CODEC(ZSTD),

    -- Business context (stock-specific)
    stock_symbol LowCardinality(String) CODEC(ZSTD),
    actor_path String CODEC(ZSTD),

    -- Additional attributes as JSON for flexibility
    attributes Map(String, String) CODEC(ZSTD),

    -- Raw log entry for debugging
    raw_log String CODEC(ZSTD)
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (service_name, level, timestamp, logger_name)
TTL timestamp + INTERVAL 30 DAY
SETTINGS index_granularity = 8192;

-- Create materialized view for aggregated metrics (optional performance optimization)
CREATE MATERIALIZED VIEW IF NOT EXISTS logs_hourly_stats
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMM(date)
ORDER BY (service_name, level, hour, logger_name)
AS SELECT
    service_name,
    level,
    logger_name,
    toStartOfHour(timestamp) as hour,
    toDate(timestamp) as date,
    count() as log_count,
    countIf(level = 'ERROR') as error_count,
    countIf(level = 'WARN') as warn_count,
    countIf(level = 'INFO') as info_count,
    countIf(level = 'DEBUG') as debug_count,
    avgIf(response_time_ms, response_time_ms > 0) as avg_response_time_ms,
    maxIf(response_time_ms, response_time_ms > 0) as max_response_time_ms
FROM application_logs
GROUP BY service_name, level, logger_name, hour, date;

-- Create indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_trace_id ON application_logs (trace_id) TYPE bloom_filter GRANULARITY 1;
CREATE INDEX IF NOT EXISTS idx_request_id ON application_logs (request_id) TYPE bloom_filter GRANULARITY 1;
CREATE INDEX IF NOT EXISTS idx_stock_symbol ON application_logs (stock_symbol) TYPE bloom_filter GRANULARITY 1;
CREATE INDEX IF NOT EXISTS idx_exception_class ON application_logs (exception_class) TYPE bloom_filter GRANULARITY 1;

-- Insert some sample data for testing (will be replaced by real logs)
INSERT INTO application_logs (
    timestamp, service_name, service_version, deployment_environment,
    level, logger_name, message, trace_id, span_id
) VALUES (
    now(), 'stocks-service', '1.0.0', 'docker',
    'INFO', 'com.techishthoughts.stocks', 'ClickHouse logging initialized',
    'test-trace-id', 'test-span-id'
);

-- Grant permissions for the OTEL Collector to insert data
-- Note: In production, create a dedicated user with limited permissions
GRANT SELECT, INSERT ON logs.* TO default;
