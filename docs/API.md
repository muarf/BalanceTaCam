# API Documentation

This document describes how OSM Camera Mapper interacts with external APIs.

## 📡 OpenStreetMap API v0.6

### Base URL

**Production**: `https://api.openstreetmap.org/`  
**Development**: `https://master.apis.dev.openstreetmap.org/`

⚠️ **Warning**: Always use the development server for testing!

### Authentication

OSM uses **OAuth 1.0a** for authentication.

#### OAuth Flow

1. **Request Token**
   ```
   POST /oauth/request_token
   ```

2. **User Authorization**
   ```
   GET /oauth/authorize?oauth_token={request_token}
   ```

3. **Access Token**
   ```
   POST /oauth/access_token
   ```

### Endpoints Used

#### Get User Details

```http
GET /api/0.6/user/details.json
Authorization: OAuth 1.0a
```

**Response**:
```json
{
  "user": {
    "id": 123456,
    "display_name": "username",
    "account_created": "2020-01-01T00:00:00Z",
    "changesets": {
      "count": 42
    }
  }
}
```

#### Create Changeset

```http
PUT /api/0.6/changeset/create
Content-Type: text/xml
Authorization: OAuth 1.0a

<?xml version="1.0" encoding="UTF-8"?>
<osm>
  <changeset>
    <tag k="comment" v="Added surveillance camera via OSM Camera Mapper" />
    <tag k="source" v="survey" />
    <tag k="created_by" v="OSM Camera Mapper v1.0.0" />
  </changeset>
</osm>
```

**Response**: Changeset ID as plain text
```
12345678
```

#### Create Node

```http
PUT /api/0.6/node/create
Content-Type: text/xml
Authorization: OAuth 1.0a

<?xml version="1.0" encoding="UTF-8"?>
<osm>
  <node changeset="12345678" lat="48.8566" lon="2.3522">
    <tag k="man_made" v="surveillance" />
    <tag k="surveillance:type" v="camera" />
    <tag k="camera:type" v="dome" />
    <tag k="camera:mount" v="pole" />
    <tag k="surveillance" v="public" />
  </node>
</osm>
```

**Response**: Node ID as plain text
```
98765432
```

#### Close Changeset

```http
PUT /api/0.6/changeset/{id}/close
Authorization: OAuth 1.0a
```

**Response**: `200 OK`

### Error Handling

Common HTTP status codes:

- **200 OK**: Success
- **400 Bad Request**: Invalid XML or parameters
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource doesn't exist
- **409 Conflict**: Changeset already closed
- **429 Too Many Requests**: Rate limited
- **500 Internal Server Error**: OSM server error

### Rate Limits

OSM API has rate limits:
- Max 10,000 changesets per day
- Max 50,000 node creations per changeset
- Be respectful and don't spam

### Best Practices

1. **Always close changesets** after creating nodes
2. **Use meaningful changeset comments**
3. **Set appropriate created_by tag**
4. **Handle errors gracefully**
5. **Don't retry immediately** on rate limit errors
6. **Test on dev server first**

## 🗺️ Overpass API

### Base URL

`https://overpass-api.de/api/interpreter`

### Query Cameras

```http
GET /api/interpreter?data={query}
```

**Query Format**:
```
[out:json][timeout:25];
(
  node["man_made"="surveillance"]["surveillance:type"="camera"](48.8,2.3,48.9,2.4);
);
out body;
>;
out skel qt;
```

**Parameters**:
- Bounding box: `(south,west,north,east)`
- Timeout: 25 seconds
- Output format: JSON

**Response**:
```json
{
  "version": 0.6,
  "elements": [
    {
      "type": "node",
      "id": 123456789,
      "lat": 48.8566,
      "lon": 2.3522,
      "tags": {
        "man_made": "surveillance",
        "surveillance:type": "camera",
        "camera:type": "dome",
        "camera:mount": "pole",
        "surveillance": "public",
        "operator": "City of Paris"
      }
    }
  ]
}
```

### Query Optimization

1. **Limit search area**: Don't query huge bounding boxes
2. **Cache results**: Store in local database
3. **Debounce queries**: Don't query on every map move
4. **Use appropriate timeout**: Balance between speed and completeness

### Overpass QL Syntax

```overpassql
[out:json][timeout:25];

// Query nodes with specific tags in bounding box
(
  node["man_made"="surveillance"]["surveillance:type"="camera"](bbox);
);

// Output format
out body;  // Include all tags
>;         // Recurse
out skel qt;  // Output skeleton, sorted by quadtile
```

### Public Instances

- `https://overpass-api.de/` (main)
- `https://overpass.kumi.systems/`
- `https://maps.mail.ru/osm/tools/overpass/`

Choose the closest/fastest instance for your users.

### Rate Limits

- Max 2 parallel requests per IP
- Timeout: 180 seconds max
- Be respectful of public infrastructure

## 🔐 Security Considerations

### OAuth Tokens

- **Never log tokens** in production
- **Store securely** using EncryptedSharedPreferences
- **Rotate if compromised**
- **Don't hardcode** consumer keys (use BuildConfig)

### API Keys

```kotlin
// Bad ❌
const val CONSUMER_KEY = "my_key_12345"

// Good ✅
const val CONSUMER_KEY = BuildConfig.OSM_CONSUMER_KEY
```

Configure in `gradle.properties`:
```properties
OSM_CONSUMER_KEY=your_key_here
OSM_CONSUMER_SECRET=your_secret_here
```

### HTTPS Only

All API calls **must** use HTTPS:
```kotlin
const val BASE_URL = "https://api.openstreetmap.org/"  // ✅
const val BASE_URL = "http://api.openstreetmap.org/"   // ❌
```

## 🧪 Testing

### Mock Responses

Use OkHttp MockWebServer for testing:

```kotlin
@Test
fun testCreateCamera() {
    val mockResponse = MockResponse()
        .setResponseCode(200)
        .setBody("12345")
    
    mockWebServer.enqueue(mockResponse)
    
    // Test your repository
}
```

### Integration Tests

Test against OSM dev server:
```kotlin
const val TEST_BASE_URL = "https://master.apis.dev.openstreetmap.org/"
```

## 📚 References

- [OSM API v0.6 Documentation](https://wiki.openstreetmap.org/wiki/API_v0.6)
- [OAuth 1.0a Specification](https://oauth.net/core/1.0a/)
- [Overpass API Documentation](https://wiki.openstreetmap.org/wiki/Overpass_API)
- [Overpass QL Language Guide](https://wiki.openstreetmap.org/wiki/Overpass_API/Language_Guide)

## 🐛 Troubleshooting

### Common Issues

**401 Unauthorized**
- Check OAuth tokens are valid
- Verify signature is correct
- Ensure clock is synchronized

**409 Conflict**
- Changeset already closed
- Create a new changeset

**429 Rate Limited**
- Implement exponential backoff
- Wait before retrying
- Check your rate limit usage

**Timeout on Overpass**
- Reduce bounding box size
- Increase timeout value
- Try a different instance


