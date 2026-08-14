# 0001 — A app não sincroniza

**Estado:** Aceite · **Desde:** 1.0.0

## Contexto

A app teve conta e sincronização. Havia entrada com o Google, os dados subiam para um servidor e
desciam noutro telemóvel, e havia uma tabela `sync_meta` a guardar o estado disso.

Deu mais problemas do que valia. A sincronização a meio de uma edição podia perder a edição; a
fusão de dois lados obrigava a perguntar ao utilizador qual queria; e cada tabela nova passava a
ter de saber subir e descer. Para uma app usada num telemóvel de cada vez, era complexidade paga
sem retorno.

## Decisão

**Tudo o que o utilizador regista vive no telemóvel dele, e mais lado nenhum.**

O `postgrest` saiu das dependências. A tabela `sync_meta` foi apagada na versão 21 do esquema. O
caminho de volta passou a ser um backup: um ficheiro que a pessoa exporta e guarda onde quiser.

Ficou de pé uma conta anónima — sem registo, sem palavra-passe, sem e-mail — usada só para o
servidor contar as utilizações da análise por foto e por texto. Nenhum dado da app lhe fica
associado.

## Consequências

**Bom:**

- Não há conflitos de fusão, nem estado de sincronização, nem edições perdidas a meio.
- A app funciona inteira sem rede, e isso é fácil de provar.
- A privacidade deixa de ser uma promessa e passa a ser uma propriedade: não há servidor onde os
  dados pudessem estar.

**Mau, e assumido:**

- **Quem perde o telemóvel sem ter feito backup, perde tudo.** Não há recuperação possível. Foi
  por isso que o backup passou a ter lugar próprio nas definições, em vez de estar escondido.
- Não há forma de usar a app em dois dispositivos ao mesmo tempo.

**Vestígios que confundem quem lê o código:**

- `supabase/migrations/` continua a criar tabelas por utilizador com RLS. Ficaram porque quem usou
  versões anteriores pode ter lá linhas, e o direito ao apagamento cobre-as na mesma — é o
  trabalho da função `delete-account`.
- A coluna `dirty` é escrita em toda a app e lida num sítio só.

Quem ler o SQL sem saber isto conclui que a app sincroniza. Não conclui.

## Como isto é defendido

O `NoSyncTest` falha se o `postgrest` voltar às dependências. O `AdaptiveTargetsOfflineTest` falha
se alguma chamada de rede entrar no cálculo do relatório semanal.
