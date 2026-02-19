CREATE TABLE sold_book_quantities (
    sold_id BIGINT NOT NULL,
    book_id UUID NOT NULL,
    quantity BIGINT NOT NULL,
    CONSTRAINT fk_sold_book_quantities_sold FOREIGN KEY (sold_id) REFERENCES sold(id) ON DELETE CASCADE,
    CONSTRAINT fk_sold_book_quantities_book FOREIGN KEY (book_id) REFERENCES book(id),
    PRIMARY KEY (sold_id, book_id)
);

INSERT INTO sold_book_quantities (sold_id, book_id, quantity)
SELECT sold_id, book_id, 1 -- quantidade padrão 1
FROM sold_book_ids;

DROP TABLE IF EXISTS sold_book_ids;

CREATE INDEX idx_sold_book_quantities_sold_id ON sold_book_quantities(sold_id);
CREATE INDEX idx_sold_book_quantities_book_id ON sold_book_quantities(book_id);