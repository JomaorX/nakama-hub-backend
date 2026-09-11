package com.nakamahub.backend.repositories;

/**
 * Fragmento de consulta compartido para el bloqueo entre usuarios.
 *
 * Cada constante empieza por un salto de línea a propósito: los textos de bloque de
 * Java recortan el espacio al final de cada línea, así que concatenar un "... and "
 * con el fragmento produciría "andnot" y la consulta no compilaría.
 *
 * Se define una sola vez porque hay que aplicarlo en el feed, el timeline, la
 * búsqueda y los comentarios. Repetirlo a mano en cada sitio garantiza que tarde
 * o temprano alguno se quede sin actualizar y filtre contenido bloqueado.
 *
 * Comprueba los dos sentidos: da igual quién bloqueara a quién, el efecto es que
 * ninguno ve al otro. Con :viewerId a null ambas comparaciones dan desconocido,
 * que en SQL no es cierto, así que un anónimo no filtra nada.
 */
public final class BlockQueries {

    public static final String NOT_BLOCKED_WITH_POST_AUTHOR = "\n" + """
            not exists (select 1
                        from User blocker
                        join blocker.blockedUsers blocked
                        where (blocker.id = :viewerId and blocked.id = p.author.id)
                           or (blocker.id = p.author.id and blocked.id = :viewerId))
            """;

    public static final String NOT_BLOCKED_WITH_COMMENT_AUTHOR = "\n" + """
            not exists (select 1
                        from User blocker
                        join blocker.blockedUsers blocked
                        where (blocker.id = :viewerId and blocked.id = c.author.id)
                           or (blocker.id = c.author.id and blocked.id = :viewerId))
            """;

    private BlockQueries() {
    }
}
