CREATE INDEX IF NOT EXISTS idx_invoices_shop_status_created ON invoices (shop_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_sales_shop_status_sold ON sales (shop_id, status, sold_at);
CREATE INDEX IF NOT EXISTS idx_sales_invoice ON sales (invoice_id);
CREATE INDEX IF NOT EXISTS idx_products_shop_barcode ON products (shop_id, barcode);
CREATE INDEX IF NOT EXISTS idx_cash_sessions_register_opened ON cash_register_sessions (register_id, status, opened_at);
