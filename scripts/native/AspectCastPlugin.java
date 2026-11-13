package __PKG__;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import androidx.mediarouter.app.MediaRouteChooserDialog;
import java.util.concurrent.Executors;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaSeekOptions;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

@CapacitorPlugin(name = "AspectCast")
public class AspectCastPlugin extends Plugin {
    private CastContext castContext;
    private RemoteMediaClient client;
    private boolean listening = false;

    private final RemoteMediaClient.Callback mediaCallback = new RemoteMediaClient.Callback() {
        @Override
        public void onStatusUpdated() {
            emitMedia();
        }
    };

    private final RemoteMediaClient.ProgressListener progressListener = new RemoteMediaClient.ProgressListener() {
        @Override
        public void onProgressUpdated(long progress, long duration) {
            emitMedia();
        }
    };

    private final SessionManagerListener<CastSession> sessionListener = new SessionManagerListener<CastSession>() {
        @Override
        public void onSessionStarting(CastSession s) {
        }

        @Override
        public void onSessionStarted(CastSession s, String id) {
            attach(s);
            emitSession("started", s);
        }

        @Override
        public void onSessionStartFailed(CastSession s, int error) {
            JSObject o = new JSObject();
            o.put("state", "failed");
            o.put("code", error);
            notifyListeners("castSession", o);
        }

        @Override
        public void onSessionEnding(CastSession s) {
        }

        @Override
        public void onSessionEnded(CastSession s, int error) {
            detach();
            JSObject o = new JSObject();
            o.put("state", "ended");
            notifyListeners("castSession", o);
        }

        @Override
        public void onSessionResuming(CastSession s, String id) {
        }

        @Override
        public void onSessionResumed(CastSession s, boolean wasSuspended) {
            attach(s);
            emitSession("resumed", s);
        }

        @Override
        public void onSessionResumeFailed(CastSession s, int error) {
        }

        @Override
        public void onSessionSuspended(CastSession s, int reason) {
        }
    };

    private void ui(Runnable r) {
        getActivity().runOnUiThread(r);
    }

    private String deviceName(CastSession s) {
        try {
            if (s != null && s.getCastDevice() != null) return s.getCastDevice().getFriendlyName();
        } catch (Exception e) {
        }
        return "";
    }

    private void emitSession(String state, CastSession s) {
        JSObject o = new JSObject();
        o.put("state", state);
        o.put("device", deviceName(s));
        notifyListeners("castSession", o);
    }

    private void attach(CastSession s) {
        detach();
        if (s == null) return;
        client = s.getRemoteMediaClient();
        if (client != null) {
            client.registerCallback(mediaCallback);
            client.addProgressListener(progressListener, 1000);
        }
    }

    private void detach() {
        if (client != null) {
            try {
                client.unregisterCallback(mediaCallback);
                client.removeProgressListener(progressListener);
            } catch (Exception e) {
            }
            client = null;
        }
    }

    private void emitMedia() {
        RemoteMediaClient c = client;
        if (c == null) return;
        MediaStatus ms = c.getMediaStatus();
        String state = "idle";
        String reason = "none";
        if (ms != null) {
            switch (ms.getPlayerState()) {
                case MediaStatus.PLAYER_STATE_PLAYING:
                    state = "playing";
                    break;
                case MediaStatus.PLAYER_STATE_PAUSED:
                    state = "paused";
                    break;
                case MediaStatus.PLAYER_STATE_BUFFERING:
                    state = "buffering";
                    break;
                case MediaStatus.PLAYER_STATE_LOADING:
                    state = "loading";
                    break;
                default:
                    state = "idle";
            }
            if (state.equals("idle")) {
                int r = ms.getIdleReason();
                if (r == MediaStatus.IDLE_REASON_ERROR) reason = "error";
                else if (r == MediaStatus.IDLE_REASON_FINISHED) reason = "finished";
                else if (r == MediaStatus.IDLE_REASON_CANCELED) reason = "canceled";
                else if (r == MediaStatus.IDLE_REASON_INTERRUPTED) reason = "interrupted";
            }
        }
        JSObject o = new JSObject();
        o.put("state", state);
        o.put("idleReason", reason);
        o.put("position", c.getApproximateStreamPosition());
        o.put("duration", c.getStreamDuration());
        notifyListeners("castMedia", o);
    }

    @PluginMethod
    public void init(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                if (castContext != null) {
                    JSObject r = new JSObject();
                    r.put("available", true);
                    call.resolve(r);
                    return;
                }
                try {
                    CastContext.getSharedInstance(getContext(), Executors.newSingleThreadExecutor())
                        .addOnSuccessListener(new OnSuccessListener<CastContext>() {
                            @Override
                            public void onSuccess(CastContext ctx) {
                                castContext = ctx;
                                if (!listening) {
                                    castContext.getSessionManager().addSessionManagerListener(sessionListener, CastSession.class);
                                    listening = true;
                                }
                                JSObject r = new JSObject();
                                r.put("available", true);
                                call.resolve(r);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                JSObject r = new JSObject();
                                r.put("available", false);
                                r.put("error", String.valueOf(e.getMessage()));
                                call.resolve(r);
                            }
                        });
                } catch (Exception e) {
                    JSObject r = new JSObject();
                    r.put("available", false);
                    r.put("error", String.valueOf(e.getMessage()));
                    call.resolve(r);
                }
            }
        });
    }

    @PluginMethod
    public void status(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                JSObject r = new JSObject();
                CastSession s = castContext == null ? null : castContext.getSessionManager().getCurrentCastSession();
                boolean ok = s != null && s.isConnected();
                r.put("connected", ok);
                r.put("device", ok ? deviceName(s) : "");
                if (ok) attach(s);
                call.resolve(r);
            }
        });
    }

    @PluginMethod
    public void pick(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                try {
                    if (castContext == null) {
                        call.reject("not-init");
                        return;
                    }
                    MediaRouteChooserDialog d = new MediaRouteChooserDialog(getActivity());
                    d.setRouteSelector(castContext.getMergedSelector());
                    d.show();
                    call.resolve();
                } catch (Exception e) {
                    call.reject("pick-failed: " + e.getMessage());
                }
            }
        });
    }

    @PluginMethod
    public void load(final PluginCall call) {
        final String url = call.getString("url");
        final String title = call.getString("title", "");
        final String type = call.getString("contentType", "video/mp4");
        final boolean live = Boolean.TRUE.equals(call.getBoolean("live", false));
        final long pos = call.getLong("position", 0L);
        ui(new Runnable() {
            @Override
            public void run() {
                try {
                    CastSession s = castContext == null ? null : castContext.getSessionManager().getCurrentCastSession();
                    if (s == null || !s.isConnected()) {
                        call.reject("no-session");
                        return;
                    }
                    attach(s);
                    RemoteMediaClient c = s.getRemoteMediaClient();
                    if (c == null) {
                        call.reject("no-client");
                        return;
                    }
                    MediaMetadata md = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
                    md.putString(MediaMetadata.KEY_TITLE, title);
                    MediaInfo info = new MediaInfo.Builder(url)
                        .setStreamType(live ? MediaInfo.STREAM_TYPE_LIVE : MediaInfo.STREAM_TYPE_BUFFERED)
                        .setContentType(type)
                        .setMetadata(md)
                        .build();
                    MediaLoadRequestData req = new MediaLoadRequestData.Builder()
                        .setMediaInfo(info)
                        .setAutoplay(true)
                        .setCurrentTime(pos)
                        .build();
                    c.load(req).setResultCallback(new com.google.android.gms.common.api.ResultCallback<RemoteMediaClient.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaClient.MediaChannelResult result) {
                            if (result.getStatus().isSuccess()) call.resolve();
                            else call.reject("load-failed " + result.getStatus().getStatusCode());
                        }
                    });
                } catch (Exception e) {
                    call.reject("load-error: " + e.getMessage());
                }
            }
        });
    }

    @PluginMethod
    public void play(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                if (client != null) client.play();
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void pause(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                if (client != null) client.pause();
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
                if (client != null) client.seek(new MediaSeekOptions.Builder().setPosition(pos).build());
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void stop(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client != null) client.stop();
                } catch (Exception e) {
                }
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void endSession(final PluginCall call) {
        ui(new Runnable() {
            @Override
            public void run() {
                try {
                    if (castContext != null) castContext.getSessionManager().endCurrentSession(true);
                } catch (Exception e) {
                }
                call.resolve();
            }
        });
    }

    @PluginMethod
    public void openVlc(final PluginCall call) {
        final String url = call.getString("url");
        final String title = call.getString("title", "");
        final long pos = call.getLong("position", 0L);
        ui(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndTypeAndNormalize(Uri.parse(url), "video/*");
                i.setPackage("org.videolan.vlc");
                i.putExtra("title", title);
                if (pos > 0) i.putExtra("position", pos);
                try {
                    getActivity().startActivity(i);
                    call.resolve();
                } catch (ActivityNotFoundException e) {
                    try {
                        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.videolan.vlc")));
                    } catch (Exception ignored) {
                    }
                    call.reject("vlc-missing");
                }
            }
        });
    }
}
