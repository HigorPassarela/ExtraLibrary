-- V5: Adicionar campos de autenticação na tabela customer
ALTER TABLE customer ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE customer ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';

-- Adicionar constraint para garantir valores válidos de role
ALTER TABLE customer ADD CONSTRAINT chk_customer_role
    CHECK (role IN ('CUSTOMER', 'ADMIN', 'LIBRARIAN'));

-- Criar índice para role
CREATE INDEX IF NOT EXISTS idx_customer_role ON customer(role);

-- Comentários
COMMENT ON COLUMN customer.password IS 'Senha criptografada do cliente';
COMMENT ON COLUMN customer.role IS 'Papel do cliente: CUSTOMER, ADMIN, LIBRARIAN';

-- Para clientes existentes sem senha, você pode definir uma senha padrão temporária
-- UPDATE customer SET password = '$2a$12$defaultHashedPassword' WHERE password IS NULL;

-- Tornar password obrigatório após migração dos dados existentes
-- ALTER TABLE customer ALTER COLUMN password SET NOT NULL;