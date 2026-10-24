# 🌸 Medina Fragances — Tienda Web de Perfumería

**Sistema web para la promoción y venta de perfumes, fragancias y decants.**

## 📌 Descripción del proyecto

Medina Fragances es una aplicación web desarrollada para presentar el catálogo de una perfumería y facilitar la interacción de los clientes con sus productos. El sitio incluye páginas informativas, consulta del catálogo, carrito y proceso de compra, además de opciones para el registro e inicio de sesión de usuarios, contacto y administración.

El proyecto está construido con Java y Spring Boot. La interfaz se sirve desde la aplicación y la información se gestiona mediante una capa de persistencia conectada a una base de datos.

## ✨ Funcionalidades principales

- Visualización de la página principal y del catálogo de fragancias.
- Consulta de productos, marcas, categorías y decants.
- Carrito de compras y flujo de checkout.
- Registro e inicio de sesión de usuarios.
- Envío de mensajes desde el formulario de contacto.
- Gestión de pedidos y productos desde la lógica de la aplicación.
- Páginas informativas de privacidad y términos.

> Las funciones disponibles pueden variar según la configuración y el estado actual del proyecto.

## 🛠️ Tecnologías utilizadas

- **Lenguaje:** Java 17.
- **Framework:** Spring Boot 3.3.5.
- **Gestión de dependencias y ejecución:** Maven (se incluye Maven Wrapper).
- **Vistas web:** HTML, CSS y JavaScript, con plantillas de servidor.
- **Persistencia:** Spring Data JPA e Hibernate.
- **Base de datos principal:** MySQL.
- **Base de datos local opcional:** H2.
- **Seguridad y validación:** Spring Security y Spring Boot Validation.

## 📋 Requisitos previos

Para ejecutar el proyecto con MySQL, instala lo siguiente:

1. **JDK 17** o una versión compatible de Java 17.
2. **MySQL Server 8.x** (o una versión compatible) y una herramienta para administrar la base de datos, como MySQL Workbench.
3. **Git**, si vas a clonar el repositorio.
4. Un navegador web actualizado.

No es obligatorio instalar Maven por separado, ya que el repositorio incluye Maven Wrapper (`mvnw` y `mvnw.cmd`).

## 🚀 Instalación y ejecución

### 1. Obtener el proyecto

Si el proyecto está alojado en un repositorio Git, clónalo y entra a su carpeta:

```bash
git clone <URL_DEL_REPOSITORIO>
cd "INTEGRADOR 1 - MEDINA FRAGANCES"
```

Si ya descargaste el proyecto como archivo ZIP, descomprímelo y abre una terminal en la carpeta raíz, donde se encuentra `pom.xml`.

### 2. Preparar la base de datos MySQL

Inicia el servicio de MySQL. La configuración predeterminada de la aplicación utiliza estos valores:

- **Base de datos:** `bd_fragrances`
- **Servidor:** `localhost:3306`
- **Usuario predeterminado:** `root`
- **Contraseña predeterminada:** `root`

La aplicación tiene configurado `createDatabaseIfNotExist=true`, por lo que puede crear la base de datos si el usuario de MySQL tiene los permisos necesarios. Hibernate también está configurado para actualizar el esquema de acuerdo con las entidades de la aplicación.

Si tu usuario o contraseña de MySQL son diferentes, configura las variables de entorno antes de iniciar la aplicación.

**PowerShell (Windows):**

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/bd_fragrances?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="tu_usuario"
$env:DB_PASSWORD="tu_contraseña"
```

Reemplaza los valores de ejemplo por las credenciales de tu entorno. No publiques contraseñas reales en el repositorio.

### 3. Inicializar la base de datos manualmente (opcional)

El proyecto incluye el archivo `database/init-db.sql`. Puedes abrirlo en MySQL Workbench y ejecutarlo si necesitas preparar la estructura inicial de la base de datos manualmente.

> Revisa el script antes de ejecutarlo, especialmente en entornos compartidos, porque puede incluir datos de demostración. No uses credenciales de prueba en producción.

Si la aplicación ya creó las tablas mediante JPA/Hibernate, no necesitas ejecutar el script para que se genere el esquema automáticamente.

### 4. Ejecutar la aplicación

En **Windows**, desde la carpeta raíz del proyecto, puedes ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

También se incluye un acceso directo que activa el perfil local con H2:

```powershell
.\start.cmd
```

El segundo método utiliza una base de datos local H2 y no requiere configurar MySQL para esa ejecución. Para iniciar con MySQL, usa el primer comando sin activar el perfil `local`.

### 5. Abrir la página web

Cuando la aplicación termine de iniciar, abre en tu navegador:

- **Sitio web:** http://localhost:8080

Si ejecutas el perfil local con H2, también estará disponible la consola de la base de datos en:

- **Consola H2:** http://localhost:8080/h2-console

La consola H2 solo corresponde al modo local; no debe exponerse en un entorno de producción.

## 📂 Estructura general del proyecto

```text
INTEGRADOR 1 - MEDINA FRAGANCES/
├── database/
│   └── init-db.sql             # Script inicial de base de datos
├── src/
│   ├── main/
│   │   ├── java/com/medina/fragrances/fragrance_app/
│   │   │   ├── controller/    # Controladores y rutas web
│   │   │   ├── dto/            # Objetos para transferencia de datos
│   │   │   ├── model/          # Entidades del sistema
│   │   │   ├── repository/     # Acceso a datos
│   │   │   ├── security/       # Configuración de seguridad
│   │   │   ├── service/        # Lógica de negocio
│   │   │   └── FragranceAppApplication.java
│   │   └── resources/
│   │       ├── static/         # CSS, JavaScript e imágenes
│   │       ├── templates/      # Páginas HTML
│   │       ├── application.properties
│   │       └── application-local.properties
│   └── test/                   # Pruebas automatizadas
├── mvnw
├── mvnw.cmd
├── pom.xml                    # Dependencias y configuración Maven
├── start.cmd                  # Inicio rápido en modo local
└── start.ps1
```

## 🔧 Comandos útiles

Ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

Generar el paquete de la aplicación:

```powershell
.\mvnw.cmd clean package
```

El archivo generado se encontrará normalmente en la carpeta `target/`.

## ⚠️ Consideraciones

- Verifica que el puerto `8080` esté libre antes de iniciar la aplicación.
- Si utilizas MySQL, comprueba que el servicio esté activo y que las credenciales sean correctas.
- La configuración incluida está orientada al desarrollo local. Antes de desplegar el sistema, cambia las credenciales predeterminadas, protege los datos sensibles y revisa la configuración de seguridad.

## 👥 Proyecto académico

**Medina Fragances** — Diseño e implementación de una página web para la promoción de productos de perfumería.
