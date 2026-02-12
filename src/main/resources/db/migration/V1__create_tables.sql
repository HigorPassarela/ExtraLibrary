-- Criação da tabela author
CREATE TABLE author (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    biography VARCHAR(1000),
    nacionality VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL
);

-- Criação da tabela customer
CREATE TABLE customer (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    cpf VARCHAR(11) UNIQUE,
    phone VARCHAR(20) NOT NULL,
    date_birth DATE NOT NULL,
    address VARCHAR(100) NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Criação da sequência para sold
CREATE SEQUENCE sold_seq START WITH 1 INCREMENT BY 1;

-- Criação da tabela sold
CREATE TABLE sold (
    id BIGINT PRIMARY KEY DEFAULT nextval('sold_seq'),
    date_sale TIMESTAMP NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2),
    final_price DECIMAL(10,2) NOT NULL,
    form_payment VARCHAR(20) NOT NULL CHECK (form_payment IN ('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'BANK_TRANSFER')),
    created_at TIMESTAMP,
    customer_id UUID NOT NULL,
    CONSTRAINT fk_sold_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- Criação da tabela book
CREATE TABLE book (
    id UUID PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    publication_date DATE NOT NULL,
    gender VARCHAR(30) NOT NULL CHECK (gender IN ('FICTION', 'ROMANCE', 'MYSTERY', 'FANTASY', 'BIOGRAPHY', 'HISTORY', 'TECHNICAL', 'CHILDREN', 'CLASSIC')),
    price DECIMAL(18,2) NOT NULL,
    book_status VARCHAR(20) NOT NULL CHECK (book_status IN ('IN_STOCK', 'SOLD')),
    author_id UUID NOT NULL,
    CONSTRAINT fk_book_author FOREIGN KEY (author_id) REFERENCES author(id)
);

-- Criação da tabela de relacionamento sold_book_ids (para ElementCollection)
CREATE TABLE sold_book_ids (
    sold_id BIGINT NOT NULL,
    book_id UUID NOT NULL,
    CONSTRAINT fk_sold_book_ids_sold FOREIGN KEY (sold_id) REFERENCES sold(id) ON DELETE CASCADE,
    CONSTRAINT fk_sold_book_ids_book FOREIGN KEY (book_id) REFERENCES book(id),
    PRIMARY KEY (sold_id, book_id)
);

-- Índices para melhorar performance
CREATE INDEX idx_book_author_id ON book(author_id);
CREATE INDEX idx_sold_customer_id ON sold(customer_id);
CREATE INDEX idx_book_isbn ON book(isbn);
CREATE INDEX idx_customer_email ON customer(email);
CREATE INDEX idx_customer_cpf ON customer(cpf);
CREATE INDEX idx_sold_date_sale ON sold(date_sale);