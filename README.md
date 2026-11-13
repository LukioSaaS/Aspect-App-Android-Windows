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
