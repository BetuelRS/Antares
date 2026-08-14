# Registo de decisões

Cada ficheiro aqui regista **uma decisão estruturante**: o que estava em cima da mesa, o que se
escolheu, e o que essa escolha custou.

Não são documentação de funcionalidades. São a resposta à pergunta que alguém faz seis meses
depois — *porque é que isto está assim?* — quando a razão já não é evidente e o código só mostra
o resultado.

Seguem o formato do [Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions.html):
**contexto**, **decisão**, **consequências**. Uma decisão nunca se apaga; se for substituída, o
ficheiro fica e passa a **Substituída**, com o apontador para a que a substituiu.

| | Decisão | Estado |
|---|---|---|
| [0001](0001-a-app-nao-sincroniza.md) | A app não sincroniza | Aceite |
| [0002](0002-lapides-e-indices-unicos.md) | Apagar é marcar, e o índice do dia conta as lápides | Aceite |
| [0003](0003-contas-em-funcoes-puras.md) | A aritmética vive em funções puras, longe do Android | Aceite |
| [0004](0004-versionamento-derivado-do-nome.md) | O `versionCode` deriva do `versionName` | Aceite |
| [0005](0005-documentacao-verificada-por-testes.md) | A documentação é verificada por testes | Aceite |
| [0006](0006-sem-minificacao.md) | A app compila sem R8 | Aceite |
