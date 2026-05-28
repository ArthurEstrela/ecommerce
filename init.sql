SELECT 'CREATE DATABASE produto_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'produto_db')\gexec

SELECT 'CREATE DATABASE carrinho_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'carrinho_db')\gexec

SELECT 'CREATE DATABASE pedido_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pedido_db')\gexec

SELECT 'CREATE DATABASE estoque_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'estoque_db')\gexec
