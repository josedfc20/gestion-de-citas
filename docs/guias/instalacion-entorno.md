# Guía · Instalar el entorno y conectar GitHub

> Para Windows. Abre **PowerShell como administrador** (clic derecho en el menú
> Inicio → "Terminal (administrador)"). Copia cada comando, pégalo y pulsa Enter.
> Si un comando `winget` no encuentra el paquete, está el enlace de descarga
> manual al lado.

## 1. Git

```powershell
winget install --id Git.Git -e
```
Manual: https://git-scm.com/download/win (deja las opciones por defecto).

Cierra y vuelve a abrir la terminal (sin administrador) y configura tu identidad.
Usa el **mismo email que tu cuenta de GitHub**:

```powershell
git config --global user.name "Jose Flores"
git config --global user.email "TU_EMAIL_DE_GITHUB"
git config --global init.defaultBranch main
git config --global core.autocrlf true
```

`core.autocrlf true` evita líos de saltos de línea entre Windows y Linux (CI).

## 2. JDK 21 (Temurin) — conviviendo con tu Java 27

Tienes **OpenJDK 27**. No hace falta desinstalarlo, pero el proyecto usa
**Java 21** porque es LTS (soporte largo) y es la versión que Spring Boot 3,
Maven y las librerías dan por probada. Una versión no LTS tan nueva suele dar
errores raros de compilación con herramientas que aún no la conocen.

Instálalo con el instalador MSI, porque te deja marcar `JAVA_HOME`:

1. Ve a https://adoptium.net/temurin/releases/?version=21&os=windows
2. Descarga el **.msi** de **JDK 21, x64**.
3. En el instalador, en "Custom Setup", marca como "se instalará en el disco
   duro local": **Add to PATH**, **Set JAVA_HOME variable** y
   **JavaSoft (Oracle) registry keys**.

Cierra y abre la terminal y comprueba:

```powershell
java --version        # debe decir 21.x
echo $env:JAVA_HOME   # debe apuntar a ...\Eclipse Adoptium\jdk-21...
```

Si `java --version` sigue diciendo 27: busca en Windows "variables de entorno"
→ "Variables de entorno…" → en **Path** (de sistema), sube la línea de
`Eclipse Adoptium\jdk-21...\bin` por encima de la de Java 27. Lo importante para
el proyecto es `JAVA_HOME`, que es lo que usa `mvnw`.

En **IntelliJ IDEA**: File → Project Structure → SDK → elige el 21. Así cada
proyecto usa su versión aunque tengas varias.

## 3. Node.js LTS

```powershell
winget install --id OpenJS.NodeJS.LTS -e
```
Manual: https://nodejs.org (botón "LTS").

Nota: en la propuesta puse Node 22, pero hoy la LTS activa es **Node 24**
(la 22 está en mantenimiento). Te recomiendo instalar la que ofrezca el botón
LTS; para Vite y React no cambia nada.

Si `npm -v` da el error "la ejecución de scripts está deshabilitada", ejecuta
una sola vez (sin administrador) y responde **S**:

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

Cierra y abre la terminal y comprueba:

```powershell
node -v
npm -v
```

## 4. Docker Desktop

Docker en Windows necesita **WSL 2** (un Linux ligero dentro de Windows):

```powershell
wsl --install
```
**Reinicia el PC.** Después:

```powershell
winget install --id Docker.DockerDesktop -e
```
Manual: https://www.docker.com/products/docker-desktop/

Abre Docker Desktop, acepta los términos (uso personal gratuito) y espera a que
abajo a la izquierda diga "Engine running". Comprueba:

```powershell
docker --version
docker run hello-world   # debe imprimir "Hello from Docker!"
```

Si dice que la virtualización está desactivada, hay que activarla en la BIOS
(Intel VT-x / AMD-V). Avísame y lo vemos.

## 5. Comprobación final

Pega esto en una terminal normal y mándame la salida (✅ completado el 27/10/2026):

```powershell
git --version; java --version; echo $env:JAVA_HOME; node -v; npm -v; docker --version
```

## 6. Crear el repositorio en GitHub (lo haces tú, yo no puedo crearlo)

1. Entra en https://github.com/new
2. **Repository name:** `gestion-de-citas`
3. **Description:** `Aplicación de gestión de citas (Spring Boot + React)`
4. **Public**
5. **No marques** "Add a README", ni .gitignore, ni licencia: tiene que
   quedar **vacío** para que yo suba la estructura inicial.
6. Pulsa **Create repository** y mándame la URL.

Luego conecta GitHub con Claude en https://claude.ai/connect-github y dale acceso
a ese repositorio.

## 7. Lo que haré yo cuando el repo exista (con tu permiso)

Subiré el primer commit a `main` con la estructura aprobada en el paso 3.5:

```
gestion-de-citas/
├── AGENTS.md
├── README.md
├── .gitignore          ← ignora .env, target/, node_modules/, ~$*.docx, *.tmp…
├── .env.example
└── docs/
    ├── Doc.docx
    ├── ProjectRules.docx
    ├── diagramas/
    ├── propuestas/
    └── guias/
```

`backend/`, `frontend/`, `docker-compose.yml` y la CI llegan en la fase 4 con su
propia rama y PR. `Documentacion.docx` (el documento antiguo de ChatGPT) queda
fuera del repo; dime si lo quieres dentro.

## 8. Traerlo a tu PC (lo haces tú, una sola vez)

```powershell
cd C:\Users\josef\Downloads\Proyectos\Aplicaciones
git clone https://github.com/TU_USUARIO/gestion-de-citas.git
```

Se crea la carpeta `gestion-de-citas`. Desde ese momento **esa** es la carpeta
de trabajo; la actual `GestionDeCitas` queda como copia de seguridad.
Ábrela en IntelliJ con File → Open.
