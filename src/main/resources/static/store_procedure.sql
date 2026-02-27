CREATE OR REPLACE FUNCTION buscar_clientes(nombre_param TEXT)
RETURNS TABLE (
    id INT,
    nombre VARCHAR,
    apellido VARCHAR,
    razon_social VARCHAR,
    cuit VARCHAR,
    fecha_nacimiento DATE,
    telefono_celular VARCHAR,
    email VARCHAR
)
AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM clientes
    WHERE LOWER(nombre) LIKE LOWER('%' || nombre_param || '%');
END;
$$ LANGUAGE plpgsql;
