# Emergency Meeting (Forge 1.20.1)

Mod para **Forge 1.20.1 (47.3.0 o más nuevo)**, compatible con servidores de Aternos. Hay que instalarlo en el servidor **y** en los clientes.

## Cómo obtener el .jar (GitHub)
1. Crea un repositorio nuevo en GitHub y sube **todo el contenido de esta carpeta** (incluida la carpeta oculta `.github`).
2. Entra a la pestaña **Actions**. Se compila solo en cada subida (también puedes darle "Run workflow").
3. Al terminar (unos 3-5 min), el `.jar` queda en **Releases → "Emergency Meeting (último build)"** y también como artefacto del workflow.
4. Aternos: en tu servidor elige Forge 1.20.1 y sube el `.jar` en la sección **Mods**.

## Qué trae
- **Botón de Emergencia** (bloque, botón grande rojo). Clic derecho = reunión:
  - Teletransporta alrededor del botón a **todos los jugadores en modo aventura** y los deja inmovilizados hasta que termina el temporizador.
  - Les aparece la animación **EMERGENCY MEETING** (rayos rojos de izquierda a derecha, alarma del color de quien lo presionó) con el audio.
  - Los jugadores en otros modos (creativo, espectador...) no participan.
- **Tablet de Administrador** (ítem): clic derecho (solo admins/OP) abre el menú para configurar la duración de cada reunión, si termina al votar todos y el enfriamiento del botón.
- **Laptop de Colores** (ítem): clic derecho para elegir entre 18 colores. Tu nombre (sobre la cabeza, chat y lista de jugadores) toma ese color.
- **Tablet en pantalla**: icono arriba a la derecha (cambia al color que elegiste). Se abre con la tecla **V** o con clic en el icono cuando tienes el cursor libre (chat, inventario, pausa). Botón **Quitar** para ocultarla; con **V** vuelve.
- **Votación** ("Who Is The Impostor?"): clic en un jugador y luego en la palomita verde, o SKIP VOTE. Gana el más votado (si empata o gana el skip, nadie sale). El expulsado pasa a **espectador**.

## Comandos (OP)
- `/meeting start` – inicia una reunión en tu posición
- `/meeting stop` – termina la reunión actual sin expulsar
- `/meeting time <segundos>` – duración de la reunión
- `/meeting color <jugador> <color|ninguno>` – asigna color

Todo está en la pestaña creativa **Reunión de Emergencia** (o con `/give @s emergencymeeting:emergency_button`, `emergencymeeting:admin_tablet`, `emergencymeeting:color_laptop`).

Nota: en un mundo de un solo jugador, el dueño del mundo puede usar la tablet de administrador sin trucos activados.
