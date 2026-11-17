# Como publicar o Aspect na Google Play (celular e Android TV)

Siga na ordem. O que é feito no GitHub está pronto; o que é feito na Play Console é com você.

## 1. Conta de desenvolvedor
1. Entre em https://play.google.com/console e crie a conta (taxa única de US$ 25).
2. Confirme a identidade quando pedir.
3. Conta **pessoal** criada depois de 13/11/2023: antes de lançar para todos, o app precisa de um **teste fechado com 12 testadores, inscritos por 14 dias seguidos**. Conta de **organização** não precisa (exige número D-U-N-S, gratuito, que pode levar semanas).

## 2. Criar a chave de assinatura (só uma vez)
1. No GitHub: **Actions → "Criar chave da Play Store (só uma vez)" → Run workflow**.
2. Quando terminar, baixe o arquivo `chave-play-aspect` (dentro do run, em *Artifacts*). Ele fica disponível por 2 dias. Dentro vêm `upload.jks` e `SENHA.txt`.
3. Envie o `upload.jks` para a pasta `signing/` do repositório (ficando `signing/upload.jks`).
4. Em **Settings → Secrets and variables → Actions → New repository secret**, crie o segredo `KEYSTORE_PASSWORD` com o conteúdo do `SENHA.txt`.
5. **Guarde uma cópia do `upload.jks` e da senha num lugar seguro** e apague o `SENHA.txt` do computador depois. Se perder a chave, só dá para recuperar pedindo ao Google um reset da chave de envio.

## 3. Política de privacidade (link público obrigatório)
1. Abra `docs/privacidade.html` e troque `[COLOQUE AQUI O SEU E-MAIL]` pelo seu e-mail.
2. Em **Settings → Pages**, escolha *Deploy from a branch*, branch `main`, pasta `/docs`.
3. O link ficará `https://SEU-USUARIO.github.io/NOME-DO-REPOSITORIO/privacidade.html`. Use esse link na Play Console.

## 4. Gerar o pacote (.aab)
1. **Actions → "Gerar app para a Play Store (.aab)" → Run workflow**.
2. Baixe o artefato `aspect-aab` (arquivo `aspect.aab`).
3. O workflow usa o Capacitor 8 e confere se o app mira o Android 16 (API 36), que a Play exige desde 31/08/2026. Se a conferência falhar, o log diz o motivo.
4. A cada envio novo, rode o workflow de novo: o número de versão sobe sozinho.

## 5. Criar o app na Play Console
- **Criar app**: nome *Aspect*, idioma português (Brasil), tipo *App*, gratuito.
- **Envio de testes**: crie a faixa de **teste fechado**, envie o `aspect.aab` e convide os 12 testadores (e-mails de contas Google).
- **Formato TV**: em *Configurações avançadas / Formatos*, adicione *Android TV*. O app já declara suporte a controle remoto e a TV.

## 6. Ficha da loja (já preparada na pasta `store/`)
| Item | Arquivo |
|---|---|
| Ícone 512×512 | `store/icone-512.png` |
| Imagem de destaque 1024×500 | `store/imagem-destaque-1024x500.png` |
| Banner de TV 1280×720 | `store/banner-tv-1280x720.png` |
| Capturas de tela | você tira (veja abaixo) |

**Capturas de tela:** pelo menos 2 do celular e, para TV, capturas 1920×1080 (use o emulador do Android Studio ou uma TV com captura de tela). Não mostre nomes de canais, filmes ou marcas de terceiros nas imagens.

**Descrição curta (até 80 caracteres):**
> Player para celular e Android TV com guia de programação, filmes e séries.

**Descrição completa:**
> O Aspect é um player de mídia para celular e Android TV, feito para você reproduzir o conteúdo do seu próprio serviço de forma simples e bonita.
>
> • Navegação pensada para o controle remoto da TV e para a tela de toque
> • Guia de programação (EPG), filmes e séries em abas separadas
> • Perfis, favoritos e "continuar assistindo"
> • Escolha de áudio e legendas
> • Transmissão para outra tela com Google Cast
> • Temas e opções de aparência
>
> IMPORTANTE: o Aspect não inclui, não vende e não fornece canais, filmes ou séries. Para usar, você precisa ter os dados de acesso de um serviço ao qual tenha direito, e usar apenas conteúdo que você tem direito de assistir.

Não use na ficha termos como "IPTV grátis", "canais liberados" ou nomes de canais, times ou filmes.

## 7. Declarações da Play Console
- **Acesso ao app:** como o app exige login, a Play pede um acesso de teste para quem for revisar. Informe os dados de um servidor de teste com conteúdo de livre uso. Sem isso, a revisão costuma reprovar.
- **Anúncios:** o app não tem anúncios.
- **Público-alvo:** 18 anos ou mais.
- **Classificação de conteúdo:** responda o questionário sobre o app em si, com sinceridade.
- **Segurança dos dados:** o app não envia dados para o desenvolvedor. O usuário e a senha vão só para o serviço que o próprio usuário configura, e há consultas a serviços de terceiros (lista na política de privacidade). Revise cada pergunta com calma e responda de acordo com isso.
- **Política de privacidade:** o link do passo 3.

## 8. Lançar
1. Depois dos 14 dias de teste fechado, em *Painel* clique em **Solicitar acesso à produção** e responda o questionário.
2. Quando liberarem, crie uma versão de **produção** e envie o `.aab` mais recente.
3. A análise costuma levar alguns dias.

## Observação sobre apps de IPTV
A Google Play aceita players, mas reprova ou derruba apps que dão acesso a conteúdo sem licença, o que pode levar à suspensão da conta. Mantenha o app apenas como player, sem lista ou servidor embutidos.
