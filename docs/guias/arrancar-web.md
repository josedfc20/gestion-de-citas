# Guía · Arrancar y probar la web en tu PC

> Bloque B7. Para Windows, con el entorno instalado (`instalacion-entorno.md`)
> y el backend funcionando (`arrancar-backend.md`). Los comandos van en
> **PowerShell**.

Al terminar habrás visto la web en el navegador del PC y del móvil, te habrás
registrado, habrás iniciado y cerrado sesión, y habrás consultado servicios,
profesionales y horas libres con los datos de demostración.

## 1. Traer el código

```powershell
cd C:\Users\josef\Downloads\Proyectos\Aplicaciones\gestion-de-citas
git fetch origin
git switch feature/web-base     # o, si ya está fusionado: git switch main; git pull
```

Ahora hay una carpeta nueva, `frontend`.

## 2. Arrancar el backend con datos de demostración

La web necesita el backend en marcha. Con Docker Desktop abierto, en una
ventana de PowerShell (apartados 4, 5 y 7.10 de `arrancar-backend.md`):

```powershell
docker compose up -d
cd backend
$env:SPRING_PROFILES_ACTIVE = "demo"
.\mvnw spring-boot:run
```

Espera a ver `Started GestionDeCitasApplication`. Deja esta ventana abierta.

> Si tu base de datos ya tiene servicios de pruebas anteriores, la demo no se
> crea (solo se crea con la base vacía). Para empezar de cero:
> `docker compose down -v` y vuelve a arrancar.

## 3. Arrancar la web

Abre **otra** ventana de PowerShell:

```powershell
cd C:\Users\josef\Downloads\Proyectos\Aplicaciones\gestion-de-citas\frontend
npm install        # solo la primera vez y cuando cambie package.json
npm run dev
```

Abre **http://localhost:5173**. Vite reenvía todo lo que empieza por `/api` al
backend (`http://localhost:8080`), así que para el navegador la web y la API
están en la misma dirección y la cookie de sesión funciona sin más.

> Si el backend está en otro puerto, copia `frontend/.env.example` como
> `frontend/.env.local` y cambia `VITE_API_PROXY`.

## 4. Qué probar

### 4.1 Zona pública (sin iniciar sesión)

1. **Inicio:** arriba sale el nombre del negocio ("Peluquería Demo"), que llega
   de `GET /api/v1/negocio`.
2. **Servicios:** seis tarjetas con duración ("1 h 30 min") y precio en euros.
3. **Profesionales:** elige "Tinte" en el filtro: solo queda quien lo realiza y
   aparece "Ver sus horas libres".
4. **Horarios libres:** elige servicio, profesional y día. Salen las horas
   libres en hora del negocio. Prueba un domingo (cerrado): sale "No hay horas
   libres" y un botón al siguiente día con horas. El selector de fecha no deja
   elegir días pasados ni más allá de 60 días. Copia la URL y ábrela en otra
   pestaña: aparece la misma búsqueda.

### 4.2 Registro (CU-001)

1. **Registrarse**, pulsa "Crear cuenta" sin rellenar nada: cada campo dice qué
   falta.
2. Pon una fecha de nacimiento de hace 10 años: "Debes tener 14 años o más".
3. Pon como contraseña `password123` (10 caracteres): el backend responde que es
   demasiado común y el error sale debajo del campo.
4. Rellénalo bien: te lleva al login con "Cuenta creada" y el correo ya puesto.
5. Repite con el mismo correo: "Ya existe una cuenta con ese correo" en el campo.

### 4.3 Login, sesión y cerrar sesión (CU-002, CU-003)

1. Entra con `cliente@demo.invalid` y tu `DEMO_PASSWORD`: llegas a "Mi área"
   (provisional; en el B8 será la zona del cliente).
2. **Recarga la página (F5):** sigues dentro. El access token solo estaba en
   memoria y se ha perdido, pero la web ha pedido otro con la cookie
   `refresh_token` (F12 → Aplicación → Cookies: verás que es `HttpOnly` y que
   JavaScript no puede leerla).
3. Abre la web en **dos pestañas** y recarga las dos a la vez: siguen dentro
   (la web hace un solo refresh a la vez; si no, el backend cerraría todas las
   sesiones por reutilizar un refresh token).
4. **Cerrar sesión:** vuelves al inicio y, al recargar, sigues fuera.
5. Prueba una contraseña mal 6 veces seguidas: "Demasiados intentos" (429).

### 4.4 Empleado con contraseña temporal (RN-005)

1. Como administrador (Swagger, apartado 7.8 de la guía del backend), da de
   alta un empleado y copia su `passwordTemporal`.
2. Entra en la web con ese empleado: te lleva a **Cambiar contraseña** y, si
   intentas ir a otra página, vuelve ahí.
3. Cámbiala: llegas a "Mi área" con "Contraseña cambiada correctamente".

### 4.5 Desde el móvil (RNF-008)

- **Rápido, en el PC:** F12 → icono del móvil (Ctrl+Shift+M) → elige un
  iPhone o un Pixel. El menú pasa a un botón ☰.
- **En tu móvil de verdad** (en la misma wifi que el PC):
  1. Para la web (Ctrl+C) y arráncala visible en la red: `npm run dev -- --host`.
  2. Vite muestra una dirección `Network: http://192.168.x.x:5173`. Ábrela en
     el móvil. Si no carga, Windows pedirá permiso para Node.js en el
     cortafuegos: permite "Redes privadas".
  3. La zona pública funciona igual. **El login no funciona desde el móvil en
     local**: la cookie de sesión es `Secure` y solo viaja por HTTPS (o a
     `localhost`). Con HTTPS en el despliegue (fase 7) funcionará. Para probar
     el login en tamaño móvil, usa el modo móvil de F12.

## 5. Pruebas y comprobaciones

En `frontend`:

```powershell
npm test            # Vitest + Testing Library + MSW (no necesita el backend)
npm run lint        # oxlint
npm run typecheck   # TypeScript estricto
npm run build       # versión de producción en frontend/dist
```

Es lo mismo que ejecuta la CI de GitHub en cada PR (job **Frontend**).

## 6. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| "No se puede conectar con el servidor" | El backend no está arrancado o no está en el puerto 8080. |
| La web arranca pero no hay servicios | La base de datos no tiene datos de demo: arranca el backend con el perfil `demo` (y la base vacía). |
| `Port 5173 is already in use` | Ya tienes otra web de Vite abierta: ciérrala o cierra la ventana de PowerShell que la tenía. |
| Tras recargar te saca de la sesión | Han pasado más de 7 días, has reiniciado la base de datos o has cambiado `JWT_SECRET`. Vuelve a entrar. |
| `npm install` falla con errores de red | Vuelve a intentarlo; si estás detrás de un proxy de empresa, configura `npm config set proxy`. |
