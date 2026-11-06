# Diseño del esquema de AVISA

Referencia para el rediseño aprobado. Todas las claves son UUID y las tablas se crean en una única migración `V1`.

```
usuaris (rol: CLIENT | PROFESSIONAL | ADMIN)
 ├─< publicacions (autor_id; tipus: OFERTA | INCIDENCIA; estat)
 │     ├─< pressupostos (professional_id; solo INCIDENCIA)
 │     └─< tasques (client_id, professional_id; pressupost_id opcional y único)
 │             └─< sollicituds_cancellacio (sollicitant_id, responsable_id, admin_id)
 └─< avisos (destinatari_id, emissor_id)
blacklist_telefons (independiente; se consulta en registro y perfil)
```

## Estados

| Entidad | Estados | Transiciones |
|---|---|---|
| Oferta | `ACTIVA`, `RETIRADA` | Se retira en lugar de borrarse; se puede contratar varias veces. Al retirarla, sus solicitudes en `SOLLICITADA` pasan a `REBUTJADA` y se avisa a los clientes |
| Incidencia | `OBERTA`, `ASSIGNADA`, `TANCADA` | Aceptar un presupuesto la pasa a `ASSIGNADA`; finalizar la tarea, a `TANCADA`; cancelar la tarea, de nuevo a `OBERTA` |
| Presupuesto | `PENDENT`, `ACCEPTAT`, `REBUTJAT` | Al aceptar uno, los demás pendientes pasan a `REBUTJAT` con `rebutjat_automaticament` y se avisa a sus profesionales |
| Tarea | ver abajo | |

### Tarea

```
Oferta:      SOLLICITADA ──profesional acepta──> EN_CURS
             SOLLICITADA ──profesional rechaza─> REBUTJADA
             SOLLICITADA ──cliente retira──────> RETIRADA
Presupuesto: al aceptarlo el cliente, nace ya en EN_CURS

EN_CURS ──cliente, profesional o admin──> FINALITZADA
EN_CURS ──cliente o profesional, con motivo──> EN_REVISIO
EN_REVISIO ──admin: sin consecuencias o negligencia──> CANCELLADA
EN_REVISIO ──admin: rechazada──> EN_CURS
```

## Reglas de negocio

- **"M'interessa"** crea la tarea en `SOLLICITADA`. El contacto del profesional solo se muestra tras aceptar. El cliente puede retirar la solicitud sin pasar por el admin.
- **Finalizar:** cliente, profesional o admin, validando en el back quién llama y sin confirmación de la otra parte. No se puede finalizar en `EN_REVISIO`.
- **Valorar:** solo el cliente, también si finalizó otro. El back rechaza a cualquier otro.
- **Cancelar:** el cliente o el profesional la solicita con motivo sobre una tarea `EN_CURS`. Se avisa a la otra parte y a todos los admin.
- **Resolución:** el admin elige sin consecuencias, negligencia (indicando el responsable) o rechazada. El servicio exige que el responsable sea el cliente o el profesional de esa tarea.
- **Cancelar una tarea que viene de un presupuesto:** la incidencia vuelve a `OBERTA`, ese presupuesto queda `REBUTJAT` y los rechazados automáticamente al aceptarlo vuelven a `PENDENT`, avisando a sus profesionales. Los que el cliente rechazó a mano siguen rechazados, gracias a `rebutjat_automaticament`.
- **Negligencia:** sin consecuencias automáticas. Queda en el historial del usuario, visible solo para el admin, que decide si avisa o añade su teléfono a la lista negra.
- **Admin:** se protege por rol, no por un ID concreto.
- **Precio:** texto libre.
- **Presupuestos:** uno por profesional en cada incidencia.
- **Identidad:** las acciones sobre el ciclo de vida usan el usuario del token JWT verificado, nunca un dato del cuerpo ni una cabecera.

## Por qué `SOLLICITADA` y no reutilizar `interessos`

Una solicitud y su tarea son el mismo acuerdo en distintos momentos: mismo cliente, profesional, publicación y precio.

- **Una fila, un ciclo de vida.** Con `interessos` habría dos tablas con estado y habría que copiar los datos al aceptar, con riesgo de que se desincronicen.
- **Sin puente.** Evita la columna `tasca_id` que hoy une ambas tablas.
- **Duplicados imposibles.** Un índice único parcial impide dos solicitudes activas del mismo cliente sobre la misma oferta.
- **Historial sin esfuerzo.** Rechazadas y retiradas quedan en `tasques` sin tablas extra.

**Coste:** las consultas de trabajo en curso deben filtrar por estado, y `data_inici` admite nulos hasta que se acepta.

## Por qué una tabla aparte para las cancelaciones

Una solicitud rechazada devuelve la tarea a `EN_CURS`, y puede solicitarse de nuevo. Con columnas en `tasques` se perdería el historial, que es justo lo que el admin necesita para ver casos de negligencia.

## Datos iniciales

| Perfil | Datos |
|---|---|
| Sin perfil o `demo` solo | Solo el administrador, con `AVISA_ADMIN_PASSWORD` |
| `dev` | `pro/propro`, `cli/clicli`, `admin@admin.es/adminadmin` |
| `dev,demo` | Los tres anteriores, más las empresas y clientes de demo |

El perfil `demo` solo cuenta como producción (véase `docs/configuracio.md`).

Los seeders nunca sobrescriben: si el usuario ya existe, no lo tocan.

## Limitación conocida

Hasta completar la fase de autenticación (fase 2 del plan), `/api/users` sigue siendo público. La regla "el contacto solo tras aceptar" no quedará garantizada hasta entonces, aunque la interfaz la respete.
