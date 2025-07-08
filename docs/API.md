# API Documentation

## Overview

The Stock Service provides a RESTful API for retrieving stock information. The API follows reactive programming principles and returns data in JSON format with proper HTTP status codes.

## Base URL

```
http://localhost:8080
```

## 📋 Endpoints

### 1. Get Stock by Symbol

Retrieve detailed information for a specific stock symbol.

```http
GET /stocks/{symbol}
```

**Parameters:**
- `symbol` (path parameter): Stock symbol (e.g., "AAPL", "GOOGL")

**Example Request:**
```bash
curl -X GET http://localhost:8080/stocks/AAPL \
  -H "Accept: application/json"
```

**Success Response (200 OK):**
```json
{
  "symbol": "AAPL",
  "name": "Apple Inc.",
  "exchange": "NASDAQ",
  "assetType": "Common Stock",
  "ipoDate": "1980-12-12",
  "country": "US",
  "currency": "USD",
  "ipo": "1980-12-12",
  "marketCapitalization": 3000000000000.0,
  "phone": "+1-408-996-1010",
  "shareOutstanding": 16000000000.0,
  "ticker": "AAPL",
  "weburl": "https://www.apple.com",
  "logo": "https://static.finnhub.io/img/87768.png",
  "finnhubIndustry": "Technology",
  "lastUpdated": "2024-01-15T10:30:00Z"
}
```

**Error Responses:**

*404 Not Found* - Stock symbol not found:
```json
{
  "code": "STOCK_NOT_FOUND",
  "message": "Stock not found for symbol: INVALID",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/stocks/INVALID"
}
```

*400 Bad Request* - Invalid stock symbol:
```json
{
  "code": "INVALID_STOCK_SYMBOL",
  "message": "Invalid stock symbol 'abc123': Stock symbol must contain only uppercase letters, numbers, dots, and hyphens",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/stocks/abc123"
}
```

---

### 2. Get All Stocks

Retrieve information for all available stocks.

```http
GET /stocks
```

**Example Request:**
```bash
curl -X GET http://localhost:8080/stocks \
  -H "Accept: application/json"
```

**Success Response (200 OK):**
```json
[
  {
    "symbol": "AAPL",
    "name": "Apple Inc.",
    "exchange": "NASDAQ",
    "assetType": "Common Stock",
    "ipoDate": "1980-12-12",
    "country": "US",
    "currency": "USD",
    "ipo": "1980-12-12",
    "marketCapitalization": 3000000000000.0,
    "phone": "+1-408-996-1010",
    "shareOutstanding": 16000000000.0,
    "ticker": "AAPL",
    "weburl": "https://www.apple.com",
    "logo": "https://static.finnhub.io/img/87768.png",
    "finnhubIndustry": "Technology",
    "lastUpdated": "2024-01-15T10:30:00Z"
  },
  {
    "symbol": "GOOGL",
    "name": "Alphabet Inc.",
    "exchange": "NASDAQ",
    "assetType": "Common Stock",
    "ipoDate": "2004-08-19",
    "country": "US",
    "currency": "USD",
    "ipo": "2004-08-19",
    "marketCapitalization": 2000000000000.0,
    "phone": "+1-650-253-0000",
    "shareOutstanding": 13000000000.0,
    "ticker": "GOOGL",
    "weburl": "https://www.google.com",
    "logo": "https://static.finnhub.io/img/87768.png",
    "finnhubIndustry": "Technology",
    "lastUpdated": "2024-01-15T10:30:00Z"
  }
]
```

**Error Responses:**

*500 Internal Server Error* - Server error:
```json
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "An unexpected error occurred. Please try again later.",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/stocks"
}
```

---

### 3. Simple Health Check

A lightweight health check endpoint that bypasses Spring Boot's health aggregation.

```http
GET /health-simple
```

**Example Request:**
```bash
curl -X GET http://localhost:8080/health-simple \
  -H "Accept: application/json"
```

**Success Response (200 OK):**
```json
{
  "status": "UP",
  "timestamp": "1705315800000",
  "message": "Application is running"
}
```

## 🔧 Management Endpoints

The application exposes Spring Boot Actuator endpoints on the same port (8080):

### Health Check

```http
GET http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963170816,
        "free": 91943415808,
        "threshold": 10485760,
        "path": "/Users/developer/stock-service/.",
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    },
    "stockActorHealthIndicator": {
      "status": "UP",
      "details": {
        "status": "Background initialization completed",
        "description": "All stock data has been loaded successfully",
        "progress": "100%",
        "totalSymbols": 1000,
        "processedSymbols": 1000
      }
    }
  }
}
```

### Metrics

```http
GET http://localhost:8080/actuator/prometheus
```

Returns Prometheus-formatted metrics for monitoring.

### Application Info

```http
GET http://localhost:8080/actuator/info
```

Returns application information including version, build details, and configuration.

## 📊 Data Models

### Stock Entity

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| symbol | String | Stock symbol | "AAPL" |
| name | String | Company name | "Apple Inc." |
| exchange | String | Stock exchange | "NASDAQ" |
| assetType | String | Type of asset | "Common Stock" |
| ipoDate | String | IPO date | "1980-12-12" |
| country | String | Country code | "US" |
| currency | String | Currency code | "USD" |
| ipo | String | IPO date (duplicate) | "1980-12-12" |
| marketCapitalization | Double | Market cap in USD | 3000000000000.0 |
| phone | String | Company phone | "+1-408-996-1010" |
| shareOutstanding | Double | Outstanding shares | 16000000000.0 |
| ticker | String | Ticker symbol | "AAPL" |
| weburl | String | Company website | "https://www.apple.com" |
| logo | String | Logo URL | "https://static.finnhub.io/img/87768.png" |
| finnhubIndustry | String | Industry classification | "Technology" |
| lastUpdated | String (ISO 8601) | Last update timestamp | "2024-01-15T10:30:00Z" |

### Error Response

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| code | String | Error code | "STOCK_NOT_FOUND" |
| message | String | Error message | "Stock not found for symbol: INVALID" |
| timestamp | String (ISO 8601) | Error timestamp | "2024-01-15T10:30:00Z" |
| path | String | Request path | "/stocks/INVALID" |

## 🚦 HTTP Status Codes

| Code | Status | Description |
|------|--------|-------------|
| 200 | OK | Request successful |
| 400 | Bad Request | Invalid request parameters |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Server error |

## 🔍 Error Codes

| Code | Description | HTTP Status |
|------|-------------|-------------|
| STOCK_NOT_FOUND | Stock symbol not found | 404 |
| INVALID_STOCK_SYMBOL | Invalid stock symbol format | 400 |
| INVALID_ARGUMENT | Invalid request argument | 400 |
| INTERNAL_SERVER_ERROR | Unexpected server error | 500 |

## 🎯 Validation Rules

### Stock Symbol Validation

Stock symbols must adhere to the following rules:
- **Length**: Maximum 10 characters
- **Characters**: Only uppercase letters, numbers, dots, and hyphens
- **Pattern**: `^[A-Z0-9\.\-]+$`
- **Non-empty**: Cannot be null or blank

**Valid Examples:**
- `AAPL`
- `GOOGL`
- `BRK.A`
- `BRK-A`

**Invalid Examples:**
- `aapl` (lowercase)
- `APPLE123456789` (too long)
- `AAPL@` (invalid character)
- ` ` (empty/blank)

## 🔄 Reactive Streaming

The API uses reactive programming with Project Reactor:

### Single Stock (Mono)
```java
Mono<Stock> stock = webClient
    .get()
    .uri("/stocks/{symbol}", "AAPL")
    .retrieve()
    .bodyToMono(Stock.class);
```

### Multiple Stocks (Flux)
```java
Flux<Stock> stocks = webClient
    .get()
    .uri("/stocks")
    .retrieve()
    .bodyToFlux(Stock.class);
```

## 📱 Client Examples

### Java with WebClient
```java
WebClient client = WebClient.create("http://localhost:8080");

// Get single stock
Mono<Stock> apple = client
    .get()
    .uri("/stocks/AAPL")
    .retrieve()
    .bodyToMono(Stock.class);

// Get all stocks
Flux<Stock> allStocks = client
    .get()
    .uri("/stocks")
    .retrieve()
    .bodyToFlux(Stock.class);
```

### JavaScript with Fetch
```javascript
// Get single stock
fetch('http://localhost:8080/stocks/AAPL')
  .then(response => response.json())
  .then(stock => console.log(stock));

// Get all stocks
fetch('http://localhost:8080/stocks')
  .then(response => response.json())
  .then(stocks => console.log(stocks));
```

### Python with requests
```python
import requests

# Get single stock
response = requests.get('http://localhost:8080/stocks/AAPL')
stock = response.json()

# Get all stocks
response = requests.get('http://localhost:8080/stocks')
stocks = response.json()
```

## 🔐 Security

### Input Validation
- All input parameters are validated
- Stock symbols are sanitized and validated
- Request size limits are enforced

### Error Handling
- No sensitive information in error responses
- Generic error messages for security
- Proper HTTP status codes

### Rate Limiting
- Consider implementing rate limiting for production
- Monitor API usage patterns
- Implement circuit breakers for external dependencies

## 📈 Performance

### Caching
- Stock data is cached in memory
- Cache invalidation based on data freshness
- Configurable cache TTL

### Async Processing
- Non-blocking I/O operations
- Reactive streams for efficient resource usage
- Actor-based concurrent processing

### Monitoring
- Response time metrics
- Error rate tracking
- Throughput monitoring

## 🧪 Testing

### API Testing Examples

```bash
# Test valid stock symbol
curl -X GET "http://localhost:8080/stocks/AAPL" \
  -H "Accept: application/json"

# Test invalid stock symbol
curl -X GET "http://localhost:8080/stocks/invalid" \
  -H "Accept: application/json"

# Test all stocks endpoint
curl -X GET "http://localhost:8080/stocks" \
  -H "Accept: application/json"

# Test health check
curl -X GET "http://localhost:8080/health-simple" \
  -H "Accept: application/json"
```

### Performance Testing
```bash
# Load test with Apache Bench
ab -n 1000 -c 10 http://localhost:8080/stocks/AAPL

# Load test with wrk
wrk -t12 -c400 -d30s http://localhost:8080/stocks/AAPL
```

---

This API documentation provides comprehensive information for integrating with the Stock Service. For additional technical details, refer to the [Architecture Documentation](ARCHITECTURE.md).
