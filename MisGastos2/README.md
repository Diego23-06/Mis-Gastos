# Mis Gastos

Aplicación de escritorio en Java (Swing) para controlar gastos personales: registro rápido por categorías y resumen semanal con comparación frente a la semana anterior.

## Requisitos

- JDK 11 o superior (para compilar) o solo JRE 11+ (para ejecutar el .jar)

## Compilar

Linux / macOS:

    ./build.sh

Windows:

    build.bat

Se genera `MisGastos.jar`.

## Ejecutar

    java -jar MisGastos.jar

## Datos

Los gastos se guardan en `~/.mis-gastos/gastos.csv` (carpeta del usuario), con el formato `id;monto;categoria;fecha;nota`.

## Estructura

- `src/gastos/Gasto.java`: modelo de un gasto.
- `src/gastos/Almacen.java`: lectura/escritura del CSV y filtro por semana.
- `src/gastos/Main.java`: interfaz gráfica (Swing).
- `MANIFEST.MF`: indica la clase principal del .jar.
