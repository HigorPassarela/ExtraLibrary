-- Migração V3: Alteração do status de customer de boolean para varchar com enum Java

-- 1. Adicionar coluna temporária como VARCHAR
ALTER TABLE customer ADD COLUMN IF NOT EXISTS status_temp VARCHAR(20);

-- 2. Migrar dados do boolean para varchar
UPDATE customer
SET status_temp = CASE
    WHEN status = TRUE THEN 'ACTIVE'
    WHEN status = FALSE THEN 'DISABLED'
    ELSE 'ACTIVE'  -- fallback
END;

-- 3. Tornar a coluna temporária NOT NULL
ALTER TABLE customer ALTER COLUMN status_temp SET NOT NULL;

-- 4. Adicionar constraint para garantir apenas valores válidos
ALTER TABLE customer ADD CONSTRAINT chk_customer_status
    CHECK (status_temp IN ('ACTIVE', 'BLOCKED', 'DISABLED'));

-- 5. Remover a coluna boolean antiga
ALTER TABLE customer DROP COLUMN status;

-- 6. Renomear a coluna temporária
ALTER TABLE customer RENAME COLUMN status_temp TO status;

-- 7. Definir valor padrão
ALTER TABLE customer ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- 8. Criar índice
CREATE INDEX IF NOT EXISTS idx_customer_status ON customer(status);

-- 9. Comentário
COMMENT ON COLUMN customer.status IS 'Status do cliente: ACTIVE, BLOCKED, DISABLED';