CREATE TABLE institutions
(
    id               UUID PRIMARY KEY,
    name             VARCHAR(160)             NOT NULL,
    tax_id           VARCHAR(14),
    email            VARCHAR(254)             NOT NULL,
    phone            VARCHAR(20),
    street           VARCHAR(160),
    address_number   VARCHAR(20),
    complement       VARCHAR(100),
    district         VARCHAR(100),
    city             VARCHAR(100)             NOT NULL,
    state            VARCHAR(2)               NOT NULL,
    postal_code      VARCHAR(8),
    singleton_marker BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_institutions_singleton
        UNIQUE (singleton_marker),

    CONSTRAINT ck_institutions_singleton
        CHECK (singleton_marker = TRUE)
);

CREATE UNIQUE INDEX uk_institutions_tax_id
    ON institutions (tax_id) WHERE tax_id IS NOT NULL;