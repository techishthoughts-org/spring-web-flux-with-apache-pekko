package com.techishthoughts.stocks.infrastructure.logging;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single step in the request processing route.
 * This class tracks individual operations within a request lifecycle,
 * providing detailed information about each processing step.
 */
public final class RouteStep {

    /** HTTP status code for successful responses (200). */
    private static final int HTTP_OK = 200;

    /** HTTP status code for client error responses (300). */
    private static final int HTTP_REDIRECT = 300;

    /** HTTP status code for server error responses (500). */
    private static final int HTTP_SERVER_ERROR = 500;

    /** Name of the processing step. */
    private String stepName;

    /** Type of the processing step. */
    private String stepType;

    /** HTTP method used in this step. */
    private String method;

    /** URL accessed in this step. */
    private String url;

    /** Request body content. */
    private String requestBody;

    /** Response body content. */
    private String responseBody;

    /** HTTP status code returned. */
    private Integer statusCode;

    /** Error message if any. */
    private String error;

    /** Start time of this step. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime startTime;

    /** End time of this step. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime endTime;

    /** HTTP headers for this step. */
    private Map<String, String> headers;

    /** Additional metadata for this step. */
    private Map<String, Object> metadata;

    /** Timestamp when this step was created. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    /**
     * Default constructor.
     * Initializes collections and sets timestamp.
     */
    public RouteStep() {
        this.headers = new HashMap<>();
        this.metadata = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with basic step information.
     *
     * @param stepName name of the step
     * @param stepType type of the step
     * @param method HTTP method
     * @param url target URL
     */
    public RouteStep(final String stepName, final String stepType,
            final String method, final String url) {
        this();
        this.stepName = stepName;
        this.stepType = stepType;
        this.method = method;
        this.url = url;
        this.startTime = LocalDateTime.now();
    }

    /**
     * Gets the step name.
     *
     * @return the step name
     */
    public String getStepName() {
        return stepName;
    }

    /**
     * Sets the step name.
     *
     * @param stepName the step name to set
     */
    public void setStepName(final String stepName) {
        this.stepName = stepName;
    }

    /**
     * Gets the step type.
     *
     * @return the step type
     */
    public String getStepType() {
        return stepType;
    }

    /**
     * Sets the step type.
     *
     * @param stepType the step type to set
     */
    public void setStepType(final String stepType) {
        this.stepType = stepType;
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
     * Gets the URL.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL.
     *
     * @param url the URL to set
     */
    public void setUrl(final String url) {
        this.url = url;
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
     * Sets the URI (alias for setUrl).
     *
     * @param uri the URI to set
     */
    public void setUri(final String uri) {
        this.url = uri;
    }

    /**
     * Sets the status (alias for setStatusCode).
     *
     * @param status the status code to set
     */
    public void setStatus(final Integer status) {
        this.statusCode = status;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(final LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the timestamp.
     *
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Check if this step was successful.
     *
     * @return true if successful (2xx status code)
     */
    @JsonIgnore
    public boolean isSuccessful() {
        return statusCode != null && statusCode >= HTTP_OK
                && statusCode < HTTP_REDIRECT;
    }

    /**
     * Check if this step had an error.
     *
     * @return true if there was an error
     */
    @JsonIgnore
    public boolean hasError() {
        return error != null || (statusCode != null && statusCode >= HTTP_REDIRECT);
    }

    /**
     * Create a new RouteStep for HTTP request.
     *
     * @param stepName name of the step
     * @param method HTTP method
     * @param url target URL
     * @return new RouteStep instance
     */
    public static RouteStep forHttpRequest(final String stepName,
            final String method,
            final String url) {
        return new RouteStep(stepName, "HTTP_REQUEST", method, url);
    }

    /**
     * Create a new RouteStep for actor processing.
     *
     * @param stepName name of the step
     * @param actorType type of actor
     * @return new RouteStep instance
     */
    public static RouteStep forActor(final String stepName,
            final String actorType) {
        return new RouteStep(stepName, "ACTOR_PROCESSING", null, actorType);
    }

    /**
     * Create a new RouteStep for database operation.
     *
     * @param stepName name of the step
     * @param operation database operation
     * @return new RouteStep instance
     */
    public static RouteStep forDatabase(final String stepName,
            final String operation) {
        return new RouteStep(stepName, "DATABASE_OPERATION", null, operation);
    }

    /**
     * Complete this step with success.
     *
     * @param statusCode HTTP status code
     * @param responseBody response body content
     * @return this RouteStep instance
     */
    public RouteStep completeSuccess(final Integer statusCode,
            final String responseBody) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.endTime = LocalDateTime.now();
        return this;
    }

    /**
     * Complete this step with error.
     *
     * @param error error message
     * @return this RouteStep instance
     */
    public RouteStep completeError(final String error) {
        this.error = error;
        this.statusCode = HTTP_SERVER_ERROR;
        this.endTime = LocalDateTime.now();
        return this;
    }

    /**
     * Calculate step duration in milliseconds
     */
    public long getDurationMs() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return ChronoUnit.MILLIS.between(startTime, endTime);
    }

    /**
     * Create an actor call step.
     *
     * @param actorName name of the actor
     * @param actorType type of the actor
     * @return new RouteStep instance
     */
    public static RouteStep actorCall(final String actorName, final String actorType) {
        return new RouteStep(actorName, "ACTOR_CALL", null, actorType);
    }

    /**
     * Create an HTTP inbound step.
     *
     * @param stepName name of the step
     * @param method HTTP method
     * @param url target URL
     * @return new RouteStep instance
     */
    public static RouteStep httpIn(final String stepName, final String method, final String url) {
        return new RouteStep(stepName, "HTTP_IN", method, url);
    }

    /**
     * Create an HTTP outbound step.
     *
     * @param stepName name of the step
     * @param method HTTP method
     * @param url target URL
     * @return new RouteStep instance
     */
    public static RouteStep httpOut(final String stepName, final String method, final String url) {
        return new RouteStep(stepName, "HTTP_OUT", method, url);
    }

    /**
     * Complete this step with status code and response.
     *
     * @param statusCode HTTP status code
     * @param responseBody response body content
     * @return this RouteStep instance
     */
    public RouteStep complete(final Integer statusCode, final String responseBody) {
        return completeSuccess(statusCode, responseBody);
    }

    /**
     * Complete this step with error.
     *
     * @param error error message
     * @return this RouteStep instance
     */
    public RouteStep fail(final String error) {
        return completeError(error);
    }

    /**
     * Create a database operation step.
     *
     * @param stepName name of the step
     * @param operation database operation
     * @return new RouteStep instance
     */
    public static RouteStep database(final String stepName, final String operation) {
        return forDatabase(stepName, operation);
    }
}
