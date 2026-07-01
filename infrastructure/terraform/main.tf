# ERP AI Platform - Terraform Configuration
# Enterprise-grade infrastructure as code

terraform {
  required_version = ">= 1.7.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.0"
    }
  }

  backend "s3" {
    bucket         = "erpai-terraform-state"
    key            = "platform/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "erpai-terraform-locks"
  }
}

provider "aws" {
  region = var.aws_region
}

# VPC
module "vpc" {
  source = "./modules/vpc"
  
  vpc_name           = "erpai-platform"
  vpc_cidr           = "10.0.0.0/16"
  availability_zones = var.availability_zones
  
  tags = local.common_tags
}

# EKS Cluster
module "eks" {
  source = "./modules/eks"
  
  cluster_name    = "erpai-platform"
  cluster_version = "1.31"
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids
  
  node_groups = {
    general = {
      instance_types = ["m6i.xlarge", "m6i.2xlarge"]
      min_size       = 3
      max_size       = 20
      desired_size   = 5
    }
    ai = {
      instance_types = ["c6i.4xlarge", "c6i.8xlarge"]
      min_size       = 1
      max_size       = 10
      desired_size   = 2
    }
  }
  
  tags = local.common_tags
}

# RDS PostgreSQL
module "rds" {
  source = "./modules/rds"
  
  identifier     = "erpai-postgres"
  engine         = "postgres"
  engine_version = "16.4"
  instance_class = "db.r6g.xlarge"
  allocated_storage = 100
  storage_encrypted = true
  
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnet_ids
  
  database_name = "erpai_platform"
  username      = "erpai"
  
  tags = local.common_tags
}

# ElastiCache Redis
module "redis" {
  source = "./modules/redis"
  
  cluster_id     = "erpai-redis"
  engine         = "redis"
  engine_version = "7.2"
  node_type      = "cache.r6g.large"
  num_nodes      = 3
  
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnet_ids
  
  tags = local.common_tags
}

# MSK Kafka
module "kafka" {
  source = "./modules/msk"
  
  cluster_name    = "erpai-kafka"
  kafka_version   = "3.7.0"
  instance_type   = "kafka.m5.large"
  number_of_nodes = 3
  
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnet_ids
  
  tags = local.common_tags
}

# S3 Buckets
module "s3" {
  source = "./modules/s3"
  
  buckets = {
    erpai-assets = {
      versioning = true
      encryption = true
    }
    erpai-models = {
      versioning = true
      encryption = true
    }
    erpai-backups = {
      versioning = true
      encryption = true
      lifecycle = true
    }
  }
  
  tags = local.common_tags
}

# Variables
variable "aws_region" {
  type        = string
  default     = "us-east-1"
  description = "AWS region"
}

variable "availability_zones" {
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b", "us-east-1c"]
  description = "AWS availability zones"
}

variable "environment" {
  type        = string
  default     = "production"
  description = "Environment name"
}

# Locals
locals {
  common_tags = {
    Environment = var.environment
    Project     = "erpai-platform"
    ManagedBy   = "terraform"
  }
}
