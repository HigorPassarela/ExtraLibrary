-- Adicionar coluna quantity na tabela book
ALTER TABLE book ADD COLUMN IF NOT EXISTS quantity BIGINT NOT NULL DEFAULT 1;

-- Atualizar o enum BookStatus para ter apenas IN_STOCK e OUT_OF_STOCK
-- Como PostgreSQL não permite alterar enum diretamente, vamos fazer isso em etapas:

-- 1. Criar novo tipo enum com apenas 2 valores
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'book_status_new') THEN
        CREATE TYPE book_status_new AS ENUM ('IN_STOCK', 'OUT_OF_STOCK');
    END IF;
END$$;

-- 2. Adicionar coluna temporária com novo enum
ALTER TABLE book ADD COLUMN IF NOT EXISTS book_status_temp book_status_new;

-- 3. Migrar dados (qualquer status existente -> IN_STOCK, exceto SOLD -> OUT_OF_STOCK)
UPDATE book
SET book_status_temp = CASE
    WHEN book_status = 'SOLD' THEN 'OUT_OF_STOCK'::book_status_new
    ELSE 'IN_STOCK'::book_status_new
END;

-- 4. Remover coluna antiga
ALTER TABLE book DROP COLUMN book_status;

-- 5. Renomear coluna temporária
ALTER TABLE book RENAME COLUMN book_status_temp TO book_status;

-- 6. Limpar tipos antigos
DO $$
BEGIN
    DROP TYPE IF EXISTS book_status_old CASCADE;
EXCEPTION
    WHEN OTHERS THEN NULL;
END$$;

-- 7. Renomear o novo tipo
ALTER TYPE book_status_new RENAME TO book_status;