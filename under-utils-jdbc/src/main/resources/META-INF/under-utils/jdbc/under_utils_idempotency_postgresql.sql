create table if not exists under_utils_idempotency (
    idem_key varchar(512) primary key,
    status varchar(32) not null,
    execution_token varchar(128),
    result_payload text,
    expire_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_under_utils_idem_expire_at
    on under_utils_idempotency (expire_at);
