package pt.antares.app.core.util

/**
 * O resumo SHA-256 de uns bytes, em hexadecimal minúsculo.
 *
 * Existe para uma coisa só: comparar o catálogo que desceu com o que o manifesto diz que
 * devia ter descido. Não é segurança contra quem controle a ligação — para isso o que vale
 * é o TLS —, é a defesa contra o caso comum, que é uma descarga cortada a meio a parecer um
 * ficheiro inteiro.
 *
 * Fica em `expect` em vez de escrito à mão porque um resumo mal implementado falha **a
 * validar**: dá sempre diferente, ou pior, dá sempre igual. O Java já traz o algoritmo, e o
 * que não se escreve não se engana.
 */
expect fun sha256(bytes: ByteArray): String
