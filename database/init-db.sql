CREATE DATABASE IF NOT EXISTS bd_fragrances
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bd_fragrances;

CREATE TABLE IF NOT EXISTS usuario (
  id INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL,
  password VARCHAR(255) NOT NULL,
  telefono VARCHAR(9) NOT NULL,
  dni VARCHAR(8) NOT NULL,
  direccion VARCHAR(200) NOT NULL,
  rol VARCHAR(20) NOT NULL DEFAULT 'CLIENTE',
  PRIMARY KEY (id),
  UNIQUE KEY uk_usuario_email (email),
  UNIQUE KEY uk_usuario_dni (dni)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE usuario
  ADD COLUMN IF NOT EXISTS rol VARCHAR(20) NOT NULL DEFAULT 'CLIENTE';

INSERT INTO usuario (nombre, email, password, telefono, dni, direccion, rol)
SELECT 'Jahir Ortiz', 'jahirortizbr@gmail.com', '123456', '999888777', '76615558', 'La Planicie', 'ADMIN'
WHERE NOT EXISTS (
  SELECT 1 FROM usuario WHERE email = 'jahirortizbr@gmail.com' OR dni = '76615558'
);

CREATE TABLE IF NOT EXISTS producto (
  id INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(150) NOT NULL,
  marca VARCHAR(100) NOT NULL,
  precio DOUBLE NOT NULL,
  descripcion VARCHAR(300),
  categoria VARCHAR(50),
  image_url VARCHAR(255),
  descuento_label VARCHAR(50),
  stock INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contacto (
  id INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL,
  asunto VARCHAR(150) NOT NULL,
  mensaje VARCHAR(1000) NOT NULL,
  fecha DATETIME(6),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pedido (
  id INT NOT NULL AUTO_INCREMENT,
  usuario_id INT NOT NULL,
  producto_id INT NOT NULL,
  cantidad INT NOT NULL,
  total DOUBLE NOT NULL,
  fecha DATETIME(6),
  estado VARCHAR(50),
  metodo_pago VARCHAR(50),
  metodo_entrega VARCHAR(50),
  direccion_entrega VARCHAR(200),
  telefono_contacto VARCHAR(20),
  notas VARCHAR(500),
  PRIMARY KEY (id),
  KEY idx_pedido_usuario (usuario_id),
  KEY idx_pedido_producto (producto_id),
  CONSTRAINT fk_pedido_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario (id),
  CONSTRAINT fk_pedido_producto
    FOREIGN KEY (producto_id) REFERENCES producto (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE pedido
  ADD COLUMN IF NOT EXISTS metodo_pago VARCHAR(50),
  ADD COLUMN IF NOT EXISTS metodo_entrega VARCHAR(50),
  ADD COLUMN IF NOT EXISTS direccion_entrega VARCHAR(200),
  ADD COLUMN IF NOT EXISTS telefono_contacto VARCHAR(20),
  ADD COLUMN IF NOT EXISTS notas VARCHAR(500);

-- =============================================
-- Tabla de decants (muestras)
-- =============================================
CREATE TABLE IF NOT EXISTS decant (
  id INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(150) NOT NULL,
  marca VARCHAR(100) NOT NULL,
  tamano VARCHAR(50) NOT NULL DEFAULT '3ml',
  precio DOUBLE NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  image_url VARCHAR(255),
  descripcion VARCHAR(500),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Carga inicial de los 8 decants de Medina Fragrances
INSERT INTO decant (nombre, marca, tamano, precio, stock, image_url, descripcion)
SELECT * FROM (
    SELECT 'Valentino Uomo Intense' AS nombre, 'Valentino' AS marca, '3ml - 10ml' AS tamano, 45.0 AS precio, 15 AS stock, '/img/Valentino Uomo.webp' AS image_url, 'Fragancia elegante y sofisticada para el hombre moderno.' AS descripcion
    UNION ALL SELECT 'JPG Le Male', 'JEAN PAUL GAULTIER', '3ml - 10ml', 20.0, 20, '/img/JEAN PAUL GAULTIER LE MALE.webp', 'Un clásico atemporal con notas de lavanda y vainilla.'
    UNION ALL SELECT 'Azzaro 9PM', 'AZZARO', '3ml - 10ml', 60.0, 15, '/img/9pm.webp', 'Frescura masculina radical con notas de bergamota.'
    UNION ALL SELECT 'Lattafa Khamrah Dukhan', 'LATTAFA', '3ml - 10ml', 75.0, 12, '/img/khamrah dukhan.webp', 'Aroma dulce, especiado y adictivo.'
    UNION ALL SELECT 'Vintage Radio', 'MONTALE', '3ml - 10ml', 55.0, 10, '/img/Vintage Radio.webp', 'Fragancia retro con un toque moderno.'
    UNION ALL SELECT 'Asad Bourbon', 'LATTAFA', '3ml - 10ml', 65.0, 14, '/img/Asad Bourbon.webp', 'Poderoso y masculino con notas de tabaco.'
    UNION ALL SELECT 'Amethyst', 'MONTALE', '3ml - 10ml', 50.0, 8, '/img/Amethyst.webp', 'Elegancia y misterio en un frasco.'
    UNION ALL SELECT 'Shaheen Gold', 'LATTAFA', '3ml - 10ml', 60.0, 10, '/img/Shaheen Gold.webp', 'Un aroma dorado y sofisticado.'
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM decant WHERE decant.nombre = tmp.nombre
);
