# Preguntas de repaso — Roles y Autorización con Cognito

Estas 10 preguntas cubren lo que aprendiste en la guía `homework_roles.md`. Debes poder
responderlas explicando **tu propio código** y tu **User Pool**.

1. ¿Cuál es la diferencia entre **autenticación** y **autorización**? En el estacionamiento,
   ¿qué parte resuelve el token "con/sin" y qué parte resuelven los roles?

   La autenticación es el proceso de identificar quién es el usuario (verificar su identidad y que tenga credenciales válidas). La autorización determina a qué recursos y operaciones específicas tiene acceso un usuario ya autenticado (sus permisos).
   En el estacionamiento, el token (con/sin token) resuelve la autenticación: valida si es un usuario legítimo que ha iniciado sesión. Los roles resuelven la autorización: deciden si el usuario autenticado tiene permisos para registrar entradas/salidas de vehículos (rol USER) o si puede crear nuevos espacios de estacionamiento (rol ADMIN).

2. Cuando un usuario pertenece a un grupo en Cognito, ¿en qué **claim** del JWT viaja esa
   información y qué forma tiene (tipo de dato)? ¿Por qué usamos el **`access_token`** y no el `id_token`?

   Viaja en el claim `cognito:groups`. Tiene la forma de un arreglo de cadenas de texto (List<String>), por ejemplo: `["ADMIN"]`.
   Usamos el `access_token` porque está diseñado bajo el estándar OAuth2 específicamente para la autorización y control de acceso a APIs protegidas por un Resource Server. El `id_token`, en cambio, contiene información de perfil e identidad pensada para ser leída por la aplicación cliente (frontend). Además, Spring Security valida el `access_token` para mapear los recursos de forma segura.

3. En tu `JwtAuthenticationConverter`, ¿por qué antepones el prefijo **`ROLE_`** al nombre del grupo?
   ¿Qué pasaría si el grupo en Cognito se llamara `ROLE_ADMIN` en lugar de `ADMIN`?

   Anteponemos el prefijo `ROLE_` porque Spring Security busca por defecto autoridades que empiecen con dicho prefijo para validar roles mediante directivas como `hasRole("ADMIN")`.
   Si el grupo en Cognito ya se llamara `ROLE_ADMIN`, el convertidor generaría la autoridad `ROLE_ROLE_ADMIN`. Como resultado, las peticiones que busquen `hasRole("ADMIN")` (que busca `ROLE_ADMIN` internamente) fallarían con un código `403 Forbidden` al no coincidir el nombre de manera exacta.

4. Explica la diferencia entre `hasRole("ADMIN")` y `hasAuthority("ADMIN")`. ¿Con cuál funciona
   tu converter tal como está escrito y por qué?

   `hasRole("ADMIN")` valida que el usuario posea una autoridad con el prefijo `ROLE_`, buscando exactamente `ROLE_ADMIN` (agrega el prefijo automáticamente).
   `hasAuthority("ADMIN")` valida la autoridad usando la coincidencia exacta de texto tal como está de manera literal, es decir, buscaría exactamente `"ADMIN"` (sin agregar prefijos).
   Nuestro convertidor funciona con `hasRole("ADMIN")` porque el convertidor mapea explícitamente las autoridades concatenando el prefijo: `SimpleGrantedAuthority("ROLE_$it")`.

5. ¿Qué código HTTP responde el sistema a una petición **sin token** y cuál a un usuario
   **autenticado pero sin el rol correcto**? ¿Quién genera esas respuestas: tu `@RestControllerAdvice`
   o Spring Security? ¿Por qué?

   A una petición sin token le responde `401 Unauthorized`. A una petición con token pero sin el rol correcto le responde `403 Forbidden`.
   Estas respuestas las genera Spring Security (a través de sus puntos de entrada de seguridad como `BearerTokenAuthenticationEntryPoint` y `AccessDeniedHandler`). No pasan por `@RestControllerAdvice` porque la denegación de acceso ocurre en la cadena de filtros de seguridad (SecurityFilterChain) antes de que la petición llegue al DispatcherServlet y al controlador de tu aplicación.

6. Tu microservicio **no** tiene tabla de usuarios ni de roles. Entonces, ¿de dónde saca Spring
   la identidad y los permisos del usuario en cada petición? ¿Qué valida exactamente contra el `issuer-uri`?

   Spring extrae la identidad y permisos del JWT (Bearer Token) enviado en las cabeceras HTTP de cada petición.
   Contra el `issuer-uri` (el URI del emisor de Cognito), Spring valida el claim `iss` del token. Además, utiliza este URI para consultar la ruta de metadatos del pool (`/.well-known/openid-configuration`), descargar la clave pública (JWKS) de Cognito de forma automática y verificar la firma criptográfica del token para asegurarse de que no haya sido alterado.

7. En `SecurityConfig`, ¿por qué el orden de las reglas de `authorizeHttpRequests` importa? ¿Qué pasaría
   si `anyRequest().authenticated()` estuviera **antes** de las reglas específicas de `/parking-spaces`?

   Importa porque Spring Security evalúa las reglas en orden secuencial (de arriba hacia abajo) y aplica la primera regla que coincida con la ruta de la petición.
   Si `anyRequest().authenticated()` se colocara al inicio, coincidiría con absolutamente cualquier petición primero. Esto anularía las excepciones públicas (como `permitAll()` en `GET /parking-spaces/available`), por lo que todas las consultas públicas requerirían token y retornarían `401 Unauthorized`.

8. Describe, paso a paso y **desde la consola web de AWS**, cómo asignaste el rol `ADMIN` a un usuario.
   ¿En qué momento ese usuario "se convierte" en `ROLE_ADMIN` dentro de tu aplicación?

   Paso a paso en la consola de AWS:
   1. Entramos a la consola web de Amazon Cognito y abrimos el User Pool correspondiente.
   2. Fuimos a la pestaña Groups (Grupos) y creamos un grupo llamado `ADMIN`.
   3. Fuimos a la pestaña Users (Usuarios) y seleccionamos el usuario `admin_parking`.
   4. En la sección de membresía de grupos del usuario, hicimos clic en "Add user to group" y lo añadimos al grupo `ADMIN`.
   En el backend, el usuario se convierte en `ROLE_ADMIN` en el momento en que inicia sesión a través de la Hosted UI de Cognito, el cual genera un `access_token` que incluye en su payload `"cognito:groups": ["ADMIN"]`. Cuando el backend recibe la petición con el token y el `JwtAuthenticationConverter` procesa dicho claim, genera la autoridad `ROLE_ADMIN` en el contexto de seguridad actual.

9. Un compañero inicia sesión, obtiene su token y **lo agregas** al grupo `ADMIN` en Cognito.
   ¿Puede crear espacios inmediatamente con ese token, o necesita algo más? Justifica.

   No, no puede crear espacios inmediatamente; necesita volver a iniciar sesión para generar un nuevo token.
   Los tokens JWT son firmados y autocontenidos (stateless). Cognito graba la lista de grupos en el token al momento del login. Como el backend valida el token localmente analizando sus claims firmados sin consultar en tiempo real a Cognito en cada request, el token antiguo sigue teniendo únicamente los claims previos al cambio.

10. Si quisieras un tercer rol (por ejemplo `SUPERVISOR`) que pueda **consultar todos los tickets**,
    ¿qué cambios harías en Cognito y qué cambios en `SecurityConfig`? ¿Tendrías que tocar el converter?

    En AWS Cognito: Crearías un nuevo grupo llamado `SUPERVISOR` y asignarías los usuarios correspondientes a él.
    En SecurityConfig: Añadirías la ruta correspondiente permitiendo el acceso a este nuevo rol, por ejemplo:
    `auth.requestMatchers(HttpMethod.GET, "/tickets/**").hasAnyRole("SUPERVISOR", "ADMIN")`
    ¿Tocar el converter?: No, no es necesario tocar el convertidor. La función `cognitoGroupsConverter()` extrae de forma dinámica cualquier valor dentro del arreglo `cognito:groups` y le añade el prefijo `ROLE_`. Por lo tanto, el nuevo grupo `SUPERVISOR` se convertirá de forma automática en la autoridad `ROLE_SUPERVISOR` en el backend.
