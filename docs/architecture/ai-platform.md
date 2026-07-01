# AI Platform Architecture

## 1. Purpose

This document defines the AI platform architecture that powers intelligent capabilities across the ERP AI Platform. The AI platform is designed as a set of specialized services that integrate with business modules to provide AI-enhanced experiences.

## 2. AI Platform Principles

- **Modular AI:** Specialized agents for each business domain
- **Model Agnostic:** Support multiple LLM providers
- **Context-Aware:** Business data context for relevant responses
- **Secure:** Tenant data isolation in AI interactions
- **Auditable:** Full conversation and action audit trail
- **Cost Managed:** Credit-based usage tracking
- **Extensible:** Easy to add new AI capabilities

## 3. AI Platform Services

### 3.1 AI Platform Service

**Purpose:** Central orchestration layer for all AI capabilities. Manages AI requests, routing, and common AI infrastructure.

**Responsibilities:**
- AI request routing to appropriate agents
- Context gathering from business services
- Prompt template management
- Response formatting and validation
- AI credit tracking and billing
- Conversation state management
- Model provider abstraction
- Fallback and retry logic
- AI usage analytics

**Why it exists:**
- Centralize AI orchestration logic
- Provide consistent AI experience across modules
- Manage AI costs and credits
- Enable rapid development of new AI features

**Dependencies:**
- PostgreSQL (conversation history, credits)
- Redis (conversation cache, rate limiting)
- Kafka (AI event streaming)
- Vector Database (RAG context)
- All AI Agents (specialized capabilities)
- All Business Services (data context)

**APIs:**
- `/api/v1/ai/chat` - General AI chat interface
- `/api/v1/ai/agents/{agentType}/chat` - Agent-specific chat
- `/api/v1/ai/credits` - Credit management
- `/api/v1/ai/usage` - Usage analytics

---

### 3.2 AI Sales Agent

**Purpose:** AI assistant specialized for sales operations. Helps with lead qualification, quotation, and sales process automation.

**Responsibilities:**
- Lead scoring and qualification
- Sales conversation assistance
- Quotation generation suggestions
- Email and communication drafting
- Sales forecast prediction
- Next best action recommendations
- Customer sentiment analysis
- Sales script generation

**Why it exists:**
- Accelerate sales cycle
- Improve lead conversion rates
- Reduce manual data entry
- Provide data-driven sales insights

**Dependencies:**
- AI Platform Service (orchestration)
- CRM Service (lead/account data)
- Sales Service (quotation/order data)
- AI Knowledge Base (sales knowledge)
- Vector Database (RAG context)

**Capabilities:**
- "Qualify this lead based on interaction history"
- "Generate quotation for this opportunity"
- "What's the best follow-up action for this customer?"
- "Predict close probability for this deal"

---

### 3.3 AI Finance Assistant

**Purpose:** AI assistant specialized for finance and accounting operations.

**Responsibilities:**
- Invoice data extraction from documents
- Expense categorization
- Financial anomaly detection
- Cash flow prediction
- Budget variance analysis
- Financial report summarization
- Audit finding assistance
- Tax compliance suggestions

**Why it exists:**
- Automate manual data entry
- Improve financial accuracy
- Accelerate month-end close
- Provide proactive financial insights

**Dependencies:**
- AI Platform Service (orchestration)
- Accounting Service (GL, journal entries)
- Sales Service (invoices, payments)
- Procurement Service (bills, expenses)
- AI Knowledge Base (accounting knowledge)
- File Service (document processing)

**Capabilities:**
- "Extract invoice data from this document"
- "Categorize this expense"
- "Flag unusual transactions this month"
- "Summarize this financial report"
- "Predict cash flow for next quarter"

---

### 3.4 AI HR Assistant

**Purpose:** AI assistant specialized for human resources operations.

**Responsibilities:**
- Resume parsing and screening
- Interview question generation
- Employee query resolution
- Policy question answering
- Performance review assistance
- Training recommendation
- Employee sentiment analysis
- HR document generation

**Why it exists:**
- Accelerate recruitment process
- Improve employee self-service
- Reduce HR administrative burden
- Provide data-driven HR insights

**Dependencies:**
- AI Platform Service (orchestration)
- HR Service (employee data)
- AI Knowledge Base (HR policies)
- File Service (resume processing)
- Vector Database (policy RAG)

**Capabilities:**
- "Screen these resumes against job description"
- "Generate interview questions for this role"
- "What's the leave policy for Nepal?"
- "Analyze employee sentiment from surveys"
- "Recommend training for this employee"

---

### 3.5 AI Analytics

**Purpose:** AI-powered analytics and business intelligence. Provides natural language querying and automated insights.

**Responsibilities:**
- Natural language to SQL conversion
- Automated insight generation
- Anomaly detection in business data
- Trend analysis and forecasting
- Dashboard natural language descriptions
- Data story generation
- Predictive analytics
- What-if analysis

**Why it exists:**
- Democratize data access (no SQL required)
- Accelerate insight discovery
- Proactive anomaly detection
- Natural data exploration

**Dependencies:**
- AI Platform Service (orchestration)
- All Business Services (data sources)
- Report Service (report data)
- Vector Database (metadata context)
- Data Warehouse (analytics data)

**Capabilities:**
- "Show me sales trend for last quarter"
- "What caused the spike in expenses?"
- "Predict inventory needs for next month"
- "Compare performance across regions"
- "Generate insights for this dashboard"

---

### 3.6 AI Knowledge Base

**Purpose:** Centralized knowledge repository for AI context. Stores business knowledge, policies, and documentation.

**Responsibilities:**
- Document ingestion and processing
- Knowledge chunking and embedding
- Vector storage and retrieval
- Knowledge freshness management
- Multi-format document support (PDF, DOC, HTML)
- Knowledge base management UI
- Access control for knowledge sources
- Knowledge quality monitoring

**Why it exists:**
- Provide context for AI responses
- Enable Retrieval-Augmented Generation (RAG)
- Centralize business knowledge
- Ensure AI responses are grounded in facts

**Dependencies:**
- PostgreSQL (knowledge metadata)
- Vector Database (embeddings)
- File Service (document storage)
- All Business Services (knowledge sources)
- All AI Agents (knowledge consumers)

**Knowledge Sources:**
- Business process documentation
- Policy documents
- Product catalogs
- Training materials
- Historical case data
- Regulatory documents
- FAQ documents

---

### 3.7 Prompt Management

**Purpose:** Centralized prompt template management and versioning. Ensures consistent and optimized AI prompts.

**Responsibilities:**
- Prompt template CRUD
- Prompt versioning and A/B testing
- Prompt variable substitution
- Prompt performance tracking
- Prompt optimization suggestions
- Multi-language prompt support
- Prompt approval workflow
- Prompt analytics

**Why it exists:**
- Centralize prompt management
- Enable prompt optimization
- Ensure prompt consistency
- Track prompt performance

**Dependencies:**
- PostgreSQL (prompt templates, versions)
- All AI Agents (prompt consumers)
- AI Platform Service (prompt resolution)

**Prompt Structure:**
```
System Prompt: [Role definition]
Context: [Business context from RAG]
User Input: [User query]
Instructions: [Specific instructions]
Output Format: [Expected response format]
Examples: [Few-shot examples]
```

---

### 3.8 Conversation History

**Purpose:** Manage conversation state and history for AI interactions. Enables context-aware conversations.

**Responsibilities:**
- Conversation session management
- Message history storage
- Context window management
- Conversation summarization
- Multi-turn conversation handling
- Conversation search and retrieval
- Conversation export
- Privacy and retention management

**Why it exists:**
- Enable multi-turn conversations
- Maintain context across interactions
- Support conversation review and audit
- Enable conversation continuity

**Dependencies:**
- PostgreSQL (conversation history)
- Redis (active conversation cache)
- All AI Agents (conversation consumers)
- AI Platform Service (conversation orchestration)

**Data Model:**
```
Conversation
├── ConversationId
├── TenantId
├── UserId
├── AgentType
├── CreatedAt
├── UpdatedAt
└── Messages[]
    ├── MessageId
    ├── Role (user/assistant/system)
    ├── Content
    ├── Timestamp
    └── Metadata
```

---

### 3.9 AI Credits

**Purpose:** Manage AI usage credits and billing. Tracks consumption and enforces limits.

**Responsibilities:**
- Credit package management
- Credit allocation per tenant
- Usage tracking per operation
- Credit deduction and refunds
- Usage limit enforcement
- Billing integration
- Credit alerting
- Usage reporting

**Why it exists:**
- Monetize AI capabilities
- Control AI costs
- Provide usage transparency
- Enable tiered AI access

**Dependencies:**
- PostgreSQL (credit transactions, packages)
- License Service (entitlement check)
- All AI Services (usage reporting)

**Credit Model:**
- Token-based: Credits per token consumed
- Operation-based: Credits per AI operation
- Tiered: Different credit costs per model
- Bundled: Credit packages for purchase

---

### 3.10 Model Providers

**Purpose:** Abstraction layer for multiple LLM providers. Enables model flexibility and cost optimization.

**Responsibilities:**
- Provider abstraction (OpenAI, Anthropic, Cohere, etc.)
- Model version management
- Provider failover and load balancing
- Cost tracking per provider/model
- Model capability registry
- Prompt format conversion
- Response normalization
- Provider health monitoring

**Why it exists:**
- Avoid vendor lock-in
- Optimize cost across providers
- Enable model switching
- Support multiple model types

**Supported Providers:**
- OpenAI (GPT-4, GPT-4-Turbo)
- Anthropic (Claude 3)
- Cohere (Command)
- Google (Gemini)
- Open Source (Llama, Mistral via vLLM)
- Local models (for sensitive data)

**Model Selection Strategy:**
- Cost-based: Choose cheapest suitable model
- Performance-based: Choose fastest suitable model
- Capability-based: Choose most capable for task
- Fallback: Primary → Secondary → Tertiary

---

### 3.11 Vector Search

**Purpose:** Semantic search and Retrieval-Augmented Generation (RAG). Enables AI to access business knowledge.

**Responsibilities:**
- Document embedding generation
- Vector storage and indexing
- Semantic search queries
- Context retrieval for RAG
- Embedding model management
- Vector database abstraction
- Hybrid search (keyword + semantic)
- Relevance scoring

**Why it exists:**
- Enable RAG for accurate AI responses
- Semantic understanding of business data
- Context-aware AI interactions
- Knowledge-grounded responses

**Dependencies:**
- Vector Database (Pinecone, Weaviate, pgvector)
- AI Knowledge Base (document source)
- All AI Agents (RAG consumers)
- Embedding Models (OpenAI, Cohere, open-source)

**RAG Pipeline:**
```
User Query
    ↓
Query Embedding
    ↓
Vector Search (Top-K documents)
    ↓
Context Assembly
    ↓
LLM with Context
    ↓
Grounded Response
```

---

## 4. AI Integration with Business Modules

### 4.1 Integration Patterns

**Pattern 1: AI-Enhanced UI**
```
User → Business Service → AI Service → Enhanced Response → User
```
Example: Sales agent suggests next action in CRM

**Pattern 2: AI Automation**
```
Event → AI Service → Action → Business Service
```
Example: Invoice processed → AI extracts data → Accounting Service

**Pattern 3: AI Copilot**
```
User → AI Copilot → Business Service API → Result → User
```
Example: "Create invoice for customer X" → AI calls Invoice API

**Pattern 4: AI Analytics**
```
User Query → AI Analytics → Business Data → Insight → User
```
Example: "Why did sales drop?" → AI analyzes data → Explanation

### 4.2 Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                        AI Platform                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ AI Platform │  │   Vector    │  │   Conversation       │  │
│  │  Service    │  │   Search    │  │   History            │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
│         │                │                     │              │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────────▼──────────┐  │
│  │   Prompt    │  │    Model    │  │      Credits        │  │
│  │ Management  │  │  Providers  │  │      Service        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└───────────────────────────┬─────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
   ┌────▼────┐         ┌────▼────┐         ┌────▼────┐
   │   AI   │         │   AI   │         │   AI   │
   │ Sales  │         │Finance │         │   HR   │
   │ Agent  │         │Assistant│        │Assistant│
   └────┬────┘         └────┬────┘         └────┬────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
   ┌────▼────┐         ┌────▼────┐         ┌────▼────┐
   │   AI   │         │   AI   │         │   AI   │
   │Analytics│        │Knowledge│        │  Other │
   │         │         │  Base  │         │ Agents │
   └────┬────┘         └────┬────┘         └────┬────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
   ┌────▼────┐         ┌────▼────┐         ┌────▼────┐
   │  CRM   │         │  Sales │         │Accounting│
   │ Service│         │ Service│         │  Service │
   └────────┘         └────────┘         └─────────┘
```

### 4.3 AI Agent Architecture

Each AI Agent follows this pattern:

```
┌─────────────────────────────────────────┐
│              AI Agent                    │
│  ┌───────────────────────────────────┐  │
│  │         Agent Controller          │  │
│  │  - Request validation             │  │
│  │  - Context gathering              │  │
│  │  - Response formatting            │  │
│  └───────────────────────────────────┘  │
│                    │                     │
│  ┌───────────────────────────────────┐  │
│  │         Agent Tools               │  │
│  │  - Business service APIs          │  │
│  │  - RAG queries                    │  │
│  │  - Custom actions                 │  │
│  └───────────────────────────────────┘  │
│                    │                     │
│  ┌───────────────────────────────────┐  │
│  │         Agent Memory              │  │
│  │  - Conversation history           │  │
│  │  - User preferences               │  │
│  │  - Business context               │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

## 5. AI Security and Compliance

### 5.1 Data Isolation

- **Tenant isolation:** AI interactions scoped to tenant
- **Data masking:** PII masked before sending to LLM
- **Context filtering:** Only relevant business data included
- **Audit logging:** All AI interactions logged

### 5.2 Prompt Injection Prevention

- Input sanitization before LLM
- Output validation after LLM
- System prompt hardening
- Rate limiting per user/tenant

### 5.3 Compliance

- **Data retention:** Conversation history retention policies
- **Right to delete:** User conversation deletion
- **Audit trail:** Complete AI interaction audit
- **Model governance:** Approved model list

## 6. AI Cost Management

### 6.1 Credit Model

| Operation | Credits | Notes |
|-----------|--------|-------|
| Simple chat (GPT-3.5) | 1 | Basic Q&A |
| Complex chat (GPT-4) | 5 | Complex reasoning |
| Document extraction | 10 | Per document |
| Code generation | 3 | Per generation |
| Data analysis | 8 | Per analysis |
| Image generation | 20 | Per image |

### 6.2 Cost Optimization

- **Model routing:** Route to appropriate model tier
- **Caching:** Cache common queries
- **Batching:** Batch similar requests
- **Compression:** Compress context where possible
- **Fallback:** Use cheaper models when appropriate

## 7. AI Roadmap

### Phase 1: Foundation
- AI Platform Service
- Basic chat interface
- OpenAI integration
- Simple RAG

### Phase 2: Specialized Agents
- AI Sales Agent
- AI Finance Assistant
- AI HR Assistant
- Prompt management

### Phase 3: Advanced AI
- AI Analytics (NLQ)
- Multi-modal support
- Fine-tuned models
- Agent orchestration

### Phase 4: Autonomous AI
- Autonomous workflows
- Multi-agent collaboration
- Self-improving prompts
- Predictive automation
