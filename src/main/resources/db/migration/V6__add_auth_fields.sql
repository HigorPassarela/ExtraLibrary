-- V6: Adicionar campos de autenticação na tabela customer (se não existirem)

-- Verificar se a coluna password existe, se não, adicionar
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'customer' AND column_name = 'password') THEN
        ALTER TABLE customer ADD COLUMN password VARCHAR(255);
    END IF;
END $$;

-- Verificar se a coluna role existe, se não, adicionar
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'customer' AND column_name = 'role') THEN
        ALTER TABLE customer ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';
    END IF;
END $$;

-- Verificar se a constraint chk_customer_role existe, se não, adicionar
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.constraint_column_usage
                   WHERE constraint_name = 'chk_customer_role') THEN
        ALTER TABLE customer ADD CONSTRAINT chk_customer_role
            CHECK (role IN ('CUSTOMER', 'ADMIN', 'LIBRARIAN'));
    END IF;
END $$;

-- Verificar se o índice idx_customer_role existe, se não, criar
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes
                   WHERE tablename = 'customer' AND indexname = 'idx_customer_role') THEN
        CREATE INDEX idx_customer_role ON customer(role);
    END IF;
END $$;

-- Comentários (sempre executados)
COMMENT ON COLUMN customer.password IS 'Senha criptografada do cliente';
COMMENT ON COLUMN customer.role IS 'Papel do cliente: CUSTOMER, ADMIN, LIBRARIAN';