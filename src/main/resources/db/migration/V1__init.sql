create table customers (
    id bigserial primary key,
    external_key varchar(128) not null unique,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table channel_accounts (
    id bigserial primary key,
    channel_code varchar(64) not null,
    external_user_id varchar(128) not null,
    customer_id bigint not null references customers(id),
    created_at timestamp not null,
    unique (channel_code, external_user_id)
);

create table conversation_sessions (
    id bigserial primary key,
    customer_id bigint not null references customers(id),
    channel_code varchar(64) not null,
    status varchar(32) not null,
    summary text,
    created_at timestamp not null,
    updated_at timestamp not null,
    last_active_at timestamp not null
);

create table customer_memories (
    id bigserial primary key,
    customer_id bigint not null unique references customers(id),
    summary text,
    preferences text,
    budget text,
    intent text,
    concerns text,
    commitments text,
    source varchar(128),
    updated_at timestamp not null
);

create table chat_messages (
    id bigserial primary key,
    external_message_id varchar(128) not null unique,
    session_id bigint not null references conversation_sessions(id),
    customer_id bigint not null references customers(id),
    direction varchar(32) not null,
    sender_id varchar(128) not null,
    receiver_id varchar(128) not null,
    content text not null,
    status varchar(32) not null,
    raw_payload_digest varchar(128),
    raw_payload_ref varchar(255),
    created_at timestamp not null
);

create table knowledge_documents (
    id bigserial primary key,
    tenant_id varchar(64) not null,
    file_name varchar(255) not null,
    content_type varchar(128) not null,
    size_bytes bigint not null,
    object_key varchar(255) not null,
    status varchar(32) not null,
    failure_reason varchar(500),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table knowledge_chunks (
    id bigserial primary key,
    document_id bigint not null references knowledge_documents(id),
    tenant_id varchar(64) not null,
    chunk_index integer not null,
    content text not null,
    vector_data text not null,
    status varchar(32) not null,
    created_at timestamp not null
);

create table model_audit_logs (
    id bigserial primary key,
    provider varchar(64) not null,
    model_name varchar(128) not null,
    prompt_digest varchar(128) not null,
    response_digest varchar(128),
    status varchar(32) not null,
    latency_ms bigint not null,
    error_message varchar(500),
    created_at timestamp not null
);
