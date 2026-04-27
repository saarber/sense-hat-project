package com.example.sensehatclient;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final String PREFS = "sense_hat_client_settings";
    private static final String DEFAULT_LABEL_A = "Sense HAT PI A - Living Room";
    private static final String DEFAULT_LABEL_B = "Sense HAT PI B - Studio";
        private static final String DEFAULT_URL_A = "https://sensors.`example`.com/sensehat-a";
    private static final String DEFAULT_URL_B = "https://sensors.example.com/sensehat-b";
    private static final int DEFAULT_REFRESH_MS = 600000;
    private static final int DEFAULT_TIMEOUT_MS = 6000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final SensorApiClient apiClient = new SensorApiClient();
    private final List<SourceCardView> cards = new ArrayList<>();

    private SharedPreferences prefs;
    private TextView globalStatus;
    private TextView refreshLabel;
    private Button refreshButton;
    private EditText labelAInput;
    private EditText urlAInput;
    private EditText labelBInput;
    private EditText urlBInput;
    private EditText refreshInput;
    private EditText timeoutInput;
    private int pendingRequests;
    private boolean isRefreshing;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            refreshAll();
            handler.postDelayed(this, getRefreshMs());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildContent());
        updateSettingsInputs();
        updateRefreshLabel();
        refreshAll();
        handler.postDelayed(pollRunnable, getRefreshMs());
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildContent() {
        FrameLayout screen = new FrameLayout(this);
        screen.addView(new PageBackground(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        screen.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(26));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root.addView(buildHeader());
        root.addView(buildIntro());
        root.addView(buildDashboardHeader());

        for (SensorSource source : getSources()) {
            SourceCardView card = new SourceCardView(source);
            cards.add(card);
            root.addView(card.container, matchWrapMargins(0, 0, 0, dp(14)));
        }

        root.addView(buildSettings());
        root.addView(buildInfoCards());
        root.addView(buildFooter());
        return screen;
    }

    private View buildHeader() {
        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setBackground(cardBackground(0.82f));
        header.setElevation(dp(2));

        TextView title = text("Project Name Placeholder", 16, getColorValue("text"), Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView link = text("Sensor data  |  Settings", 13, getColorValue("accent"), Typeface.NORMAL);
        link.setGravity(Gravity.END);
        header.addView(link);
        return header;
    }

    private View buildIntro() {
        LinearLayout panel = card();
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.addView(eyebrow("Raspberry Pi Sense HAT"));
        panel.addView(text("Open sensor data dashboard", 32, getColorValue("text"), Typeface.BOLD));
        TextView lead = text(
                "A native Android client for viewing live Raspberry Pi Sense HAT readings. It follows the same clean, neutral dashboard style as the web page and uses the same backend endpoints.",
                16,
                getColorValue("muted"),
                Typeface.NORMAL
        );
        lead.setPadding(0, dp(10), 0, dp(14));
        panel.addView(lead);

        LinearLayout actions = horizontal();
        actions.setGravity(Gravity.CENTER_VERTICAL);
        refreshButton = new Button(this);
        refreshButton.setText("Refresh now");
        refreshButton.setTextColor(Color.WHITE);
        refreshButton.setAllCaps(false);
        refreshButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        refreshButton.setBackground(buttonBackground());
        refreshButton.setOnClickListener(view -> refreshAll());
        actions.addView(refreshButton, new LinearLayout.LayoutParams(dp(148), dp(46)));

        globalStatus = text("Waiting for the first poll...", 14, getColorValue("muted"), Typeface.NORMAL);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        statusParams.setMargins(dp(12), 0, 0, 0);
        actions.addView(globalStatus, statusParams);
        panel.addView(actions);

        return panel;
    }

    private View buildDashboardHeader() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, dp(24), 0, dp(12));
        panel.addView(eyebrow("Live readings"));
        panel.addView(text("Sense HAT sensor data", 24, getColorValue("text"), Typeface.BOLD));
        TextView help = text("Values are refreshed from each configured API source. Open Settings below to change source labels, URLs, refresh interval, or timeout.", 14, getColorValue("muted"), Typeface.NORMAL);
        help.setPadding(0, dp(6), 0, dp(8));
        panel.addView(help);
        refreshLabel = text("", 14, getColorValue("muted"), Typeface.NORMAL);
        panel.addView(refreshLabel);
        return panel;
    }

    private View buildSettings() {
        LinearLayout panel = card();
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.addView(eyebrow("Configuration"));
        panel.addView(text("Project settings", 23, getColorValue("text"), Typeface.BOLD));

        TextView help = text("Customize the mobile client without changing source code. These values are saved on this device.", 14, getColorValue("muted"), Typeface.NORMAL);
        help.setPadding(0, dp(8), 0, dp(14));
        panel.addView(help);

        labelAInput = input("Sensor A label");
        urlAInput = input("Sensor A API base URL");
        labelBInput = input("Sensor B label");
        urlBInput = input("Sensor B API base URL");
        refreshInput = input("Refresh interval, seconds");
        timeoutInput = input("API timeout, seconds");
        refreshInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        timeoutInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        panel.addView(labeledInput("Sensor A label", labelAInput));
        panel.addView(labeledInput("Sensor A API base URL", urlAInput));
        panel.addView(labeledInput("Sensor B label", labelBInput));
        panel.addView(labeledInput("Sensor B API base URL", urlBInput));
        panel.addView(labeledInput("Refresh interval, seconds", refreshInput));
        panel.addView(labeledInput("API timeout, seconds", timeoutInput));

        LinearLayout actions = horizontal();
        actions.setPadding(0, dp(10), 0, 0);

        Button save = actionButton("Save settings");
        save.setOnClickListener(view -> saveSettings());
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(46), 1f));

        Button reset = actionButton("Reset");
        reset.setOnClickListener(view -> resetSettings());
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, dp(46), 0.7f);
        resetParams.setMargins(dp(10), 0, 0, 0);
        actions.addView(reset, resetParams);
        panel.addView(actions);

        return panel;
    }

    private View buildInfoCards() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, dp(14), 0, 0);

        LinearLayout apiCard = card();
        apiCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        apiCard.addView(eyebrow("API endpoints"));
        apiCard.addView(text("Expected sensor routes", 22, getColorValue("text"), Typeface.BOLD));
        apiCard.addView(text("Each configured source should expose /api/get_temperature, /api/get_humidity, /api/get_pressure, and /api/get_north.", 14, getColorValue("muted"), Typeface.NORMAL));
        wrapper.addView(apiCard, matchWrapMargins(0, 0, 0, dp(14)));

        LinearLayout reuseCard = card();
        reuseCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        reuseCard.addView(eyebrow("Open-source use"));
        reuseCard.addView(text("Reuse and adapt", 22, getColorValue("text"), Typeface.BOLD));
        reuseCard.addView(text("Replace placeholder project text, package name, app icon, and default API URLs before publishing your own build.", 14, getColorValue("muted"), Typeface.NORMAL));
        wrapper.addView(reuseCard);
        return wrapper;
    }

    private View buildFooter() {
        TextView footer = text("Open-source Sense HAT dashboard placeholder\nUpdate license and repository links before publishing.", 13, getColorValue("muted"), Typeface.NORMAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(12), dp(22), dp(12), 0);
        return footer;
    }

    private void refreshAll() {
        if (isRefreshing) return;
        isRefreshing = true;
        pendingRequests = cards.size();
        refreshButton.setEnabled(false);
        refreshButton.setText("Refreshing...");
        globalStatus.setText("Polling both Sense HAT sources. Each API call waits up to " + getTimeoutSeconds() + "s for a response.");

        List<SensorSource> sources = getSources();
        for (int i = 0; i < cards.size(); i++) {
            SourceCardView card = cards.get(i);
            SensorSource source = sources.get(i);
            card.setSource(source);
            card.setLoading(getTimeoutSeconds());
            executor.execute(() -> {
                try {
                    SensorReading reading = apiClient.fetchReading(source, getTimeoutMs());
                    runOnUiThread(() -> finishCard(card, reading, null));
                } catch (Exception error) {
                    runOnUiThread(() -> finishCard(card, null, error));
                }
            });
        }
    }

    private void finishCard(SourceCardView card, SensorReading reading, Exception error) {
        if (reading != null) {
            card.setOnline(reading);
        } else {
            card.setOffline(error == null ? "Unknown error" : error.getMessage());
        }

        pendingRequests -= 1;
        if (pendingRequests <= 0) {
            isRefreshing = false;
            refreshButton.setEnabled(true);
            refreshButton.setText("Refresh now");
            int online = 0;
            for (SourceCardView sourceCard : cards) {
                if (sourceCard.isOnline()) {
                    online += 1;
                }
            }
            if (online == cards.size()) {
                globalStatus.setText("Both sensor sources are online.");
            } else if (online == 0) {
                globalStatus.setText("No sensor source responded. Check API reachability or proxy settings.");
            } else {
                globalStatus.setText(online + " of " + cards.size() + " sensor sources responded.");
            }
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("label_a", valueOrDefault(labelAInput, DEFAULT_LABEL_A));
        editor.putString("url_a", valueOrDefault(urlAInput, DEFAULT_URL_A));
        editor.putString("label_b", valueOrDefault(labelBInput, DEFAULT_LABEL_B));
        editor.putString("url_b", valueOrDefault(urlBInput, DEFAULT_URL_B));
        editor.putInt("refresh_ms", Math.max(5, parseInt(refreshInput, DEFAULT_REFRESH_MS / 1000)) * 1000);
        editor.putInt("timeout_ms", Math.max(1, parseInt(timeoutInput, DEFAULT_TIMEOUT_MS / 1000)) * 1000);
        editor.apply();

        handler.removeCallbacks(pollRunnable);
        handler.postDelayed(pollRunnable, getRefreshMs());
        updateRefreshLabel();
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        refreshAll();
    }

    private void resetSettings() {
        prefs.edit().clear().apply();
        updateSettingsInputs();
        updateRefreshLabel();
        Toast.makeText(this, "Settings reset", Toast.LENGTH_SHORT).show();
        refreshAll();
    }

    private void updateSettingsInputs() {
        labelAInput.setText(prefs.getString("label_a", DEFAULT_LABEL_A));
        urlAInput.setText(prefs.getString("url_a", DEFAULT_URL_A));
        labelBInput.setText(prefs.getString("label_b", DEFAULT_LABEL_B));
        urlBInput.setText(prefs.getString("url_b", DEFAULT_URL_B));
        refreshInput.setText(String.valueOf(getRefreshMs() / 1000));
        timeoutInput.setText(String.valueOf(getTimeoutMs() / 1000));
    }

    private void updateRefreshLabel() {
        refreshLabel.setText("Auto-refresh every " + (getRefreshMs() / 1000) + "s - " + getTimeoutSeconds() + "s response timeout");
    }

    private List<SensorSource> getSources() {
        List<SensorSource> sources = new ArrayList<>();
        sources.add(new SensorSource(prefs.getString("label_a", DEFAULT_LABEL_A), prefs.getString("url_a", DEFAULT_URL_A)));
        sources.add(new SensorSource(prefs.getString("label_b", DEFAULT_LABEL_B), prefs.getString("url_b", DEFAULT_URL_B)));
        return sources;
    }

    private int getRefreshMs() {
        return prefs.getInt("refresh_ms", DEFAULT_REFRESH_MS);
    }

    private int getTimeoutMs() {
        return prefs.getInt("timeout_ms", DEFAULT_TIMEOUT_MS);
    }

    private int getTimeoutSeconds() {
        return Math.max(1, getTimeoutMs() / 1000);
    }

    private String valueOrDefault(EditText input, String fallback) {
        String value = input.getText().toString().trim();
        return value.isEmpty() ? fallback : value;
    }

    private int parseInt(EditText input, int fallback) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private LinearLayout labeledInput(String label, EditText input) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(7), 0, dp(7));
        box.addView(text(label, 13, getColorValue("muted"), Typeface.BOLD));
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        return box;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setTextColor(getColorValue("text"));
        input.setHintTextColor(getColorValue("muted"));
        input.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColorValue("surfaceMuted"));
        background.setStroke(dp(1), getColorValue("border"));
        background.setCornerRadius(dp(7));
        input.setBackground(background);
        return input;
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(buttonBackground());
        return button;
    }

    private TextView eyebrow(String value) {
        TextView view = text(value, 12, getColorValue("accentStrong"), Typeface.BOLD);
        view.setLetterSpacing(0.08f);
        view.setAllCaps(true);
        return view;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        text.setIncludeFontPadding(true);
        text.setLineSpacing(dp(1), 1.0f);
        return text;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(cardBackground(0.9f));
        layout.setElevation(dp(2));
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private GradientDrawable cardBackground(float alpha) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(applyAlpha(getColorValue("surface"), alpha));
        background.setStroke(dp(1), getColorValue("border"));
        background.setCornerRadius(dp(8));
        return background;
    }

    private GradientDrawable buttonBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColorValue("accent"));
        background.setCornerRadius(dp(7));
        return background;
    }

    private LinearLayout.LayoutParams matchWrapMargins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private int getColorValue(String name) {
        switch (name) {
            case "surface": return Color.rgb(255, 255, 255);
            case "surfaceMuted": return Color.rgb(240, 242, 244);
            case "text": return Color.rgb(31, 41, 51);
            case "muted": return Color.rgb(95, 107, 118);
            case "border": return Color.rgb(217, 222, 227);
            case "accent": return Color.rgb(37, 99, 235);
            case "accentStrong": return Color.rgb(29, 78, 216);
            case "ok": return Color.rgb(21, 128, 61);
            case "warn": return Color.rgb(180, 83, 9);
            case "bad": return Color.rgb(185, 28, 28);
            default: return Color.BLACK;
        }
    }

    private int applyAlpha(int color, float alpha) {
        return Color.argb(Math.round(255 * alpha), Color.red(color), Color.green(color), Color.blue(color));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class SourceCardView {
        final LinearLayout container;
        private final TextView sourceName;
        private final TextView sourceUrl;
        private final TextView statusText;
        private final View statusDot;
        private final TextView temperature;
        private final TextView humidity;
        private final TextView pressure;
        private final TextView north;
        private final TextView compassDegrees;
        private final TextView compassDirection;
        private final TextView lastUpdated;
        private final CompassView compass;
        private boolean online;

        SourceCardView(SensorSource source) {
            container = card();
            container.setPadding(dp(18), dp(18), dp(18), dp(18));

            LinearLayout header = horizontal();
            header.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout titleBlock = new LinearLayout(MainActivity.this);
            titleBlock.setOrientation(LinearLayout.VERTICAL);
            sourceName = text(source.label, 14, getColorValue("muted"), Typeface.NORMAL);
            TextView title = text("Sensor source", 21, getColorValue("text"), Typeface.BOLD);
            sourceUrl = text(source.baseUrl, 13, getColorValue("muted"), Typeface.NORMAL);
            titleBlock.addView(sourceName);
            titleBlock.addView(title);
            titleBlock.addView(sourceUrl);
            header.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout badge = horizontal();
            badge.setGravity(Gravity.CENTER_VERTICAL);
            badge.setPadding(dp(10), dp(6), dp(10), dp(6));
            badge.setBackground(pillBackground());
            statusDot = new View(MainActivity.this);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(getColorValue("warn"));
            statusDot.setBackground(dotBg);
            badge.addView(statusDot, new LinearLayout.LayoutParams(dp(8), dp(8)));
            statusText = text("Awaiting data...", 12, getColorValue("muted"), Typeface.NORMAL);
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            statusParams.setMargins(dp(8), 0, 0, 0);
            badge.addView(statusText, statusParams);
            header.addView(badge);
            container.addView(header);

            LinearLayout metrics = new LinearLayout(MainActivity.this);
            metrics.setOrientation(LinearLayout.VERTICAL);
            metrics.setPadding(0, dp(14), 0, 0);
            temperature = addMetric(metrics, "Temperature label placeholder", "Temperature from the Sense HAT sensor.");
            humidity = addMetric(metrics, "Humidity label placeholder", "Relative humidity from the Sense HAT sensor.");
            pressure = addMetric(metrics, "Pressure label placeholder", "Atmospheric pressure in hPa.");
            north = addMetric(metrics, "Heading label placeholder", "Compass heading in degrees.");
            container.addView(metrics);

            LinearLayout compassCard = card();
            compassCard.setPadding(dp(14), dp(14), dp(14), dp(14));
            compassCard.addView(eyebrow("Compass"));
            compass = new CompassView(MainActivity.this);
            compassCard.addView(compass, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
            LinearLayout readout = horizontal();
            readout.setGravity(Gravity.CENTER);
            compassDegrees = text("0.0 deg", 16, getColorValue("text"), Typeface.BOLD);
            compassDirection = text("", 16, getColorValue("muted"), Typeface.NORMAL);
            readout.addView(compassDegrees);
            LinearLayout.LayoutParams dirParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dirParams.setMargins(dp(12), 0, 0, 0);
            readout.addView(compassDirection, dirParams);
            compassCard.addView(readout);
            container.addView(compassCard, matchWrapMargins(0, dp(12), 0, 0));

            lastUpdated = text("Last update: -", 13, getColorValue("muted"), Typeface.NORMAL);
            lastUpdated.setPadding(0, dp(12), 0, 0);
            container.addView(lastUpdated);
        }

        void setSource(SensorSource source) {
            sourceName.setText(source.label);
            sourceUrl.setText(source.baseUrl);
        }

        void setLoading(int timeoutSeconds) {
            online = false;
            statusText.setText("Waiting up to " + timeoutSeconds + "s per API response...");
            setDotColor(getColorValue("warn"));
        }

        void setOnline(SensorReading reading) {
            online = true;
            temperature.setText(String.format(Locale.US, "%.1f C", reading.temperature));
            humidity.setText(String.format(Locale.US, "%.1f%%", reading.humidity));
            pressure.setText(String.format(Locale.US, "%.1f hPa", reading.pressure));
            north.setText(String.format(Locale.US, "%.1f deg", reading.north));
            compassDegrees.setText(String.format(Locale.US, "%.1f deg", reading.north));
            compassDirection.setText(SensorReading.direction(reading.north));
            compass.setDegrees((float) reading.north);
            statusText.setText("Live feed healthy");
            setDotColor(getColorValue("ok"));
            lastUpdated.setText("Last update: " + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date()));
        }

        void setOffline(String message) {
            online = false;
            temperature.setText("-");
            humidity.setText("-");
            pressure.setText("-");
            north.setText("-");
            compassDegrees.setText("0.0 deg");
            compassDirection.setText("Offline");
            compass.setDegrees(0f);
            statusText.setText("Source unavailable: " + message);
            setDotColor(getColorValue("bad"));
            lastUpdated.setText("Last update: " + DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date()));
        }

        boolean isOnline() {
            return online;
        }

        private TextView addMetric(LinearLayout parent, String label, String help) {
            LinearLayout metric = card();
            metric.setPadding(dp(14), dp(12), dp(14), dp(12));
            metric.addView(text(label, 12, getColorValue("muted"), Typeface.BOLD));
            TextView value = text("-", 26, getColorValue("text"), Typeface.BOLD);
            value.setPadding(0, dp(4), 0, dp(3));
            metric.addView(value);
            metric.addView(text(help, 13, getColorValue("muted"), Typeface.NORMAL));
            parent.addView(metric, matchWrapMargins(0, 0, 0, dp(10)));
            return value;
        }

        private GradientDrawable pillBackground() {
            GradientDrawable background = new GradientDrawable();
            background.setColor(getColorValue("surfaceMuted"));
            background.setStroke(dp(1), getColorValue("border"));
            background.setCornerRadius(dp(999));
            return background;
        }

        private void setDotColor(int color) {
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(color);
            statusDot.setBackground(dotBg);
        }
    }

    private final class PageBackground extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PageBackground() {
            super(MainActivity.this);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            paint.setShader(new LinearGradient(0, 0, width, height,
                    new int[]{Color.rgb(248, 251, 255), Color.rgb(245, 247, 251), Color.rgb(255, 248, 237)},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(null);
            paint.setColor(Color.argb(30, 37, 99, 235));
            canvas.drawCircle(width * 0.1f, height * 0.08f, dp(180), paint);
            paint.setColor(Color.argb(34, 20, 184, 166));
            canvas.drawCircle(width * 0.95f, height * 0.18f, dp(190), paint);
            paint.setColor(Color.argb(26, 245, 158, 11));
            canvas.drawCircle(width * 0.75f, height * 0.95f, dp(210), paint);
        }
    }

    private final class CompassView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float degrees;

        CompassView(Activity activity) {
            super(activity);
        }

        void setDegrees(float degrees) {
            this.degrees = (float) SensorReading.normalizeDegrees(degrees);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(getWidth(), getHeight());
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = size * 0.42f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(getColorValue("border"));
            canvas.drawCircle(cx, cy, radius, paint);
            canvas.drawCircle(cx, cy, radius * 0.68f, paint);

            paint.setColor(Color.rgb(154, 165, 177));
            for (int i = 0; i < 24; i++) {
                float angle = (float) Math.toRadians(i * 15 - 90);
                float start = i % 6 == 0 ? radius - dp(15) : radius - dp(10);
                float stroke = i % 6 == 0 ? dp(2) : dp(1);
                paint.setStrokeWidth(stroke);
                canvas.drawLine(
                        cx + (float) Math.cos(angle) * start,
                        cy + (float) Math.sin(angle) * start,
                        cx + (float) Math.cos(angle) * radius,
                        cy + (float) Math.sin(angle) * radius,
                        paint
                );
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(dp(13));
            paint.setColor(getColorValue("muted"));
            canvas.drawText("N", cx, cy - radius + dp(28), paint);
            canvas.drawText("S", cx, cy + radius - dp(18), paint);

            canvas.save();
            canvas.rotate(degrees, cx, cy);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(getColorValue("accent"));
            Path arrow = new Path();
            arrow.moveTo(cx, cy - radius * 0.62f);
            arrow.lineTo(cx - dp(9), cy);
            arrow.lineTo(cx + dp(9), cy);
            arrow.close();
            canvas.drawPath(arrow, paint);
            paint.setColor(Color.argb(70, 31, 41, 51));
            Path tail = new Path();
            tail.moveTo(cx, cy + radius * 0.42f);
            tail.lineTo(cx - dp(7), cy);
            tail.lineTo(cx + dp(7), cy);
            tail.close();
            canvas.drawPath(tail, paint);
            canvas.restore();

            paint.setColor(getColorValue("text"));
            canvas.drawCircle(cx, cy, dp(5), paint);
        }
    }
}
