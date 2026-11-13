package __PKG__;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Player nativo (ExoPlayer) usado para MKV: mostra as faixas de áudio e as legendas do arquivo.
 * O vídeo aparece atrás da WebView (transparente) e a interface do Aspect continua por cima.
 */
@UnstableApi
@CapacitorPlugin(name = "AspectPlayer")
public class AspectPlayerPlugin extends Plugin {
    private ExoPlayer player;
    private PlayerView view;
    private boolean liveMode = false;
    private int retries = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (player == null) return;
            emitState();
            if (player.getPlaybackState() != Player.STATE_ENDED) handler.postDelayed(this, 500);
        }
    };

    private void ui(Runnable r) {
        getActivity().runOnUiThread(r);
    }

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int state) {
            emitState();
            if (state == Player.STATE_READY) {
                retries = 0;
                emitTracks();
            }
        }

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            emitState();
        }

        @Override
        public void onTracksChanged(Tracks tracks) {
            emitTracks();
        }

        @Override
        public void onCues(CueGroup cueGroup) {
            emitCue(cueGroup);
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            if (player == null) return;
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition();
                player.prepare();
                return;
            }
            boolean net = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                || error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                || error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                || error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED;
            if (liveMode && net && retries < 3) {
                retries++;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (player != null) {
                            player.seekToDefaultPosition();
                            player.prepare();
                        }
                    }
                }, 1500L * retries);
                return;
            }
            JSObject o = new JSObject();
            o.put("code", error.errorCode);
            o.put("message", String.valueOf(error.getMessage()));
            notifyListeners("exoError", o);
        }
    };

    private void emitState() {
        if (player == null) return;
        int s = player.getPlaybackState();
        JSObject o = new JSObject();
        o.put("state", s == Player.STATE_BUFFERING ? "buffering" : s == Player.STATE_READY ? "ready" : s == Player.STATE_ENDED ? "ended" : "idle");
        o.put("playing", player.getPlayWhenReady() && s != Player.STATE_ENDED);
        o.put("seekable", player.isCurrentMediaItemSeekable());
        o.put("position", player.getCurrentPosition());
        long d = player.getDuration();
        o.put("duration", d == C.TIME_UNSET ? 0 : d);
        notifyListeners("exoState", o);
    }

    private String describe(Format f, int type) {
        String m = f.sampleMimeType == null ? "" : f.sampleMimeType;
        String codec = "";
        if (type == C.TRACK_TYPE_AUDIO) {
            if (m.contains("eac3")) codec = "E-AC3";
            else if (m.contains("ac3")) codec = "AC3";
            else if (m.contains("dts")) codec = "DTS";
            else if (m.contains("true-hd") || m.contains("truehd")) codec = "TrueHD";
            else if (m.contains("mp4a") || m.contains("aac")) codec = "AAC";
            else if (m.contains("opus")) codec = "Opus";
            else if (m.contains("vorbis")) codec = "Vorbis";
            else if (m.contains("mpeg")) codec = "MP3";
            String ch = "";
            if (f.channelCount == 6) ch = "5.1";
            else if (f.channelCount == 8) ch = "7.1";
            else if (f.channelCount == 2) ch = "2.0";
            else if (f.channelCount > 0) ch = f.channelCount + "ch";
            return (codec + " " + ch).trim();
        }
        if (m.contains("subrip")) return "SRT";
        if (m.contains("ssa")) return "ASS";
        if (m.contains("vtt")) return "VTT";
        return "";
    }

    private boolean isBitmapSubtitle(Format f) {
        String m = f.sampleMimeType == null ? "" : f.sampleMimeType;
        return m.contains("pgs") || m.contains("dvbsubs") || m.contains("vobsub");
    }

    private void emitTracks() {
        if (player == null) return;
        JSArray audio = new JSArray();
        JSArray text = new JSArray();
        Tracks tracks = player.getCurrentTracks();
        for (int gi = 0; gi < tracks.getGroups().size(); gi++) {
            Tracks.Group g = tracks.getGroups().get(gi);
            int type = g.getType();
            if (type != C.TRACK_TYPE_AUDIO && type != C.TRACK_TYPE_TEXT) continue;
            for (int ti = 0; ti < g.length; ti++) {
                if (!g.isTrackSupported(ti)) continue;
                Format f = g.getTrackFormat(ti);
                if (type == C.TRACK_TYPE_TEXT && isBitmapSubtitle(f)) continue;
                JSObject t = new JSObject();
                t.put("id", gi + ":" + ti);
                t.put("lang", f.language == null ? "" : f.language);
                t.put("label", f.label == null ? "" : f.label);
                t.put("desc", describe(f, type));
                t.put("sel", g.isTrackSelected(ti));
                if (type == C.TRACK_TYPE_AUDIO) audio.put(t);
                else text.put(t);
            }
        }
        JSObject o = new JSObject();
        o.put("audio", audio);
        o.put("text", text);
        notifyListeners("exoTracks", o);
    }

    private void emitCue(CueGroup cg) {
        StringBuilder sb = new StringBuilder();
        for (Cue c : cg.cues) {
            if (c.text != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(c.text.toString());
            }
        }
        JSObject o = new JSObject();
        o.put("text", sb.toString());
        notifyListeners("exoCue", o);
    }

    private void select(int type, String id) {
        if (player == null) return;
        TrackSelectionParameters.Builder b = player.getTrackSelectionParameters().buildUpon();
        if (id == null || "off".equals(id)) {
            b.setTrackTypeDisabled(type, true);
        } else {
            try {
                String[] p = id.split(":");
                int gi = Integer.parseInt(p[0]);
                int ti = Integer.parseInt(p[1]);
                Tracks.Group g = player.getCurrentTracks().getGroups().get(gi);
                b.setTrackTypeDisabled(type, false);
                b.setOverrideForType(new TrackSelectionOverride(g.getMediaTrackGroup(), ti));
            } catch (Exception e) {
                return;
            }
        }
        player.setTrackSelectionParameters(b.build());
    }

    private void release() {
        handler.removeCallbacks(ticker);
        if (player != null) {
            try {
                player.removeListener(listener);
                player.release();
            } catch (Exception e) {
            }
            player = null;
        }
        if (view != null) {
            ViewGroup p = (ViewGroup) view.getParent();
            if (p != null) p.removeView(view);
            view = null;
        }
    }

    @PluginMethod
    public void open(final PluginCall call) {
        final String url = call.getString("url");
        final long pos = call.getLong("position", 0L);
        final float rate = call.getFloat("rate", 1f);
        final boolean live = Boolean.TRUE.equals(call.getBoolean("live", false));
        ui(new Runnable() {
            @Override
            public void run() {
                try {
                    release();
                    liveMode = live;
                    retries = 0;
                    DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(30000)
                        .setUserAgent("Mozilla/5.0 (Linux; Android) Aspect");
                    DefaultRenderersFactory rf = new DefaultRenderersFactory(getContext()).setEnableDecoderFallback(true);
                    // buffer menor e limitado em bytes: a TV não fica sem memória nem trava ao coletar lixo
                    DefaultLoadControl.Builder lc = new DefaultLoadControl.Builder();
                    if (live) lc.setBufferDurationsMs(15000, 30000, 2000, 4000);
                    else lc.setBufferDurationsMs(20000, 40000, 2500, 5000);
                    lc.setTargetBufferBytes(48 * 1024 * 1024).setPrioritizeTimeOverSizeThresholds(false);
                    player = new ExoPlayer.Builder(getContext(), rf)
                        .setLoadControl(lc.build())
                        .setMediaSourceFactory(new DefaultMediaSourceFactory(getContext()).setDataSourceFactory(http))
                        .setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
                        .setHandleAudioBecomingNoisy(true)
                        .build();
                    // a legenda é desenhada pelo próprio Aspect: o player só entrega o texto
                    player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build());
                    player.addListener(listener);

                    view = new PlayerView(getContext());
                    view.setUseController(false);
                    view.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);
                    view.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    view.setBackgroundColor(Color.BLACK);
                    view.setFocusable(false);
                    view.setFocusableInTouchMode(false);
                    if (view.getSubtitleView() != null) view.getSubtitleView().setVisibility(View.GONE);
                    view.setPlayer(player);

                    WebView wv = getBridge().getWebView();
                    ViewGroup parent = (ViewGroup) wv.getParent();
                    parent.addView(view, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    wv.setBackgroundColor(Color.TRANSPARENT);
                    wv.requestFocus();

                    player.setMediaItem(MediaItem.fromUri(url), pos);
                    player.setPlaybackParameters(new PlaybackParameters(rate));
                    player.prepare();
                    player.setPlayWhenReady(true);
                    handler.removeCallbacks(ticker);
                    handler.post(ticker);
                    call.resolve();
                } catch (Exception e) {
                    release();
                    call.reject("exo-open: " + e.getMessage());
                }
            }
        });
    }

    @PluginMethod
    public void play(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                if (player != null) {
                    if (player.getPlaybackState() == Player.STATE_ENDED) player.seekTo(0);
                    player.setPlayWhenReady(true);
                }
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void pause(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                if (player != null) player.setPlayWhenReady(false);
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void seek(final PluginCall call) {
        final long pos = call.getLong("position", 0L);
        ui(new Runnable() {
            @Override
            public void run() {
                if (player != null) player.seekTo(pos);
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void rate(final PluginCall call) {
        final float r = call.getFloat("rate", 1f);
        ui(new Runnable() {
            @Override
            public void run() {
                if (player != null) player.setPlaybackParameters(new PlaybackParameters(r));
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void selectAudio(final PluginCall call) {
        final String id = call.getString("id");
        ui(new Runnable() {
            @Override
            public void run() {
                select(C.TRACK_TYPE_AUDIO, id);
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void selectText(final PluginCall call) {
        final String id = call.getString("id");
        ui(new Runnable() {
            @Override
            public void run() {
                select(C.TRACK_TYPE_TEXT, id);
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void close(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                release();
                call.resolve();
            }
        });
    }

    @Override
    protected void handleOnPause() {
        if (player != null) player.setPlayWhenReady(false);
    }

    @Override
    protected void handleOnDestroy() {
        release();
    }
}
