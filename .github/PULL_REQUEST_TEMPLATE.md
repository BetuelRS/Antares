<!--
Se isto corrige uma issue, escreve "Fecha #123" no corpo.
Uma alteração por pull request: dois defeitos são dois PR.
-->

## O que muda, e porquê

<!-- O problema primeiro; a solução depois. -->

## Como verificaste

<!--
Que testes acrescentaste ou correste, e o que fizeste no telemóvel.
"Corri a suite" chega para alterações internas; para o que se vê, diz o que abriste e o que viste.
-->

## Antes de submeter

- [ ] `./gradlew build` passa
- [ ] `cd supabase && deno test --allow-env --allow-net functions/_shared/` passa, se mexeste no servidor
- [ ] Há um teste que falharia sem esta alteração
- [ ] Se um teste-guarda falhou, percebi que decisão ele defende antes de o mudar
- [ ] Comentários e nomes de teste em português, no estilo do código à volta
