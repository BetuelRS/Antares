# O que fica e o que sai

Este documento existe para uma pessoa poder **verificar** as afirmações da app, em vez de
acreditar nelas.

## O que fica

Tudo o que registas: peso, medidas, comida, água, treinos, corridas, jejuns, fotos de progresso e
ciclo. Vive na base de dados do Android, dentro da app, e nas preferências. As fotos ficam em
ficheiros da própria app.

Não há conta com os teus dados, não há servidor com uma cópia, e não há sincronização — ver
[a decisão](decisoes/0001-a-app-nao-sincroniza.md).

O caminho de volta, se perderes o telemóvel, é o **backup**: um ficheiro com tudo, fotos de
progresso incluídas. Desde a 2.1.0 a app escreve-o sozinha, de três em três dias, para a pasta
«Documentos/Antares» do telemóvel, e guarda as cinco últimas. Continua a poder ser exportado à
mão para onde quiseres. Nada disto sai do aparelho: a cópia automática da Google foi desligada
nessa mesma versão.

## O que sai

Cinco destinos. Os dois últimos não são pedidos que faças de propósito, e por isso são os que mais
importam.

### 1. A pesquisa de alimentos, na Open Food Facts

Quando procuras um produto ou lês um código de barras, o texto ou o código vão à
`world.openfoodfacts.org`. O que lá chega é a tua procura — não quem és, nem o que registaste.

A Open Food Facts exige que quem chama se identifique no `User-Agent`, com nome e contacto, sob
pena de bloquear.

### 2. A análise por foto e por texto

Se descreveres uma refeição ou lhe tirares uma fotografia, esse texto ou essa imagem vão para uma
Edge Function, que a envia ao modelo da Anthropic e devolve os valores. **A imagem não é
guardada.**

Fica guardado, do lado do servidor:

- uma linha por pedido, com o identificador anónimo, a função chamada, se correu bem, o escalão, e
  o **endereço IP em resumo criptográfico** — nunca o IP em claro;
- os valores nutricionais que a análise apurou, numa cache por chave. É informação sobre
  alimentos, não sobre pessoas.

### 3. O pedido de apagamento

O ecrã de privacidade chama a função `delete-account`, que apaga do servidor tudo o que lhe esteja
associado, e a própria conta anónima.

### 4. O mapa das corridas

Enquanto corres com o mapa à vista, a app descarrega os quadrados do mapa de
`tiles.openfreemap.org`. **Cada pedido diz a esse serviço, pelo teu endereço IP, que zona do mundo
estás a ver** — e durante uma corrida, a zona que estás a ver é onde estás.

O percurso em si nunca é enviado: fica na base de dados do telemóvel. Mas os quadrados que a app
vai buscar denunciam a área. É a troca que qualquer mapa que não venha dentro da app obriga a
fazer, e está aqui dita porque não é evidente.

Endereço em
`composeApp/src/androidMain/kotlin/pt/antares/app/feature/running/ui/RunMap.android.kt`.

### 5. As imagens dos exercícios

O catálogo vem dentro da app, mas as **imagens** não: são descarregadas de
`raw.githubusercontent.com` à medida que abres cada exercício. Quem serve os ficheiros vê o
endereço IP e que exercício foi pedido.

Endereço base em
`composeApp/src/commonMain/kotlin/pt/antares/app/feature/workout/data/ExerciseSeeder.kt`.

## A conta anónima

Há uma conta, e convém ser claro sobre o que ela é: **cria-se sozinha, não tem registo,
palavra-passe nem e-mail**, e existe só para o servidor conseguir contar as utilizações da análise
por dispositivo. É a única coisa que liga este telemóvel ao servidor, e nenhum dado da app lhe
fica associado.

Sem `SUPABASE_URL` e `SUPABASE_ANON_KEY` configurados, a app nem sequer chega a criar conta
nenhuma.

## O Health Connect

A app lê e escreve no Health Connect, se lhe deres autorização no sistema. **Lê** peso, massa
gorda, massa magra, passos, exercício e calorias ativas; **escreve** nutrição, exercício, massa
gorda, massa magra e calorias ativas.

É uma troca dentro do teu telemóvel, entre a Antares e as outras apps que autorizaste. Nada disto
passa por servidor nenhum.

## O que a app pede ao sistema

| Permissão | Para quê |
|---|---|
| `INTERNET` | pesquisa, análise, mapa e imagens |
| `CAMERA` | ler códigos de barras e fotografar refeições |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | o percurso das corridas |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` | continuar a registar a corrida com o ecrã apagado |
| `POST_NOTIFICATIONS` | lembretes de refeição, de pesagem e o fim do descanso |
| `SCHEDULE_EXACT_ALARM` | o descanso entre séries acabar à hora certa |
| `VIBRATE` | o aviso do fim do descanso |
| `START_VIEW_PERMISSION_USAGE` | mostrar ao Health Connect o ecrã que explica para que servem as permissões de saúde |

São estas dez, e mais nenhuma. **`ACCESS_BACKGROUND_LOCATION` não está na lista**, e o
`ManifestPermissionsTest` falha se entrar: a app segue-te enquanto corres e com o ecrã apagado,
mas nunca quando não lhe pediste nada.

A localização só é pedida quando arrancas uma corrida, e a câmara quando abres o leitor.

## Verificar em vez de acreditar

```bash
# Todos os endereços que o código conhece
grep -rhoE 'https?://[^"]+' composeApp/src --include=*.kt | sort -u
```

Devolve os cinco destinos acima, e mais nada.

| Teste | O que prova |
|---|---|
| `NoSyncTest` | a biblioteca que sincronizaria não está no *build* |
| `AdaptiveTargetsOfflineTest` | o relatório semanal não chama a rede |
| `GdprTableParityTest` | nenhuma tabela de dados teus fica de fora da exportação |
| `ManifestPermissionsTest` | a localização em segundo plano continua fora do manifesto |

E, sem ler código nenhum: põe o telemóvel em modo de avião. Tudo funciona menos a pesquisa em
linha, a análise, o mapa e as imagens dos exercícios.
