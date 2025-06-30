package com.techishthoughts.stocks.infrastructure.logging;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced context object to hold comprehensive request lifecycle information.
 * This class is designed to track HTTP request processing through various
 * stages and provide detailed logging capabilities.
 */
public final class RequestContext {

    /** HTTP status code for successful responses (200). */
    private static final int HTTP_OK = 200;

    /** HTTP status code for client error responses (300). */
    private static final int HTTP_REDIRECT = 300;


    /** Unique identifier for the request. */
    private String requestId;

    /** HTTP method (GET, POST, etc.). */
    private String method;

    /** Request URI. */
    private String uri;

    /** HTTP headers map. */
    private Map<String, String> headers;

    /** Request body content. */
    private String requestBody;

    /** Response body content. */
    private String responseBody;

    /** HTTP status code. */
    private Integer statusCode;

    /** Error message if any. */
    private String error;

    /** User agent string. */
    private String userAgent;

    /** Content type header. */
    private String contentType;

    /** Authorization header. */
    private String authorization;

    /** Request start time. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime startTime;

    /** Request end time. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime endTime;

    /** List of route steps for tracking processing flow. */
    private List<RouteStep> routeSteps;

    /** Additional metadata map. */
    private Map<String, Object> metadata;

    /**
     * Default constructor.
     * Initializes collections to prevent null pointer exceptions.
     */
    public RequestContext() {
        this.headers = new HashMap<>();
        this.routeSteps = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Constructor with basic request information.
     *
     * @param requestId unique request identifier
     * @param method HTTP method
     * @param uri request URI
     */
    public RequestContext(final String requestId, final String method,
            final String uri) {
        this();
        this.requestId = requestId;
        this.method = method;
        this.uri = uri;
        this.startTime = LocalDateTime.now();
    }

    /**
     * Gets the request ID.
     *
     * @return the request ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Sets the request ID.
     *
     * @param requestId the request ID to set
     */
    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    /**
     * Gets the HTTP method.
     *
     * @return the HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the HTTP method.
     *
     * @param method the HTTP method to set
     */
    public void setMethod(final String method) {
        this.method = method;
    }

    /**
     * Gets the request URI.
     *
     * @return the request URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the request URI.
     *
     * @param uri the request URI to set
     */
    public void setUri(final String uri) {
        this.uri = uri;
    }

    /**
     * Gets the headers map.
     *
     * @return the headers map
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Sets the headers map.
     *
     * @param headers the headers map to set
     */
    public void setHeaders(final Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Gets the request body.
     *
     * @return the request body
     */
    public String getRequestBody() {
        return requestBody;
    }

    /**
     * Sets the request body.
     *
     * @param requestBody the request body to set
     */
    public void setRequestBody(final String requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Gets the response body.
     *
     * @return the response body
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Sets the response body.
     *
     * @param responseBody the response body to set
     */
    public void setResponseBody(final String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return the HTTP status code
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP status code.
     *
     * @param statusCode the HTTP status code to set
     */
    public void setStatusCode(final Integer statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the error message.
     *
     * @return the error message
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error message.
     *
     * @param error the error message to set
     */
    public void setError(final String error) {
        this.error = error;
    }

    /**
     * Gets the user agent.
     *
     * @return the user agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the user agent.
     *
     * @param userAgent the user agent to set
     */
    public void setUserAgent(final String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Gets the content type.
     *
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type.
     *
     * @param contentType the content type to set
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the authorization header.
     *
     * @return the authorization header
     */
    public String getAuthorization() {
        return authorization;
    }

    /**
     * Sets the authorization header.
     *
     * @param authorization the authorization header to set
     */
    public void setAuthorization(final String authorization) {
        this.authorization = authorization;
    }

    /**
     * Gets the start time.
     *
     * @return the start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time.
     *
     * @param startTime the start time to set
     */
    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the end time.
     *
     * @return the end time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time.
     *
     * @param endTime the end time to set
     */
    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Gets the route steps list.
     *
     * @return the route steps list
     */
    public List<RouteStep> getRouteSteps() {
        return routeSteps;
    }

    /**
     * Sets the route steps list.
     *
     * @param routeSteps the route steps list to set
     */
    public void setRouteSteps(final List<RouteStep> routeSteps) {
        this.routeSteps = routeSteps;
    }

    /**
     * Gets the metadata map.
     *
     * @return the metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata map.
     *
     * @param metadata the metadata map to set
     */
    public void setMetadata(final Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Add a header to the context.
     *
     * @param name header name
     * @param value header value
     */
    public void addHeader(final String name, final String value) {
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
        this.headers.put(name, value);
    }

    /**
     * Add a route step to track processing flow.
     *
     * @param method HTTP method
     * @param uri request URI
     * @param status HTTP status code
     * @param requestBody request body content
     * @param responseBody response body content
     */
    public void addRouteStep(final String method, final String uri,
            final Integer status, final String requestBody,
            final String responseBody) {
        if (this.routeSteps == null) {
            this.routeSteps = new ArrayList<>();
        }
        final RouteStep step = new RouteStep();
        step.setMethod(method);
        step.setUri(uri);
        step.setStatus(status);
        step.setRequestBody(requestBody);
        step.setResponseBody(responseBody);
        step.setTimestamp(LocalDateTime.now());
        this.routeSteps.add(step);
    }

    /**
     * Calculate request duration in milliseconds.
     *
     * @return duration in milliseconds
     */
    @JsonIgnore
    public long getDurationMs() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return ChronoUnit.MILLIS.between(startTime, endTime);
    }

    /**
     * Check if request was successful.
     *
     * @return true if successful (2xx status code)
     */
    @JsonIgnore
    public boolean isSuccessful() {
        return statusCode != null && statusCode >= HTTP_OK
                && statusCode < HTTP_REDIRECT;
    }

    /**
     * Get total number of route steps.
     *
     * @return number of route steps
     */
    @JsonIgnore
    public int getRouteStepCount() {
        return routeSteps != null ? routeSteps.size() : 0;
    }

    /**
     * Convert to the expected JSON log format.
     *
     * @return log data as map
     */
    public Map<String, Object> toLogFormat() {
        final Map<String, Object> logData = new HashMap<>();

        logData.put("method", method);
        logData.put("uri", uri);
        logData.put("headers", headers != null ? headers : new HashMap<>());
        logData.put("requestBody", requestBody);
        logData.put("status", statusCode);
        logData.put("responseBody", responseBody);
        logData.put("routesSteps",
                routeSteps != null ? routeSteps : new ArrayList<>());

        return logData;
    }

    /**
     * Builder pattern for creating RequestContext.
     *
     * @return new RequestContextBuilder instance
     */
    public static RequestContextBuilder builder() {
        return new RequestContextBuilder();
    }

    /**
     * Builder class for RequestContext.
     */
    public static final class RequestContextBuilder {
        /** Request ID for builder. */
        private String requestId;

        /** HTTP method for builder. */
        private String method;

        /** Request URI for builder. */
        private String uri;

        /** Headers map for builder. */
        private Map<String, String> headers;

        /** Start time for builder. */
        private LocalDateTime startTime;

        /** Route steps for builder. */
        private List<RouteStep> routeSteps;

        /** Metadata for builder. */
        private Map<String, Object> metadata;

        /**
         * Sets the request ID.
         *
         * @param requestId the request ID
         * @return this builder instance
         */
        public RequestContextBuilder requestId(final String requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * Sets the HTTP method.
         *
         * @param method the HTTP method
         * @return this builder instance
         */
        public RequestContextBuilder method(final String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the request URI.
         *
         * @param uri the request URI
         * @return this builder instance
         */
        public RequestContextBuilder uri(final String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Sets the headers map.
         *
         * @param headers the headers map
         * @return this builder instance
         */
        public RequestContextBuilder headers(final Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the start time.
         *
         * @param startTime the start time
         * @return this builder instance
         */
        public RequestContextBuilder startTime(final LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * Sets the route steps.
         *
         * @param routeSteps the route steps list
         * @return this builder instance
         */
        public RequestContextBuilder routeSteps(final List<RouteStep> routeSteps) {
            this.routeSteps = routeSteps;
            return this;
        }

        /**
         * Sets the metadata.
         *
         * @param metadata the metadata map
         * @return this builder instance
         */
        public RequestContextBuilder metadata(final Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds the RequestContext instance.
         *
         * @return new RequestContext instance
         */
        public RequestContext build() {
            final RequestContext context = new RequestContext();
            context.setRequestId(this.requestId);
            context.setMethod(this.method);
            context.setUri(this.uri);
            context.setHeaders(this.headers != null
                    ? this.headers : new HashMap<>());
            context.setStartTime(this.startTime != null
                    ? this.startTime : LocalDateTime.now());
            context.setRouteSteps(this.routeSteps != null
                    ? this.routeSteps : new ArrayList<>());
            context.setMetadata(this.metadata != null
                    ? this.metadata : new HashMap<>());
            return context;
        }
    }
}
