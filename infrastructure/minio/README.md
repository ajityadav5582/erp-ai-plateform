# MinIO Infrastructure

Object storage for the ERP AI Platform using MinIO, an S3-compatible storage solution.

## Directory Structure

```
infrastructure/minio/
├── config/
│   ├── minio.env               # Environment configuration
│   ├── minio-client.json       # MinIO client configuration
│   └── policy/                 # Bucket policies
│       ├── public-read.json
│       └── private.json
├── scripts/
│   ├── init-buckets.sh         # Bucket initialization script
│   └── healthcheck.sh          # Health check script
├── backups/
│   └── README.md               # Backup documentation
└── README.md                   # This file
```

## Quick Start

### Start MinIO

```bash
# Start MinIO with infrastructure services
docker compose -f compose.base.yml -f compose.infrastructure.yml up minio

# Access MinIO Console
open http://localhost:9001/

# Access MinIO API
open http://localhost:9000/
```

### Default Credentials

| Field | Value |
|-------|-------|
| Access Key | `minioadmin` |
| Secret Key | `minioadmin` |
| Console Port | `9001` |
| API Port | `9000` |

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MINIO_ROOT_USER` | Admin access key | `minioadmin` |
| `MINIO_ROOT_PASSWORD` | Admin secret key | `minioadmin` |
| `MINIO_REGION` | Region for buckets | `us-east-1` |
| `MINIO_BROWSER` | Enable web console | `true` |

### Docker Configuration

The MinIO service is defined in [`compose.infrastructure.yml`](../../compose.infrastructure.yml) with:

- **Image:** minio/minio:latest
- **Ports:** 9000 (API), 9001 (Console)
- **Health Check:** `/minio/health/live` endpoint
- **Resources:** 1GB memory, 0.5 CPU

## Bucket Strategy

### Bucket Naming Convention

```
{environment}.{tenant-context}.{domain}.{purpose}
```

### Infrastructure Buckets

| Bucket | Purpose | Access | Retention |
|--------|---------|--------|-----------|
| `dev.tenant.documents` | Document storage | Private | 30 days |
| `dev.tenant.attachments` | File attachments | Private | 30 days |
| `dev.tenant.imports` | Data import files | Private | 7 days |
| `dev.tenant.exports` | Data export files | Private | 7 days |
| `dev.tenant.templates` | Document templates | Public | 365 days |
| `dev.tenant.reports` | Generated reports | Private | 90 days |
| `dev.tenant.backups` | System backups | Private | 30 days |
| `dev.tenant.avatars` | User avatars | Public | 365 days |

### Bucket Policies

#### Private Bucket Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": ["*"]
      },
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": [
        "arn:aws:s3:::dev.tenant.documents/*"
      ]
    }
  ]
}
```

#### Public Read Bucket Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": ["*"]
      },
      "Action": [
        "s3:GetObject"
      ],
      "Resource": [
        "arn:aws:s3:::dev.tenant.templates/*"
      ]
    }
  ]
}
```

## Security Configuration

### Access Control

- **Multi-tenancy:** Each tenant has isolated buckets
- **Authentication:** JWT token validation via presigned URLs
- **Authorization:** Bucket-level and object-level permissions
- **Encryption:** TLS for data in transit, SSE-S3 for data at rest

### Security Best Practices

- [ ] Use presigned URLs for temporary access
- [ ] Enable bucket versioning for critical data
- [ ] Configure lifecycle policies for data retention
- [ ] Use IAM policies for fine-grained access control
- [ ] Enable audit logging for all operations
- [ ] Encrypt sensitive data before upload
- [ ] Regular security scanning of uploaded files

## Use Cases

### 1. Document Storage

**Description:** Store and retrieve business documents with tenant isolation.

**Use Case:** Users upload invoices, contracts, and other business documents.

```java
@Service
public class DocumentStorageService {
    
    private final MinioClient minioClient;
    private final String bucketName = "dev.tenant.documents";
    
    public String uploadDocument(String tenantId, String documentId, InputStream content, String contentType) {
        String objectKey = tenantId + "/" + documentId;
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(content, content.available(), -1)
                .contentType(contentType)
                .build()
        );
        
        return objectKey;
    }
    
    public InputStream downloadDocument(String tenantId, String documentId) {
        String objectKey = tenantId + "/" + documentId;
        
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build()
        );
    }
    
    public String getPresignedUrl(String tenantId, String documentId, Duration expiry) {
        String objectKey = tenantId + "/" + documentId;
        
        return minioClient.presignGetObject(
            PresignedGetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .expiry((int) expiry.toSeconds())
                .build()
        );
    }
}
```

### 2. File Attachments

**Description:** Store file attachments for business entities.

**Use Case:** Users attach files to invoices, purchase orders, and other records.

```java
@Service
public class AttachmentService {
    
    private final MinioClient minioClient;
    private final String bucketName = "dev.tenant.attachments";
    
    public Attachment uploadAttachment(AttachmentUploadRequest request) {
        String objectKey = request.entityType() + "/" + request.entityId() + "/" + UUID.randomUUID();
        
        // Upload file
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(request.content(), request.size(), -1)
                .contentType(request.contentType())
                .build()
        );
        
        // Save metadata to database
        return attachmentRepository.save(
            new Attachment(
                objectKey,
                request.filename(),
                request.size(),
                request.contentType(),
                request.entityType(),
                request.entityId()
            )
        );
    }
    
    public List<Attachment> getAttachments(String entityType, String entityId) {
        return attachmentRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
}
```

### 3. Data Import/Export

**Description:** Handle bulk data import and export operations.

**Use Case:** Users import/export data via CSV, Excel, or JSON files.

```java
@Service
public class DataImportExportService {
    
    private final MinioClient minioClient;
    private final String importBucket = "dev.tenant.imports";
    private final String exportBucket = "dev.tenant.exports";
    
    public String createExportJob(ExportRequest request) {
        String jobId = UUID.randomUUID().toString();
        String objectKey = "exports/" + jobId + "/data.json";
        
        // Generate export data
        InputStream data = generateExportData(request);
        
        // Upload to MinIO
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(exportBucket)
                .object(objectKey)
                .stream(data, data.available(), -1)
                .contentType("application/json")
                .build()
        );
        
        return jobId;
    }
    
    public void processImport(String importId) {
        String objectKey = "imports/" + importId + "/data.csv";
        
        // Download from MinIO
        InputStream data = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(importBucket)
                .object(objectKey)
                .build()
        );
        
        // Process import
        processImportData(data);
    }
}
```

### 4. Report Generation

**Description:** Store generated reports and analytics.

**Use Case:** System generates PDF/Excel reports for users.

```java
@Service
public class ReportService {
    
    private final MinioClient minioClient;
    private final String bucketName = "dev.tenant.reports";
    
    public Report generateReport(ReportRequest request) {
        String reportId = UUID.randomUUID().toString();
        String objectKey = request.tenantId() + "/" + request.reportType() + "/" + reportId + ".pdf";
        
        // Generate report
        InputStream reportStream = generatePdfReport(request);
        
        // Upload to MinIO
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(reportStream, reportStream.available(), -1)
                .contentType("application/pdf")
                .build()
        );
        
        return new Report(reportId, objectKey, Instant.now());
    }
    
    public List<Report> getReports(String tenantId, String reportType) {
        // List objects in bucket
        return minioClient.listObjects(
            ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(tenantId + "/" + reportType + "/")
                .build()
        ).stream()
         .map(obj -> new Report(obj.object(), obj.object(), obj.lastModified()))
         .collect(Collectors.toList());
    }
}
```

### 5. User Avatars

**Description:** Store user profile pictures with public read access.

**Use Case:** Users upload and view profile avatars.

```java
@Service
public class AvatarService {
    
    private final MinioClient minioClient;
    private final String bucketName = "dev.tenant.avatars";
    
    public String uploadAvatar(String userId, InputStream avatar, String contentType) {
        String objectKey = userId + "/avatar.jpg";
        
        // Upload avatar
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(avatar, avatar.available(), -1)
                .contentType(contentType)
                .build()
        );
        
        return "/api/avatars/" + userId;
    }
    
    public String getAvatarUrl(String userId) {
        // Return public URL (bucket has public-read policy)
        return "http://localhost:9000/" + bucketName + "/" + userId + "/avatar.jpg";
    }
}
```

### 6. Backup Storage

**Description:** Store system backups and archives.

**Use Case:** Automated backup jobs store database dumps and configuration backups.

```java
@Service
public class BackupStorageService {
    
    private final MinioClient minioClient;
    private final String bucketName = "dev.tenant.backups";
    
    public void storeBackup(BackupRequest request) {
        String objectKey = "backups/" + request.serviceName() + "/" + 
                          LocalDate.now() + "/" + request.filename();
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(request.content(), request.size(), -1)
                .contentType("application/gzip")
                .build()
        );
    }
    
    public InputStream retrieveBackup(String serviceName, LocalDate date, String filename) {
        String objectKey = "backups/" + serviceName + "/" + date + "/" + filename;
        
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build()
        );
    }
}
```

## Health Checks

### Docker Health Check

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
  interval: 30s
  timeout: 10s
  retries: 5
```

### Manual Health Check

```bash
# Check MinIO health
curl -f http://localhost:9000/minio/health/live
curl -f http://localhost:9000/minio/health/ready

# Check MinIO version
curl http://localhost:9000/minio/health/info
```

## Backup and Recovery

See [`backups/README.md`](backups/README.md) for backup procedures.

## Troubleshooting

### MinIO not starting

```bash
# Check logs
docker compose logs minio

# Check health status
docker compose ps minio
```

### Cannot access bucket

```bash
# Check if bucket exists
docker compose exec minio ls -la /data

# Create bucket manually
docker compose exec minio \
  mc mb --ignore-existing local/dev.tenant.documents
```

### Permission denied

```bash
# Check bucket policy
docker compose exec minio \
  mc policy list local/dev.tenant.documents

# Set bucket policy
docker compose exec minio \
  mc policy set private local/dev.tenant.documents
```

## Best Practices

- [ ] Use tenant ID as prefix for bucket isolation
- [ ] Enable versioning for critical buckets
- [ ] Configure lifecycle policies for data retention
- [ ] Use presigned URLs for temporary access
- [ ] Monitor storage usage
- [ ] Regular backup of MinIO data
- [ ] Enable audit logging
- [ ] Use HTTPS in production
- [ ] Encrypt sensitive data before upload
