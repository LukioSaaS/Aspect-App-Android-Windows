# Aspect – app Android (celular e Android TV)

Este repositório gera o APK sozinho, pelo GitHub (sem Android Studio).

## Como gerar o APK
1. Suba todos os arquivos deste projeto para um repositório seu (inclusive a pasta `.github`).
2. Abra a aba **Actions**. Se pedir, clique em "I understand my workflows, go ahead and enable them".
3. Clique em **Gerar APK** e depois em **Run workflow**. Leva de 5 a 10 minutos.
4. Quando ficar verde, abra **Releases** (coluna da direita da página inicial) e baixe o `aspect.apk`.

Link fixo da versão mais nova:
`https://github.com/SEU-USUARIO/SEU-REPOSITORIO/releases/latest/download/aspect.apk`

## Instalar
- **Celular:** abra o link acima no navegador do celular, baixe e toque no arquivo. Se pedir, permita "instalar apps desconhecidos" para o navegador.
- **Android TV:** instale o app "Downloader" (loja da TV), digite o link acima e instale. Permita "fontes desconhecidas" para o Downloader quando a TV pedir.

## Atualizar o app
Troque o arquivo `www/index.html` pelo novo HTML e faça o commit. O APK é gerado de novo sozinho e as versões novas instalam por cima das antigas.

## Transmitir para a TV
No player do celular existe o botão de transmitir. Ele usa o Google Cast (Chromecast, Google TV, Android TV).
Se a TV não aceitar o arquivo, o app abre o mesmo título no VLC automaticamente (o VLC precisa estar instalado).

## Windows
O instalador do Windows é gerado pelo workflow **Gerar app Windows** (aba Actions › Run workflow).
Link fixo para baixar: `https://github.com/SEU-USUARIO/SEU-REPOSITORIO/releases/download/windows/Aspect-Setup.exe`
Como o instalador não é assinado, o Windows mostra "O Windows protegeu o computador": clique em "Mais informações" e depois em "Executar assim mesmo".

## MKV com várias faixas de áudio e legendas (Android)
O app usa o player nativo do Android (ExoPlayer) para os vídeos e para a TV ao vivo. Ele mostra as faixas de áudio e as legendas de texto (SRT/ASS) do arquivo em "Áudio e legendas".
Configurações › Reprodução › "Player do Android" permite usar o player nativo só em MKV/AVI ou desligá-lo.

## JOGOS HOJE com API (jogos ao vivo)
A linha JOGOS HOJE consulta uma API gratuita (ESPN, sem chave) a cada 60 s e mostra só os jogos de futebol que estão rolando agora, com escudos, placar e minuto. Cada jogo é ligado ao canal que o transmite: o app procura, na programação (EPG) e no nome dos canais, o título com os dois times. Tocar no cartão abre o canal.
Ajustes no topo do `www/index.html` (CONFIG): `GAMES_API` (liga/desliga), `GAMES_REFRESH` (segundos), `GAMES_LEAGUES` (campeonatos; vazio = lista padrão) e `GAMES_PER_GAME` (quantos canais por jogo).
Se a API falhar, a linha volta a funcionar só pela programação, como antes.
Os jogos achados só pela programação também mostram escudos (`GAMES_BADGES`): o app busca o escudo pelo nome do time e guarda no aparelho. Sem escudo, o cartão mostra a sigla do time.
Um jogo aparece uma vez só, no canal em HD (o servidor pode mandar o mesmo canal como SD, HD, FHD, ESPN 1, ESPN 2...). `GAMES_PER_GAME` muda isso (1 = só o melhor canal).
Os jogos de hoje que ainda vão começar também entram na linha, com o horário no lugar do placar e o canal onde vão passar (achado pela programação do canal). Jogos sem canal encontrado não aparecem.

## Modo TV
Na TV o app abre por padrão no estilo Clássico e com animações reduzidas. Dá para mudar em Configurações.

## Play Store
O passo a passo para publicar na Google Play (celular e Android TV) está no arquivo `PLAY-STORE.md`.
