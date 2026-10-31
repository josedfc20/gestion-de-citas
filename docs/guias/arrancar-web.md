# Guía · Arrancar y probar la web en tu PC

> Bloques B7, B8 y B9. Para Windows, con el entorno instalado (`instalacion-entorno.md`)
> y el backend funcionando (`arrancar-backend.md`). Los comandos van en
> **PowerShell**.

Al terminar habrás visto la web en el navegador del PC y del móvil, te habrás
registrado, habrás iniciado y cerrado sesión, y habrás consultado servicios,
profesionales y horas libres con los datos de demostración.

## 1. Traer el código

```powershell
cd C:\Users\josef\Downloads\Proyectos\Aplicaciones\gestion-de-citas
git fetch origin
git switch feature/web-cliente-perfil  # o, si ya está fusionado: git switch main; git pull
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

1. Entra con `cliente@demo.invalid` y tu `DEMO_PASSWORD`: llegas a "Mis
   citas"; el empleado llega a su **Agenda** y el admin, a **Reservas**.
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
3. Cámbiala: llegas a tu **Agenda** con "Contraseña cambiada correctamente".

### 4.5 Zona del cliente: reservar, mis citas y menores (B8a)

Si cambias de rama con la web en marcha, ejecuta antes `npm install` por si
hay dependencias nuevas.

1. **Reservar sin sesión:** cierra sesión, ve a **Horarios libres**, elige
   servicio, profesional y día, y pulsa una hora. Te pide iniciar sesión; entra
   con `cliente@demo.invalid` y vuelves a **Reservar cita** con esa hora.
2. **Reservar para ti:** revisa el resumen (la hora es la del negocio,
   Europe/Madrid), deja "Para mí" y pulsa **Confirmar reserva**: llegas al
   detalle con "Cita reservada". Vuelve a los horarios de ese día: esa hora ya
   no sale.
3. **Reservar para un menor:** en otra hora, pulsa "Añadir un menor a cargo",
   rellena sus datos (menor de 14 años) y guárdalo: queda elegido. Confirma.
   Prueba también una fecha de nacimiento de un adulto: el formulario lo avisa.
4. **Hora ocupada (409):** abre la misma hora en dos pestañas y confirma en
   las dos. La segunda dice "Ese horario ya no está disponible" y ofrece
   **Elegir otra hora**.
5. **Mis citas:** "Próximas" en orden, con el estado y "Para ..." si es de un
   menor. La pestaña **Historial** muestra las pasadas (con la demo puede estar
   vacía).
6. **Cancelar:** abre una cita de dentro de más de 24 h, pulsa **Cancelar
   cita**, escribe un motivo y confirma: pasa a "Cancelada" y la hora vuelve a
   estar libre en los horarios. Una cita de dentro de menos de 24 h dice "Ya
   no puedes cancelarla online".
7. **Menores a mi cargo** (menú): corrige los apellidos de un menor y luego
   elimínalo; si tenía citas próximas, en "Mis citas" salen canceladas.
8. Como **empleado o admin**, las horas de "Horarios libres" son solo
   etiquetas y `/reservar` dice "No tienes acceso a esta página".

### 4.6 Perfil, mis datos y recuperar la contraseña (B8b)

1. Como cliente, pulsa **Mi perfil (tu nombre)** arriba a la derecha. Cambia
   tu nombre y guarda: "Datos guardados" y la cabecera muestra el nombre nuevo.
   Pon el correo de otra cuenta (por ejemplo `ana@demo.invalid`): "Ese correo
   ya lo usa otra cuenta".
2. **Cambiar contraseña** desde el perfil: al guardar vuelves al perfil con
   "Contraseña cambiada correctamente".
3. **Descargar mis datos:** se descarga `mis-datos.json` con tus datos, tus
   citas y tus menores (sin contraseñas).
4. **Recuperar la contraseña:** cierra sesión, en el login pulsa "¿Has
   olvidado la contraseña?" y escribe `cliente@demo.invalid`. Abre Mailpit
   (http://localhost:8025), pulsa el enlace del correo: se abre "Elegir
   contraseña nueva" y el token desaparece de la barra de direcciones. Elige
   una contraseña y entra con ella. Si vuelves a usar el mismo enlace: "El
   enlace no es válido o ha caducado" con "Pedir otro enlace".
5. Prueba con un correo que no existe: el mensaje es el mismo (la web no
   revela qué correos tienen cuenta).
6. **Eliminar la cuenta** (hazlo con una cuenta de prueba que registres tú,
   no con la de la demo): en el perfil, "Quiero eliminar mi cuenta", escribe
   una contraseña mal (error debajo del campo) y luego la buena. Vuelves al
   inicio con "Tu cuenta se ha eliminado" y sin sesión; ya no puedes entrar
   con ese correo.
7. Como **empleado o admin**, el perfil no tiene "Eliminar mi cuenta".

### 4.7 Zona del empleado: agenda, citas y estados (B9a)

Entra con un empleado de la demo (`ana@demo.invalid` y tu `DEMO_PASSWORD`).
Antes, como cliente, reserva un par de citas con Ana (una para hoy si aún
quedan horas) para tener algo que ver.

1. **Agenda:** llegas a la semana actual, con hoy resaltado. Cada día muestra
   su horario ("09:00–14:00 · 16:00–20:00"), "No trabajas" o "Cerrado (día de
   cierre)" y sus citas con hora, servicio y cliente (o "Lucía (menor), a
   cargo de ..."). Prueba **Semana siguiente**, **Semana anterior** y **Hoy**.
   En el ordenador los siete días van en columnas; en el móvil (F12), uno
   debajo de otro.
2. **Mis citas** (menú): tarjetas con el cliente y su teléfono. Filtra por
   estado o por fechas: los filtros quedan en la dirección y "Atrás" los
   recupera. Un rango al revés da el aviso del servidor.
3. **Detalle de una cita futura:** el teléfono es un enlace para llamar
   (`tel:`); dice "Podrás marcarla como completada o no presentado a partir de
   las ..." y solo ofrece **Cancelar cita**. Pulsa confirmar sin motivo: "Indica
   el motivo de la cancelación". Con motivo, pasa a "Cancelada" y la hora
   vuelve a estar libre en los horarios.
4. **Detalle de una cita ya empezada** (la de hoy, cuando llegue su hora):
   **Marcar como completada** o **No se presentó**, con confirmación.
5. Entra como cliente en la cita cancelada por el empleado: ves el motivo.
6. **Mis servicios:** los que el admin le asignó, con duración y precio.
7. Como cliente, `/empleado/agenda` dice "No tienes acceso a esta página".

### 4.8 Reservas del administrador (B9b)

Entra con el administrador (`ADMIN_EMAIL` y `ADMIN_PASSWORD` de tu `.env`).

1. Llegas a **Reservas**: las de hoy en adelante, con el cliente, su teléfono
   y el estado. Filtra por estado, fechas y profesional; en "Filtrar por
   cliente", busca `cli` (3 letras o más) y elige al cliente de la demo: solo
   salen sus reservas. "Ver todos los clientes" quita el filtro.
2. **Detalle de una reserva:** además del teléfono, el correo del cliente y el
   **Historial**: quién la reservó ("por el cliente" o "por Administrador") y
   cada cambio de estado con quién lo hizo ("por Ana M."). Cancela una con
   motivo: aparece una línea nueva "Confirmada → Cancelada · por
   Administrador".
3. **Nueva reserva** (menú): te lleva a **Horarios libres**. Elige servicio,
   profesional y día y pulsa una hora: llegas a "Nueva reserva" con el
   resumen. Busca al cliente, elígelo y decide si es para él o para uno de sus
   menores. **Confirmar reserva** te lleva al detalle con "Reserva creada".
4. Busca un texto que no coincida con nadie: "antes debe registrarse" (el
   admin no crea clientes).
5. Abre la misma hora en dos pestañas y confirma en las dos: la segunda da
   "Ese horario ya no está disponible" con **Elegir otra hora**.
6. Entra como ese cliente: la cita reservada por el admin sale en "Mis
   citas".

### 4.9 Panel del administrador: servicios, empleados, horarios y negocio (B9c)

Como administrador, con el menú **Servicios**, **Empleados**, **Horarios** y
**Negocio**:

1. **Servicios:** la lista solo muestra los activos; marca "Ver también los
   desactivados". **Nuevo servicio:** la duración se elige de una lista con
   los múltiplos del intervalo (con 30 min: 30 min, 1 h, 1 h 30 min...). Pon
   el precio con coma (`12,50`). Crea uno con el nombre de otro: "Ya existe un
   servicio con ese nombre" debajo del nombre. **Desactívalo**: desaparece de
   la zona pública (`/servicios`) y las citas ya reservadas se mantienen.
2. **Empleados → Dar de alta:** rellena los datos y marca sus servicios. Al
   guardar se muestra **la contraseña temporal una sola vez** (botón
   "Copiar"). Entra con ella en otra ventana privada: te pide cambiarla.
3. **Ficha de un empleado:** corrige sus datos; quítale un servicio del que
   tenga citas futuras y verás el aviso. **Desactivar** a un empleado con
   citas futuras confirmadas da el error con "Ver sus citas confirmadas". En
   "Cuenta", **Generar otra contraseña temporal**.
4. **Su horario** (desde la ficha): cada día "Como el horario general" o
   "Horario propio". Pon el lunes como propio y quita sus tramos: "No trabaja
   este día". Al guardar, "Lo que trabaja cada semana" lo refleja y en
   "Horarios libres" ese lunes ya no tiene horas para él.
5. **Horarios → Horario general:** cambia una hora, quita o añade tramos (como
   mucho 4 por día). Pon un fin anterior al inicio: el error sale en ese día.
   Haz que dos tramos se toquen (14:00–16:00 y 16:00–20:00): el servidor lo
   rechaza nombrando el día. Si alguna cita confirmada queda fuera, se lista
   con enlace ("No se han cancelado: revísalas").
6. **Días de cierre:** añade uno de todo el negocio y otro de un empleado
   (con motivo). Repetir el mismo día da "Ya hay un cierre ese día"; un día
   pasado da su error. **Quitar** pide confirmación. En la agenda del
   empleado ese día sale "Cerrado (día de cierre)".
7. **Negocio:** cambia el nombre: la cabecera cambia al guardar. Cambiar el
   intervalo o la zona horaria pide confirmación. Con un servicio de 30 min,
   pon el intervalo en 60: el error sale debajo del intervalo (RN-011).
8. En el ordenador (1024 px o más) el menú del admin cabe en una línea; en el
   móvil va en el botón ☰.

### 4.10 Desde el móvil (RNF-008)

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
npm run test:cobertura  # lo mismo midiendo la cobertura (informe en frontend\coverage\index.html)
npm run lint        # oxlint
npm run typecheck   # TypeScript estricto
npm run build       # versión de producción en frontend/dist
```

Es lo mismo que ejecuta la CI de GitHub en cada PR (job **Frontend**, que
ejecuta las pruebas con cobertura).

## 6. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| "No se puede conectar con el servidor" | El backend no está arrancado o no está en el puerto 8080. |
| La web arranca pero no hay servicios | La base de datos no tiene datos de demo: arranca el backend con el perfil `demo` (y la base vacía). |
| `Port 5173 is already in use` | Ya tienes otra web de Vite abierta: ciérrala o cierra la ventana de PowerShell que la tenía. |
| Tras recargar te saca de la sesión | Han pasado más de 7 días, has reiniciado la base de datos o has cambiado `JWT_SECRET`. Vuelve a entrar. |
| `npm install` falla con errores de red | Vuelve a intentarlo; si estás detrás de un proxy de empresa, configura `npm config set proxy`. |
