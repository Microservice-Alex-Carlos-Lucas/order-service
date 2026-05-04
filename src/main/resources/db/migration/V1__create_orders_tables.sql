CREATE TABLE IF NOT EXISTS orders."order" (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    id_account UUID      NOT NULL,
    date       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders.order_item (
    id         UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    id_order   UUID    NOT NULL REFERENCES orders."order"(id),
    id_product UUID    NOT NULL,
    quantity   INT     NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL
);
