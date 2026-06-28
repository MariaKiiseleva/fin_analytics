ALTER TABLE daily_finance_entries
    DROP CONSTRAINT chk_daily_row_shape;

ALTER TABLE daily_finance_entries
    DROP COLUMN cost_amount,
    DROP COLUMN tax_amount,
    DROP COLUMN product_profit_amount,
    DROP COLUMN has_cost;

ALTER TABLE daily_finance_entries
    ADD CONSTRAINT chk_daily_row_shape CHECK (
        (nm_id IS NOT NULL)
        OR
        (
            nm_id IS NULL
            AND sales_quantity = 0
            AND return_quantity = 0
            AND net_quantity = 0
            AND sales_amount = 0
            AND returns_amount = 0
            AND net_revenue_amount = 0
            AND commission_amount = 0
            AND logistics_amount = 0
        )
    );
