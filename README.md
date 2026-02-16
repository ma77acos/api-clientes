# API Clientes — Challenge Backend

## 📌 Descripción

API REST desarrollada en Java con Spring Boot para la gestión de clientes.
Permite realizar operaciones CRUD completas, búsqueda por nombre y validación de datos.

La aplicación utiliza PostgreSQL como base de datos e incluye documentación automática mediante Swagger.

---

## 🧰 Tecnologías utilizadas

* Java 21
* Spring Boot
* Spring Data JPA
* PostgreSQL
* Swagger (Springdoc OpenAPI)
* Lombok
* Gradle

---

## 🗄️ Base de datos

Ejecutar el archivo:

```
schema.sql
```

Este script crea la tabla `clientes` e inserta datos de prueba.

---

## ▶️ Cómo ejecutar el proyecto

1️⃣ Clonar el repositorio

```
git clone <repo-url>
```

2️⃣ Configurar conexión a base de datos en:

```
application.yml
```

3️⃣ Ejecutar la aplicación

```
./gradlew bootRun
```

---

## 📚 Documentación API

Swagger UI disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

---

## 🌐 Endpoints

### Obtener todos los clientes

GET `/clientes`

### Obtener cliente por ID

GET `/clientes/{id}`

### Buscar clientes por nombre

GET `/clientes/search?nombre=`

### Crear cliente

POST `/clientes`

### Actualizar cliente

PUT `/clientes/{id}`

### Eliminar cliente

DELETE `/clientes/{id}`

---

## ✅ Validaciones implementadas

* Campos obligatorios:

    * Nombre
    * Apellido
    * CUIT
    * Email
    * Teléfono celular
    * Razón social

* Formatos validados:

    * Email válido
    * CUIT con formato correcto
    * Fecha de nacimiento válida

---

## ⚙️ Stored Procedure

Se utiliza un stored procedure en PostgreSQL para la búsqueda de clientes por nombre.

---

## 🧪 Pruebas

Los endpoints pueden probarse mediante:

* Postman
* Insomnia
* Swagger UI

---

## 📄 Autor

Desarrollado como parte de challenge técnico backend.
