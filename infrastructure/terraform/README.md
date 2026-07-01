# ERP AI Platform - Terraform Infrastructure

Infrastructure as Code for AWS deployment.

## Prerequisites

- Terraform >= 1.7.0
- AWS CLI configured
- AWS credentials with appropriate permissions

## Structure

```
infrastructure/terraform/
├── main.tf                 # Main configuration
├── variables.tf            # Input variables
├── outputs.tf              # Output values
├── providers.tf            # Provider configuration
├── backend.tf              # Remote state configuration
├── modules/
│   ├── vpc/                # VPC and networking
│   ├── eks/                # EKS cluster
│   ├── rds/                # RDS PostgreSQL
│   ├── redis/              # ElastiCache Redis
│   ├── msk/                # MSK Kafka
│   ├── s3/                 # S3 buckets
│   └── security/           # Security groups, IAM
└── environments/
    ├── dev.tfvars          # Development overrides
    ├── staging.tfvars      # Staging overrides
    └── prod.tfvars         # Production overrides
```

## Usage

```bash
# Initialize Terraform
terraform init

# Plan changes
terraform plan -var-file="environments/dev.tfvars"

# Apply changes
terraform apply -var-file="environments/dev.tfvars"

# Destroy infrastructure
terraform destroy -var-file="environments/dev.tfvars"
```

## Security

- All resources encrypted at rest
- Private subnets for data layer
- Security groups with least privilege
- IAM roles with minimal permissions
- Secrets stored in AWS Secrets Manager
