alter table conversation_sessions
    add column if not exists takeover_reason varchar(255);

alter table conversation_sessions
    add column if not exists takeover_at timestamp;

create table async_tasks (
    id bigserial primary key,
    task_type varchar(64) not null,
    business_key varchar(128) not null,
    payload text not null,
    status varchar(32) not null,
    attempts integer not null,
    max_attempts integer not null,
    next_run_at timestamp not null,
    last_error varchar(1000),
    created_at timestamp not null,
    updated_at timestamp not null,
    unique (task_type, business_key)
);

create index idx_async_tasks_status_next_run on async_tasks(status, next_run_at);

create table tenants (
    id bigserial primary key,
    tenant_id varchar(64) not null unique,
    name varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table tenant_configs (
    id bigserial primary key,
    tenant_id varchar(64) not null,
    config_key varchar(128) not null,
    config_value text,
    updated_at timestamp not null,
    unique (tenant_id, config_key)
);

create table tag_dictionaries (
    id bigserial primary key,
    tag_code varchar(64) not null unique,
    tag_name varchar(128) not null,
    rule_keywords varchar(500),
    created_at timestamp not null
);

create table customer_tags (
    id bigserial primary key,
    customer_id bigint not null references customers(id),
    tag_code varchar(64) not null,
    tag_name varchar(128) not null,
    source varchar(64) not null,
    confidence numeric(8, 4) not null,
    created_at timestamp not null,
    unique (customer_id, tag_code)
);

create table follow_up_tasks (
    id bigserial primary key,
    customer_id bigint not null references customers(id),
    session_id bigint references conversation_sessions(id),
    title varchar(255) not null,
    priority varchar(32) not null,
    status varchar(32) not null,
    owner_id varchar(128),
    due_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null
);
